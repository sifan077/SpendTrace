package com.sifan.spendtrace.data.backup

import androidx.room.withTransaction
import com.sifan.spendtrace.data.AppPreferences
import com.sifan.spendtrace.data.local.SpendTraceDatabase
import com.sifan.spendtrace.data.local.entity.CategoryEntity
import com.sifan.spendtrace.data.local.entity.ExpenseEntity
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** 备份过程中可以直接展示给用户的错误。 */
class BackupException(message: String) : Exception(message)

/** 备份文件里对用户有意义的信息，导入前用来让用户确认。 */
data class BackupPreview(
    val exportedAt: Instant,
    val appVersion: String?,
    val categoryCount: Int,
    val expenseCount: Int,
    val totalAmountMinor: Long,
)

/** 校验通过、尚未写入数据库的备份内容。 */
data class BackupContents(
    val preview: BackupPreview,
    val categories: List<CategoryEntity>,
    val expenses: List<ExpenseEntity>,
)

/**
 * 本地备份：把分类与支出记录导出成一份 JSON，导入时先完整校验再整体覆盖。
 *
 * 备份只包含纯文本数据，体积很小，因此直接使用 JSON 而不是 ZIP。
 * 导入过程全程在 Room 事务中完成，校验或写入失败都不会改动现有数据。
 */
class BackupService(
    private val database: SpendTraceDatabase,
    private val appVersion: String,
    private val preferences: AppPreferences,
) {

    /** 导出全部数据，返回格式化后的 JSON 文本。 */
    suspend fun export(now: Instant = Instant.now()): String = withContext(Dispatchers.Default) {
        val categories = database.categoryDao().getAll()
        val expenses = database.expenseDao().getAll()

        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("formatVersion", FORMAT_VERSION)
        root.put("appVersion", appVersion)
        root.put("exportedAt", DateTimeFormatter.ISO_INSTANT.format(now))
        root.put("categoryCount", categories.size)
        root.put("expenseCount", expenses.size)
        root.put("totalAmountMinor", expenses.sumOf { it.amountMinor })
        root.put("categories", JSONArray().apply {
            categories.forEach { category ->
                put(JSONObject().apply {
                    put("id", category.id)
                    put("name", category.name)
                    put("icon", category.icon)
                    put("color", category.color)
                    put("sortOrder", category.sortOrder)
                    put("isActive", category.isActive)
                })
            }
        })
        root.put("expenses", JSONArray().apply {
            expenses.forEach { expense ->
                put(JSONObject().apply {
                    put("id", expense.id)
                    put("amountMinor", expense.amountMinor)
                    put("categoryId", expense.categoryId)
                    put("note", expense.note ?: JSONObject.NULL)
                    put("spentAt", expense.spentAt)
                    put("createdAt", expense.createdAt)
                    put("updatedAt", expense.updatedAt)
                })
            }
        })
        root.toString(2)
    }

    /**
     * 完整校验备份文件，不修改任何数据。
     *
     * 任何一处不合法都会抛出 [BackupException]，由调用方提示用户。
     */
    suspend fun inspect(bytes: ByteArray): BackupContents = withContext(Dispatchers.Default) {
        if (bytes.isEmpty()) throw BackupException("备份文件为空。")
        if (bytes.size > MAX_BACKUP_BYTES) throw BackupException("备份文件过大，无法导入。")

        val text = String(bytes, Charsets.UTF_8).removePrefix("\uFEFF")
        val root = try {
            JSONObject(text)
        } catch (error: JSONException) {
            throw BackupException("备份文件不是有效的 JSON，可能已损坏。")
        }

        if (root.optString("format") != FORMAT) {
            throw BackupException("这不是花迹的备份文件，或文件格式已损坏。")
        }
        val formatVersion = root.optInt("formatVersion", -1)
        if (formatVersion < MIN_FORMAT_VERSION || formatVersion > FORMAT_VERSION) {
            throw BackupException("不支持的备份版本，请升级应用后重试。")
        }

        val exportedAt = readInstant(root, "exportedAt", "备份时间")
        val backupVersion = root.optString("appVersion").takeIf { it.isNotBlank() }

        val categories = readCategories(root)
        val categoryIds = categories.mapTo(HashSet()) { it.id }
        val expenses = readExpenses(root, categoryIds)

        BackupContents(
            preview = BackupPreview(
                exportedAt = exportedAt,
                appVersion = backupVersion,
                categoryCount = categories.size,
                expenseCount = expenses.size,
                totalAmountMinor = expenses.sumOf { it.amountMinor },
            ),
            categories = categories,
            expenses = expenses,
        )
    }

    /** 用备份内容整体替换本机数据；失败时事务回滚，现有数据保持不变。 */
    suspend fun restore(contents: BackupContents) {
        try {
            database.withTransaction {
                // 支出记录外键引用分类，必须先清空记录再清空分类
                database.expenseDao().deleteAll()
                database.categoryDao().deleteAll()
                database.categoryDao().insertAll(contents.categories)
                database.expenseDao().insertAll(contents.expenses)
            }
        } catch (error: Exception) {
            throw BackupException("写入本机数据库失败，现有数据未被改动。")
        }
        // 备份里的分类 id 可能与本机不同，清掉「上次使用的分类」避免选到不存在的分类
        preferences.clearLastCategoryId()
    }

    private fun readCategories(root: JSONObject): List<CategoryEntity> {
        val raw = root.optJSONArray("categories")
            ?: throw BackupException("备份缺少分类列表。")
        if (raw.length() == 0) throw BackupException("备份里没有任何分类，无法导入。")
        if (raw.length() > MAX_CATEGORIES) throw BackupException("备份中的分类数量超出限制。")
        if (root.optInt("categoryCount", raw.length()) != raw.length()) {
            throw BackupException("备份中的分类数量与清单不一致。")
        }

        val result = ArrayList<CategoryEntity>(raw.length())
        val ids = HashSet<Long>()
        for (index in 0 until raw.length()) {
            val item = raw.optJSONObject(index)
                ?: throw BackupException("第 ${index + 1} 个分类的数据格式错误。")
            val category = readCategory(item, index)
            if (!ids.add(category.id)) throw BackupException("备份中存在重复的分类 ID。")
            result += category
        }
        // 全部停用会让记账页没有可选分类，这种备份直接拒绝
        if (result.none { it.isActive }) throw BackupException("备份中没有可用的分类，无法导入。")
        return result
    }

    private fun readCategory(json: JSONObject, index: Int): CategoryEntity {
        val prefix = "第 ${index + 1} 个分类"
        val id = readPositiveLong(json, "id", "${prefix}的 ID")
        val name = readString(json, "name", "${prefix}的名称").trim()
        if (name.isEmpty() || name.length > MAX_NAME_LENGTH) {
            throw BackupException("${prefix}的名称无效。")
        }
        val icon = readString(json, "icon", "${prefix}的图标").trim()
        if (icon.isEmpty() || icon.length > MAX_ICON_LENGTH) {
            throw BackupException("${prefix}的图标标识无效。")
        }
        // 颜色统一按 32 位 ARGB 处理，兼容历史数据里写成有符号整数的值
        val color = readLong(json, "color", "${prefix}的颜色") and 0xFFFFFFFFL
        if (color == 0L) throw BackupException("${prefix}的颜色无效。")
        val sortOrder = readInt(json, "sortOrder", "${prefix}的排序")
        val isActive = readBoolean(json, "isActive", "${prefix}的启用状态")
        return CategoryEntity(
            id = id,
            name = name,
            icon = icon,
            color = color,
            sortOrder = sortOrder,
            isActive = isActive,
        )
    }

    private fun readExpenses(root: JSONObject, categoryIds: Set<Long>): List<ExpenseEntity> {
        val raw = root.optJSONArray("expenses")
            ?: throw BackupException("备份缺少记录列表。")
        if (raw.length() > MAX_EXPENSES) throw BackupException("备份中的记录数量超出限制。")
        if (root.optInt("expenseCount", raw.length()) != raw.length()) {
            throw BackupException("备份中的记录数量与清单不一致。")
        }

        val result = ArrayList<ExpenseEntity>(raw.length())
        val ids = HashSet<Long>()
        for (index in 0 until raw.length()) {
            val item = raw.optJSONObject(index)
                ?: throw BackupException("第 ${index + 1} 条记录的数据格式错误。")
            val expense = readExpense(item, index)
            if (expense.categoryId !in categoryIds) {
                throw BackupException("第 ${index + 1} 条记录关联的分类不存在。")
            }
            if (!ids.add(expense.id)) throw BackupException("备份中存在重复的记录 ID。")
            result += expense
        }
        return result
    }

    private fun readExpense(json: JSONObject, index: Int): ExpenseEntity {
        val prefix = "第 ${index + 1} 条记录"
        val id = readPositiveLong(json, "id", "${prefix}的 ID")
        val amountMinor = readPositiveLong(json, "amountMinor", "${prefix}的金额")
        if (amountMinor > MAX_AMOUNT_MINOR) throw BackupException("${prefix}的金额超出范围。")
        val categoryId = readPositiveLong(json, "categoryId", "${prefix}的分类")
        val spentAt = readPositiveLong(json, "spentAt", "${prefix}的消费时间")
        val createdAt = readPositiveLong(json, "createdAt", "${prefix}的创建时间")
        val updatedAt = readPositiveLong(json, "updatedAt", "${prefix}的修改时间")

        val note = if (json.isNull("note")) {
            null
        } else {
            readString(json, "note", "${prefix}的备注").trim().ifEmpty { null }?.also {
                if (it.length > MAX_NOTE_LENGTH) throw BackupException("${prefix}的备注过长。")
            }
        }

        return ExpenseEntity(
            id = id,
            amountMinor = amountMinor,
            categoryId = categoryId,
            note = note,
            spentAt = spentAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    private fun readString(json: JSONObject, key: String, label: String): String {
        val value = json.opt(key)
        if (value !is String) throw BackupException("${label}格式错误。")
        return value
    }

    private fun readLong(json: JSONObject, key: String, label: String): Long {
        val value = json.opt(key)
        if (value !is Number) throw BackupException("${label}格式错误。")
        return value.toLong()
    }

    private fun readPositiveLong(json: JSONObject, key: String, label: String): Long {
        val value = readLong(json, key, label)
        if (value <= 0L) throw BackupException("${label}必须是正整数。")
        return value
    }

    private fun readInt(json: JSONObject, key: String, label: String): Int {
        val value = readLong(json, key, label)
        if (value < Int.MIN_VALUE || value > Int.MAX_VALUE) {
            throw BackupException("${label}超出范围。")
        }
        return value.toInt()
    }

    private fun readBoolean(json: JSONObject, key: String, label: String): Boolean {
        val value = json.opt(key)
        return when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            else -> throw BackupException("${label}格式错误。")
        }
    }

    private fun readInstant(json: JSONObject, key: String, label: String): Instant {
        val value = readString(json, key, label)
        return try {
            Instant.parse(value)
        } catch (error: DateTimeParseException) {
            throw BackupException("${label}格式错误。")
        }
    }

    companion object {
        /** 备份文件标识，导入时用于识别文件类型。 */
        const val FORMAT = "spendtrace-backup"

        /** 当前备份格式版本；读取时兼容 [MIN_FORMAT_VERSION] 起的旧版本。 */
        const val FORMAT_VERSION = 1
        const val MIN_FORMAT_VERSION = 1

        const val MIME_TYPE = "application/json"
        const val FILE_EXTENSION = "json"

        /** 纯文本备份，16MB 已经远超正常数据量。 */
        const val MAX_BACKUP_BYTES = 16 * 1024 * 1024

        private const val MAX_CATEGORIES = 200
        private const val MAX_EXPENSES = 200_000
        private const val MAX_NAME_LENGTH = 40
        private const val MAX_ICON_LENGTH = 32
        private const val MAX_NOTE_LENGTH = 200

        /** 与输入框一致：整数部分最多 9 位。 */
        private const val MAX_AMOUNT_MINOR = 99_999_999_999L
    }
}

package com.sifan.spendtrace.data.repository

import com.sifan.spendtrace.core.DateFormats
import com.sifan.spendtrace.data.local.dao.ExpenseDao
import com.sifan.spendtrace.data.local.dao.ExpenseWithCategoryRow
import com.sifan.spendtrace.data.local.entity.ExpenseEntity
import com.sifan.spendtrace.data.model.Category
import com.sifan.spendtrace.data.model.Expense
import com.sifan.spendtrace.data.model.ExpenseDraft
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 支出数据的唯一入口，负责实体与领域模型的转换。 */
class ExpenseRepository(private val expenseDao: ExpenseDao) {

    fun observeMonth(month: YearMonth, zone: ZoneId = ZoneId.systemDefault()): Flow<List<Expense>> {
        val (start, end) = DateFormats.monthRange(month, zone)
        return expenseDao.observeBetween(start, end).map { rows -> rows.map { it.toExpense() } }
    }

    /**
     * 新增或更新一笔支出，返回记录 id。
     *
     * 更新时若记录已被删除会抛出 [IllegalStateException]，由上层提示用户。
     */
    suspend fun save(draft: ExpenseDraft, now: Instant = Instant.now()): Long {
        val nowMillis = now.toEpochMilli()
        val note = draft.note?.trim()?.takeIf { it.isNotEmpty() }
        val spentAt = draft.spentAt.toEpochMilli()

        val existingId = draft.id
        if (existingId == null) {
            return expenseDao.insert(
                ExpenseEntity(
                    amountMinor = draft.amountMinor,
                    categoryId = draft.categoryId,
                    note = note,
                    spentAt = spentAt,
                    createdAt = nowMillis,
                    updatedAt = nowMillis,
                )
            )
        }

        val existing = expenseDao.findById(existingId)
            ?: throw IllegalStateException("记录不存在，可能已被删除")
        expenseDao.update(
            existing.copy(
                amountMinor = draft.amountMinor,
                categoryId = draft.categoryId,
                note = note,
                spentAt = spentAt,
                updatedAt = nowMillis,
            )
        )
        return existingId
    }

    /** 全部记录数，用于设置页的数据概览。 */
    fun observeTotalCount(): Flow<Int> = expenseDao.observeCount()

    suspend fun delete(id: Long): Boolean = expenseDao.deleteById(id) > 0

    suspend fun find(id: Long): Expense? = expenseDao.findRowById(id)?.toExpense()
}

internal fun ExpenseWithCategoryRow.toExpense(): Expense = Expense(
    id = id,
    amountMinor = amountMinor,
    note = note,
    spentAt = Instant.ofEpochMilli(spentAt),
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    category = Category(
        id = categoryId,
        name = categoryName,
        icon = categoryIcon,
        color = categoryColor,
        sortOrder = categorySortOrder,
        isActive = categoryIsActive,
    ),
)

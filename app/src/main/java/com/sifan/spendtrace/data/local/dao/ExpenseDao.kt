package com.sifan.spendtrace.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sifan.spendtrace.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

/** 支出记录查询结果，包含所属分类的快照信息。 */
data class ExpenseWithCategoryRow(
    val id: Long,
    val amountMinor: Long,
    val categoryId: Long,
    val note: String?,
    val spentAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
    @ColumnInfo(name = "cat_name") val categoryName: String,
    @ColumnInfo(name = "cat_icon") val categoryIcon: String,
    @ColumnInfo(name = "cat_color") val categoryColor: Long,
    @ColumnInfo(name = "cat_sortOrder") val categorySortOrder: Int,
    @ColumnInfo(name = "cat_isActive") val categoryIsActive: Boolean,
)

private const val SELECT_WITH_CATEGORY = """
    SELECT e.id AS id,
           e.amountMinor AS amountMinor,
           e.categoryId AS categoryId,
           e.note AS note,
           e.spentAt AS spentAt,
           e.createdAt AS createdAt,
           e.updatedAt AS updatedAt,
           c.name AS cat_name,
           c.icon AS cat_icon,
           c.color AS cat_color,
           c.sortOrder AS cat_sortOrder,
           c.isActive AS cat_isActive
    FROM expenses e
    INNER JOIN categories c ON c.id = e.categoryId
"""

@Dao
interface ExpenseDao {

    /** 观察 [startMillis, endMillis) 区间内的支出记录，按消费时间倒序。 */
    @Query(
        "$SELECT_WITH_CATEGORY WHERE e.spentAt >= :startMillis AND e.spentAt < :endMillis " +
            "ORDER BY e.spentAt DESC, e.id DESC"
    )
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<ExpenseWithCategoryRow>>

    @Query("$SELECT_WITH_CATEGORY WHERE e.id = :id")
    suspend fun findRowById(id: Long): ExpenseWithCategoryRow?

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun findById(id: Long): ExpenseEntity?

    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    /** 导出用：全部记录，按消费时间正序。 */
    @Query("SELECT * FROM expenses ORDER BY spentAt ASC, id ASC")
    suspend fun getAll(): List<ExpenseEntity>

    @Insert
    suspend fun insertAll(expenses: List<ExpenseEntity>)

    /** 导入时清空现有记录，需与分类写入放在同一事务里。 */
    @Query("DELETE FROM expenses")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM expenses")
    fun observeCount(): Flow<Int>
}

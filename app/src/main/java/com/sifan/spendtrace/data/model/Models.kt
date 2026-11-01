package com.sifan.spendtrace.data.model

/** 支出分类。 */
data class Category(
    val id: Long,
    val name: String,
    val icon: String,
    val color: Long,
    val sortOrder: Int,
    val isActive: Boolean,
)

/** 一笔支出记录（已带上分类信息）。 */
data class Expense(
    val id: Long,
    val amountMinor: Long,
    val note: String?,
    val spentAt: java.time.Instant,
    val createdAt: java.time.Instant,
    val updatedAt: java.time.Instant,
    val category: Category,
)

/** 新增或编辑支出时提交的数据。 */
data class ExpenseDraft(
    val id: Long? = null,
    val amountMinor: Long,
    val categoryId: Long,
    val note: String?,
    val spentAt: java.time.Instant,
)

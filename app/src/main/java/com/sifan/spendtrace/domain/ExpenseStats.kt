package com.sifan.spendtrace.domain

import com.sifan.spendtrace.core.DateFormats
import com.sifan.spendtrace.data.model.Category
import com.sifan.spendtrace.data.model.Expense
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** 按天分组的支出，[expenses] 内按时间倒序。 */
data class ExpenseDayGroup(
    val date: LocalDate,
    val totalMinor: Long,
    val expenses: List<Expense>,
)

/** 分类汇总，[share] 为占当月总支出的比例（0f..1f）。 */
data class CategoryTotal(
    val category: Category,
    val amountMinor: Long,
    val share: Float,
)

/**
 * 月度统计的纯函数实现，便于单元测试。
 *
 * 所有统计都基于数据库中真实记录计算，不做任何模拟数据。
 */
object ExpenseStats {

    fun total(expenses: List<Expense>): Long = expenses.sumOf { it.amountMinor }

    fun todayTotal(
        expenses: List<Expense>,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long = expenses
        .filter { DateFormats.toLocalDate(it.spentAt, zone) == today }
        .sumOf { it.amountMinor }

    /**
     * 日均支出：当月总支出 / 天数。
     *
     * 查看当前月份时按「已过天数」计算，查看历史月份时按整月天数计算。
     */
    fun dailyAverage(totalMinor: Long, month: YearMonth, today: LocalDate): Long {
        if (totalMinor <= 0L) return 0L
        val days = if (month == YearMonth.from(today)) today.dayOfMonth else month.lengthOfMonth()
        if (days <= 0) return 0L
        return Math.round(totalMinor.toDouble() / days)
    }

    /** 按天倒序分组，天内的记录按消费时间倒序。 */
    fun groupByDay(
        expenses: List<Expense>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<ExpenseDayGroup> = expenses
        .groupBy { DateFormats.toLocalDate(it.spentAt, zone) }
        .map { (date, dayExpenses) ->
            val sorted = dayExpenses.sortedByDescending { it.spentAt }
            ExpenseDayGroup(
                date = date,
                totalMinor = sorted.sumOf { it.amountMinor },
                expenses = sorted,
            )
        }
        .sortedByDescending { it.date }

    /** 按分类汇总，金额从大到小排序，并计算占比。 */
    fun categoryTotals(expenses: List<Expense>): List<CategoryTotal> {
        val total = total(expenses)
        if (total <= 0L) return emptyList()
        return expenses
            .groupBy { it.category.id }
            .map { (_, categoryExpenses) ->
                val amount = categoryExpenses.sumOf { it.amountMinor }
                CategoryTotal(
                    category = categoryExpenses.first().category,
                    amountMinor = amount,
                    share = amount.toFloat() / total.toFloat(),
                )
            }
            .sortedWith(compareByDescending<CategoryTotal> { it.amountMinor }.thenBy { it.category.sortOrder })
    }
}

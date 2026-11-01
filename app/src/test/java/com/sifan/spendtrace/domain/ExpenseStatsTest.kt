package com.sifan.spendtrace.domain

import com.sifan.spendtrace.data.model.Category
import com.sifan.spendtrace.data.model.Expense
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseStatsTest {

    private val zone = ZoneId.of("Asia/Shanghai")

    private val food = Category(1L, "餐饮", "food", 0xFFE08D64, 0, true)
    private val transport = Category(2L, "交通", "transport", 0xFF6E9BC5, 1, true)

    private fun expense(
        id: Long,
        amountMinor: Long,
        category: Category = food,
        dateTime: LocalDateTime,
    ): Expense {
        val instant = dateTime.atZone(zone).toInstant()
        return Expense(
            id = id,
            amountMinor = amountMinor,
            note = null,
            spentAt = instant,
            createdAt = instant,
            updatedAt = instant,
            category = category,
        )
    }

    @Test
    fun `total 汇总当月全部记录`() {
        val expenses = listOf(
            expense(1L, 1500L, dateTime = LocalDateTime.of(2026, 10, 2, 12, 0)),
            expense(2L, 800L, dateTime = LocalDateTime.of(2026, 10, 3, 9, 30)),
        )
        assertEquals(2300L, ExpenseStats.total(expenses))
        assertEquals(0L, ExpenseStats.total(emptyList()))
    }

    @Test
    fun `todayTotal 只统计今天`() {
        val today = LocalDate.of(2026, 10, 3)
        val expenses = listOf(
            expense(1L, 1500L, dateTime = LocalDateTime.of(2026, 10, 2, 12, 0)),
            expense(2L, 800L, dateTime = LocalDateTime.of(2026, 10, 3, 9, 30)),
            expense(3L, 200L, dateTime = LocalDateTime.of(2026, 10, 3, 23, 59)),
        )
        assertEquals(1000L, ExpenseStats.todayTotal(expenses, today, zone))
    }

    @Test
    fun `dailyAverage 当前月份按已过天数计算`() {
        val month = YearMonth.of(2026, 10)
        val today = LocalDate.of(2026, 10, 10)
        // 1000 分 / 10 天
        assertEquals(100L, ExpenseStats.dailyAverage(1000L, month, today))
    }

    @Test
    fun `dailyAverage 历史月份按整月天数计算`() {
        val month = YearMonth.of(2026, 9)
        val today = LocalDate.of(2026, 10, 10)
        // 3000 分 / 30 天
        assertEquals(100L, ExpenseStats.dailyAverage(3000L, month, today))
    }

    @Test
    fun `dailyAverage 无支出时为零`() {
        assertEquals(0L, ExpenseStats.dailyAverage(0L, YearMonth.of(2026, 10), LocalDate.of(2026, 10, 10)))
    }

    @Test
    fun `groupByDay 按天倒序分组并汇总当天金额`() {
        val expenses = listOf(
            expense(1L, 1500L, dateTime = LocalDateTime.of(2026, 10, 2, 12, 0)),
            expense(2L, 800L, dateTime = LocalDateTime.of(2026, 10, 2, 19, 0)),
            expense(3L, 200L, dateTime = LocalDateTime.of(2026, 10, 5, 8, 0)),
        )
        val groups = ExpenseStats.groupByDay(expenses, zone)

        assertEquals(2, groups.size)
        assertEquals(LocalDate.of(2026, 10, 5), groups[0].date)
        assertEquals(200L, groups[0].totalMinor)
        assertEquals(LocalDate.of(2026, 10, 2), groups[1].date)
        assertEquals(2300L, groups[1].totalMinor)
        // 同一天内按时间倒序
        assertEquals(listOf(2L, 1L), groups[1].expenses.map { it.id })
    }

    @Test
    fun `categoryTotals 按金额倒序并计算占比`() {
        val expenses = listOf(
            expense(1L, 1500L, food, LocalDateTime.of(2026, 10, 2, 12, 0)),
            expense(2L, 500L, transport, LocalDateTime.of(2026, 10, 2, 18, 0)),
            expense(3L, 1000L, food, LocalDateTime.of(2026, 10, 5, 8, 0)),
        )
        val totals = ExpenseStats.categoryTotals(expenses)

        assertEquals(2, totals.size)
        assertEquals(food.id, totals[0].category.id)
        assertEquals(2500L, totals[0].amountMinor)
        assertEquals(0.8333f, totals[0].share, 0.001f)
        assertEquals(transport.id, totals[1].category.id)
        assertEquals(0.1666f, totals[1].share, 0.001f)
    }

    @Test
    fun `categoryTotals 无数据时返回空列表`() {
        assertTrue(ExpenseStats.categoryTotals(emptyList()).isEmpty())
    }
}

package com.sifan.spendtrace.core

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 日期时间格式化与月份区间计算，统一使用设备默认时区。 */
object DateFormats {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val fullDateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日")
    private val shortDateFormatter = DateTimeFormatter.ofPattern("M月d日")
    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy年M月")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm")

    /** 备份文件名使用的时间戳，例如 20261101-1404。 */
    private val fileTimestampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")

    private val weekdays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    fun monthLabel(month: YearMonth): String = month.format(monthFormatter)

    fun fullDateLabel(date: LocalDate): String = date.format(fullDateFormatter)

    fun shortDateLabel(date: LocalDate): String = date.format(shortDateFormatter)

    fun weekdayLabel(date: LocalDate): String = weekdays[date.dayOfWeek.value - 1]

    /** 分组标题：今天 / 昨天 / 10月28日 周三。 */
    fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "今天"
        today.minusDays(1) -> "昨天"
        else -> "${shortDateLabel(date)} ${weekdayLabel(date)}"
    }

    fun timeLabel(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
        instant.atZone(zone).format(timeFormatter)

    /** 日期 + 时间，用于备份详情等场景。 */
    fun dateTimeLabel(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
        instant.atZone(zone).format(dateTimeFormatter)

    /** 文件名用的时间戳，例如 20261101-1404。 */
    fun fileTimestamp(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
        instant.atZone(zone).format(fileTimestampFormatter)

    fun toLocalDate(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        instant.atZone(zone).toLocalDate()

    fun toLocalTime(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): LocalTime =
        instant.atZone(zone).toLocalTime()

    fun toInstant(
        date: LocalDate,
        time: LocalTime,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Instant = date.atTime(time).atZone(zone).toInstant()

    /** 返回某个月的 [startMillis, endMillis) 区间，左闭右开，避免月末边界重复统计。 */
    fun monthRange(month: YearMonth, zone: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }
}

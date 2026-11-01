package com.sifan.spendtrace.core

/**
 * 金额工具。
 *
 * 数据库与业务层统一使用「分」为单位的 [Long] 保存金额，避免浮点误差；
 * 只在展示与输入时转换为字符串。
 */
object Money {

    /** 整数部分最多 9 位（即最大 999,999,999.99 元）。 */
    const val MAX_INTEGER_DIGITS = 9

    /** 小数部分最多 2 位。 */
    const val MAX_FRACTION_DIGITS = 2

    /**
     * 过滤用户输入：只保留数字与一个小数点，并限制整数位与小数位长度。
     * 用于 TextField 的 onValueChange。
     */
    fun sanitizeInput(raw: String): String {
        val digitsAndDot = raw.filter { it.isDigit() || it == '.' }
        val dotIndex = digitsAndDot.indexOf('.')
        if (dotIndex < 0) {
            return digitsAndDot.take(MAX_INTEGER_DIGITS)
        }
        val intPart = digitsAndDot.substring(0, dotIndex).take(MAX_INTEGER_DIGITS)
        val fractionPart = digitsAndDot.substring(dotIndex + 1)
            .filter { it.isDigit() }
            .take(MAX_FRACTION_DIGITS)
        return "$intPart.$fractionPart"
    }

    /**
     * 将输入文本解析为「分」。
     *
     * 仅接受大于 0、最多两位小数的金额，非法输入返回 null。
     */
    fun parseToMinor(text: String): Long? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        val parts = trimmed.split('.')
        if (parts.size > 2) return null

        val intPart = parts[0]
        val fractionPart = if (parts.size == 2) parts[1] else ""

        if (intPart.any { !it.isDigit() }) return null
        if (fractionPart.any { !it.isDigit() }) return null
        if (intPart.length > MAX_INTEGER_DIGITS) return null
        if (fractionPart.length > MAX_FRACTION_DIGITS) return null
        if (intPart.isEmpty() && fractionPart.isEmpty()) return null

        val yuan = if (intPart.isEmpty()) 0L else intPart.toLongOrNull() ?: return null
        val cents = when (fractionPart.length) {
            0 -> 0L
            1 -> (fractionPart.toLongOrNull() ?: return null) * 10
            else -> fractionPart.toLongOrNull() ?: return null
        }

        val minor = yuan * 100 + cents
        return if (minor > 0L) minor else null
    }

    /** 带千分位的展示格式，例如 1,234.50。 */
    fun format(minor: Long): String {
        val negative = minor < 0L
        val abs = if (negative) -minor else minor
        val yuan = abs / 100
        val cents = abs % 100
        val grouped = yuan.toString().reversed().chunked(3).joinToString(",").reversed()
        return buildString {
            if (negative) append('-')
            append(grouped)
            append('.')
            append(cents.toString().padStart(2, '0'))
        }
    }

    /** 带货币符号的展示格式，例如 ¥1,234.50。 */
    fun formatWithSymbol(minor: Long): String = "¥${format(minor)}"

    /** 不带千分位的格式，用于输入框回填，例如 1234.50。 */
    fun formatPlain(minor: Long): String {
        val negative = minor < 0L
        val abs = if (negative) -minor else minor
        val sign = if (negative) "-" else ""
        return sign + (abs / 100) + "." + (abs % 100).toString().padStart(2, '0')
    }
}

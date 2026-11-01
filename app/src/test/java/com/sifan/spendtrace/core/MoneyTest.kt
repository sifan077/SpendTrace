package com.sifan.spendtrace.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun `parseToMinor 解析合法金额`() {
        assertEquals(1234L, Money.parseToMinor("12.34"))
        assertEquals(1200L, Money.parseToMinor("12"))
        assertEquals(1250L, Money.parseToMinor("12.5"))
        assertEquals(5L, Money.parseToMinor("0.05"))
        assertEquals(1L, Money.parseToMinor(".01"))
        assertEquals(99999999999L, Money.parseToMinor("999999999.99"))
    }

    @Test
    fun `parseToMinor 拒绝非法金额`() {
        assertNull(Money.parseToMinor(""))
        assertNull(Money.parseToMinor("   "))
        assertNull(Money.parseToMinor("0"))
        assertNull(Money.parseToMinor("0.00"))
        assertNull(Money.parseToMinor("-1"))
        assertNull(Money.parseToMinor("1.234"))
        assertNull(Money.parseToMinor("abc"))
        assertNull(Money.parseToMinor("1.2.3"))
        assertNull(Money.parseToMinor("1000000000"))
    }

    @Test
    fun `sanitizeInput 只保留数字与一个小数点`() {
        assertEquals("12.34", Money.sanitizeInput("12.34"))
        assertEquals("12.34", Money.sanitizeInput("1a2.3b4"))
        assertEquals("12.34", Money.sanitizeInput("12.3456"))
        assertEquals("12.", Money.sanitizeInput("12."))
        assertEquals("12.34", Money.sanitizeInput("12.3.4"))
        assertEquals("123", Money.sanitizeInput("1,2,3"))
    }

    @Test
    fun `format 输出带千分位的金额`() {
        assertEquals("1,234.50", Money.format(123450))
        assertEquals("0.05", Money.format(5))
        assertEquals("0.00", Money.format(0))
        assertEquals("999,999,999.99", Money.format(99999999999))
        assertEquals("¥1,234.50", Money.formatWithSymbol(123450))
    }

    @Test
    fun `formatPlain 输出可回填输入框的金额`() {
        assertEquals("1234.50", Money.formatPlain(123450))
        assertEquals("0.05", Money.formatPlain(5))
    }
}

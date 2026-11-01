package com.sifan.spendtrace.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sifan.spendtrace.data.local.SpendTraceDatabase
import com.sifan.spendtrace.data.local.entity.CategoryEntity
import com.sifan.spendtrace.data.local.entity.ExpenseEntity
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** 支出表的增删改查测试（内存数据库）。 */
@RunWith(AndroidJUnit4::class)
class ExpenseDaoTest {

    private lateinit var database: SpendTraceDatabase

    private val zone: ZoneId = ZoneId.systemDefault()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SpendTraceDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun insertCategory(name: String = "餐饮"): Long =
        database.categoryDao().insert(
            CategoryEntity(name = name, icon = "food", color = 0xFFE08D64, sortOrder = 0)
        )

    private fun millisOf(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Long =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun insertExpenseAndQueryWithCategory() = runBlocking {
        val categoryId = insertCategory()
        val spentAt = millisOf(2026, 10, 5)

        val id = database.expenseDao().insert(
            ExpenseEntity(
                amountMinor = 1234L,
                categoryId = categoryId,
                note = "午饭",
                spentAt = spentAt,
                createdAt = spentAt,
                updatedAt = spentAt,
            )
        )

        val rows = database.expenseDao().observeBetween(spentAt - 1, spentAt + 1).first()
        assertEquals(1, rows.size)
        assertEquals(id, rows[0].id)
        assertEquals(1234L, rows[0].amountMinor)
        assertEquals("午饭", rows[0].note)
        assertEquals("餐饮", rows[0].categoryName)
    }

    @Test
    fun queryRangeOnlyReturnsExpensesInRange() = runBlocking {
        val categoryId = insertCategory()
        val october = millisOf(2026, 10, 5)
        val september = millisOf(2026, 9, 5)
        listOf(october, september).forEach { spentAt ->
            database.expenseDao().insert(
                ExpenseEntity(
                    amountMinor = 500L,
                    categoryId = categoryId,
                    note = null,
                    spentAt = spentAt,
                    createdAt = spentAt,
                    updatedAt = spentAt,
                )
            )
        }

        val start = millisOf(2026, 10, 1, 0, 0)
        val end = millisOf(2026, 11, 1, 0, 0)
        val rows = database.expenseDao().observeBetween(start, end).first()

        assertEquals(1, rows.size)
        assertEquals(october, rows[0].spentAt)
    }

    @Test
    fun updateExpenseChangesAmount() = runBlocking {
        val categoryId = insertCategory()
        val spentAt = millisOf(2026, 10, 5)
        val id = database.expenseDao().insert(
            ExpenseEntity(
                amountMinor = 1000L,
                categoryId = categoryId,
                note = null,
                spentAt = spentAt,
                createdAt = spentAt,
                updatedAt = spentAt,
            )
        )

        val saved = database.expenseDao().findById(id)
        assertNotNull(saved)
        database.expenseDao().update(saved!!.copy(amountMinor = 2000L, note = "改过"))

        val updated = database.expenseDao().findById(id)
        assertEquals(2000L, updated?.amountMinor)
        assertEquals("改过", updated?.note)
    }

    @Test
    fun deleteExpenseRemovesRecord() = runBlocking {
        val categoryId = insertCategory()
        val spentAt = millisOf(2026, 10, 5)
        val id = database.expenseDao().insert(
            ExpenseEntity(
                amountMinor = 1000L,
                categoryId = categoryId,
                note = null,
                spentAt = spentAt,
                createdAt = spentAt,
                updatedAt = spentAt,
            )
        )

        assertEquals(1, database.expenseDao().deleteById(id))
        assertEquals(0, database.expenseDao().count())
        assertNull(database.expenseDao().findById(id))
    }
}

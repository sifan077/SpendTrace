package com.sifan.spendtrace.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sifan.spendtrace.data.local.SeedDefaultCategoriesCallback
import com.sifan.spendtrace.data.local.SpendTraceDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** 首次创建数据库时应写入 7 个默认分类。 */
@RunWith(AndroidJUnit4::class)
class SpendTraceDatabaseTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val databaseName = "seed-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun defaultCategoriesAreSeededOnCreate() = runBlocking {
        context.deleteDatabase(databaseName)
        val database = Room.databaseBuilder(
            context,
            SpendTraceDatabase::class.java,
            databaseName,
        )
            .addCallback(SeedDefaultCategoriesCallback)
            .allowMainThreadQueries()
            .build()

        val categories = database.categoryDao().getAll()

        assertEquals(7, categories.size)
        assertEquals(
            listOf("餐饮", "交通", "购物", "日用", "娱乐", "医疗", "其他"),
            categories.map { it.name },
        )
        database.close()
    }
}

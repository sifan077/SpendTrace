package com.sifan.spendtrace.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sifan.spendtrace.data.local.dao.CategoryDao
import com.sifan.spendtrace.data.local.dao.ExpenseDao
import com.sifan.spendtrace.data.local.entity.CategoryEntity
import com.sifan.spendtrace.data.local.entity.ExpenseEntity

@Database(
    entities = [ExpenseEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SpendTraceDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao

    abstract fun categoryDao(): CategoryDao

    companion object {
        private const val DATABASE_NAME = "spendtrace.db"

        fun build(context: Context): SpendTraceDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                SpendTraceDatabase::class.java,
                DATABASE_NAME,
            )
                .addCallback(SeedDefaultCategoriesCallback)
                .build()
    }
}

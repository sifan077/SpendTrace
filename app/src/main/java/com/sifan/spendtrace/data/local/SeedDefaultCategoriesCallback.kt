package com.sifan.spendtrace.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库首次创建时写入默认分类。
 *
 * 使用 INSERT OR IGNORE 保证幂等：即使回调被重复执行也不会产生重复分类。
 */
internal object SeedDefaultCategoriesCallback : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        DefaultCategories.all.forEach { category ->
            db.execSQL(
                "INSERT OR IGNORE INTO categories (id, name, icon, color, sortOrder, isActive) " +
                    "VALUES (?, ?, ?, ?, ?, ?)",
                arrayOf<Any>(
                    category.id,
                    category.name,
                    category.icon,
                    category.color,
                    category.sortOrder,
                    if (category.isActive) 1 else 0,
                ),
            )
        }
    }
}

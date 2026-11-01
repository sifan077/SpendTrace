package com.sifan.spendtrace.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sifan.spendtrace.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY sortOrder ASC, id ASC")
    fun observeActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, id ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun findById(id: Long): CategoryEntity?

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    /** 导入时按备份中的 id 原样写回，保持记录与分类的对应关系。 */
    @Insert
    suspend fun insertAll(categories: List<CategoryEntity>)

    /** 导入时清空现有分类，需先清空引用它的支出记录。 */
    @Query("DELETE FROM categories")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}

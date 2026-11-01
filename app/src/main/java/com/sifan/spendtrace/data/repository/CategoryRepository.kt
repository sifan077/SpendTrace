package com.sifan.spendtrace.data.repository

import com.sifan.spendtrace.data.local.dao.CategoryDao
import com.sifan.spendtrace.data.local.entity.CategoryEntity
import com.sifan.spendtrace.data.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepository(private val categoryDao: CategoryDao) {

    fun observeActive(): Flow<List<Category>> =
        categoryDao.observeActive().map { entities -> entities.map { it.toCategory() } }

    suspend fun all(): List<Category> = categoryDao.getAll().map { it.toCategory() }
}

internal fun CategoryEntity.toCategory(): Category = Category(
    id = id,
    name = name,
    icon = icon,
    color = color,
    sortOrder = sortOrder,
    isActive = isActive,
)

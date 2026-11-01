package com.sifan.spendtrace.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 支出分类。
 *
 * [icon] 保存图标标识（对应项目内置矢量图），[color] 保存 ARGB 颜色值；
 * 保留 [isActive] 与 [sortOrder]，便于以后扩展自定义分类管理。
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val icon: String,
    val color: Long,
    val sortOrder: Int,
    val isActive: Boolean = true,
)

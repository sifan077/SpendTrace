package com.sifan.spendtrace.data.local

import com.sifan.spendtrace.data.local.entity.CategoryEntity

/**
 * 首次创建数据库时写入的默认分类。
 *
 * 分类主键固定为 1..7，方便「上次使用的分类」等历史数据在升级后仍能正确对应。
 */
object DefaultCategories {

    const val ICON_FOOD = "food"
    const val ICON_TRANSPORT = "transport"
    const val ICON_SHOPPING = "shopping"
    const val ICON_DAILY = "daily"
    const val ICON_ENTERTAINMENT = "entertainment"
    const val ICON_MEDICAL = "medical"
    const val ICON_OTHER = "other"

    val all: List<CategoryEntity> = listOf(
        CategoryEntity(id = 1L, name = "餐饮", icon = ICON_FOOD, color = 0xFFE08D64, sortOrder = 0),
        CategoryEntity(id = 2L, name = "交通", icon = ICON_TRANSPORT, color = 0xFF6E9BC5, sortOrder = 1),
        CategoryEntity(id = 3L, name = "购物", icon = ICON_SHOPPING, color = 0xFFD98BA6, sortOrder = 2),
        CategoryEntity(id = 4L, name = "日用", icon = ICON_DAILY, color = 0xFF8FA98B, sortOrder = 3),
        CategoryEntity(id = 5L, name = "娱乐", icon = ICON_ENTERTAINMENT, color = 0xFFA98FC9, sortOrder = 4),
        CategoryEntity(id = 6L, name = "医疗", icon = ICON_MEDICAL, color = 0xFFD98C8C, sortOrder = 5),
        CategoryEntity(id = 7L, name = "其他", icon = ICON_OTHER, color = 0xFFA9A29A, sortOrder = 6),
    )
}

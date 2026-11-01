package com.sifan.spendtrace.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一笔支出记录。
 *
 * [amountMinor] 为整数「分」；[spentAt] 为消费时间（epoch millis），
 * [createdAt] / [updatedAt] 记录写入与修改时间。
 */
@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("spentAt"), Index("categoryId")],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val amountMinor: Long,
    val categoryId: Long,
    val note: String?,
    val spentAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
)

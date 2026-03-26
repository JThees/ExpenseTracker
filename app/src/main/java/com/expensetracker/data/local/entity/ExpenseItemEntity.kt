package com.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expense_items",
    foreignKeys = [ForeignKey(
        entity = BatchEntity::class,
        parentColumns = ["id"],
        childColumns = ["batchId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("batchId"), Index("parentExpenseId")]
)
data class ExpenseItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val batchId: Long,
    val date: Long,
    val amount: Double,
    val category: String,
    val description: String = "",
    val notes: String = "",
    val receiptImageUri: String? = null,
    val isSplitItem: Boolean = false,
    val parentExpenseId: Long? = null
)

package com.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.expensetracker.data.local.entity.ExpenseItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseItemDao {
    @Query("SELECT * FROM expense_items WHERE batchId = :batchId ORDER BY date DESC")
    fun getItemsForBatch(batchId: Long): Flow<List<ExpenseItemEntity>>

    @Query("SELECT * FROM expense_items WHERE id = :id")
    suspend fun getById(id: Long): ExpenseItemEntity?

    @Query("SELECT * FROM expense_items WHERE parentExpenseId = :parentId")
    fun getSplitChildren(parentId: Long): Flow<List<ExpenseItemEntity>>

    @Insert
    suspend fun insert(item: ExpenseItemEntity): Long

    @Insert
    suspend fun insertAll(items: List<ExpenseItemEntity>)

    @Update
    suspend fun update(item: ExpenseItemEntity)

    @Delete
    suspend fun delete(item: ExpenseItemEntity)

    @Query("DELETE FROM expense_items WHERE parentExpenseId = :parentId")
    suspend fun deleteSplitChildren(parentId: Long)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expense_items WHERE batchId = :batchId")
    fun getTotalExpensesForBatch(batchId: Long): Flow<Double>
}

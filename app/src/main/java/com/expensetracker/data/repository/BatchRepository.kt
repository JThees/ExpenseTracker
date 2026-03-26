package com.expensetracker.data.repository

import com.expensetracker.data.local.dao.BatchDao
import com.expensetracker.data.local.dao.ExpenseItemDao
import com.expensetracker.data.local.dao.MileageEntryDao
import com.expensetracker.data.local.entity.BatchEntity
import com.expensetracker.data.local.entity.ExpenseItemEntity
import com.expensetracker.data.local.entity.MileageEntryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatchRepository @Inject constructor(
    private val batchDao: BatchDao,
    private val expenseItemDao: ExpenseItemDao,
    private val mileageEntryDao: MileageEntryDao
) {
    fun getAllBatches(): Flow<List<BatchEntity>> = batchDao.getAllBatches()

    fun getBatchById(id: Long): Flow<BatchEntity?> = batchDao.getBatchById(id)

    suspend fun getBatchByIdOnce(id: Long): BatchEntity? = batchDao.getBatchByIdOnce(id)

    suspend fun createBatch(title: String, notes: String = ""): Long {
        return batchDao.insert(
            BatchEntity(
                title = title,
                createdAt = System.currentTimeMillis(),
                notes = notes
            )
        )
    }

    suspend fun updateBatch(batch: BatchEntity) = batchDao.update(batch)

    suspend fun deleteBatch(batch: BatchEntity) = batchDao.delete(batch)

    suspend fun markBatchSubmitted(batchId: Long) {
        val batch = batchDao.getBatchByIdOnce(batchId) ?: return
        batchDao.update(batch.copy(isSubmitted = true, submittedAt = System.currentTimeMillis()))
    }

    // Expense items
    fun getExpenseItemsForBatch(batchId: Long): Flow<List<ExpenseItemEntity>> =
        expenseItemDao.getItemsForBatch(batchId)

    suspend fun getExpenseItemById(id: Long): ExpenseItemEntity? = expenseItemDao.getById(id)

    suspend fun addExpenseItem(item: ExpenseItemEntity): Long = expenseItemDao.insert(item)

    suspend fun addExpenseItems(items: List<ExpenseItemEntity>) = expenseItemDao.insertAll(items)

    suspend fun updateExpenseItem(item: ExpenseItemEntity) = expenseItemDao.update(item)

    suspend fun deleteExpenseItem(item: ExpenseItemEntity) {
        expenseItemDao.deleteSplitChildren(item.id)
        expenseItemDao.delete(item)
    }

    fun getTotalExpensesForBatch(batchId: Long): Flow<Double> =
        expenseItemDao.getTotalExpensesForBatch(batchId)

    fun getSplitChildren(parentId: Long): Flow<List<ExpenseItemEntity>> =
        expenseItemDao.getSplitChildren(parentId)

    // Mileage entries
    fun getMileageEntriesForBatch(batchId: Long): Flow<List<MileageEntryEntity>> =
        mileageEntryDao.getEntriesForBatch(batchId)

    suspend fun getMileageEntryById(id: Long): MileageEntryEntity? = mileageEntryDao.getById(id)

    suspend fun addMileageEntry(entry: MileageEntryEntity): Long = mileageEntryDao.insert(entry)

    suspend fun updateMileageEntry(entry: MileageEntryEntity) = mileageEntryDao.update(entry)

    suspend fun deleteMileageEntry(entry: MileageEntryEntity) = mileageEntryDao.delete(entry)

    fun getTotalMileageForBatch(batchId: Long): Flow<Double> =
        mileageEntryDao.getTotalMileageForBatch(batchId)
}

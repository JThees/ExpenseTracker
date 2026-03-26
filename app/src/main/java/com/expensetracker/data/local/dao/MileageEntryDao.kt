package com.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.expensetracker.data.local.entity.MileageEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MileageEntryDao {
    @Query("SELECT * FROM mileage_entries WHERE batchId = :batchId ORDER BY date DESC")
    fun getEntriesForBatch(batchId: Long): Flow<List<MileageEntryEntity>>

    @Query("SELECT * FROM mileage_entries WHERE id = :id")
    suspend fun getById(id: Long): MileageEntryEntity?

    @Insert
    suspend fun insert(entry: MileageEntryEntity): Long

    @Update
    suspend fun update(entry: MileageEntryEntity)

    @Delete
    suspend fun delete(entry: MileageEntryEntity)

    @Query("SELECT COALESCE(SUM(calculatedAmount), 0) FROM mileage_entries WHERE batchId = :batchId")
    fun getTotalMileageForBatch(batchId: Long): Flow<Double>
}

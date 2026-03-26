package com.expensetracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.expensetracker.data.local.dao.BatchDao
import com.expensetracker.data.local.dao.ExpenseItemDao
import com.expensetracker.data.local.dao.MileageEntryDao
import com.expensetracker.data.local.dao.SettingsDao
import com.expensetracker.data.local.entity.BatchEntity
import com.expensetracker.data.local.entity.ExpenseItemEntity
import com.expensetracker.data.local.entity.MileageEntryEntity
import com.expensetracker.data.local.entity.SettingsEntity

@Database(
    entities = [
        BatchEntity::class,
        ExpenseItemEntity::class,
        MileageEntryEntity::class,
        SettingsEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun batchDao(): BatchDao
    abstract fun expenseItemDao(): ExpenseItemDao
    abstract fun mileageEntryDao(): MileageEntryDao
    abstract fun settingsDao(): SettingsDao
}

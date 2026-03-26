package com.expensetracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun batchDao(): BatchDao
    abstract fun expenseItemDao(): ExpenseItemDao
    abstract fun mileageEntryDao(): MileageEntryDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE settings ADD COLUMN darkMode INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}

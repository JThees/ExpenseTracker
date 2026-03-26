package com.expensetracker.di

import android.content.Context
import androidx.room.Room
import com.expensetracker.data.local.AppDatabase
import com.expensetracker.data.local.dao.BatchDao
import com.expensetracker.data.local.dao.ExpenseItemDao
import com.expensetracker.data.local.dao.MileageEntryDao
import com.expensetracker.data.local.dao.SettingsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "expense_tracker.db"
        ).addMigrations(AppDatabase.MIGRATION_1_2).build()
    }

    @Provides
    fun provideBatchDao(db: AppDatabase): BatchDao = db.batchDao()

    @Provides
    fun provideExpenseItemDao(db: AppDatabase): ExpenseItemDao = db.expenseItemDao()

    @Provides
    fun provideMileageEntryDao(db: AppDatabase): MileageEntryDao = db.mileageEntryDao()

    @Provides
    fun provideSettingsDao(db: AppDatabase): SettingsDao = db.settingsDao()
}

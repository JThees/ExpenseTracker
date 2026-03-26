package com.expensetracker.data.repository

import com.expensetracker.data.local.dao.SettingsDao
import com.expensetracker.data.local.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val settingsDao: SettingsDao
) {
    fun getSettings(): Flow<SettingsEntity?> = settingsDao.getSettings()

    suspend fun getSettingsOnce(): SettingsEntity {
        return settingsDao.getSettingsOnce() ?: SettingsEntity().also { settingsDao.upsert(it) }
    }

    suspend fun updateMileageRate(rate: Double) {
        val current = getSettingsOnce()
        settingsDao.upsert(current.copy(mileageRate = rate))
    }

    suspend fun updateEmailRecipients(recipients: String) {
        val current = getSettingsOnce()
        settingsDao.upsert(current.copy(emailRecipients = recipients))
    }

    suspend fun updateSenderName(name: String) {
        val current = getSettingsOnce()
        settingsDao.upsert(current.copy(senderName = name))
    }

    suspend fun updateSignatureUri(uri: String?) {
        val current = getSettingsOnce()
        settingsDao.upsert(current.copy(signatureImageUri = uri))
    }

    suspend fun updateDarkMode(enabled: Boolean) {
        val current = getSettingsOnce()
        settingsDao.upsert(current.copy(darkMode = enabled))
    }
}

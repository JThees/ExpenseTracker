package com.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val mileageRate: Double = 0.67,
    val signatureImageUri: String? = null,
    val emailRecipients: String = "",
    val senderName: String = ""
)

package com.expensetracker.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object Dashboard

@Serializable
data class BatchDetail(val batchId: Long)

@Serializable
data class AddEditExpense(val batchId: Long, val expenseId: Long = -1L)

@Serializable
data class AddEditMileage(val batchId: Long, val mileageId: Long = -1L)

@Serializable
data class ReviewSubmit(val batchId: Long)

@Serializable
data object ControlPanel

@Serializable
data object SignatureCapture

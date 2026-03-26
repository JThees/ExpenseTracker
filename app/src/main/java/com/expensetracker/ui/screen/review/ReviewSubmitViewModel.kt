package com.expensetracker.ui.screen.review

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.local.entity.BatchEntity
import com.expensetracker.data.local.entity.ExpenseItemEntity
import com.expensetracker.data.local.entity.MileageEntryEntity
import com.expensetracker.data.local.entity.SettingsEntity
import com.expensetracker.data.repository.BatchRepository
import com.expensetracker.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ReviewSubmitViewModel @Inject constructor(
    private val batchRepository: BatchRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _batchId = MutableStateFlow<Long?>(null)

    fun setBatchId(id: Long) { _batchId.value = id }

    @OptIn(ExperimentalCoroutinesApi::class)
    val batch: StateFlow<BatchEntity?> = _batchId
        .filterNotNull()
        .flatMapLatest { batchRepository.getBatchById(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val expenseItems: StateFlow<List<ExpenseItemEntity>> = _batchId
        .filterNotNull()
        .flatMapLatest { batchRepository.getExpenseItemsForBatch(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val mileageEntries: StateFlow<List<MileageEntryEntity>> = _batchId
        .filterNotNull()
        .flatMapLatest { batchRepository.getMileageEntriesForBatch(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<SettingsEntity> = settingsRepository.getSettings()
        .map { it ?: SettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsEntity())

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalExpenses: StateFlow<Double> = _batchId
        .filterNotNull()
        .flatMapLatest { batchRepository.getTotalExpensesForBatch(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalMileage: StateFlow<Double> = _batchId
        .filterNotNull()
        .flatMapLatest { batchRepository.getTotalMileageForBatch(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val grandTotal: StateFlow<Double> = combine(totalExpenses, totalMileage) { e, m -> e + m }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun submitBatch(context: Context, onSubmitted: () -> Unit) {
        viewModelScope.launch {
            val currentBatch = batch.value ?: return@launch
            val expenses = expenseItems.value
            val mileage = mileageEntries.value
            val currentSettings = settingsRepository.getSettingsOnce()
            val total = grandTotal.value

            val emailBody = buildEmailBody(currentBatch, expenses, mileage, total, currentSettings)

            // Collect attachment URIs
            val attachments = ArrayList<Uri>()

            // Receipt images
            for (expense in expenses) {
                expense.receiptImageUri?.let { uriStr ->
                    val file = File(uriStr)
                    if (file.exists()) {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        attachments.add(uri)
                    } else {
                        try {
                            attachments.add(Uri.parse(uriStr))
                        } catch (_: Exception) {}
                    }
                }
            }

            // Signature image
            currentSettings.signatureImageUri?.let { uriStr ->
                val file = File(uriStr)
                if (file.exists()) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    attachments.add(uri)
                }
            }

            // Build email intent
            val recipients = currentSettings.emailRecipients
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toTypedArray()

            val intent = if (attachments.size > 1) {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, attachments)
                }
            } else if (attachments.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_STREAM, attachments[0])
                }
            } else {
                Intent(Intent.ACTION_SEND)
            }

            intent.apply {
                type = if (attachments.isNotEmpty()) "message/rfc822" else "text/plain"
                putExtra(Intent.EXTRA_EMAIL, recipients)
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "Expense Report: ${currentBatch.title}"
                )
                putExtra(Intent.EXTRA_TEXT, emailBody)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Send Expense Report"))

            // Mark batch as submitted
            batchRepository.markBatchSubmitted(currentBatch.id)
            onSubmitted()
        }
    }

    private fun buildEmailBody(
        batch: BatchEntity,
        expenses: List<ExpenseItemEntity>,
        mileage: List<MileageEntryEntity>,
        total: Double,
        settings: SettingsEntity
    ): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
        val sb = StringBuilder()

        sb.appendLine("EXPENSE REPORT: ${batch.title}")
        sb.appendLine("Date: ${dateFormat.format(Date(batch.createdAt))}")
        if (settings.senderName.isNotBlank()) {
            sb.appendLine("Submitted by: ${settings.senderName}")
        }
        sb.appendLine()

        if (expenses.isNotEmpty()) {
            sb.appendLine("--- EXPENSES ---")
            for (item in expenses) {
                sb.appendLine(
                    "${dateFormat.format(Date(item.date))} | " +
                    "${item.category.replaceFirstChar { it.uppercase() }} | " +
                    "${currencyFormat.format(item.amount)} | " +
                    item.description
                )
                if (item.notes.isNotBlank()) sb.appendLine("  Notes: ${item.notes}")
                if (item.isSplitItem) sb.appendLine("  (Split item)")
            }
            sb.appendLine("Expenses Subtotal: ${currencyFormat.format(expenses.sumOf { it.amount })}")
            sb.appendLine()
        }

        if (mileage.isNotEmpty()) {
            sb.appendLine("--- MILEAGE ---")
            for (entry in mileage) {
                sb.appendLine(
                    "${dateFormat.format(Date(entry.date))} | " +
                    "${entry.distance} mi @ ${currencyFormat.format(entry.rate)}/mi | " +
                    currencyFormat.format(entry.calculatedAmount)
                )
                if (entry.notes.isNotBlank()) sb.appendLine("  Notes: ${entry.notes}")
            }
            sb.appendLine("Mileage Subtotal: ${currencyFormat.format(mileage.sumOf { it.calculatedAmount })}")
            sb.appendLine()
        }

        sb.appendLine("================================")
        sb.appendLine("GRAND TOTAL: ${currencyFormat.format(total)}")
        sb.appendLine("================================")

        if (batch.notes.isNotBlank()) {
            sb.appendLine()
            sb.appendLine("Batch Notes: ${batch.notes}")
        }

        return sb.toString()
    }
}

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
import com.expensetracker.ui.component.PdfReportGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.withContext
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

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    fun submitBatch(context: Context, onSubmitted: () -> Unit) {
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                val currentBatch = batch.value ?: return@launch
                val expenses = expenseItems.value
                val mileage = mileageEntries.value
                val currentSettings = settingsRepository.getSettingsOnce()
                val total = grandTotal.value

                // Generate PDF on IO thread
                val pdfFile = withContext(Dispatchers.IO) {
                    PdfReportGenerator(context).generate(
                        batch = currentBatch,
                        expenses = expenses,
                        mileage = mileage,
                        settings = currentSettings,
                        grandTotal = total
                    )
                }

                val pdfUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    pdfFile
                )

                // Build email body as plain-text fallback summary
                val emailBody = buildEmailBody(currentBatch, expenses, mileage, total, currentSettings)

                val recipients = currentSettings.emailRecipients
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .toTypedArray()

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, pdfUri)
                    putExtra(Intent.EXTRA_EMAIL, recipients)
                    putExtra(Intent.EXTRA_SUBJECT, "Expense Report: ${currentBatch.title}")
                    putExtra(Intent.EXTRA_TEXT, emailBody)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(Intent.createChooser(intent, "Send Expense Report"))

                batchRepository.markBatchSubmitted(currentBatch.id)
                onSubmitted()
            } finally {
                _isGenerating.value = false
            }
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
        sb.appendLine("Please see the attached PDF for the full expense report with receipt images and signature.")
        sb.appendLine()

        sb.appendLine("--- SUMMARY ---")
        if (expenses.isNotEmpty()) {
            sb.appendLine("Expenses: ${currencyFormat.format(expenses.sumOf { it.amount })} (${expenses.size} items)")
        }
        if (mileage.isNotEmpty()) {
            sb.appendLine("Mileage: ${currencyFormat.format(mileage.sumOf { it.calculatedAmount })} (${mileage.size} entries)")
        }
        sb.appendLine("GRAND TOTAL: ${currencyFormat.format(total)}")

        return sb.toString()
    }
}

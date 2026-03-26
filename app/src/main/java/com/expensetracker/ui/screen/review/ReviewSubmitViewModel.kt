package com.expensetracker.ui.screen.review

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
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

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    // Holds the generated PDF file for the save dialog
    private val _pendingPdfFile = MutableStateFlow<File?>(null)
    val pendingPdfFile: StateFlow<File?> = _pendingPdfFile

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

                // Store the file so the dialog can offer to save it
                _pendingPdfFile.value = pdfFile

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

    fun savePdfToDownloads(context: Context) {
        val pdfFile = _pendingPdfFile.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, pdfFile.name)
                        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = context.contentResolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues
                    )
                    if (uri != null) {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            pdfFile.inputStream().use { input -> input.copyTo(output) }
                        }
                    }
                } else {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    )
                    val dest = File(downloadsDir, pdfFile.name)
                    pdfFile.copyTo(dest, overwrite = true)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "PDF saved to Downloads", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
                }
            }
            _pendingPdfFile.value = null
        }
    }

    fun dismissSaveDialog() {
        _pendingPdfFile.value = null
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

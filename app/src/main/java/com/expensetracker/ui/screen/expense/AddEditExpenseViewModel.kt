package com.expensetracker.ui.screen.expense

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.local.entity.ExpenseItemEntity
import com.expensetracker.data.repository.BatchRepository
import com.expensetracker.ui.component.ScannedReceiptData
import com.expensetracker.ui.component.scanReceipt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditExpenseViewModel @Inject constructor(
    private val batchRepository: BatchRepository
) : ViewModel() {

    private val _date = MutableStateFlow(System.currentTimeMillis())
    val date: StateFlow<Long> = _date

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount

    private val _category = MutableStateFlow("other")
    val category: StateFlow<String> = _category

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes

    private val _receiptUri = MutableStateFlow<String?>(null)
    val receiptUri: StateFlow<String?> = _receiptUri

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing

    private val _scannedData = MutableStateFlow<ScannedReceiptData?>(null)
    val scannedData: StateFlow<ScannedReceiptData?> = _scannedData

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private var existingItem: ExpenseItemEntity? = null

    val categories = listOf("meals", "transport", "lodging", "supplies", "entertainment", "other")

    fun loadExpense(expenseId: Long) {
        viewModelScope.launch {
            batchRepository.getExpenseItemById(expenseId)?.let { item ->
                existingItem = item
                _isEditing.value = true
                _date.value = item.date
                _amount.value = item.amount.toString()
                _category.value = item.category
                _description.value = item.description
                _notes.value = item.notes
                _receiptUri.value = item.receiptImageUri
            }
        }
    }

    fun setDate(millis: Long) { _date.value = millis }
    fun setAmount(value: String) { _amount.value = value }
    fun setCategory(value: String) { _category.value = value }
    fun setDescription(value: String) { _description.value = value }
    fun setNotes(value: String) { _notes.value = value }
    fun setReceiptUri(uri: Uri?) { _receiptUri.value = uri?.toString() }
    fun setReceiptUriString(uri: String?) { _receiptUri.value = uri }

    fun scanReceiptImage(context: Context, uri: Uri) {
        _isScanning.value = true
        viewModelScope.launch {
            try {
                val data = scanReceipt(context, uri)
                _scannedData.value = data
                // Auto-fill fields from scan
                data.totalAmount?.let { _amount.value = String.format("%.2f", it) }
                data.vendor?.let { _description.value = it }
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun saveSplitItems(batchId: Long, items: List<Pair<String, Double>>, onSaved: () -> Unit) {
        viewModelScope.launch {
            val parentItem = ExpenseItemEntity(
                batchId = batchId,
                date = _date.value,
                amount = items.sumOf { it.second },
                category = _category.value,
                description = _description.value,
                notes = _notes.value,
                receiptImageUri = _receiptUri.value
            )
            val parentId = batchRepository.addExpenseItem(parentItem)

            val splitItems = items.map { (desc, amt) ->
                ExpenseItemEntity(
                    batchId = batchId,
                    date = _date.value,
                    amount = amt,
                    category = _category.value,
                    description = desc,
                    notes = "",
                    isSplitItem = true,
                    parentExpenseId = parentId
                )
            }
            batchRepository.addExpenseItems(splitItems)
            onSaved()
        }
    }

    fun save(batchId: Long, onSaved: () -> Unit) {
        val amountValue = _amount.value.toDoubleOrNull() ?: return
        viewModelScope.launch {
            if (_isEditing.value && existingItem != null) {
                batchRepository.updateExpenseItem(
                    existingItem!!.copy(
                        date = _date.value,
                        amount = amountValue,
                        category = _category.value,
                        description = _description.value,
                        notes = _notes.value,
                        receiptImageUri = _receiptUri.value
                    )
                )
            } else {
                batchRepository.addExpenseItem(
                    ExpenseItemEntity(
                        batchId = batchId,
                        date = _date.value,
                        amount = amountValue,
                        category = _category.value,
                        description = _description.value,
                        notes = _notes.value,
                        receiptImageUri = _receiptUri.value
                    )
                )
            }
            onSaved()
        }
    }
}

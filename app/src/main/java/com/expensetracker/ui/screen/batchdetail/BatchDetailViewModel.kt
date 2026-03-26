package com.expensetracker.ui.screen.batchdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.local.entity.BatchEntity
import com.expensetracker.data.local.entity.ExpenseItemEntity
import com.expensetracker.data.local.entity.MileageEntryEntity
import com.expensetracker.data.repository.BatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BatchDetailViewModel @Inject constructor(
    private val batchRepository: BatchRepository
) : ViewModel() {

    private val _batchId = MutableStateFlow<Long?>(null)

    fun setBatchId(id: Long) {
        _batchId.value = id
    }

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

    val grandTotal: StateFlow<Double> = combine(totalExpenses, totalMileage) { exp, mil ->
        exp + mil
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun deleteExpenseItem(item: ExpenseItemEntity) {
        viewModelScope.launch {
            batchRepository.deleteExpenseItem(item)
        }
    }

    fun deleteMileageEntry(entry: MileageEntryEntity) {
        viewModelScope.launch {
            batchRepository.deleteMileageEntry(entry)
        }
    }

    fun deleteBatch(onDeleted: () -> Unit) {
        viewModelScope.launch {
            batch.value?.let { batchRepository.deleteBatch(it) }
            onDeleted()
        }
    }
}

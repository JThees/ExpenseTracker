package com.expensetracker.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.local.entity.BatchEntity
import com.expensetracker.data.repository.BatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val batchRepository: BatchRepository
) : ViewModel() {

    val batches: StateFlow<List<BatchEntity>> = batchRepository.getAllBatches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createBatch(title: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = batchRepository.createBatch(title)
            onCreated(id)
        }
    }

    fun deleteBatch(batch: BatchEntity) {
        viewModelScope.launch {
            batchRepository.deleteBatch(batch)
        }
    }
}

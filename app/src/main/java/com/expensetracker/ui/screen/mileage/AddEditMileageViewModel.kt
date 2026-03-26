package com.expensetracker.ui.screen.mileage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.local.entity.MileageEntryEntity
import com.expensetracker.data.repository.BatchRepository
import com.expensetracker.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditMileageViewModel @Inject constructor(
    private val batchRepository: BatchRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _date = MutableStateFlow(System.currentTimeMillis())
    val date: StateFlow<Long> = _date

    private val _distance = MutableStateFlow("")
    val distance: StateFlow<String> = _distance

    private val _rate = MutableStateFlow(0.67)
    val rate: StateFlow<Double> = _rate

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing

    val calculatedAmount: StateFlow<Double> = combine(_distance, _rate) { dist, rate ->
        (dist.toDoubleOrNull() ?: 0.0) * rate
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private var existingEntry: MileageEntryEntity? = null

    init {
        viewModelScope.launch {
            val settings = settingsRepository.getSettingsOnce()
            _rate.value = settings.mileageRate
        }
    }

    fun loadMileageEntry(mileageId: Long) {
        viewModelScope.launch {
            batchRepository.getMileageEntryById(mileageId)?.let { entry ->
                existingEntry = entry
                _isEditing.value = true
                _date.value = entry.date
                _distance.value = entry.distance.toString()
                _rate.value = entry.rate
                _notes.value = entry.notes
            }
        }
    }

    fun setDate(millis: Long) { _date.value = millis }
    fun setDistance(value: String) { _distance.value = value }
    fun setNotes(value: String) { _notes.value = value }

    fun save(batchId: Long, onSaved: () -> Unit) {
        val dist = _distance.value.toDoubleOrNull() ?: return
        val amount = dist * _rate.value
        viewModelScope.launch {
            if (_isEditing.value && existingEntry != null) {
                batchRepository.updateMileageEntry(
                    existingEntry!!.copy(
                        date = _date.value,
                        distance = dist,
                        rate = _rate.value,
                        notes = _notes.value,
                        calculatedAmount = amount
                    )
                )
            } else {
                batchRepository.addMileageEntry(
                    MileageEntryEntity(
                        batchId = batchId,
                        date = _date.value,
                        distance = dist,
                        rate = _rate.value,
                        notes = _notes.value,
                        calculatedAmount = amount
                    )
                )
            }
            onSaved()
        }
    }
}

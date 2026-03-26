package com.expensetracker.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.local.entity.SettingsEntity
import com.expensetracker.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ControlPanelViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<SettingsEntity> = settingsRepository.getSettings()
        .map { it ?: SettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsEntity())

    fun updateMileageRate(rate: Double) {
        viewModelScope.launch { settingsRepository.updateMileageRate(rate) }
    }

    fun updateEmailRecipients(recipients: String) {
        viewModelScope.launch { settingsRepository.updateEmailRecipients(recipients) }
    }

    fun updateSenderName(name: String) {
        viewModelScope.launch { settingsRepository.updateSenderName(name) }
    }

    fun updateSignatureUri(uri: String?) {
        viewModelScope.launch { settingsRepository.updateSignatureUri(uri) }
    }
}

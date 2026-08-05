package com.bioguard.movil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bioguard.movil.datastore.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isSyncEnabled: Boolean = true,
    val syncIntervalMinutes: Int = 15,
    val batchStartHour: Int = 2,
    val batchEndHour: Int = 6,
    val isNightGuardianEnabled: Boolean = true,
    val nightGuardianStartHour: Int = 22,
    val nightGuardianEndHour: Int = 6,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val prefs: UserPreferences
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val syncEnabled = prefs.isSyncEnabled.first()
            val interval = prefs.syncIntervalMinutes.first()
            val bStart = prefs.batchStartHour.first()
            val bEnd = prefs.batchEndHour.first()
            val nightEnabled = prefs.isNightGuardianEnabled.first()
            val nStart = prefs.nightGuardianStartHour.first()
            val nEnd = prefs.nightGuardianEndHour.first()

            _uiState.update {
                it.copy(
                    isSyncEnabled = syncEnabled,
                    syncIntervalMinutes = interval,
                    batchStartHour = bStart,
                    batchEndHour = bEnd,
                    isNightGuardianEnabled = nightEnabled,
                    nightGuardianStartHour = nStart,
                    nightGuardianEndHour = nEnd
                )
            }
        }
    }

    fun updateSyncEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isSyncEnabled = enabled) }
    }

    fun updateSyncInterval(minutes: Int) {
        _uiState.update { it.copy(syncIntervalMinutes = minutes) }
    }

    fun updateBatchHours(start: Int, end: Int) {
        _uiState.update { it.copy(batchStartHour = start, batchEndHour = end) }
    }

    fun updateNightGuardianEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isNightGuardianEnabled = enabled) }
    }

    fun updateNightGuardianHours(start: Int, end: Int) {
        _uiState.update { it.copy(nightGuardianStartHour = start, nightGuardianEndHour = end) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveSuccess = false) }
            val state = _uiState.value

            prefs.saveSyncSettings(
                isSyncEnabled = state.isSyncEnabled,
                syncIntervalMinutes = state.syncIntervalMinutes,
                batchStartHour = state.batchStartHour,
                batchEndHour = state.batchEndHour
            )

            prefs.saveNightGuardianSettings(
                isEnabled = state.isNightGuardianEnabled,
                startHour = state.nightGuardianStartHour,
                endHour = state.nightGuardianEndHour
            )

            _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
        }
    }

    fun resetSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
}

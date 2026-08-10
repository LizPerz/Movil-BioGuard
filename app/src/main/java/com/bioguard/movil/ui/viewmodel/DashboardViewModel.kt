package com.bioguard.movil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bioguard.movil.data.local.CachedDataDao
import com.bioguard.movil.data.local.CachedReadingEntity
import com.bioguard.movil.datastore.UserPreferences
import com.bioguard.movil.network.DashboardSummary
import com.bioguard.movil.network.LecturaSensorResponse
import com.bioguard.movil.service.WearableConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = false,
    val summary: DashboardSummary? = null,
    val lecturasRecientes: List<LecturaSensorResponse> = emptyList(),
    val pacienteId: String? = null,
    val connectionState: WearableConnectionState = WearableConnectionState.DISCONNECTED,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
    private val cachedDataDao: CachedDataDao,
    private val prefs: UserPreferences
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private var dashboardJob: Job? = null

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        dashboardJob?.cancel()
        dashboardJob = viewModelScope.launch {
            val patientId = prefs.patientId.first() ?: "paciente-local"
            _uiState.update { it.copy(isLoading = false, error = null, pacienteId = patientId) }

            cachedDataDao.getAllCachedReadings(10).collectLatest { cachedReadings ->
                val latest = cachedReadings.firstOrNull()?.toResponse()
                val connState = if (cachedReadings.isNotEmpty()) WearableConnectionState.STREAMING else WearableConnectionState.PAIRED
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        connectionState = connState,
                        summary = DashboardSummary(
                            ultimaLectura = latest
                        ),
                        lecturasRecientes = cachedReadings.map { reading -> reading.toResponse() },
                        error = null
                    )
                }
            }
        }
    }

    private fun CachedReadingEntity.toResponse(): LecturaSensorResponse {
        return LecturaSensorResponse(
            id = id,
            timestamp = fechaHora,
            pulsoBpm = pulsoBpm,
            temperaturaC = temperaturaC,
            sudoracionGsr = sudoracionGsr,
            hrv = hrv,
            spo2 = spo2,
            probabilidadPico = null,
            nivelRiesgo = null
        )
    }
}

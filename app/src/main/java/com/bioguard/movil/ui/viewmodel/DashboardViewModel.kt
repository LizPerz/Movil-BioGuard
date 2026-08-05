package com.bioguard.movil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.repository.PacienteRepository
import com.bioguard.movil.data.repository.SensorRepository
import com.bioguard.movil.datastore.UserPreferences
import com.bioguard.movil.network.DashboardSummary
import com.bioguard.movil.network.LecturaSensorResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = false,
    val summary: DashboardSummary? = null,
    val lecturasRecientes: List<LecturaSensorResponse> = emptyList(),
    val pacienteId: String? = null,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
    private val sensorRepository: SensorRepository,
    private val pacienteRepository: PacienteRepository,
    private val prefs: UserPreferences
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            val patientId = prefs.patientId.first() ?: run {
                _uiState.update { it.copy(error = "No hay paciente configurado") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null, pacienteId = patientId) }

            when (val summaryResult = pacienteRepository.getDashboardSummary(patientId)) {
                is Resource.Success -> {
                    val summaryData = summaryResult.data
                    _uiState.update { it.copy(summary = summaryData) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(error = summaryResult.message) }
                }
                is Resource.Loading -> {}
            }

            when (val readingsResult = sensorRepository.getLecturas(patientId, 10)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            lecturasRecientes = readingsResult.data ?: emptyList()
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = it.error ?: readingsResult.message
                        )
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }
}

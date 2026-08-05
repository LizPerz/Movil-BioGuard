package com.example.bioguard_movil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bioguard_movil.data.Resource
import com.example.bioguard_movil.data.repository.PacienteRepository
import com.example.bioguard_movil.data.repository.SensorRepository
import com.example.bioguard_movil.datastore.UserPreferences
import com.example.bioguard_movil.network.DashboardSummary
import com.example.bioguard_movil.network.LecturaSensorResponse
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

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = UserPreferences(application)
    private val sensorRepository = SensorRepository()
    private val pacienteRepository = PacienteRepository()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val pacienteId = prefs.patientId.first()
            if (pacienteId != null) {
                _uiState.update { it.copy(pacienteId = pacienteId) }
                loadSummary(pacienteId)
                loadLecturas(pacienteId)
            } else {
                _uiState.update { it.copy(isLoading = false, error = "No se encontro un paciente vinculado") }
            }
        }
    }

    private suspend fun loadSummary(pacienteId: String) {
        when (val result = pacienteRepository.getDashboardSummary(pacienteId)) {
            is Resource.Success -> _uiState.update {
                it.copy(summary = result.data)
            }
            is Resource.Error -> _uiState.update {
                it.copy(error = result.message)
            }
            is Resource.Loading -> {}
        }
    }

    private suspend fun loadLecturas(pacienteId: String) {
        when (val result = sensorRepository.getLecturas(pacienteId, 5)) {
            is Resource.Success -> _uiState.update {
                it.copy(lecturasRecientes = result.data, isLoading = false)
            }
            is Resource.Error -> _uiState.update {
                it.copy(isLoading = false, error = result.message)
            }
            is Resource.Loading -> {}
        }
    }

    fun refresh() {
        val pacienteId = _uiState.value.pacienteId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            loadSummary(pacienteId)
            loadLecturas(pacienteId)
        }
    }
}

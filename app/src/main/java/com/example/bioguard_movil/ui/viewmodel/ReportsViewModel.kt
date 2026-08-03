package com.example.bioguard_movil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bioguard_movil.data.Resource
import com.example.bioguard_movil.data.repository.PacienteRepository
import com.example.bioguard_movil.data.repository.ReporteRepository
import com.example.bioguard_movil.data.repository.SensorRepository
import com.example.bioguard_movil.datastore.UserPreferences
import com.example.bioguard_movil.network.EventoMetabolicoResponse
import com.example.bioguard_movil.network.ReporteResumenResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReportsUiState(
    val isLoading: Boolean = false,
    val reporte: ReporteResumenResponse? = null,
    val eventos: List<EventoMetabolicoResponse> = emptyList(),
    val error: String? = null
)

class ReportsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = UserPreferences(application)
    private val pacienteRepository = PacienteRepository()
    private val reporteRepository = ReporteRepository()
    private val sensorRepository = SensorRepository()

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        loadReportes()
    }

    fun loadReportes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val pacienteId = pacienteRepository.resolvePatientId(prefs)
                ?: return@launch _uiState.update { it.copy(isLoading = false, error = "No se encontro un paciente vinculado") }
            when (val result = reporteRepository.getReporteResumen(pacienteId)) {
                is Resource.Success -> _uiState.update {
                    it.copy(reporte = result.data)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(error = result.message)
                }
                is Resource.Loading -> {}
            }
            when (val result = sensorRepository.getEventos(pacienteId, 10)) {
                is Resource.Success -> _uiState.update {
                    it.copy(eventos = result.data, isLoading = false)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun refresh() {
        loadReportes()
    }
}

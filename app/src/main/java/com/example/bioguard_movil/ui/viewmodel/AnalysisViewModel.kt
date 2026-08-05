package com.example.bioguard_movil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bioguard_movil.data.Formatters
import com.example.bioguard_movil.data.Resource
import com.example.bioguard_movil.data.repository.PacienteRepository
import com.example.bioguard_movil.data.repository.SensorRepository
import com.example.bioguard_movil.datastore.UserPreferences
import com.example.bioguard_movil.network.LecturaSensorResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnalysisUiState(
    val isLoading: Boolean = false,
    val lecturas: List<LecturaSensorResponse> = emptyList(),
    val selectedMetric: String = "Pulso",
    val selectedTimeFilter: String = "Hoy",
    val error: String? = null
)

class AnalysisViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = UserPreferences(application)
    private val pacienteRepository = PacienteRepository()
    private val sensorRepository = SensorRepository()

    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private var allLecturas: List<LecturaSensorResponse> = emptyList()

    init {
        loadLecturas()
    }

    fun loadLecturas() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val pacienteId = pacienteRepository.resolvePatientId(prefs)
                ?: return@launch _uiState.update { it.copy(isLoading = false, error = "No se encontro un paciente vinculado") }
            when (val result = sensorRepository.getLecturas(pacienteId, 100)) {
                is Resource.Success -> {
                    allLecturas = result.data
                    _uiState.update {
                        it.copy(lecturas = filterByTime(allLecturas, it.selectedTimeFilter), isLoading = false)
                    }
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun selectMetric(metric: String) {
        _uiState.update { it.copy(selectedMetric = metric) }
    }

    fun selectTimeFilter(filter: String) {
        _uiState.update {
            it.copy(selectedTimeFilter = filter, lecturas = filterByTime(allLecturas, filter))
        }
    }

    private fun filterByTime(lecturas: List<LecturaSensorResponse>, filter: String): List<LecturaSensorResponse> {
        val now = System.currentTimeMillis()
        val cutoff = when (filter) {
            "1h" -> now - 3600_000L
            "4h" -> now - 4 * 3600_000L
            "7 d\u00edas" -> now - 7 * 24 * 3600_000L
            else -> now - 24 * 3600_000L
        }
        return lecturas.filter { lectura ->
            val ts = Formatters.parseIsoTimestamp(lectura.timestamp)?.toEpochMilli() ?: return@filter true
            ts >= cutoff
        }
    }
}

package com.bioguard.movil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bioguard.movil.data.Formatters
import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.repository.PacienteRepository
import com.bioguard.movil.data.repository.SensorRepository
import com.bioguard.movil.datastore.UserPreferences
import com.bioguard.movil.network.EventoMetabolicoResponse
import com.bioguard.movil.network.LecturaSensorResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val isLoading: Boolean = false,
    val lecturas: List<LecturaSensorResponse> = emptyList(),
    val eventos: List<EventoMetabolicoResponse> = emptyList(),
    val selectedFilter: String = "Hoy",
    val error: String? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    application: Application,
    private val sensorRepository: SensorRepository,
    private val pacienteRepository: PacienteRepository,
    private val prefs: UserPreferences
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var allLecturas: List<LecturaSensorResponse> = emptyList()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val pacienteId = pacienteRepository.resolvePatientId(prefs)
            if (pacienteId == null) {
                _uiState.update { it.copy(isLoading = false, error = "No se encontro un paciente vinculado") }
                return@launch
            }
            when (val result = sensorRepository.getLecturas(pacienteId, 100)) {
                is Resource.Success -> {
                    allLecturas = result.data
                    _uiState.update {
                        it.copy(lecturas = filterByTime(allLecturas, it.selectedFilter), isLoading = false)
                    }
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> {}
            }
            when (val result = sensorRepository.getEventos(pacienteId, 50)) {
                is Resource.Success -> _uiState.update {
                    it.copy(eventos = result.data)
                }
                is Resource.Loading -> {}
                is Resource.Error -> {}
            }
        }
    }

    fun selectFilter(filter: String) {
        _uiState.update {
            it.copy(selectedFilter = filter, lecturas = filterByTime(allLecturas, filter))
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

package com.bioguard.movil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bioguard.movil.data.Formatters
import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.local.CachedDataDao
import com.bioguard.movil.data.local.CachedReadingEntity
import com.bioguard.movil.data.repository.PacienteRepository
import com.bioguard.movil.data.repository.SensorRepository
import com.bioguard.movil.datastore.UserPreferences
import com.bioguard.movil.network.LecturaSensorResponse
import com.bioguard.movil.realtime.RealtimeHubClient
import com.bioguard.movil.ui.model.UserRole
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

data class AnalysisUiState(
    val isLoading: Boolean = false,
    val lecturas: List<LecturaSensorResponse> = emptyList(),
    val selectedMetric: String = "Pulso",
    val selectedTimeFilter: String = "Hoy",
    val error: String? = null
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    application: Application,
    private val sensorRepository: SensorRepository,
    private val pacienteRepository: PacienteRepository,
    private val cachedDataDao: CachedDataDao,
    private val prefs: UserPreferences
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private var allLecturas: List<LecturaSensorResponse> = emptyList()
    private var cacheJob: Job? = null
    private var pollingJob: Job? = null

    private val role = prefs.userRole.value

    init {
        loadLecturas()
        if (role == UserRole.CUIDADOR) startCuidadorPolling()
    }

    private fun startCuidadorPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(CUIDADOR_POLL_INTERVAL_MS)
                loadLecturas()
            }
        }
    }

    fun loadLecturas() {
        cacheJob?.cancel()
        cacheJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val pacienteId = pacienteRepository.resolveEffectivePatientId(prefs)
                ?: return@launch _uiState.update { it.copy(isLoading = false, error = "No se encontro un paciente vinculado") }

            // Fuente primaria: caché local filtrada por paciente. Se actualiza en tiempo real
            // conforme llegan lecturas del reloj (evita fugas entre pacientes).
            cachedDataDao.getCachedReadings(pacienteId, 500).collectLatest { cached ->
                allLecturas = cached.map { it.toResponse() }
                _uiState.update {
                    it.copy(lecturas = filterByTime(allLecturas, it.selectedTimeFilter), isLoading = false, error = null)
                }
            }
        }

        // Respaldo remoto: backfill del historial para no quedarse sin datos al reinstalar.
        viewModelScope.launch {
            val pacienteId = pacienteRepository.resolveEffectivePatientId(prefs) ?: return@launch
            when (val result = sensorRepository.getLecturas(pacienteId, 200)) {
                is Resource.Success -> {
                    if (result.data.isNotEmpty()) {
                        val knownIds = allLecturas.map { it.id }.toSet()
                        val nuevos = result.data.filter { it.id !in knownIds }
                        if (nuevos.isNotEmpty()) {
                            allLecturas = (allLecturas + nuevos).distinctBy { it.id }
                            _uiState.update {
                                it.copy(lecturas = filterByTime(allLecturas, it.selectedTimeFilter), isLoading = false)
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    // Si no hay datos locales ni remotos, se muestra el estado vacío sin bloquear.
                    if (allLecturas.isEmpty()) {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
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

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }

    private fun CachedReadingEntity.toResponse(): LecturaSensorResponse {
        return LecturaSensorResponse(
            id = id,
            timestamp = fechaHora,
            pulsoBpm = pulsoBpm,
            temperaturaC = temperaturaC,
            estresPct = estresPct,
            hrv = hrv,
            spo2 = spo2,
            probabilidadPico = null,
            nivelRiesgo = null
        )
    }

    companion object {
        private const val CUIDADOR_POLL_INTERVAL_MS = 30_000L
    }
}

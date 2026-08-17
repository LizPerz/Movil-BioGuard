package com.bioguard.movil.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bioguard.movil.data.Formatters
import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.repository.PacienteRepository
import com.bioguard.movil.data.repository.SensorRepository
import com.bioguard.movil.datastore.UserPreferences
import com.bioguard.movil.network.TrackingResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class LocationUiState(
    val isLoading: Boolean = true,
    val ubicacion: TrackingResponse? = null,
    val lastUpdatedAt: Instant? = null,
    val sinUbicacion: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LocationViewModel @Inject constructor(
    private val sensorRepository: SensorRepository,
    private val pacienteRepository: PacienteRepository,
    private val prefs: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationUiState())
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    init {
        startPolling()
    }

    fun retry() {
        startPolling()
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                pollOnce()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    internal suspend fun pollOnce() {
        val patientId = pacienteRepository.resolveEffectivePatientId(prefs)
        if (patientId.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    sinUbicacion = false,
                    error = "No tienes un paciente asignado. Pide un código QR de acceso a la familia del paciente."
                )
            }
            return
        }
        _uiState.update { it.copy(isLoading = false, error = null) }
        when (val result = sensorRepository.getTrackingActualOrNull(patientId)) {
            is Resource.Success -> {
                val ubicacion = result.data
                _uiState.update {
                    it.copy(
                        ubicacion = ubicacion,
                        lastUpdatedAt = if (ubicacion != null) {
                            Formatters.parseIsoTimestamp(ubicacion.timestamp) ?: Instant.now()
                        } else {
                            it.lastUpdatedAt
                        },
                        sinUbicacion = ubicacion == null,
                        error = null
                    )
                }
            }
            is Resource.Error -> _uiState.update { it.copy(error = result.message) }
            is Resource.Loading -> {}
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val POLL_INTERVAL_MS = 5_000L
    }
}

package com.bioguard.movil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.repository.AlertaRepository
import com.bioguard.movil.data.repository.PacienteRepository
import com.bioguard.movil.datastore.UserPreferences
import com.bioguard.movil.network.AlertaResponse
import com.bioguard.movil.realtime.RealtimeHubClient
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlertUiState(
    val isLoading: Boolean = false,
    val alertasPendientes: List<AlertaResponse> = emptyList(),
    val todasAlertas: List<AlertaResponse> = emptyList(),
    val alertasPendientesCount: Int = 0,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class AlertViewModel @Inject constructor(
    application: Application,
    private val repository: AlertaRepository,
    private val pacienteRepository: PacienteRepository,
    private val prefs: UserPreferences,
    private val realtimeHubClient: RealtimeHubClient
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AlertUiState())
    val uiState: StateFlow<AlertUiState> = _uiState.asStateFlow()

    init {
        // Tiempo real: nueva alerta del paciente → refrescar lista
        viewModelScope.launch {
            realtimeHubClient.events.collect { event ->
                if (event == RealtimeHubClient.EventAlerta) {
                    loadAlertas()
                }
            }
        }
        loadAlertas()
    }

    fun loadAlertas() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val pacienteId = pacienteRepository.resolveEffectivePatientId(prefs)
                ?: return@launch _uiState.update { it.copy(isLoading = false, error = "No se encontro un paciente vinculado") }
            when (val result = repository.getAlertas(pacienteId)) {
                is Resource.Success -> {
                    val todas = result.data
                    val pendientes = todas.filter { !it.atendida }
                    _uiState.update {
                        it.copy(
                            alertasPendientes = pendientes,
                            todasAlertas = todas,
                            alertasPendientesCount = pendientes.size,
                            isLoading = false
                        )
                    }
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun atenderAlerta(id: String, notasAtencion: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val cuidadorId = prefs.userId.first().orEmpty()
            when (val result = repository.atenderAlerta(id, cuidadorId, notasAtencion)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Alerta atendida") }
                    loadAlertas()
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}

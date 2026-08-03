package com.example.bioguard_movil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bioguard_movil.data.Resource
import com.example.bioguard_movil.data.repository.DispositivoRepository
import com.example.bioguard_movil.data.repository.PacienteRepository
import com.example.bioguard_movil.datastore.UserPreferences
import com.example.bioguard_movil.network.InfoCompletaDispositivo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeviceUiState(
    val isLoading: Boolean = false,
    val dispositivo: InfoCompletaDispositivo? = null,
    val error: String? = null,
    val successMessage: String? = null
)

class DeviceViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = UserPreferences(application)
    private val pacienteRepository = PacienteRepository()
    private val repository = DispositivoRepository()

    private val _uiState = MutableStateFlow(DeviceUiState())
    val uiState: StateFlow<DeviceUiState> = _uiState.asStateFlow()

    init {
        loadDispositivo()
    }

    fun loadDispositivo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val pacienteId = pacienteRepository.resolvePatientId(prefs)
                ?: return@launch _uiState.update { it.copy(isLoading = false, error = "No se encontro un paciente vinculado") }
            when (val result = repository.getInfoCompleta(pacienteId)) {
                is Resource.Success -> _uiState.update {
                    it.copy(dispositivo = result.data, isLoading = false)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun vincular(nombreDispositivo: String, macAddress: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.vincularDispositivo(nombreDispositivo, macAddress)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Dispositivo vinculado") }
                    loadDispositivo()
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

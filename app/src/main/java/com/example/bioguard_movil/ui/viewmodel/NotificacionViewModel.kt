package com.example.bioguard_movil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bioguard_movil.data.Resource
import com.example.bioguard_movil.data.repository.NotificacionRepository
import com.example.bioguard_movil.data.repository.PacienteRepository
import com.example.bioguard_movil.datastore.UserPreferences
import com.example.bioguard_movil.network.NotificacionResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificacionUiState(
    val isLoading: Boolean = false,
    val notificaciones: List<NotificacionResponse> = emptyList(),
    val error: String? = null
)

class NotificacionViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = UserPreferences(application)
    private val pacienteRepository = PacienteRepository()
    private val repository = NotificacionRepository()

    private val _uiState = MutableStateFlow(NotificacionUiState())
    val uiState: StateFlow<NotificacionUiState> = _uiState.asStateFlow()

    init {
        loadNotificaciones()
    }

    fun loadNotificaciones() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val pacienteId = pacienteRepository.resolvePatientId(prefs)
                ?: return@launch _uiState.update { it.copy(isLoading = false, error = "No se encontro un paciente vinculado") }
            when (val result = repository.getNotificaciones(pacienteId)) {
                is Resource.Success -> _uiState.update {
                    it.copy(notificaciones = result.data, isLoading = false)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            when (val result = repository.markNotificacionLeida(id)) {
                is Resource.Success -> loadNotificaciones()
                is Resource.Error -> _uiState.update {
                    it.copy(error = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null) }
    }
}

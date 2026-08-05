package com.example.bioguard_movil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bioguard_movil.data.Resource
import com.example.bioguard_movil.data.repository.UsuarioRepository
import com.example.bioguard_movil.network.MiPlanResponse
import com.example.bioguard_movil.network.UsuarioWebResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val perfil: UsuarioWebResponse? = null,
    val plan: MiPlanResponse? = null,
    val error: String? = null,
    val successMessage: String? = null
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UsuarioRepository()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getMiPerfil()) {
                is Resource.Success -> _uiState.update {
                    it.copy(perfil = result.data)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(error = result.message)
                }
                is Resource.Loading -> {}
            }
            when (val result = repository.getMiPlan()) {
                is Resource.Success -> _uiState.update {
                    it.copy(plan = result.data, isLoading = false)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun updatePerfil(nombre: String?, apellidoPaterno: String?, apellidoMaterno: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.updateMiPerfil(nombre, apellidoPaterno, apellidoMaterno)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Perfil actualizado") }
                    loadProfile()
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun deleteSesion(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.deleteSesion(id)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Sesión remota eliminada") }
                    loadProfile()
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateFotoPerfil(base64Foto: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.updateFotoPerfil(base64Foto)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Foto de perfil actualizada") }
                    loadProfile()
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun cancelarPlan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.cancelarPlan()) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Suscripción cancelada correctamente") }
                    loadProfile()
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

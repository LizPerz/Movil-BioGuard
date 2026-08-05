package com.bioguard.movil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.repository.CuidadorRepository
import com.bioguard.movil.network.CuidadorResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CuidadorUiState(
    val isLoading: Boolean = false,
    val cuidadores: List<CuidadorResponse> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class CuidadorViewModel @Inject constructor(
    application: Application,
    private val repository: CuidadorRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CuidadorUiState())
    val uiState: StateFlow<CuidadorUiState> = _uiState.asStateFlow()

    init {
        loadCuidadores()
    }

    fun loadCuidadores() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getCuidadores()) {
                is Resource.Success -> _uiState.update {
                    it.copy(cuidadores = result.data, isLoading = false)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun crearCuidador(
        nombre: String,
        parentesco: String,
        telefono: String,
        correo: String,
        nivelAcceso: String,
        pacienteId: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.crearCuidador(pacienteId, nombre, parentesco, telefono, correo, nivelAcceso)) {
                is Resource.Success -> {
                    val code = result.data.codigoAccesoQr.ifBlank { result.data.message }
                    _uiState.update {
                        it.copy(isLoading = false, successMessage = "Cuidador creado. Codigo de acceso: $code")
                    }
                    loadCuidadores()
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun deleteCuidador(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.deleteCuidador(id)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Cuidador eliminado") }
                    loadCuidadores()
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateNivelAcceso(id: String, nivelAcceso: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.updateNivelAcceso(id, nivelAcceso)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Nivel de acceso actualizado") }
                    loadCuidadores()
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

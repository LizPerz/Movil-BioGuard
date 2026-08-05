package com.example.bioguard_movil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bioguard_movil.data.Resource
import com.example.bioguard_movil.data.repository.MedicamentoRepository
import com.example.bioguard_movil.data.repository.PacienteRepository
import com.example.bioguard_movil.datastore.UserPreferences
import com.example.bioguard_movil.network.MedicamentoResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MedicationUiState(
    val isLoading: Boolean = false,
    val medicamentos: List<MedicamentoResponse> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)

class MedicationViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = UserPreferences(application)
    private val pacienteRepository = PacienteRepository()
    private val repository = MedicamentoRepository()

    private val _uiState = MutableStateFlow(MedicationUiState())
    val uiState: StateFlow<MedicationUiState> = _uiState.asStateFlow()

    init {
        loadMedicamentos()
    }

    fun loadMedicamentos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val pacienteId = pacienteRepository.resolvePatientId(prefs)
                ?: return@launch _uiState.update { it.copy(isLoading = false, error = "No se encontro un paciente vinculado") }
            when (val result = repository.getMedicamentos(pacienteId)) {
                is Resource.Success -> _uiState.update {
                    it.copy(medicamentos = result.data, isLoading = false)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun crearMedicamento(nombre: String, dosis: String, frecuencia: String, notas: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val pacienteId = pacienteRepository.resolvePatientId(prefs)
                ?: return@launch _uiState.update { it.copy(isLoading = false, error = "No se encontro un paciente vinculado") }
            when (val result = repository.crearMedicamento(pacienteId, nombre, dosis, frecuencia, notas)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Medicamento creado") }
                    loadMedicamentos()
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun registrarToma(id: String) {
        viewModelScope.launch {
            when (val result = repository.registrarToma(id)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(successMessage = "Toma registrada") }
                    loadMedicamentos()
                }
                is Resource.Error -> _uiState.update {
                    it.copy(error = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun deleteMedicamento(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.deleteMedicamento(id)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Medicamento eliminado") }
                    loadMedicamentos()
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

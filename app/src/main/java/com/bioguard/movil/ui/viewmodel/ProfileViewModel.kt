package com.bioguard.movil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.repository.PacienteRepository
import com.bioguard.movil.data.repository.UsuarioRepository
import com.bioguard.movil.datastore.UserPreferences
import com.bioguard.movil.network.MiPlanResponse
import com.bioguard.movil.network.PacienteResponse
import com.bioguard.movil.network.UsuarioWebResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BiometriaPacienteState(
    val fechaNacimiento: String = "",
    val sexo: String = "",
    val pesoKg: Double = 0.0,
    val estaturaCm: Double = 0.0,
    val esDiabetico: Boolean = false,
    val familiaresDiabetes: Boolean = false,
    val actividadFisica: String = ""
)

data class ProfileUiState(
    val isLoading: Boolean = false,
    val perfil: UsuarioWebResponse? = null,
    val paciente: PacienteResponse? = null,
    val plan: MiPlanResponse? = null,
    val biometria: BiometriaPacienteState = BiometriaPacienteState(),
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    application: Application,
    private val repository: UsuarioRepository,
    private val pacienteRepository: PacienteRepository,
    private val prefs: UserPreferences
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        loadBiometria()
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

    fun loadBiometria() {
        viewModelScope.launch {
            val birth = prefs.patientBirthDate.first().orEmpty()
            val sex = prefs.patientSex.first().orEmpty()
            val weight = prefs.patientWeight.first()?.toDoubleOrNull() ?: 0.0
            val height = prefs.patientHeight.first()?.toDoubleOrNull() ?: 0.0
            val diabetic = prefs.patientIsDiabetic.first()
            val family = prefs.patientFamilyDiabetes.first()
            val activity = prefs.patientActivityLevel.first().orEmpty()

            val prefsEmpty = birth.isBlank() && sex.isBlank() &&
                weight <= 0.0 && height <= 0.0 && activity.isBlank()

            // Respaldo: consume GET /api/Pacientes/{id} para traer los datos del paciente.
            val pacienteId = pacienteRepository.resolvePatientId(prefs)
            val paciente = if (pacienteId != null) {
                when (val result = pacienteRepository.getPaciente(pacienteId)) {
                    is Resource.Success -> result.data
                    else -> null
                }
            } else null

            val usePrefs = !prefsEmpty
            val birthFinal = if (usePrefs) birth else paciente?.fechaNacimiento.orEmpty()
            val sexFinal = if (usePrefs) sex else paciente?.sexo.orEmpty()
            val weightFinal = if (usePrefs && weight > 0.0) weight else (paciente?.pesoKg ?: 0.0)
            val heightFinal = if (usePrefs && height > 0.0) height else (paciente?.estaturaCm ?: 0.0)
            val diabeticFinal = if (usePrefs) diabetic else (paciente?.esDiabetico ?: false)
            val familyFinal = if (usePrefs) family else (paciente?.familiaresDiabetes ?: false)
            val activityFinal = if (usePrefs) activity else paciente?.actividadFisica.orEmpty()

            // Solo persiste en prefs cuando estas estaban vacías, para no pisar ediciones locales.
            if (prefsEmpty && paciente != null) {
                prefs.savePatientBiometrics(
                    birthDate = birthFinal,
                    sex = sexFinal,
                    weight = weightFinal.toString(),
                    height = heightFinal.toString(),
                    isDiabetic = diabeticFinal,
                    familyDiabetes = familyFinal,
                    activityLevel = activityFinal
                )
            }

            _uiState.update {
                it.copy(
                    paciente = paciente,
                    biometria = BiometriaPacienteState(
                        fechaNacimiento = birthFinal,
                        sexo = sexFinal,
                        pesoKg = weightFinal,
                        estaturaCm = heightFinal,
                        esDiabetico = diabeticFinal,
                        familiaresDiabetes = familyFinal,
                        actividadFisica = activityFinal
                    )
                )
            }
        }
    }

    fun updateBiometriaPaciente(
        fechaNacimiento: String,
        sexo: String,
        pesoKg: Double,
        estaturaCm: Double,
        esDiabetico: Boolean,
        familiaresDiabetes: Boolean,
        actividadFisica: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            prefs.savePatientBiometrics(
                birthDate = fechaNacimiento,
                sex = sexo,
                weight = pesoKg.toString(),
                height = estaturaCm.toString(),
                isDiabetic = esDiabetico,
                familyDiabetes = familiaresDiabetes,
                activityLevel = actividadFisica
            )

            val pacienteId = pacienteRepository.resolvePatientId(prefs)
            if (pacienteId != null) {
                when (val result = pacienteRepository.updateBiometria(
                    pacienteId, fechaNacimiento, sexo, pesoKg, estaturaCm, esDiabetico, familiaresDiabetes, actividadFisica
                )) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                successMessage = "Datos médicos del paciente actualizados con éxito",
                                biometria = BiometriaPacienteState(
                                    fechaNacimiento, sexo, pesoKg, estaturaCm, esDiabetico, familiaresDiabetes, actividadFisica
                                )
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                successMessage = "Datos médicos guardados en almacenamiento local",
                                biometria = BiometriaPacienteState(
                                    fechaNacimiento, sexo, pesoKg, estaturaCm, esDiabetico, familiaresDiabetes, actividadFisica
                                )
                            )
                        }
                    }
                    is Resource.Loading -> {}
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Perfil médico guardado",
                        biometria = BiometriaPacienteState(
                            fechaNacimiento, sexo, pesoKg, estaturaCm, esDiabetico, familiaresDiabetes, actividadFisica
                        )
                    )
                }
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

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}

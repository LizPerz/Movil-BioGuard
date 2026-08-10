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

            val pacienteId = pacienteRepository.resolvePatientId(prefs)
            val prefsEmpty = birth.isBlank() && sex.isBlank() &&
                weight <= 0.0 && height <= 0.0 && activity.isBlank()

            if (pacienteId != null) {
                val biometriaApi = pacienteRepository.getBiometria(pacienteId)
                if (biometriaApi is Resource.Success) {
                    val apiData = biometriaApi.data
                    val biometria = BiometriaPacienteState(
                        fechaNacimiento = apiData.fechaNacimiento.orEmpty(),
                        sexo = apiData.sexo.orEmpty(),
                        pesoKg = apiData.pesoKg ?: 0.0,
                        estaturaCm = apiData.estaturaCm ?: 0.0,
                        esDiabetico = apiData.esDiabetico,
                        familiaresDiabetes = apiData.familiaresDiabetes,
                        actividadFisica = apiData.actividadFisica.orEmpty()
                    )
                    prefs.savePatientBiometrics(
                        birthDate = biometria.fechaNacimiento,
                        sex = biometria.sexo,
                        weight = biometria.pesoKg.toString(),
                        height = biometria.estaturaCm.toString(),
                        isDiabetic = biometria.esDiabetico,
                        familyDiabetes = biometria.familiaresDiabetes,
                        activityLevel = biometria.actividadFisica
                    )
                    _uiState.update { it.copy(biometria = biometria) }
                } else {
                    // Respaldo: consume GET /api/Pacientes/{id} para datos del paciente y su biometría.
                    val paciente = when (val result = pacienteRepository.getPaciente(pacienteId)) {
                        is Resource.Success -> result.data
                        else -> null
                    }
                    val usePrefs = !prefsEmpty && paciente == null
                    val birthFinal = if (usePrefs) birth else paciente?.fechaNacimiento.orEmpty()
                    val sexFinal = if (usePrefs) sex else paciente?.sexo.orEmpty()
                    val weightFinal = if (usePrefs && weight > 0.0) weight else (paciente?.pesoKg ?: 0.0)
                    val heightFinal = if (usePrefs && height > 0.0) height else (paciente?.estaturaCm ?: 0.0)
                    val diabeticFinal = if (usePrefs) diabetic else (paciente?.esDiabetico ?: false)
                    val familyFinal = if (usePrefs) family else (paciente?.familiaresDiabetes ?: false)
                    val activityFinal = if (usePrefs) activity else paciente?.actividadFisica.orEmpty()

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
            } else {
                _uiState.update {
                    it.copy(
                        biometria = BiometriaPacienteState(
                            fechaNacimiento = birth,
                            sexo = sex,
                            pesoKg = weight,
                            estaturaCm = height,
                            esDiabetico = diabetic,
                            familiaresDiabetes = family,
                            actividadFisica = activity
                        )
                    )
                }
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

            val pacienteId = pacienteRepository.resolvePatientId(prefs)
            if (pacienteId != null) {
                when (val result = pacienteRepository.updateBiometria(
                    pacienteId, fechaNacimiento, sexo, pesoKg, estaturaCm, esDiabetico, familiaresDiabetes, actividadFisica
                )) {
                    is Resource.Success -> {
                        val verifyResult = pacienteRepository.getBiometria(pacienteId)
                        if (verifyResult is Resource.Success) {
                            val verified = verifyResult.data
                            val confirmedBiometria = BiometriaPacienteState(
                                fechaNacimiento = verified.fechaNacimiento ?: fechaNacimiento,
                                sexo = verified.sexo ?: sexo,
                                pesoKg = verified.pesoKg ?: pesoKg,
                                estaturaCm = verified.estaturaCm ?: estaturaCm,
                                esDiabetico = verified.esDiabetico,
                                familiaresDiabetes = verified.familiaresDiabetes,
                                actividadFisica = verified.actividadFisica ?: actividadFisica
                            )
                            prefs.savePatientBiometrics(
                                birthDate = confirmedBiometria.fechaNacimiento,
                                sex = confirmedBiometria.sexo,
                                weight = confirmedBiometria.pesoKg.toString(),
                                height = confirmedBiometria.estaturaCm.toString(),
                                isDiabetic = confirmedBiometria.esDiabetico,
                                familyDiabetes = confirmedBiometria.familiaresDiabetes,
                                activityLevel = confirmedBiometria.actividadFisica
                            )
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    successMessage = "Datos médicos actualizados y verificados en servidor",
                                    biometria = confirmedBiometria
                                )
                            }
                        } else {
                            prefs.savePatientBiometrics(
                                birthDate = fechaNacimiento,
                                sex = sexo,
                                weight = pesoKg.toString(),
                                height = estaturaCm.toString(),
                                isDiabetic = esDiabetico,
                                familyDiabetes = familiaresDiabetes,
                                activityLevel = actividadFisica
                            )
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    successMessage = "Datos guardados localmente (servidor no disponible)",
                                    biometria = BiometriaPacienteState(
                                        fechaNacimiento, sexo, pesoKg, estaturaCm, esDiabetico, familiaresDiabetes, actividadFisica
                                    )
                                )
                            }
                        }
                    }
                    is Resource.Error -> {
                        prefs.savePatientBiometrics(
                            birthDate = fechaNacimiento,
                            sex = sexo,
                            weight = pesoKg.toString(),
                            height = estaturaCm.toString(),
                            isDiabetic = esDiabetico,
                            familyDiabetes = familiaresDiabetes,
                            activityLevel = actividadFisica
                        )
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                successMessage = "Datos guardados solo localmente: ${result.message}",
                                biometria = BiometriaPacienteState(
                                    fechaNacimiento, sexo, pesoKg, estaturaCm, esDiabetico, familiaresDiabetes, actividadFisica
                                )
                            )
                        }
                    }
                    is Resource.Loading -> {}
                }
            } else {
                prefs.savePatientBiometrics(
                    birthDate = fechaNacimiento,
                    sex = sexo,
                    weight = pesoKg.toString(),
                    height = estaturaCm.toString(),
                    isDiabetic = esDiabetico,
                    familyDiabetes = familiaresDiabetes,
                    activityLevel = actividadFisica
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Perfil médico guardado localmente (sin paciente vinculado)",
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

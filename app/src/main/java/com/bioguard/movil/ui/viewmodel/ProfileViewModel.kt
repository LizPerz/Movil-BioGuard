package com.bioguard.movil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.repository.PacienteRepository
import com.bioguard.movil.data.repository.UsuarioRepository
import com.bioguard.movil.datastore.UserPreferences
import com.bioguard.movil.network.MiPlanResponse
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
    val edad: Int? = null,
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
            val pacienteId = pacienteRepository.resolvePatientId(prefs)

            if (pacienteId != null) {
                when (val result = pacienteRepository.getBiometria(pacienteId)) {
                    is Resource.Success -> {
                        val apiData = result.data
                        val biometria = BiometriaPacienteState(
                            fechaNacimiento = apiData.fechaNacimiento.orEmpty(),
                            edad = apiData.edad ?: com.bioguard.movil.data.Formatters.calculateAge(apiData.fechaNacimiento),
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
                    }
                    is Resource.Error -> {
                        val birth = prefs.patientBirthDate.first().orEmpty()
                        val sex = prefs.patientSex.first().orEmpty()
                        val weight = prefs.patientWeight.first()?.toDoubleOrNull() ?: 0.0
                        val height = prefs.patientHeight.first()?.toDoubleOrNull() ?: 0.0
                        val diabetic = prefs.patientIsDiabetic.first()
                        val family = prefs.patientFamilyDiabetes.first()
                        val activity = prefs.patientActivityLevel.first().orEmpty()
                        _uiState.update {
                            it.copy(
                                biometria = BiometriaPacienteState(
                                    fechaNacimiento = birth,
                                    edad = com.bioguard.movil.data.Formatters.calculateAge(birth),
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
                    is Resource.Loading -> {}
                }
            } else {
                val birth = prefs.patientBirthDate.first().orEmpty()
                val sex = prefs.patientSex.first().orEmpty()
                val weight = prefs.patientWeight.first()?.toDoubleOrNull() ?: 0.0
                val height = prefs.patientHeight.first()?.toDoubleOrNull() ?: 0.0
                val diabetic = prefs.patientIsDiabetic.first()
                val family = prefs.patientFamilyDiabetes.first()
                val activity = prefs.patientActivityLevel.first().orEmpty()
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
                val edad = com.bioguard.movil.data.Formatters.calculateAge(fechaNacimiento)
                when (val result = pacienteRepository.updateBiometria(
                    pacienteId, fechaNacimiento, edad ?: 0, sexo, pesoKg, estaturaCm, esDiabetico, familiaresDiabetes, actividadFisica
                )) {
                    is Resource.Success -> {
                        val verifyResult = pacienteRepository.getBiometria(pacienteId)
                        if (verifyResult is Resource.Success) {
                            val verified = verifyResult.data
                            val confirmedBiometria = BiometriaPacienteState(
                                fechaNacimiento = verified.fechaNacimiento ?: fechaNacimiento,
                                edad = verified.edad ?: edad,
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
                                        fechaNacimiento = fechaNacimiento,
                                        edad = com.bioguard.movil.data.Formatters.calculateAge(fechaNacimiento),
                                        sexo = sexo,
                                        pesoKg = pesoKg,
                                        estaturaCm = estaturaCm,
                                        esDiabetico = esDiabetico,
                                        familiaresDiabetes = familiaresDiabetes,
                                        actividadFisica = actividadFisica
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
                                            fechaNacimiento = fechaNacimiento,
                                            edad = com.bioguard.movil.data.Formatters.calculateAge(fechaNacimiento),
                                            sexo = sexo,
                                            pesoKg = pesoKg,
                                            estaturaCm = estaturaCm,
                                            esDiabetico = esDiabetico,
                                            familiaresDiabetes = familiaresDiabetes,
                                            actividadFisica = actividadFisica
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
                            fechaNacimiento = fechaNacimiento,
                            edad = com.bioguard.movil.data.Formatters.calculateAge(fechaNacimiento),
                            sexo = sexo,
                            pesoKg = pesoKg,
                            estaturaCm = estaturaCm,
                            esDiabetico = esDiabetico,
                            familiaresDiabetes = familiaresDiabetes,
                            actividadFisica = actividadFisica
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

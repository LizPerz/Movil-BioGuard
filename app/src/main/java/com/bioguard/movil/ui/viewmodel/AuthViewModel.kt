package com.bioguard.movil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.repository.AuthRepository
import com.bioguard.movil.datastore.SecureTokenStorage
import com.bioguard.movil.datastore.UserPreferences
import com.bioguard.movil.ui.model.UserRole
import com.bioguard.movil.ui.model.EffectiveAccess
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val role: UserRole? = null,
    val access: EffectiveAccess = EffectiveAccess(),
    val userName: String? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val requiresVerification: Boolean = false,
    val pendingEmail: String? = null,
    val biometriaCompletada: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    application: Application,
    private val repository: AuthRepository,
    private val prefs: UserPreferences
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        restoreSession()
    }

    private fun restoreSession() {
        viewModelScope.launch {
            val restored = repository.restoreSession()
            val role = UserRole.from(prefs.userRole.first())
            if (restored) {
                val access = repository.getEffectiveAccess(role)
                _uiState.update { it.copy(isAuthenticated = true, role = access.role, access = access, userName = prefs.userName.first(), biometriaCompletada = hasBiometria()) }
            }
        }
    }

    private suspend fun hasBiometria(): Boolean = !prefs.patientBirthDate.first().isNullOrBlank()

    fun loginWithCode(codigoAcceso: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.loginWithCode(codigoAcceso)) {
                is Resource.Success -> {
                    val access = repository.getEffectiveAccess(UserRole.from(result.data.rol))
                    _uiState.update { it.copy(isLoading = false, isAuthenticated = true, role = access.role, access = access, userName = result.data.nombre, biometriaCompletada = hasBiometria()) }
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.login(email, password)) {
                is Resource.Success -> {
                    val access = repository.getEffectiveAccess(UserRole.from(result.data.rol))
                    _uiState.update { it.copy(isLoading = false, isAuthenticated = true, role = access.role, access = access, userName = result.data.nombre, biometriaCompletada = hasBiometria()) }
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun register(
        nombre: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        correo: String,
        password: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Parse full name if apellidoPaterno is empty
            var finalNombre = nombre.trim()
            var finalApellidoPaterno = apellidoPaterno.trim()
            val finalApellidoMaterno = apellidoMaterno.trim()

            if (finalApellidoPaterno.isEmpty()) {
                val parts = finalNombre.split("\\s+".toRegex())
                if (parts.size > 1) {
                    finalNombre = parts.first()
                    finalApellidoPaterno = parts.drop(1).joinToString(" ")
                } else {
                    finalApellidoPaterno = "Sin especificar"
                }
            }

            when (val result = repository.register(finalNombre, finalApellidoPaterno, finalApellidoMaterno, correo, password)) {
                is Resource.Success -> {
                    val response = result.data
                    if (response.requiresVerification == true) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                requiresVerification = true,
                                pendingEmail = correo,
                                successMessage = response.message
                            )
                        }
                    } else {
                        when (val loginResult = repository.login(correo, password)) {
                            is Resource.Success -> {
                                val access = repository.getEffectiveAccess(UserRole.from(loginResult.data.rol))
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        isAuthenticated = true,
                                        role = access.role,
                                        access = access,
                                        userName = loginResult.data.nombre,
                                        biometriaCompletada = hasBiometria(),
                                        successMessage = "Cuenta creada. Bienvenido"
                                    )
                                }
                            }
                            is Resource.Error -> _uiState.update {
                                it.copy(isLoading = false, error = loginResult.message)
                            }
                            is Resource.Loading -> {}
                        }
                    }
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun forgotPassword(correo: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.forgotPassword(correo)) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, successMessage = result.data)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun resetPassword(token: String, correo: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.resetPassword(token, correo, newPassword)) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, successMessage = "Contrasena restablecida")
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.update { AuthUiState() }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun verificarOtp(correo: String, codigoOtp: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.verificar2fa(correo, codigoOtp)) {
                is Resource.Success -> {
                    val access = repository.getEffectiveAccess(UserRole.from(result.data.rol))
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            requiresVerification = false,
                            isAuthenticated = true,
                            role = access.role,
                            access = access,
                            userName = result.data.nombre,
                            biometriaCompletada = hasBiometria(),
                            successMessage = "Cuenta verificada exitosamente"
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

    fun clearVerificationState() {
        _uiState.update { it.copy(requiresVerification = false, pendingEmail = null) }
    }
}

package com.bioguard.movil.domain.usecase

import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.repository.AuthRepository
import com.bioguard.movil.datastore.SecureTokenStorage
import com.bioguard.movil.network.LoginWebResponse
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenStorage: SecureTokenStorage
) {
    suspend operator fun invoke(correo: String, password: String): Resource<LoginWebResponse> {
        if (correo.isBlank() || !correo.contains("@")) {
            return Resource.Error("Por favor ingresa un correo electrónico válido")
        }
        if (password.length < 6) {
            return Resource.Error("La contraseña debe tener al menos 6 caracteres")
        }
        val result = authRepository.login(correo.trim(), password)
        if (result is Resource.Success) {
            result.data.let { response ->
                tokenStorage.saveAuthToken(response.token)
                response.refreshToken?.let { tokenStorage.saveRefreshToken(it) }
            }
        }
        return result
    }
}

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Full logout: invalidates server session, clears tokens, preferences,
     * and purges offline pending data queue.
     * Delegates to AuthRepository.logout() which handles all cleanup.
     */
    suspend operator fun invoke() {
        authRepository.logout()
    }
}

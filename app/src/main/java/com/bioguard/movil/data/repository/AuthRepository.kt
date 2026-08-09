package com.bioguard.movil.data.repository

import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.local.PendingDataDao
import com.bioguard.movil.data.local.CachedDataDao
import com.bioguard.movil.data.toUserMessage
import com.bioguard.movil.datastore.SecureTokenStorage
import com.bioguard.movil.datastore.UserPreferences
import com.bioguard.movil.network.ApiService
import com.bioguard.movil.network.RetrofitClient
import com.bioguard.movil.network.LoginWebResponse
import com.bioguard.movil.network.LoginCodigoResponse
import com.bioguard.movil.network.LoginCodigoRequest
import com.bioguard.movil.network.LoginWebRequest
import com.bioguard.movil.network.RegisterWebRequest
import com.bioguard.movil.network.ForgotPasswordRequest
import com.bioguard.movil.network.ResetPasswordRequest
import com.bioguard.movil.network.CambiarPasswordRequest
import com.bioguard.movil.network.RefreshTokenRequest
import com.bioguard.movil.network.Verificar2faRequest
import com.bioguard.movil.network.MessageResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val prefs: UserPreferences,
    private val tokenStorage: SecureTokenStorage,
    private val pendingDataDao: PendingDataDao,
    private val cachedDataDao: CachedDataDao
) {

    init {
        // Los callbacks de RetrofitClient se invocan desde el thread de OkHttp (no suspendible),
        // por lo que la lectura síncrona de DataStore requiere runBlocking. Es deliberado y
        // acotado; no se ejecuta en el main thread.
        RetrofitClient.refreshTokenProvider = {
            runBlocking { tokenStorage.refreshToken.first() }
        }
        RetrofitClient.onTokenRefreshed = { access, refresh ->
            runBlocking {
                tokenStorage.saveAuthToken(access)
                tokenStorage.saveRefreshToken(refresh)
            }
        }
    }

    suspend fun login(email: String, password: String): Resource<LoginWebResponse> {
        return try {
            val response = api.loginWeb(LoginWebRequest(correo = email, password = password))
            if (response.requires2FA) {
                return Resource.Error("Se requiere verificacion en dos pasos. Revisa tu correo o usa el codigo QR")
            }
            persistSession(response)
            Resource.Success(response)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al iniciar sesion"))
        }
    }

    suspend fun loginWithCode(codigoAcceso: String): Resource<LoginCodigoResponse> {
        return try {
            val response = api.loginCodigo(LoginCodigoRequest(codigoAcceso = codigoAcceso))
            persistSession(response)
            Resource.Success(response)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al iniciar sesion con codigo"))
        }
    }

    suspend fun register(
        nombre: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        correo: String,
        password: String
    ): Resource<MessageResponse> {
        return try {
            val response = api.register(
                RegisterWebRequest(
                    nombre = nombre,
                    apellidoPaterno = apellidoPaterno,
                    apellidoMaterno = apellidoMaterno,
                    correo = correo,
                    password = password,
                    planNombre = "Gratis"
                )
            )
            Resource.Success(response)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al registrar"))
        }
    }

    private suspend fun persistSession(response: LoginWebResponse) {
        RetrofitClient.setToken(response.token)
        tokenStorage.saveAuthToken(response.token)
        response.refreshToken?.let { tokenStorage.saveRefreshToken(it) }
        prefs.saveUserData(response.userId, response.nombre, response.rol)
        if (response.rol.equals("paciente", ignoreCase = true)) {
            prefs.savePatientId(response.userId)
        }
    }

    private suspend fun persistSession(response: LoginCodigoResponse) {
        RetrofitClient.setToken(response.effectiveAccessToken)
        tokenStorage.saveAuthToken(response.effectiveAccessToken)
        tokenStorage.saveRefreshToken(response.refreshToken)
        prefs.saveUserData(response.userId, response.nombre, response.rol)
        if (response.rol.equals("paciente", ignoreCase = true)) {
            prefs.savePatientId(response.userId)
        }
    }

    suspend fun forgotPassword(correo: String): Resource<String> {
        return try {
            val response = api.forgotPassword(ForgotPasswordRequest(correo = correo))
            Resource.Success(response.message)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al enviar correo"))
        }
    }

    suspend fun resetPassword(token: String, correo: String, nuevaPassword: String): Resource<String> {
        return try {
            val response = api.resetPassword(ResetPasswordRequest(token = token, correo = correo, nuevaPassword = nuevaPassword))
            Resource.Success(response.message)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al restablecer contrasena"))
        }
    }

    suspend fun cambiarPassword(oldPassword: String, newPassword: String): Resource<String> {
        return try {
            val response = api.cambiarPassword(CambiarPasswordRequest(passwordActual = oldPassword, nuevaPassword = newPassword))
            Resource.Success(response.message)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al cambiar contrasena"))
        }
    }

    suspend fun refreshSession(): Boolean {
        val accessToken = tokenStorage.authToken.first() ?: return false
        val refreshToken = tokenStorage.refreshToken.first() ?: return false
        return try {
            val response = api.refreshToken(RefreshTokenRequest(accessToken = accessToken, refreshToken = refreshToken))
            RetrofitClient.setToken(response.accessToken)
            tokenStorage.saveAuthToken(response.accessToken)
            tokenStorage.saveRefreshToken(response.refreshToken)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun restoreSession(): Boolean {
        val token = tokenStorage.authToken.first() ?: return false
        RetrofitClient.setToken(token)
        return true
    }

    suspend fun verificar2fa(correo: String, codigoOtp: String): Resource<LoginWebResponse> {
        return try {
            val response = api.verificar2fa(Verificar2faRequest(correo = correo, codigoOtp = codigoOtp))
            persistSession(response)
            Resource.Success(response)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Código de verificación incorrecto"))
        }
    }

    suspend fun logout() {
        try {
            api.logout()
        } catch (e: Exception) {
            // Local session is cleared regardless of server response
        }
        RetrofitClient.setToken(null)
        tokenStorage.clear()
        prefs.clearAll()
        // Purga la cola offline para no dejar datos del usuario anterior
        pendingDataDao.clearReadings()
        pendingDataDao.clearGps()
        pendingDataDao.clearEvents()
        pendingDataDao.clearAlerts()
        cachedDataDao.clearAllReadings()
        cachedDataDao.clearAllEvents()
        cachedDataDao.clearAllAlerts()
    }
}

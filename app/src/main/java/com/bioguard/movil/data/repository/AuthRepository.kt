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
import com.bioguard.movil.ui.model.AppPermission
import com.bioguard.movil.ui.model.EffectiveAccess
import com.bioguard.movil.ui.model.UserRole
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
        guardAccountSwitch(response.userId)
        RetrofitClient.setToken(response.token)
        tokenStorage.saveAuthToken(response.token)
        response.refreshToken?.let { tokenStorage.saveRefreshToken(it) }
        prefs.saveUserData(response.userId, response.nombre, response.rol)
        if (response.rol.equals("paciente", ignoreCase = true)) {
            prefs.savePatientId(response.userId)
        }
    }

    private suspend fun persistSession(response: LoginCodigoResponse) {
        guardAccountSwitch(response.userId)
        RetrofitClient.setToken(response.effectiveAccessToken)
        tokenStorage.saveAuthToken(response.effectiveAccessToken)
        tokenStorage.saveRefreshToken(response.refreshToken)
        prefs.saveUserData(response.userId, response.nombre, response.rol)
        if (response.rol.equals("paciente", ignoreCase = true)) {
            prefs.savePatientId(response.userId)
        }
    }

    // Si se inicia sesión con una cuenta distinta, se descarta la biometría local de la anterior
    // y se purgan las colas/caché para no mezclar datos entre cuentas.
    private suspend fun guardAccountSwitch(newUserId: String) {
        val prevUserId = prefs.userId.first()
        if (!prevUserId.isNullOrBlank() && prevUserId != newUserId) {
            prefs.clearPatientBiometrics()
            prefs.resetOnboarding()
            pendingDataDao.clearReadings()
            pendingDataDao.clearGps()
            pendingDataDao.clearEvents()
            pendingDataDao.clearAlerts()
            cachedDataDao.clearAllReadings()
            cachedDataDao.clearAllEvents()
            cachedDataDao.clearAllAlerts()
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

    suspend fun getEffectiveAccess(fallbackRole: UserRole): EffectiveAccess {
        return try {
            val response = api.getMiAcceso()
            val role = UserRole.from(response.rol)
            val access = EffectiveAccess(
                role = role,
                patientId = response.pacienteId,
                caregiverAccessLevel = response.nivelAccesoCuidador,
                caregiverWithinPlan = response.cuidadorDentroDelPlan,
                planName = response.plan?.nombre,
                planGpsActivo = response.plan?.gpsActivo ?: false,
                permissions = response.permisos.mapNotNull { AppPermission.fromCode(it) }.toSet()
            )
            switchPatientIfNeeded(access.patientId)
            prefs.saveEffectiveAccess(
                patientId = access.patientId,
                caregiverAccessLevel = access.caregiverAccessLevel,
                planName = access.planName,
                permissionCodes = access.permissions.map { it.code }.toSet(),
                planGpsActivo = access.planGpsActivo
            )
            access
        } catch (_: Exception) {
            val cachedCodes = prefs.accessPermissions.first()
            if (cachedCodes.isNotEmpty()) {
                EffectiveAccess(
                    role = fallbackRole,
                    patientId = prefs.patientId.first(),
                    caregiverAccessLevel = prefs.caregiverAccessLevel.first(),
                    caregiverWithinPlan = true,
                    planName = prefs.planName.first(),
                    planGpsActivo = prefs.planGpsActivo.first(),
                    permissions = cachedCodes.mapNotNull { AppPermission.fromCode(it) }.toSet()
                )
            } else {
                EffectiveAccess.restricted(fallbackRole, prefs.patientId.first())
            }
        }
    }

    // Evita fugas de datos entre pacientes/cuentas: al cambiar el paciente activo se descarta
    // la biometría/foto local del anterior y se purgan sus colas y caché para que las lecturas
    // pendientes no se suban atribuidas al paciente equivocado.
    private suspend fun switchPatientIfNeeded(newPatientId: String?) {
        val prevPatientId = prefs.patientId.first()
        if (!prevPatientId.isNullOrBlank() && newPatientId != prevPatientId) {
            prefs.clearPatientBiometrics()
            pendingDataDao.clearReadings()
            pendingDataDao.clearGps()
            pendingDataDao.clearEvents()
            pendingDataDao.clearAlerts()
            cachedDataDao.clearReadings(prevPatientId)
            cachedDataDao.clearEvents(prevPatientId)
            cachedDataDao.clearAlerts(prevPatientId)
            android.util.Log.w(
                "AuthRepository",
                "Paciente activo cambio ($prevPatientId -> $newPatientId); datos locales del anterior purgados"
            )
        }
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
        // Pairing identity and UI preferences belong to this installation, not to the cloud session.
        prefs.clearSession()
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

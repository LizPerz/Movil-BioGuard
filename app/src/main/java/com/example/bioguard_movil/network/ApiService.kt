package com.example.bioguard_movil.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.*

// =============================================
// AUTH
// =============================================
data class LoginWebRequest(val correo: String, val password: String)
data class LoginCodigoRequest(val codigoAcceso: String)
data class RegisterWebRequest(
    val nombre: String,
    val apellidoPaterno: String,
    val apellidoMaterno: String,
    val correo: String,
    val password: String,
    val planNombre: String? = null
)
data class LoginWebResponse(
    val token: String,
    val refreshToken: String? = null,
    val userId: String,
    val nombre: String,
    val rol: String,
    val plan: String? = null,
    val requires2FA: Boolean = false
)
data class LoginCodigoResponse(
    val accessToken: String? = null,
    val token: String? = null,
    val refreshToken: String,
    val userId: String,
    val nombre: String,
    val rol: String
) {
    // El API desplegado responde "token"; el backend net10 alineado responde "accessToken".
    val effectiveAccessToken: String get() = accessToken ?: token.orEmpty()
}
data class RefreshTokenRequest(val accessToken: String, val refreshToken: String)
data class RefreshTokenResponse(val accessToken: String, val refreshToken: String)
data class ForgotPasswordRequest(val correo: String)
data class Enviar2faRequest(val correo: String)
data class Verificar2faRequest(
    val correo: String,
    val codigoOtp: String,
    val codigo: String? = codigoOtp
)
data class ResetPasswordRequest(val token: String, val correo: String, val nuevaPassword: String)
data class CambiarPasswordRequest(val passwordActual: String, val nuevaPassword: String)

// =============================================
// PACIENTES
// =============================================
data class PacienteResumen(
    val id: String,
    val nombre: String,
    val esDiabetico: Boolean = false,
    val perfilCompletado: Boolean = false
)
data class CrearPacienteRequest(val nombre: String, val esDiabetico: Boolean = false)
data class CrearPacienteResponse(val pacienteId: String, val codigoAccesoQr: String, val message: String)
data class ActualizarBiometriaRequest(
    val fechaNacimiento: String,
    val sexo: String,
    val pesoKg: Double,
    val estaturaCm: Double,
    val esDiabetico: Boolean = false,
    val familiaresDiabetes: Boolean = false,
    val actividadFisica: String
)
data class TrackingResponse(val longitud: Double, val latitud: Double, val timestamp: String, val esEmergencia: Boolean = false)
data class DispositivoEstado(val vinculado: Boolean = false, val nombreDispositivo: String? = null, val macAddress: String? = null, val conectado: Boolean = false)
data class DashboardSummary(
    val paciente: PacienteResumen? = null,
    val ultimaLectura: LecturaSensorResponse? = null,
    val ultimaUbicacion: TrackingResponse? = null,
    val dispositivo: DispositivoEstado? = null,
    val alertasPendientesCount: Int = 0,
    val alertasRecientes: List<AlertaResponse> = emptyList(),
    val eventosRecientes: List<EventoMetabolicoResponse> = emptyList()
)

// =============================================
// SENSORES
// =============================================
data class LecturaSensorRequest(
    val pulsoBpm: Double,
    val temperaturaC: Double,
    val sudoracionGsr: Double,
    val hrv: Double? = null,
    val spo2: Double? = null,
    val timestamp: String
)
data class LecturaSensorResponse(
    val id: String? = null,
    val timestamp: String,
    val pulsoBpm: Double,
    val temperaturaC: Double,
    val sudoracionGsr: Double,
    val hrv: Double? = null,
    val spo2: Double? = null,
    val probabilidadPico: Double? = null,
    val nivelRiesgo: String? = null
)
data class CrearEventoRequest(
    val pacienteId: String,
    val dispositivoMac: String,
    val nivelRiesgo: String,
    val probabilidadMl: Double,
    val descripcion: String
)
data class EventoMetabolicoResponse(
    val id: String? = null,
    val nivelRiesgo: String? = null,
    val probabilidadMl: Double = 0.0,
    val descripcion: String? = null,
    val fechaEvento: String? = null,
    val atendida: Boolean = false
)
data class AtenderEventoRequest(val cuidadorId: String, val notasAtencion: String? = null)
data class TrackingGpsRequest(val latitud: Double, val longitud: Double, val esEmergencia: Boolean = false)

// =============================================
// USUARIOS WEB
// =============================================
data class UsuarioWebResponse(
    val id: String,
    val nombre: String,
    val correo: String,
    val fechaRegistro: String,
    val planId: String? = null,
    val planNombre: String? = null
)
data class UpdatePerfilRequest(val nombre: String?, val apellidoPaterno: String?, val apellidoMaterno: String?)
data class UpdateCorreoRequest(val nuevoCorreo: String, val passwordActual: String)
data class MiPlanResponse(
    val planId: String? = null,
    val nombre: String? = null,
    val limitePacientes: Int = 0,
    val limiteCuidadores: Int = 0,
    val retencionHistorialDias: Int = 0,
    val gpsActivo: Boolean = false,
    val consolaIaActiva: Boolean = false
)
data class SesionResponse(
    val id: String? = null,
    val dispositivo: String? = null,
    val ip: String? = null,
    val ultimaActividad: String? = null,
    val esActual: Boolean = false
)

// =============================================
// CUIDADORES
// =============================================
data class CuidadorResponse(
    val id: String? = null,
    val pacienteId: String? = null,
    val nombre: String? = null,
    val parentesco: String? = null,
    val telefono: String? = null,
    val correo: String? = null,
    val nivelAcceso: String? = null,
    val codigoAccesoQr: String? = null,
    val fechaRegistro: String? = null
)
data class CrearCuidadorRequest(
    val pacienteId: String,
    val nombre: String,
    val parentesco: String,
    val telefono: String,
    val correo: String,
    val nivelAcceso: String
)
data class CrearCuidadorResponse(val cuidadorId: String, val codigoAccesoQr: String, val message: String)

// =============================================
// DISPOSITIVOS
// =============================================
data class VincularDispositivoRequest(val nombre: String, val macAddress: String)
data class VincularDispositivoResponse(val dispositivoId: String, val message: String)
data class DispositivoRelojInfo(
    val modelo: String? = null,
    val conectado: Boolean = false,
    val bateria: Int? = null,
    val ultimaSincronizacion: String? = null,
    val sensoresDisponibles: List<String>? = null
)
data class DispositivoTelefonoInfo(
    val modelo: String? = null,
    val sistemaOperativo: String? = null,
    val bateria: Int? = null,
    val ahorroEnergia: Boolean = false,
    val conectividad: String? = null
)
data class InfoCompletaDispositivo(val reloj: DispositivoRelojInfo? = null, val telefono: DispositivoTelefonoInfo? = null)

// =============================================
// NOTIFICACIONES
// =============================================
data class NotificacionResponse(val id: String, val titulo: String, val mensaje: String, val leida: Boolean = false, val fechaEnvio: String)

// =============================================
// MEDICAMENTOS
// =============================================
data class MedicamentoResponse(
    val id: String,
    val pacienteId: String? = null,
    val nombre: String,
    val dosis: String,
    @SerializedName("horario") val frecuencia: String,
    val notas: String? = null,
    val activo: Boolean = true,
    val fechaCreacion: String? = null,
    val ultimaToma: String? = null
)
data class CrearMedicamentoRequest(val pacienteId: String, val nombre: String, val dosis: String, val horario: String, val notas: String? = null)
data class CrearMedicamentoResponse(val medicamentoId: String, val message: String)
data class ActualizarMedicamentoRequest(val nombre: String, val dosis: String, val horario: String, val notas: String? = null)

// =============================================
// ALERTAS
// =============================================
data class AlertaResponse(
    val id: String,
    val tipoAlerta: String = "",
    val descripcion: String = "",
    val atendida: Boolean = false,
    val fechaCreacion: String = "",
    val fechaAtencion: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null
)
data class CrearAlertaRequest(val pacienteId: String, val tipoAlerta: String, val descripcion: String, val latitud: Double? = null, val longitud: Double? = null)
data class CrearAlertaResponse(val alertaId: String, val message: String)
data class AtenderAlertaRequest(val notasAtencion: String)

// =============================================
// ML
// =============================================
data class PrediccionResponse(
    val id: String? = null,
    val probabilidad: Double,
    val nivelRiesgo: String,
    val recomendacion: String? = null,
    val fechaPrediccion: String,
    val horasEstimadas: Double? = null,
    val modeloVersion: String? = null
)
data class DiagnosticarRequest(val pacienteId: String, val pulsoBpm: Double, val temperaturaC: Double, val sudoracionGsr: Double)
data class DiagnosticarResponse(
    val pacienteId: String,
    val nivelRiesgo: String,
    val probabilidad: Double,
    val recomendacion: String? = null,
    val horasEstimadas: Double? = null,
    val fechaPrediccion: String? = null,
    val modeloVersion: String? = null
)

// =============================================
// REPORTES
// =============================================
data class ReporteResumenResponse(
    val totalLecturas: Int = 0,
    val totalEventos: Int = 0,
    val totalAlertas: Int = 0,
    val totalMedicamentos: Int = 0,
    val eventosCriticos: Int = 0,
    val alertasPendientes: Int = 0,
    val promedioPulso: Double? = null,
    val ultimaLectura: String? = null
)

// =============================================
// GENERIC
// =============================================
data class MessageResponse(
    val message: String,
    val requiresVerification: Boolean? = null,
    val userId: String? = null,
    val correo: String? = null
)
data class RegisterFcmTokenRequest(val token: String)
data class UpdateNivelAccesoRequest(val nivelAcceso: String)
data class UpdateFotoRequest(val fotoUrl: String)
data class HeartbeatRequest(
    val pacienteId: String? = null,
    val bateria: Int? = null,
    val sensoresActivos: List<String>? = null
)

data class CrearTicketRequest(
    val asunto: String,
    val descripcion: String,
    val categoria: String? = null,
    val prioridad: String? = null
)

data class TicketResponse(
    val id: String,
    val asunto: String,
    val descripcion: String,
    val categoria: String,
    val prioridad: String,
    val estado: String,
    val fechaCreacion: String,
    val fechaActualizacion: String
)

interface ApiService {

    // =============================================
    // AUTH
    // =============================================
    @POST("api/Auth/register")
    suspend fun register(@Body request: RegisterWebRequest): MessageResponse

    @POST("api/Auth/login-web")
    suspend fun loginWeb(@Body request: LoginWebRequest): LoginWebResponse

    @POST("api/Auth/login-codigo")
    suspend fun loginCodigo(@Body request: LoginCodigoRequest): LoginCodigoResponse

    @POST("api/Auth/2FA/enviar")
    suspend fun enviar2fa(@Body request: Enviar2faRequest): MessageResponse

    @POST("api/Auth/2FA/verificar")
    suspend fun verificar2fa(@Body request: Verificar2faRequest): LoginWebResponse

    @POST("api/Auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): RefreshTokenResponse

    @POST("api/Auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): MessageResponse

    @POST("api/Auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): MessageResponse

    @PUT("api/Auth/cambiar-password")
    suspend fun cambiarPassword(@Body request: CambiarPasswordRequest): MessageResponse

    @POST("api/Auth/logout")
    suspend fun logout(): MessageResponse

    // =============================================
    // PACIENTES
    // =============================================
    @POST("api/Pacientes")
    suspend fun crearPaciente(@Body request: CrearPacienteRequest): CrearPacienteResponse

    @PUT("api/Pacientes/{id}/biometria")
    suspend fun updateBiometria(@Path("id") id: String, @Body request: ActualizarBiometriaRequest): MessageResponse

    @GET("api/Pacientes/{id}/dashboard-summary")
    suspend fun getDashboardSummary(@Path("id") id: String): DashboardSummary

    // =============================================
    // SENSORES
    // =============================================
    @POST("api/Sensores/lecturas")
    suspend fun sendLecturas(@Body requests: List<LecturaSensorRequest>): MessageResponse

    @GET("api/Sensores/lecturas/{pacienteId}")
    suspend fun getLecturas(@Path("pacienteId") pacienteId: String, @Query("limite") limite: Int = 100): List<LecturaSensorResponse>

    @POST("api/Sensores/evento")
    suspend fun sendEvento(@Body request: CrearEventoRequest): MessageResponse

    @GET("api/Sensores/eventos/{pacienteId}")
    suspend fun getEventos(@Path("pacienteId") pacienteId: String, @Query("limite") limite: Int = 50): List<EventoMetabolicoResponse>

    @PUT("api/Sensores/eventos/{eventoId}/atender")
    suspend fun atenderEvento(@Path("eventoId") eventoId: String, @Body request: AtenderEventoRequest): MessageResponse

    @POST("api/Sensores/tracking")
    suspend fun sendTracking(@Body request: TrackingGpsRequest): MessageResponse

    @GET("api/Sensores/tracking/{pacienteId}/actual")
    suspend fun getTrackingActual(@Path("pacienteId") pacienteId: String): TrackingResponse

    @GET("api/Sensores/tracking/{pacienteId}/ruta")
    suspend fun getTrackingRuta(
        @Path("pacienteId") pacienteId: String,
        @Query("desde") desde: String,
        @Query("hasta") hasta: String
    ): List<TrackingResponse>

    // =============================================
    // USUARIOS WEB
    // =============================================
    @GET("api/UsuariosWeb/mi-perfil")
    suspend fun getMiPerfil(): UsuarioWebResponse

    @PUT("api/UsuariosWeb/mi-perfil")
    suspend fun updateMiPerfil(@Body request: UpdatePerfilRequest): MessageResponse

    @PUT("api/UsuariosWeb/mi-perfil/correo")
    suspend fun updateMiCorreo(@Body request: UpdateCorreoRequest): MessageResponse

    @GET("api/UsuariosWeb/mi-plan")
    suspend fun getMiPlan(): MiPlanResponse

    @GET("api/UsuariosWeb/mis-sesiones")
    suspend fun getMisSesiones(): List<SesionResponse>

    // =============================================
    // CUIDADORES
    // =============================================
    @GET("api/Cuidadores")
    suspend fun getCuidadores(): List<CuidadorResponse>

    @GET("api/Cuidadores/{id}")
    suspend fun getCuidadorById(@Path("id") id: String): CuidadorResponse

    @POST("api/Cuidadores")
    suspend fun crearCuidador(@Body request: CrearCuidadorRequest): CrearCuidadorResponse

    @DELETE("api/Cuidadores/{id}")
    suspend fun deleteCuidador(@Path("id") id: String)

    // =============================================
    // DISPOSITIVOS
    // =============================================
    @POST("api/Dispositivos/vincular")
    suspend fun vincularDispositivo(@Body request: VincularDispositivoRequest): VincularDispositivoResponse

    @GET("api/Dispositivos/{pacienteId}/info-completa")
    suspend fun getInfoCompleta(@Path("pacienteId") pacienteId: String): InfoCompletaDispositivo

    // =============================================
    // NOTIFICACIONES
    // =============================================
    @GET("api/Notificaciones/by-paciente/{pacienteId}")
    suspend fun getNotificaciones(@Path("pacienteId") pacienteId: String): List<NotificacionResponse>

    @PUT("api/Notificaciones/{id}/leer")
    suspend fun markNotificacionLeida(@Path("id") id: String): MessageResponse

    // =============================================
    // MEDICAMENTOS
    // =============================================
    @GET("api/Medicamentos/by-paciente/{pacienteId}")
    suspend fun getMedicamentos(@Path("pacienteId") pacienteId: String): List<MedicamentoResponse>

    @POST("api/Medicamentos")
    suspend fun crearMedicamento(@Body request: CrearMedicamentoRequest): CrearMedicamentoResponse

    @PUT("api/Medicamentos/{id}")
    suspend fun actualizarMedicamento(@Path("id") id: String, @Body request: ActualizarMedicamentoRequest): MessageResponse

    @PUT("api/Medicamentos/{id}/toma")
    suspend fun registrarToma(@Path("id") id: String): MessageResponse

    @DELETE("api/Medicamentos/{id}")
    suspend fun deleteMedicamento(@Path("id") id: String)

    // =============================================
    // ALERTAS
    // =============================================
    @POST("api/Alertas")
    suspend fun crearAlerta(@Body request: CrearAlertaRequest): CrearAlertaResponse

    @POST("api/Alertas/{id}/atender")
    suspend fun atenderAlerta(@Path("id") id: String, @Body request: AtenderAlertaRequest): MessageResponse

    // =============================================
    // ML
    // =============================================
    @GET("api/ML/predicciones/{pacienteId}")
    suspend fun getPredicciones(@Path("pacienteId") pacienteId: String): List<PrediccionResponse>

    @GET("api/ML/predicciones/{pacienteId}/actual")
    suspend fun getPrediccionActual(@Path("pacienteId") pacienteId: String): PrediccionResponse

    @POST("api/ML/diagnosticar")
    suspend fun diagnosticar(@Body request: DiagnosticarRequest): DiagnosticarResponse

    // =============================================
    // REPORTES
    // =============================================
    @GET("api/Reportes/resumen/{pacienteId}")
    suspend fun getReporteResumen(@Path("pacienteId") pacienteId: String): ReporteResumenResponse

    // =============================================
    // ADAPTACIONES PDF (PUNTOS FALTANTES)
    // =============================================
    @POST("api/Dispositivos/heartbeat")
    suspend fun sendDeviceHeartbeat(@Body request: HeartbeatRequest? = null): MessageResponse

    @POST("api/Sensores/lectura-batch")
    suspend fun sendLecturasBatch(@Body requests: List<LecturaSensorRequest>): MessageResponse

    @POST("api/Sensores/tracking-batch")
    suspend fun sendTrackingBatch(@Body requests: List<TrackingGpsRequest>): MessageResponse

    @POST("api/Notificaciones/fcm/registrar-token")
    suspend fun registerFcmToken(@Body request: RegisterFcmTokenRequest): MessageResponse

    @DELETE("api/Notificaciones/fcm/token/{token}")
    suspend fun deleteFcmToken(@Path("token") token: String): MessageResponse

    @DELETE("api/UsuariosWeb/mis-sesiones/{id}")
    suspend fun deleteSesion(@Path("id") id: String): MessageResponse

    @PATCH("api/Cuidadores/{id}/nivel-acceso")
    suspend fun updateNivelAcceso(@Path("id") id: String, @Body request: UpdateNivelAccesoRequest): MessageResponse

    @PUT("api/UsuariosWeb/mi-perfil/foto")
    suspend fun updateFotoPerfil(@Body request: UpdateFotoRequest): MessageResponse

    @POST("api/Pagos/cancelar")
    suspend fun cancelarSuscripcion(): MessageResponse

    @POST("api/Tickets")
    suspend fun crearTicket(@Body request: CrearTicketRequest): MessageResponse

    @GET("api/Tickets/mis-tickets")
    suspend fun listarMisTickets(): List<TicketResponse>
}

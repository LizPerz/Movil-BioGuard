package com.bioguard.movil.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.*

// =============================================
// AUTH
// =============================================
data class LoginWebRequest(@SerializedName("correo") val correo: String, @SerializedName("password") val password: String)
data class LoginCodigoRequest(@SerializedName("codigoAcceso") val codigoAcceso: String)
data class RegisterWebRequest(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("apellidoPaterno") val apellidoPaterno: String,
    @SerializedName("apellidoMaterno") val apellidoMaterno: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("password") val password: String,
    @SerializedName("planNombre") val planNombre: String? = null
)
data class LoginWebResponse(
    @SerializedName("token") val token: String,
    @SerializedName("refreshToken") val refreshToken: String? = null,
    @SerializedName("userId") val userId: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("rol") val rol: String,
    @SerializedName("plan") val plan: String? = null,
    @SerializedName("requires2FA") val requires2FA: Boolean = false
)
data class LoginGoogleRequest(@SerializedName("idToken") val idToken: String)
data class LoginCodigoResponse(
    @SerializedName("accessToken") val accessToken: String? = null,
    @SerializedName("token") val token: String? = null,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("rol") val rol: String
) {
    // El API desplegado responde "token"; el backend net10 alineado responde "accessToken".
    val effectiveAccessToken: String get() = accessToken ?: token.orEmpty()
}
data class RefreshTokenRequest(@SerializedName("accessToken") val accessToken: String, @SerializedName("refreshToken") val refreshToken: String)
data class RefreshTokenResponse(@SerializedName("accessToken") val accessToken: String, @SerializedName("refreshToken") val refreshToken: String)
data class ForgotPasswordRequest(@SerializedName("correo") val correo: String)
data class Enviar2faRequest(@SerializedName("correo") val correo: String)
data class Verificar2faRequest(
    @SerializedName("correo") val correo: String,
    @SerializedName("codigoOtp") val codigoOtp: String,
    @SerializedName("codigo") val codigo: String? = codigoOtp
)
data class ResetPasswordRequest(@SerializedName("token") val token: String, @SerializedName("correo") val correo: String, @SerializedName("nuevaPassword") val nuevaPassword: String)
data class CambiarPasswordRequest(@SerializedName("passwordActual") val passwordActual: String, @SerializedName("nuevaPassword") val nuevaPassword: String)

// =============================================
// PACIENTES
// =============================================
data class PacienteResumen(
    @SerializedName("id") val id: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("esDiabetico") val esDiabetico: Boolean = false,
    @SerializedName("perfilCompletado") val perfilCompletado: Boolean = false
)
data class CrearPacienteRequest(@SerializedName("nombre") val nombre: String, @SerializedName("esDiabetico") val esDiabetico: Boolean = false)
data class CrearPacienteResponse(@SerializedName("pacienteId") val pacienteId: String, @SerializedName("codigoAccesoQr") val codigoAccesoQr: String, @SerializedName("message") val message: String)
data class ActualizarBiometriaRequest(
    @SerializedName("fechaNacimiento") val fechaNacimiento: String,
    @SerializedName("sexo") val sexo: String,
    @SerializedName("pesoKg") val pesoKg: Double,
    @SerializedName("estaturaCm") val estaturaCm: Double,
    @SerializedName("esDiabetico") val esDiabetico: Boolean = false,
    @SerializedName("familiaresDiabetes") val familiaresDiabetes: Boolean = false,
    @SerializedName("actividadFisica") val actividadFisica: String
)
data class TrackingResponse(@SerializedName("longitud") val longitud: Double, @SerializedName("latitud") val latitud: Double, @SerializedName("timestamp") val timestamp: String, @SerializedName("esEmergencia") val esEmergencia: Boolean = false)
data class DispositivoEstado(@SerializedName("vinculado") val vinculado: Boolean = false, @SerializedName("nombreDispositivo") val nombreDispositivo: String? = null, @SerializedName("macAddress") val macAddress: String? = null, @SerializedName("conectado") val conectado: Boolean = false)
data class DashboardSummary(
    @SerializedName("paciente") val paciente: PacienteResumen? = null,
    @SerializedName("ultimaLectura") val ultimaLectura: LecturaSensorResponse? = null,
    @SerializedName("ultimaUbicacion") val ultimaUbicacion: TrackingResponse? = null,
    @SerializedName("dispositivo") val dispositivo: DispositivoEstado? = null,
    @SerializedName("alertasPendientesCount") val alertasPendientesCount: Int = 0,
    @SerializedName("alertasRecientes") val alertasRecientes: List<AlertaResponse> = emptyList(),
    @SerializedName("eventosRecientes") val eventosRecientes: List<EventoMetabolicoResponse> = emptyList()
)

// =============================================
// SENSORES
// =============================================
data class LecturaSensorRequest(
    @SerializedName("pulsoBpm") val pulsoBpm: Double,
    @SerializedName("temperaturaC") val temperaturaC: Double,
    @SerializedName("sudoracionGsr") val sudoracionGsr: Double,
    @SerializedName("hrv") val hrv: Double? = null,
    @SerializedName("spo2") val spo2: Double? = null,
    @SerializedName("timestamp") val timestamp: String
)
data class LecturaSensorResponse(
    @SerializedName("id") val id: String? = null,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("pulsoBpm") val pulsoBpm: Double,
    @SerializedName("temperaturaC") val temperaturaC: Double,
    @SerializedName("sudoracionGsr") val sudoracionGsr: Double,
    @SerializedName("hrv") val hrv: Double? = null,
    @SerializedName("spo2") val spo2: Double? = null,
    @SerializedName("probabilidadPico") val probabilidadPico: Double? = null,
    @SerializedName("nivelRiesgo") val nivelRiesgo: String? = null
)
data class CrearEventoRequest(
    @SerializedName("pacienteId") val pacienteId: String,
    @SerializedName("dispositivoMac") val dispositivoMac: String,
    @SerializedName("nivelRiesgo") val nivelRiesgo: String,
    @SerializedName("probabilidadMl") val probabilidadMl: Double,
    @SerializedName("descripcion") val descripcion: String
)
data class EventoMetabolicoResponse(
    @SerializedName("id") val id: String? = null,
    @SerializedName("nivelRiesgo") val nivelRiesgo: String? = null,
    @SerializedName("probabilidadMl") val probabilidadMl: Double = 0.0,
    @SerializedName("descripcion") val descripcion: String? = null,
    @SerializedName("fechaEvento") val fechaEvento: String? = null,
    @SerializedName("atendida") val atendida: Boolean = false
)
data class AtenderEventoRequest(@SerializedName("cuidadorId") val cuidadorId: String, @SerializedName("notasAtencion") val notasAtencion: String? = null)
data class TrackingGpsRequest(@SerializedName("latitud") val latitud: Double, @SerializedName("longitud") val longitud: Double, @SerializedName("esEmergencia") val esEmergencia: Boolean = false)

// =============================================
// USUARIOS WEB
// =============================================
data class UsuarioWebResponse(
    @SerializedName("id") val id: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("fechaRegistro") val fechaRegistro: String,
    @SerializedName("planId") val planId: String? = null,
    @SerializedName("planNombre") val planNombre: String? = null
)
data class UpdatePerfilRequest(@SerializedName("nombre") val nombre: String?, @SerializedName("apellidoPaterno") val apellidoPaterno: String?, @SerializedName("apellidoMaterno") val apellidoMaterno: String?)
data class UpdateCorreoRequest(@SerializedName("nuevoCorreo") val nuevoCorreo: String, @SerializedName("passwordActual") val passwordActual: String)
data class MiPlanResponse(
    @SerializedName("planId") val planId: String? = null,
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("limitePacientes") val limitePacientes: Int = 0,
    @SerializedName("limiteCuidadores") val limiteCuidadores: Int = 0,
    @SerializedName("retencionHistorialDias") val retencionHistorialDias: Int = 0,
    @SerializedName("gpsActivo") val gpsActivo: Boolean = false,
    @SerializedName("consolaIaActiva") val consolaIaActiva: Boolean = false
)
data class SesionResponse(
    @SerializedName("id") val id: String? = null,
    @SerializedName("dispositivo") val dispositivo: String? = null,
    @SerializedName("ip") val ip: String? = null,
    @SerializedName("ultimaActividad") val ultimaActividad: String? = null,
    @SerializedName("esActual") val esActual: Boolean = false
)

// =============================================
// CUIDADORES
// =============================================
data class CuidadorResponse(
    @SerializedName("id") val id: String? = null,
    @SerializedName("pacienteId") val pacienteId: String? = null,
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("parentesco") val parentesco: String? = null,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("correo") val correo: String? = null,
    @SerializedName("nivelAcceso") val nivelAcceso: String? = null,
    @SerializedName("codigoAccesoQr") val codigoAccesoQr: String? = null,
    @SerializedName("fechaRegistro") val fechaRegistro: String? = null
)
data class CrearCuidadorRequest(
    @SerializedName("pacienteId") val pacienteId: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("parentesco") val parentesco: String,
    @SerializedName("telefono") val telefono: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("nivelAcceso") val nivelAcceso: String
)
data class CrearCuidadorResponse(@SerializedName("cuidadorId") val cuidadorId: String, @SerializedName("codigoAccesoQr") val codigoAccesoQr: String, @SerializedName("message") val message: String)

// =============================================
// DISPOSITIVOS
// =============================================
data class VincularDispositivoRequest(@SerializedName("nombre") val nombre: String, @SerializedName("macAddress") val macAddress: String)
data class VincularDispositivoResponse(@SerializedName("dispositivoId") val dispositivoId: String, @SerializedName("message") val message: String)
data class DispositivoRelojInfo(
    @SerializedName("modelo") val modelo: String? = null,
    @SerializedName("conectado") val conectado: Boolean = false,
    @SerializedName("bateria") val bateria: Int? = null,
    @SerializedName("ultimaSincronizacion") val ultimaSincronizacion: String? = null,
    @SerializedName("sensoresDisponibles") val sensoresDisponibles: List<String>? = null
)
data class DispositivoTelefonoInfo(
    @SerializedName("modelo") val modelo: String? = null,
    @SerializedName("sistemaOperativo") val sistemaOperativo: String? = null,
    @SerializedName("bateria") val bateria: Int? = null,
    @SerializedName("ahorroEnergia") val ahorroEnergia: Boolean = false,
    @SerializedName("conectividad") val conectividad: String? = null
)
data class InfoCompletaDispositivo(@SerializedName("reloj") val reloj: DispositivoRelojInfo? = null, @SerializedName("telefono") val telefono: DispositivoTelefonoInfo? = null)

// =============================================
// NOTIFICACIONES
// =============================================
data class NotificacionResponse(@SerializedName("id") val id: String, @SerializedName("titulo") val titulo: String, @SerializedName("mensaje") val mensaje: String, @SerializedName("leida") val leida: Boolean = false, @SerializedName("fechaEnvio") val fechaEnvio: String)

// =============================================
// MEDICAMENTOS
// =============================================
data class MedicamentoResponse(
    @SerializedName("id") val id: String,
    @SerializedName("pacienteId") val pacienteId: String? = null,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("dosis") val dosis: String,
    @SerializedName("horario") val frecuencia: String,
    @SerializedName("notas") val notas: String? = null,
    @SerializedName("activo") val activo: Boolean = true,
    @SerializedName("fechaCreacion") val fechaCreacion: String? = null,
    @SerializedName("ultimaToma") val ultimaToma: String? = null
)
data class CrearMedicamentoRequest(@SerializedName("pacienteId") val pacienteId: String, @SerializedName("nombre") val nombre: String, @SerializedName("dosis") val dosis: String, @SerializedName("horario") val horario: String, @SerializedName("notas") val notas: String? = null)
data class CrearMedicamentoResponse(@SerializedName("medicamentoId") val medicamentoId: String, @SerializedName("message") val message: String)
data class ActualizarMedicamentoRequest(@SerializedName("nombre") val nombre: String, @SerializedName("dosis") val dosis: String, @SerializedName("horario") val horario: String, @SerializedName("notas") val notas: String? = null)

// =============================================
// ALERTAS
// =============================================
data class AlertaResponse(
    @SerializedName("id") val id: String,
    @SerializedName("tipoAlerta") val tipoAlerta: String = "",
    @SerializedName("descripcion") val descripcion: String = "",
    @SerializedName("atendida") val atendida: Boolean = false,
    @SerializedName("fechaCreacion") val fechaCreacion: String = "",
    @SerializedName("fechaAtencion") val fechaAtencion: String? = null,
    @SerializedName("latitud") val latitud: Double? = null,
    @SerializedName("longitud") val longitud: Double? = null
)
data class CrearAlertaRequest(@SerializedName("pacienteId") val pacienteId: String, @SerializedName("tipoAlerta") val tipoAlerta: String, @SerializedName("descripcion") val descripcion: String, @SerializedName("latitud") val latitud: Double? = null, @SerializedName("longitud") val longitud: Double? = null)
data class CrearAlertaResponse(@SerializedName("alertaId") val alertaId: String, @SerializedName("message") val message: String)
data class AtenderAlertaRequest(@SerializedName("notasAtencion") val notasAtencion: String)

// =============================================
// ML
// =============================================
data class PrediccionResponse(
    @SerializedName("id") val id: String? = null,
    @SerializedName("probabilidad") val probabilidad: Double,
    @SerializedName("nivelRiesgo") val nivelRiesgo: String,
    @SerializedName("recomendacion") val recomendacion: String? = null,
    @SerializedName("fechaPrediccion") val fechaPrediccion: String,
    @SerializedName("horasEstimadas") val horasEstimadas: Double? = null,
    @SerializedName("modeloVersion") val modeloVersion: String? = null
)
data class DiagnosticarRequest(@SerializedName("pacienteId") val pacienteId: String, @SerializedName("pulsoBpm") val pulsoBpm: Double, @SerializedName("temperaturaC") val temperaturaC: Double, @SerializedName("sudoracionGsr") val sudoracionGsr: Double)
data class DiagnosticarResponse(
    @SerializedName("pacienteId") val pacienteId: String,
    @SerializedName("nivelRiesgo") val nivelRiesgo: String,
    @SerializedName("probabilidad") val probabilidad: Double,
    @SerializedName("recomendacion") val recomendacion: String? = null,
    @SerializedName("horasEstimadas") val horasEstimadas: Double? = null,
    @SerializedName("fechaPrediccion") val fechaPrediccion: String? = null,
    @SerializedName("modeloVersion") val modeloVersion: String? = null
)

// =============================================
// REPORTES
// =============================================
data class ReporteResumenResponse(
    @SerializedName("totalLecturas") val totalLecturas: Int = 0,
    @SerializedName("totalEventos") val totalEventos: Int = 0,
    @SerializedName("totalAlertas") val totalAlertas: Int = 0,
    @SerializedName("totalMedicamentos") val totalMedicamentos: Int = 0,
    @SerializedName("eventosCriticos") val eventosCriticos: Int = 0,
    @SerializedName("alertasPendientes") val alertasPendientes: Int = 0,
    @SerializedName("promedioPulso") val promedioPulso: Double? = null,
    @SerializedName("ultimaLectura") val ultimaLectura: String? = null
)

// =============================================
// GENERIC
// =============================================
data class MessageResponse(
    @SerializedName("message") val message: String,
    @SerializedName("requiresVerification") val requiresVerification: Boolean? = null,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("correo") val correo: String? = null
)
data class RegisterFcmTokenRequest(@SerializedName("token") val token: String)
data class UpdateNivelAccesoRequest(@SerializedName("nivelAcceso") val nivelAcceso: String)
data class UpdateFotoRequest(@SerializedName("fotoUrl") val fotoUrl: String)
data class HeartbeatRequest(
    @SerializedName("pacienteId") val pacienteId: String? = null,
    @SerializedName("bateria") val bateria: Int? = null,
    @SerializedName("sensoresActivos") val sensoresActivos: List<String>? = null
)

data class CrearTicketRequest(
    @SerializedName("asunto") val asunto: String,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("categoria") val categoria: String? = null,
    @SerializedName("prioridad") val prioridad: String? = null
)

data class TicketResponse(
    @SerializedName("id") val id: String,
    @SerializedName("asunto") val asunto: String,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("categoria") val categoria: String,
    @SerializedName("prioridad") val prioridad: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("fechaCreacion") val fechaCreacion: String,
    @SerializedName("fechaActualizacion") val fechaActualizacion: String
)

data class CrearSesionPagoRequest(
    @SerializedName("planNombre") val planNombre: String,
    @SerializedName("procesador") val procesador: String
)

data class CrearSesionPagoResponse(
    @SerializedName("pagoId") val pagoId: String,
    @SerializedName("monto") val monto: Double,
    @SerializedName("moneda") val moneda: String,
    @SerializedName("sesionUrl") val sesionUrl: String
)

data class HistorialPagoResponse(
    @SerializedName("id") val id: String,
    @SerializedName("monto") val monto: Double,
    @SerializedName("moneda") val moneda: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("fechaPago") val fechaPago: String,
    @SerializedName("metodoPago") val metodoPago: String
)

interface ApiService {

    // =============================================
    // AUTH
    // =============================================
    @POST("api/Auth/register")
    suspend fun register(@Body request: RegisterWebRequest): MessageResponse

    @POST("api/Auth/login-web")
    suspend fun loginWeb(@Body request: LoginWebRequest): LoginWebResponse

    @POST("api/Auth/login-google")
    suspend fun loginGoogle(@Body request: LoginGoogleRequest): LoginWebResponse

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

    @POST("api/Pagos/crear-sesion")
    suspend fun crearSesionPago(@Body request: CrearSesionPagoRequest): CrearSesionPagoResponse

    @GET("api/Pagos/historial")
    suspend fun getHistorialPagos(): List<HistorialPagoResponse>

    @POST("api/Tickets")
    suspend fun crearTicket(@Body request: CrearTicketRequest): MessageResponse

    @GET("api/Tickets/mis-tickets")
    suspend fun listarMisTickets(): List<TicketResponse>
}

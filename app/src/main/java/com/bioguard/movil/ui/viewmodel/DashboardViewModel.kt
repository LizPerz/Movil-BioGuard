package com.bioguard.movil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.local.CachedDataDao
import com.bioguard.movil.data.local.CachedReadingEntity
import com.bioguard.movil.data.repository.MlRepository
import com.bioguard.movil.data.repository.PacienteRepository
import com.bioguard.movil.data.repository.PredictionMlSyncRepository
import com.bioguard.movil.data.repository.SensorRepository
import com.bioguard.movil.datastore.UserPreferences
import com.bioguard.movil.ml.GlycemicPeakPredictor
import com.bioguard.movil.ml.GlycemicPrediction
import com.bioguard.movil.network.DashboardSummary
import com.bioguard.movil.network.GuardarPrediccionRequest
import com.bioguard.movil.network.LecturaSensorResponse
import com.bioguard.movil.network.PrediccionResponse
import com.bioguard.movil.realtime.RealtimeHubClient
import com.bioguard.movil.service.WearableConnectionState
import com.bioguard.movil.ui.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = false,
    val summary: DashboardSummary? = null,
    val lecturasRecientes: List<LecturaSensorResponse> = emptyList(),
    val pacienteId: String? = null,
    val connectionState: WearableConnectionState = WearableConnectionState.DISCONNECTED,
    val ultimaPrediccion: GlycemicPrediction? = null,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
    private val cachedDataDao: CachedDataDao,
    private val prefs: UserPreferences,
    private val mlRepository: MlRepository,
    private val sensorRepository: SensorRepository,
    private val pacienteRepository: PacienteRepository,
    private val syncRepository: PredictionMlSyncRepository,
    private val realtimeHubClient: RealtimeHubClient
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private var dashboardJob: Job? = null
    private var backendJob: Job? = null

    // Predicción ML vigente del backend: enriquece la card de Riesgo IA en ambos roles
    // cuando la lectura aún no trae probabilidad/riesgo persistidos.
    private var backendPrediccion: PrediccionResponse? = null

    init {
        // Tiempo real: nueva lectura/alerta/ubicación del paciente → refrescar dashboard
        viewModelScope.launch {
            realtimeHubClient.events.collect { event ->
                when (event) {
                    RealtimeHubClient.EventLectura,
                    RealtimeHubClient.EventAlerta,
                    RealtimeHubClient.EventUbicacion -> loadDashboard()
                }
            }
        }
        loadDashboard()
        // Cuidador: además de los eventos SignalR, se hace un polling ligero para no
        // depender 100% de que el hub esté activo y pintar siempre la última lectura.
        viewModelScope.launch {
            while (true) {
                delay(CUIDADOR_POLL_INTERVAL_MS)
                if (UserRole.from(prefs.userRole.first()) == UserRole.CUIDADOR) {
                    loadDashboard()
                }
            }
        }
    }

    fun loadDashboard() {
        dashboardJob?.cancel()
        dashboardJob = viewModelScope.launch {
            val patientId = prefs.patientId.first() ?: "paciente-local"
            _uiState.update { it.copy(isLoading = false, error = null, pacienteId = patientId) }

            // Peso/estatura para el IMC (F1) usado por el predictor local de pico glucémico.
            val pesoKg = prefs.patientWeight.first()?.toDoubleOrNull() ?: 0.0
            val estaturaCm = prefs.patientHeight.first()?.toDoubleOrNull() ?: 0.0
            val tieneBiometria = pesoKg > 0 && estaturaCm > 0
            val predictor = GlycemicPeakPredictor()

            // Cuidador: no recibe el stream Bluetooth del reloj (está vinculado al teléfono
            // del paciente); su pantalla se alimenta de las lecturas REALES que el backend
            // ya tiene sincronizadas para el paciente asignado.
            if (UserRole.from(prefs.userRole.first()) == UserRole.CUIDADOR) {
                // El pacienteId no viene en el claim del token del cuidador: se resuelve
                // vía /mi-acceso (PacienteId vinculado por QR). Si aún no hay vínculo,
                // se muestra un error claro en lugar de fallar con 403 en el backend.
                val cuidadorPatientId = pacienteRepository.resolveCaregiverPatientId(prefs)
                if (cuidadorPatientId.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pacienteId = null,
                            error = "No tienes un paciente asignado. Pide un código QR de acceso a la familia del paciente."
                        )
                    }
                    return@launch
                }
                _uiState.update { it.copy(pacienteId = cuidadorPatientId) }
                when (val result = sensorRepository.getLecturas(cuidadorPatientId, 100)) {
                    is Resource.Success -> pintarLecturas(
                        responses = result.data,
                        predictor = predictor,
                        pesoKg = pesoKg,
                        estaturaCm = estaturaCm,
                        tieneBiometria = tieneBiometria
                    )
                    is Resource.Error -> _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                    is Resource.Loading -> {}
                }
                return@launch
            }

            // Paciente: la caché local es la fuente en tiempo real (stream Wear → teléfono).
            // Si la caché está vacía (reinstalación / dispositivo nuevo), se rescatan del
            // backend las lecturas reales ya sincronizadas para no pintar una pantalla vacía.
            val backendLecturas = when (val result = sensorRepository.getLecturas(patientId, 100)) {
                is Resource.Success -> result.data
                else -> emptyList()
            }

            // La caché se filtra por paciente para no mezclar datos entre cuentas/pacientes.
            cachedDataDao.getCachedReadings(patientId, 2000).collectLatest { cachedReadings ->
                val responses = if (cachedReadings.isNotEmpty()) {
                    cachedReadings.map { reading ->
                        // El predictor local complementa; si la lectura ya trae probabilidad/riesgo
                        // persistidos (calculados por el motor ML al recibirla), se conservan.
                        val pred = if (tieneBiometria) {
                            predictor.predecir(
                                pesoKg = pesoKg,
                                estaturaCm = estaturaCm,
                                pulsoBpm = reading.pulsoBpm,
                                temperaturaC = reading.temperaturaC,
                                estresPct = reading.estresPct
                            )
                        } else null
                        reading.toResponse(
                            probabilidadPico = pred?.pPico ?: reading.probabilidadPico.takeIf { it > 0.0 },
                            nivelRiesgo = pred?.nivelRiesgo ?: reading.nivelRiesgo.takeIf { it.isNotBlank() }
                        )
                    }
                } else {
                    backendLecturas
                }
                pintarLecturas(
                    responses = responses,
                    predictor = predictor,
                    pesoKg = pesoKg,
                    estaturaCm = estaturaCm,
                    tieneBiometria = tieneBiometria
                )
            }
        }

        // Predicción ML vigente del backend: se aplica a la card de Riesgo IA cuando la
        // lectura del momento aún no trae probabilidad/riesgo (p. ej. cuidador recién conectado).
        backendJob?.cancel()
        backendJob = viewModelScope.launch {
            val patientId = prefs.patientId.first() ?: return@launch
            val prediccion = runCatching { mlRepository.getPrediccionActual(patientId) }.getOrNull()
            backendPrediccion = (prediccion as? Resource.Success)?.data
        }
    }

    private fun pintarLecturas(
        responses: List<LecturaSensorResponse>,
        predictor: GlycemicPeakPredictor,
        pesoKg: Double,
        estaturaCm: Double,
        tieneBiometria: Boolean
    ) {
        // Si ninguna lectura trae probabilidad pero hay predicción ML vigente en el backend,
        // se usa como respaldo para no pintar la card de Riesgo IA vacía.
        val withPred = responses.map { r ->
            val p = backendPrediccion
            if (p == null || r.probabilidadPico != null) r
            else r.copy(probabilidadPico = p.probabilidadPico, nivelRiesgo = p.nivelRiesgo)
        }
        val latest = withPred.firstOrNull()
        val connState = if (withPred.isNotEmpty()) WearableConnectionState.STREAMING else WearableConnectionState.PAIRED
        _uiState.update {
            it.copy(
                isLoading = false,
                connectionState = connState,
                summary = DashboardSummary(ultimaLectura = latest),
                lecturasRecientes = withPred,
                ultimaPrediccion = latest?.let { r ->
                    if (tieneBiometria && r.probabilidadPico != null) {
                        predictor.predecir(
                            pesoKg = pesoKg,
                            estaturaCm = estaturaCm,
                            pulsoBpm = r.pulsoBpm,
                            temperaturaC = r.temperaturaC,
                            estresPct = r.estresPct
                        )
                    } else if (r.probabilidadPico != null) {
                        // Cuidador sin biometría local: se muestra el riesgo IA real ya
                        // calculado por el móvil del paciente y persistido en el backend.
                        GlycemicPrediction(
                            imc = 0.0,
                            z = 0.0,
                            pPico = r.probabilidadPico,
                            casoClinico = r.nivelRiesgo ?: "Por evaluar",
                            nivelRiesgo = r.nivelRiesgo ?: "Por evaluar",
                            accionAutomatizada = null
                        )
                    } else null
                },
                error = null
            )
        }
    }

    /**
     * Generar reporte glucémico y enviarlo al backend directamente
     * Si falla, se guarda localmente para envío posterior
     */
    fun generarReporteGlucemico() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                val pacienteId = _uiState.value.pacienteId ?: return@launch
                val ultimaLectura = _uiState.value.summary?.ultimaLectura ?: return@launch
                
                // Get biometría para calcular IMC
                val biometriaResult = pacienteRepository.getBiometria(pacienteId)
                val biometria = (biometriaResult as? com.bioguard.movil.data.Resource.Success)?.data
                    ?: run {
                        _uiState.update { it.copy(isLoading = false, error = "No se pudo obtener biometría") }
                        return@launch
                    }
                
                val pesoKg = biometria.pesoKg ?: 0.0
                val estaturaCm = biometria.estaturaCm ?: 0.0
                
                if (pesoKg <= 0 || estaturaCm <= 0) {
                    _uiState.update { it.copy(isLoading = false, error = "Falta peso o estatura para calcular IMC") }
                    return@launch
                }
                
                // Computar predicción localmente (F1-F3)
                val predictor = GlycemicPeakPredictor()
                val prediction = predictor.predecir(
                    pesoKg = pesoKg,
                    estaturaCm = estaturaCm,
                    pulsoBpm = ultimaLectura.pulsoBpm,
                    temperaturaC = ultimaLectura.temperaturaC,
                    estresPct = ultimaLectura.estresPct
                )
                
                // Construir reporte
                val reportRequest = GuardarPrediccionRequest(
                    pacienteId = pacienteId,
                    probabilidadPico = prediction.pPico,
                    nivelRiesgo = prediction.nivelRiesgo,
                    casoClinico = prediction.casoClinico,
                    accionAutomatizada = prediction.accionAutomatizada,
                    imc = prediction.imc,
                    z = prediction.z,
                    pPico = prediction.pPico,
                    recomendacion = prediction.accionAutomatizada,
                    horasEstimadas = null,
                    modeloVersion = GlycemicPeakPredictor.VERSION
                )
                
                // Intentar enviar al backend
                val sendResult = mlRepository.guardarPrediccion(reportRequest)
                
                if (sendResult is com.bioguard.movil.data.Resource.Success) {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                } else {
                    // Si falla, guardar localmente para sincronización posterior
                    val localResult = syncRepository.guardarLocalmente(reportRequest)
                    if (localResult is com.bioguard.movil.data.Resource.Success) {
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                error = "Reporte guardado localmente. Se sincronizará automáticamente."
                            ) 
                        }
                    } else {
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                error = (sendResult as? com.bioguard.movil.data.Resource.Error)?.message ?: "Error desconocido"
                            ) 
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Error: ${e.message}") }
            }
        }
    }

    /**
     * Forzar sincronización manual: dispara el servicio que vacía toda la cola offline
     * (lecturas, GPS, eventos, alertas) y además sincroniza las predicciones ML pendientes.
     */
    fun sincronizarManualmente() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                com.bioguard.movil.service.BioGuardMonitoringService.requestCloudSync(getApplication())
                val result = syncRepository.sincronizarLote()

                if (result is com.bioguard.movil.data.Resource.Success) {
                    val sincronizados = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = if (sincronizados > 0) "Se sincronizaron $sincronizados reportes y lecturas pendientes" else "Sincronizacion completada (cola vacia o sin red)"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = (result as? com.bioguard.movil.data.Resource.Error)?.message
                                ?: "Sincronizacion disparada. Revisa la conexion."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Error: ${e.message}") }
            }
        }
    }

    private fun CachedReadingEntity.toResponse(
        probabilidadPico: Double? = null,
        nivelRiesgo: String? = null
    ): LecturaSensorResponse {
        return LecturaSensorResponse(
            id = id,
            timestamp = fechaHora,
            pulsoBpm = pulsoBpm,
            temperaturaC = temperaturaC,
            estresPct = estresPct,
            hrv = hrv.takeIf { it > 0.0 },
            spo2 = spo2.takeIf { it > 0.0 },
            pasos = pasos.takeIf { it > 0 },
            glucosaEstimadaMgDl = glucosaEstimadaMgDl.takeIf { it > 0.0 },
            probabilidadPico = probabilidadPico?.takeIf { it > 0.0 },
            nivelRiesgo = nivelRiesgo?.takeIf { it.isNotBlank() }
        )
    }

    private companion object {
        const val CUIDADOR_POLL_INTERVAL_MS = 30_000L
    }
}

package com.bioguard.movil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bioguard.movil.data.local.CachedDataDao
import com.bioguard.movil.data.local.CachedReadingEntity
import com.bioguard.movil.data.repository.MlRepository
import com.bioguard.movil.data.repository.PacienteRepository
import com.bioguard.movil.data.repository.PredictionMlSyncRepository
import com.bioguard.movil.datastore.UserPreferences
import com.bioguard.movil.ml.GlycemicPeakPredictor
import com.bioguard.movil.network.DashboardSummary
import com.bioguard.movil.network.GuardarPrediccionRequest
import com.bioguard.movil.network.LecturaSensorResponse
import com.bioguard.movil.service.PredictionMlSyncWorker
import com.bioguard.movil.service.WearableConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DashboardUiState(
    val isLoading: Boolean = false,
    val summary: DashboardSummary? = null,
    val lecturasRecientes: List<LecturaSensorResponse> = emptyList(),
    val pacienteId: String? = null,
    val connectionState: WearableConnectionState = WearableConnectionState.DISCONNECTED,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
    private val cachedDataDao: CachedDataDao,
    private val prefs: UserPreferences,
    private val mlRepository: MlRepository,
    private val pacienteRepository: PacienteRepository,
    private val syncRepository: PredictionMlSyncRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private var dashboardJob: Job? = null

    init {
        loadDashboard()
        // Programar sincronización automática en background
        PredictionMlSyncWorker.scheduleAutoSync(application)
    }

    fun loadDashboard() {
        dashboardJob?.cancel()
        dashboardJob = viewModelScope.launch {
            val patientId = prefs.patientId.first() ?: "paciente-local"
            _uiState.update { it.copy(isLoading = false, error = null, pacienteId = patientId) }

            // La caché se filtra por paciente para no mezclar datos entre cuentas/pacientes.
            cachedDataDao.getCachedReadings(patientId, 2000).collectLatest { cachedReadings ->
                val latest = cachedReadings.firstOrNull()?.toResponse()
                val connState = if (cachedReadings.isNotEmpty()) WearableConnectionState.STREAMING else WearableConnectionState.PAIRED
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        connectionState = connState,
                        summary = DashboardSummary(
                            ultimaLectura = latest
                        ),
                        lecturasRecientes = cachedReadings.map { reading -> reading.toResponse() },
                        error = null
                    )
                }
            }
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

    private fun CachedReadingEntity.toResponse(): LecturaSensorResponse {
        return LecturaSensorResponse(
            id = id,
            timestamp = fechaHora,
            pulsoBpm = pulsoBpm,
            temperaturaC = temperaturaC,
            estresPct = estresPct,
            hrv = hrv,
            spo2 = spo2,
            probabilidadPico = null,
            nivelRiesgo = null
        )
    }
}

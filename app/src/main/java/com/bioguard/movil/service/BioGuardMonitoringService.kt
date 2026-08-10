package com.bioguard.movil.service

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.bioguard.movil.data.local.BioGuardDatabase
import com.bioguard.movil.data.local.CachedReadingEntity
import com.bioguard.movil.data.local.PendingAlertEntity
import com.bioguard.movil.data.local.PendingEventEntity
import com.bioguard.movil.data.local.PendingGpsEntity
import com.bioguard.movil.data.local.PendingReadingEntity
import com.bioguard.movil.datastore.UserPreferences
import com.bioguard.movil.ml.LocalRiskAssessment
import com.bioguard.movil.ml.LocalRiskLevel
import com.bioguard.movil.ml.PersonalizedAnomalyModel
import com.bioguard.movil.ml.VitalSample
import com.bioguard.movil.network.ApiService
import com.bioguard.movil.network.LecturaSensorRequest
import com.bioguard.movil.network.RetrofitClient
import com.bioguard.movil.network.TrackingGpsRequest
import com.bioguard.movil.network.CrearEventoRequest
import com.bioguard.movil.network.CrearAlertaRequest
import com.bioguard.movil.network.HeartbeatRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.UUID

class BioGuardMonitoringService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var database: BioGuardDatabase
    private val api: ApiService = RetrofitClient.api
    private lateinit var prefs: UserPreferences
    private lateinit var wearableConnector: WearableConnector
    private lateinit var localAlertNotifier: LocalAlertNotifier
    private val localRiskModel = PersonalizedAnomalyModel()
    private val fusedLocation by lazy { LocationServices.getFusedLocationProviderClient(this) }
    @Volatile private var ultimaLatitud: Double? = null
    @Volatile private var ultimaLongitud: Double? = null
    private var riskThresholdsSent = false
    private var consecutiveElevatedReadings = 0
    private val cloudSyncMutex = Mutex()

    companion object {
        private const val TAG = "BioGuardMonitoring"
        private const val ACTION_SYNC_NOW = "com.bioguard.movil.action.SYNC_NOW"

        fun start(context: Context) {
            val intent = Intent(context, BioGuardMonitoringService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, BioGuardMonitoringService::class.java)
            context.stopService(intent)
        }

        fun requestCloudSync(context: Context) {
            CloudSyncStatusStore.publish(CloudSyncStatus(phase = CloudSyncPhase.RUNNING))
            val intent = Intent(context, BioGuardMonitoringService::class.java).setAction(ACTION_SYNC_NOW)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "BioGuard::MonitoringWakeLock"
            )?.apply {
                acquire(60 * 60 * 1000L) // 1 hour timeout, similar to wearable
            }
            Log.d(TAG, "WakeLock adquirido correctamente")
        } catch (e: SecurityException) {
            Log.e(TAG, "No se pudo adquirir WakeLock: ${e.message}")
        }
        database = BioGuardDatabase.getDatabase(this)
        prefs = UserPreferences(this)
        localAlertNotifier = LocalAlertNotifier(this).also { it.createChannels() }
        startForeground(
            LocalAlertNotifier.NOTIFICATION_MONITORING,
            localAlertNotifier.monitoringNotification()
        )

        wearableConnector = WearableConnector(
            context = this,
            onReadingReceived = reading@{ request, sourceMessageId ->
                val patientId = prefs.patientId.first()
                try {
                    val insertedId = database.pendingDataDao().insertReading(
                        PendingReadingEntity(
                            pulsoBpm = request.pulsoBpm,
                            temperaturaC = request.temperaturaC,
                            sudoracionGsr = request.sudoracionGsr,
                            hrv = request.hrv,
                            spo2 = request.spo2,
                            pasos = request.pasos,
                            timestamp = request.timestamp,
                            sourceMessageId = sourceMessageId
                        )
                    )
                    if (insertedId == -1L) {
                        Log.d(TAG, "Lectura duplicada confirmada sin volver a procesarla")
                        return@reading true
                    }
                    val patientId = prefs.patientId.first() ?: "paciente-local"
                    val baseline = database.cachedDataDao().getRecentReadingsSnapshot(patientId).map { cached ->
                        VitalSample(
                            heartRateBpm = cached.pulsoBpm,
                            temperatureC = cached.temperaturaC.takeIf { it > 0.0 },
                            gsr = cached.sudoracionGsr.takeIf { it > 0.0 },
                            hrvMs = cached.hrv.takeIf { it > 0.0 },
                            spo2Percent = cached.spo2.takeIf { it > 0.0 }
                        )
                    }
                    val assessment = localRiskModel.assess(
                        current = VitalSample(
                            heartRateBpm = request.pulsoBpm,
                            temperatureC = request.temperaturaC.takeIf { it > 0.0 },
                            gsr = request.sudoracionGsr.takeIf { it > 0.0 },
                            hrvMs = request.hrv?.takeIf { it > 0.0 },
                            spo2Percent = request.spo2?.takeIf { it > 0.0 }
                        ),
                        baseline = baseline,
                        personalizedAnalysisEnabled = prefs.isLocalAnalysisEnabled.first()
                    )
                    val validSpo2 = if (request.spo2 != null && request.spo2 > 0.0) request.spo2 else 98.0
                    val validPasos = if (request.pasos != null && request.pasos > 0) request.pasos else (request.pulsoBpm.toInt() * 15 % 1500 + 450)
                    val validHrv = request.hrv ?: 45.0
                    val calculatedGlucose = (95.0 +
                            (request.pulsoBpm - 72.0) * 0.45 +
                            (request.temperaturaC - 36.5) * 12.0 +
                            kotlin.math.max(0.0, request.sudoracionGsr - 45.0) * 0.5 +
                            kotlin.math.max(0.0, 45.0 - validHrv) * 0.4
                    ).coerceIn(70.0, 220.0)

                    database.cachedDataDao().insertReadings(
                        listOf(
                            CachedReadingEntity(
                                id = sourceMessageId ?: UUID.randomUUID().toString(),
                                pacienteId = patientId,
                                pulsoBpm = request.pulsoBpm,
                                temperaturaC = request.temperaturaC,
                                sudoracionGsr = request.sudoracionGsr,
                                hrv = validHrv,
                                spo2 = validSpo2,
                                pasos = validPasos,
                                calorias = (validPasos * 0.04),
                                accelX = request.accelX ?: 0.12,
                                accelY = request.accelY ?: 0.98,
                                accelZ = request.accelZ ?: 0.04,
                                grasaCorporalPct = request.grasaCorporalPct ?: 18.5,
                                masaMuscularKg = request.masaMuscularKg ?: 32.0,
                                faseSueno = request.faseSueno ?: "Sueño Profundo",
                                glucosaEstimadaMgDl = calculatedGlucose,
                                fechaHora = request.timestamp
                            )
                        )
                    )
                    evaluarRiesgoOffline(
                        patientId = patientId,
                        request = request,
                        assessment = assessment
                    )
                    Log.d(TAG, "Lectura del reloj persistida en base de datos local")
                    true
                } catch (e: Exception) {
                    Log.w(TAG, "Error encolando lectura del reloj", e)
                    false
                }
            },
            onEventReceived = { request ->
                serviceScope.launch {
                    val patientId = prefs.patientId.first() ?: "paciente-local"
                    val updated = request.copy(pacienteId = patientId)
                    database.pendingDataDao().insertEvent(
                        PendingEventEntity(
                            pacienteId = patientId, dispositivoMac = updated.dispositivoMac,
                            nivelRiesgo = updated.nivelRiesgo, probabilidadMl = updated.probabilidadMl,
                            descripcion = updated.descripcion, timestamp = Instant.now().toString()
                        )
                    )
                }
            },
            onAlertReceived = { request ->
                serviceScope.launch {
                    val patientId = prefs.patientId.first() ?: "paciente-local"
                    val conUbicacion = request.copy(pacienteId = patientId)
                    database.pendingDataDao().insertAlert(
                        PendingAlertEntity(
                            pacienteId = patientId, tipoAlerta = conUbicacion.tipo,
                            descripcion = conUbicacion.mensaje, latitud = ultimaLatitud,
                            longitud = ultimaLongitud, timestamp = Instant.now().toString()
                        )
                    )
                    localAlertNotifier.notifyEmergencySos("[SOS] BOTÓN DE PÁNICO PRESIONADO. ${conUbicacion.mensaje}")
                    localAlertNotifier.notifyAssessment(
                        LocalRiskAssessment(
                            score = 95.0,
                            safetyRuleScore = 95.0,
                            anomalyProbability = null,
                            level = LocalRiskLevel.CRITICAL,
                            reasons = listOf(conUbicacion.mensaje.take(200)),
                            personalizedModelReady = false,
                            modelVersion = "wearable-rule-v1"
                        ),
                        nightGuardian = isInNightGuardianWindow()
                    )
                    try {
                        if (patientId != "paciente-local") {
                            api.crearAlerta(conUbicacion)
                            Log.d(TAG, "Alerta SOS/Emergencia enviada inmediatamente al servidor para notificar a cuidadores")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "No se pudo enviar alerta SOS inmediatamente al servidor (quedara encolada): ${e.message}")
                    }
                }
            },
            onHeartbeatReceived = { request ->
                serviceScope.launch {
                    val patientId = prefs.patientId.first()
                    if (patientId == null) {
                        Log.d(TAG, "Heartbeat del reloj omitido: sin paciente vinculado")
                        return@launch
                    }
                    val updated = request.copy(pacienteId = patientId)
                    try {
                        api.sendDeviceHeartbeat(updated)
                    } catch (e: Exception) {
                        Log.w(TAG, "No se pudo enviar heartbeat del reloj", e)
                    }
                }
            },
            trustedNodeIdProvider = { prefs.deviceNodeId.first() },
            pacienteIdProvider = { prefs.patientId.first() }
        )
        wearableConnector.register()

        serviceScope.launch {
            delay(2000)
            if (wearableConnector.connectionState.value == com.bioguard.movil.service.WearableConnectionState.CONNECTED) {
                wearableConnector.sendRiskThresholds()
                riskThresholdsSent = true
            }
        }

        startMonitoringLoops()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SYNC_NOW) {
            serviceScope.launch { syncAllPending("manual") }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
        if (::wearableConnector.isInitialized) {
            wearableConnector.unregister()
        }
        serviceScope.cancel()
        Log.d(TAG, "BioGuardMonitoringService destroyed, resources cleaned up")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun isInBatchWindow(): Boolean {
        val startHour = prefs.batchStartHour.first()
        val endHour = prefs.batchEndHour.first()
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return if (startHour <= endHour) {
            currentHour in startHour until endHour
        } else {
            currentHour >= startHour || currentHour < endHour
        }
    }

    private suspend fun isInNightGuardianWindow(): Boolean {
        if (!prefs.isNightGuardianEnabled.first()) return false
        val startHour = prefs.nightGuardianStartHour.first()
        val endHour = prefs.nightGuardianEndHour.first()
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return if (startHour <= endHour) {
            currentHour in startHour until endHour
        } else {
            currentHour >= startHour || currentHour < endHour
        }
    }

    private fun startMonitoringLoops() {
        // Loop: Heartbeat del teléfono
        serviceScope.launch {
            while (true) {
                delay(60000)
                try {
                    val patientId = prefs.patientId.first()
                    if (patientId == null) {
                        Log.d(TAG, "Heartbeat omitido: sin paciente vinculado")
                        continue
                    }
                    api.sendDeviceHeartbeat(
                        HeartbeatRequest(
                            pacienteId = patientId,
                            bateria = obtenerBateriaTelefono(),
                            sensoresActivos = emptyList()
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Heartbeat del teléfono falló", e)
                }
            }
        }

        // Un solo coordinador evita carreras y envíos duplicados entre colas.
        serviceScope.launch {
            while (true) {
                val intervalMs = prefs.syncIntervalMinutes.first().coerceIn(5, 120) * 60_000L
                val batchAllowed = !prefs.isBatchSyncEnabled.first() || isInBatchWindow()
                if (prefs.isSyncEnabled.first() && batchAllowed && hasValidatedInternet()) {
                    syncAllPending("automatico")
                }
                delay(intervalMs)
            }
        }

        // Loop: GPS real del teléfono (Guardián Nocturno consciente)
        serviceScope.launch {
            while (true) {
                if (!prefs.isNightGuardianEnabled.first()) {
                    delay(15 * 60 * 1000L)
                    continue
                }
                if (!isInNightGuardianWindow()) {
                    delay(15 * 60 * 1000L)
                    continue
                }
                delay(5 * 60 * 1000L)
                val patientId = prefs.patientId.first()
                if (patientId != null) {
                    enviarUbicacionReal()
                }
            }
        }

        // Loop: Reconnect wearable + resend thresholds if needed
        serviceScope.launch {
            while (true) {
                delay(60_000L)
                val state = wearableConnector.connectionState.value
                if (state != com.bioguard.movil.service.WearableConnectionState.CONNECTED &&
                    state != com.bioguard.movil.service.WearableConnectionState.UNAVAILABLE
                ) {
                    riskThresholdsSent = false
                    Log.d(TAG, "Wearable disconnected, attempting reconnect...")
                    wearableConnector.discoverAndConnect()
                }
                if (wearableConnector.connectionState.value == com.bioguard.movil.service.WearableConnectionState.CONNECTED && !riskThresholdsSent) {
                    wearableConnector.sendRiskThresholds()
                    riskThresholdsSent = true
                    Log.d(TAG, "Risk thresholds sent after reconnection")
                }
            }
        }
    }

    private suspend fun enviarUbicacionReal() {
        val tienePermiso = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!tienePermiso) {
            Log.d(TAG, "Sin permiso de ubicación; se omite registro GPS")
            return
        }
        try {
            val cts = CancellationTokenSource()
            val location = fusedLocation.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token
            ).await()
            if (location == null) {
                Log.d(TAG, "Ubicación no disponible; se omite registro GPS")
                return
            }
            ultimaLatitud = location.latitude
            ultimaLongitud = location.longitude
            val gpsRequest = TrackingGpsRequest(
                latitud = location.latitude,
                longitud = location.longitude,
                esEmergencia = false
            )
            try {
                database.pendingDataDao().insertGps(
                    PendingGpsEntity(
                        latitud = gpsRequest.latitud,
                        longitud = gpsRequest.longitud,
                        esEmergencia = gpsRequest.esEmergencia,
                        timestamp = Instant.now().toString()
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Envío de GPS falló; guardando offline", e)
                database.pendingDataDao().insertGps(
                    PendingGpsEntity(
                        latitud = gpsRequest.latitud,
                        longitud = gpsRequest.longitud,
                        esEmergencia = gpsRequest.esEmergencia,
                        timestamp = Instant.now().toString()
                    )
                )
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Permiso de ubicación revocado", e)
        } catch (e: Exception) {
            Log.w(TAG, "Error obteniendo ubicación", e)
        }
    }

    private fun hasValidatedInternet(): Boolean {
        val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private suspend fun syncAllPending(trigger: String) = cloudSyncMutex.withLock {
        if (!hasValidatedInternet()) {
            Log.d(TAG, "Sin conexion validada; la cola offline se conserva")
            CloudSyncStatusStore.publish(
                CloudSyncStatus(
                    phase = CloudSyncPhase.NO_NETWORK,
                    pendingItems = countPendingItems(),
                    completedAtMillis = System.currentTimeMillis()
                )
            )
            return@withLock
        }
        if (prefs.patientId.first() == null) {
            CloudSyncStatusStore.publish(
                CloudSyncStatus(
                    phase = CloudSyncPhase.FAILED,
                    pendingItems = countPendingItems(),
                    completedAtMillis = System.currentTimeMillis()
                )
            )
            return@withLock
        }
        CloudSyncStatusStore.publish(CloudSyncStatus(phase = CloudSyncPhase.RUNNING))
        Log.d(TAG, "Sincronizacion $trigger iniciada desde la cola Room")
        val successful = listOf(
            syncPendingReadings(),
            syncPendingGps(),
            syncPendingEvents(),
            syncPendingAlerts()
        ).all { it }
        CloudSyncStatusStore.publish(
            CloudSyncStatus(
                phase = if (successful) CloudSyncPhase.SUCCESS else CloudSyncPhase.FAILED,
                pendingItems = countPendingItems(),
                completedAtMillis = System.currentTimeMillis()
            )
        )
    }

    private suspend fun countPendingItems(): Int =
        database.pendingDataDao().countPendingReadings() +
            database.pendingDataDao().countPendingGps() +
            database.pendingDataDao().countPendingEvents() +
            database.pendingDataDao().countPendingAlerts()

    private suspend fun syncPendingReadings(): Boolean {
        if (prefs.patientId.first() == null) return false
        val pending = database.pendingDataDao().getPendingReadings(100)
        if (pending.isNotEmpty()) {
            val installId = com.bioguard.movil.util.InstallationIdentity.getOrCreate(this)
            val requests = pending.map {
                LecturaSensorRequest(
                    pacienteId = prefs.patientId.first(),
                    pulsoBpm = it.pulsoBpm,
                    temperaturaC = it.temperaturaC,
                    sudoracionGsr = it.sudoracionGsr,
                    hrv = it.hrv,
                    spo2 = it.spo2,
                    pasos = it.pasos,
                    timestamp = it.timestamp,
                    sourceMessageId = it.sourceMessageId ?: "$installId:reading:${it.id}"
                )
            }
            try {
                api.sendLecturasBatch(requests)
                database.pendingDataDao().deleteReadings(pending.map { it.id })
            } catch (e: Exception) {
                Log.w(TAG, "Error al sincronizar lecturas en lote (se reintentara): ${e.message}")
                return false
            }
        }
        return true
    }

    private suspend fun syncPendingGps(): Boolean {
        if (prefs.patientId.first() == null) return false
        val pending = database.pendingDataDao().getPendingGps(100)
        if (pending.isNotEmpty()) {
            val installId = com.bioguard.movil.util.InstallationIdentity.getOrCreate(this)
            val requests = pending.map {
                TrackingGpsRequest(
                    pacienteId = prefs.patientId.first(),
                    latitud = it.latitud,
                    longitud = it.longitud,
                    esEmergencia = it.esEmergencia,
                    sourceMessageId = "$installId:gps:${it.id}"
                )
            }
            try {
                api.sendTrackingBatch(requests)
                database.pendingDataDao().deleteGps(pending.map { it.id })
            } catch (e: Exception) {
                Log.w(TAG, "Error al sincronizar GPS en lote (se reintentara): ${e.message}")
                return false
            }
        }
        return true
    }

    private suspend fun syncPendingEvents(): Boolean {
        if (prefs.patientId.first() == null) return false
        val pending = database.pendingDataDao().getPendingEvents(50)
        if (pending.isEmpty()) return true
        val enviados = mutableListOf<Long>()
        var successful = true
        for (evento in pending) {
            try {
                api.sendEvento(
                    CrearEventoRequest(
                        pacienteId = evento.pacienteId,
                        dispositivoMac = evento.dispositivoMac,
                        nivelRiesgo = evento.nivelRiesgo,
                        probabilidadMl = evento.probabilidadMl,
                        descripcion = evento.descripcion
                    )
                )
                enviados.add(evento.id)
            } catch (e: Exception) {
                Log.w(TAG, "Error al sincronizar evento offline ${evento.id} (se reintentara): ${e.message}")
                successful = false
            }
        }
        if (enviados.isNotEmpty()) database.pendingDataDao().deleteEvents(enviados)
        return successful
    }

    private suspend fun syncPendingAlerts(): Boolean {
        if (prefs.patientId.first() == null) return false
        val pending = database.pendingDataDao().getPendingAlerts(50)
        if (pending.isEmpty()) return true
        val enviadas = mutableListOf<Long>()
        var successful = true
        for (alerta in pending) {
            try {
                api.crearAlerta(
                    CrearAlertaRequest(
                        pacienteId = alerta.pacienteId,
                        tipo = alerta.tipoAlerta,
                        nivel = "CRITICAL",
                        titulo = alerta.tipoAlerta,
                        mensaje = alerta.descripcion
                    )
                )
                enviadas.add(alerta.id)
            } catch (e: Exception) {
                Log.w(TAG, "Error al sincronizar alerta offline ${alerta.id} (se reintentara): ${e.message}")
                successful = false
            }
        }
        if (enviadas.isNotEmpty()) database.pendingDataDao().deleteAlerts(enviadas)
        return successful
    }

    private fun obtenerBateriaTelefono(): Int? {
        return try {
            val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) level * 100 / scale else null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun evaluarRiesgoOffline(
        patientId: String?,
        request: LecturaSensorRequest,
        assessment: LocalRiskAssessment
    ) {
        val activeGuardian = isInNightGuardianWindow()
        val threshold = if (activeGuardian) 70.0 else 80.0
        val elevated = assessment.score >= threshold
        consecutiveElevatedReadings = if (elevated) consecutiveElevatedReadings + 1 else 0

        val confirmed = assessment.level == LocalRiskLevel.CRITICAL || consecutiveElevatedReadings >= 2
        if (!confirmed || !prefs.isLocalAlertsEnabled.first()) return

        val notificationResult = localAlertNotifier.notifyAssessment(assessment, activeGuardian)
        if (!notificationResult.acceptedByPolicy) return

        val now = Instant.now().toString()
        val reason = assessment.reasons.joinToString(separator = "; ")
            .ifBlank { "Cambio preventivo detectado por análisis local" }
            .take(500)

        if (patientId != null) {
            database.pendingDataDao().insertAlert(
                PendingAlertEntity(
                    pacienteId = patientId,
                    tipoAlerta = "ANALISIS_LOCAL_${assessment.level.name}",
                    descripcion = "$reason. Modelo ${assessment.modelVersion}.",
                    latitud = ultimaLatitud,
                    longitud = ultimaLongitud,
                    timestamp = now
                )
            )

            val deviceId = prefs.deviceId.first()
            val anomalyProbability = assessment.anomalyProbability
            if (!deviceId.isNullOrBlank() && anomalyProbability != null) {
                database.pendingDataDao().insertEvent(
                    PendingEventEntity(
                        pacienteId = patientId,
                        dispositivoMac = deviceId,
                        nivelRiesgo = assessment.level.name,
                        probabilidadMl = anomalyProbability,
                        descripcion = "$reason. Modelo ${assessment.modelVersion}.",
                        timestamp = now
                    )
                )
            }
        }

        if (::wearableConnector.isInitialized) {
            wearableConnector.sendAlertCommandToWatch(
                request.pulsoBpm.toFloat(),
                request.temperaturaC.toFloat(),
                request.sudoracionGsr.toFloat(),
                (assessment.score / 100.0).toFloat()
            )
        }
        Log.i(
            TAG,
            "Alerta local aceptada: level=${assessment.level}, model=${assessment.modelVersion}, " +
                "modelReady=${assessment.personalizedModelReady}, displayed=${notificationResult.displayed}"
        )
    }
}

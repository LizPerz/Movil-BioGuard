package com.bioguard.movil.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bioguard.movil.data.local.BioGuardDatabase
import com.bioguard.movil.data.local.CachedReadingEntity
import com.bioguard.movil.data.local.PendingAlertEntity
import com.bioguard.movil.data.local.PendingEventEntity
import com.bioguard.movil.data.local.PendingGpsEntity
import com.bioguard.movil.data.local.PendingReadingEntity
import com.bioguard.movil.datastore.UserPreferences
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
    private val fusedLocation by lazy { LocationServices.getFusedLocationProviderClient(this) }
    @Volatile private var ultimaLatitud: Double? = null
    @Volatile private var ultimaLongitud: Double? = null
    private var riskThresholdsSent = false
    private val cloudSyncMutex = Mutex()

    companion object {
        private const val TAG = "BioGuardMonitoring"
        private const val CHANNEL_ID = "bioguard_estandar"
        private const val NOTIFICATION_ID = 991
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
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        wearableConnector = WearableConnector(
            context = this,
            onReadingReceived = { request ->
                val patientId = prefs.patientId.first()
                try {
                    database.pendingDataDao().insertReading(
                        PendingReadingEntity(
                            pulsoBpm = request.pulsoBpm,
                            temperaturaC = request.temperaturaC,
                            sudoracionGsr = request.sudoracionGsr,
                            hrv = request.hrv,
                            spo2 = request.spo2,
                            timestamp = request.timestamp
                        )
                    )
                    if (patientId != null) {
                        database.cachedDataDao().insertReadings(
                            listOf(
                                CachedReadingEntity(
                                    id = UUID.randomUUID().toString(),
                                    pacienteId = patientId,
                                    pulsoBpm = request.pulsoBpm,
                                    temperaturaC = request.temperaturaC,
                                    sudoracionGsr = request.sudoracionGsr,
                                    hrv = request.hrv ?: 0.0,
                                    spo2 = request.spo2 ?: 0.0,
                                    pasos = 0,
                                    calorias = 0.0,
                                    fechaHora = request.timestamp
                                )
                            )
                        )
                    }
                    evaluarRiesgoOffline(
                        request.pulsoBpm,
                        request.temperaturaC,
                        request.sudoracionGsr,
                        request.hrv ?: 55.0,
                        request.spo2 ?: 98.0
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
                    val patientId = prefs.patientId.first()
                    if (patientId == null) {
                        Log.d(TAG, "Evento del reloj omitido: sin paciente vinculado")
                        return@launch
                    }
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
                    val patientId = prefs.patientId.first()
                    if (patientId == null) {
                        Log.d(TAG, "Alerta del reloj omitida: sin paciente vinculado")
                        return@launch
                    }
                    val conUbicacion = request.copy(
                        pacienteId = patientId,
                        latitud = request.latitud ?: ultimaLatitud,
                        longitud = request.longitud ?: ultimaLongitud
                    )
                    database.pendingDataDao().insertAlert(
                        PendingAlertEntity(
                            pacienteId = patientId, tipoAlerta = conUbicacion.tipoAlerta,
                            descripcion = conUbicacion.descripcion, latitud = conUbicacion.latitud,
                            longitud = conUbicacion.longitud, timestamp = Instant.now().toString()
                        )
                    )
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
            trustedNodeIdProvider = { prefs.deviceNodeId.first() }
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Monitoreo BioGuard",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificacion persistente de monitoreo de signos vitales"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BioGuard Activo")
            .setContentText("Monitoreando signos vitales en segundo plano")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

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

        // Loop: Sincronización automática de lecturas (configurable)
        serviceScope.launch {
            while (true) {
                val isSync = prefs.isSyncEnabled.first()
                val intervalMin = prefs.syncIntervalMinutes.first()
                if (false && isSync) {
                    try {
                        syncPendingReadings()
                    } catch (e: Exception) {
                        Log.w(TAG, "Sync de lecturas pendientes falló", e)
                    }
                    delay(intervalMin * 60 * 1000L)
                } else {
                    delay(5 * 60 * 1000L) // Wait 5 minutes to check again
                }
            }
        }

        // Loop: Sincronización en lotes pesados (Batch window)
        serviceScope.launch {
            while (true) {
                if (false && isInBatchWindow()) {
                    try {
                        Log.d(TAG, "Ventana de transmision por lotes activa. Sincronizando...")
                        syncPendingReadings()
                        syncPendingGps()
                        syncPendingEvents()
                        syncPendingAlerts()
                    } catch (e: Exception) {
                        Log.w(TAG, "Sync de lote pesado falló", e)
                    }
                }
                delay(15 * 60 * 1000L) // Check every 15 minutes
            }
        }

        // Loop: Sincronizar eventos y alertas individuales (Frecuente)
        serviceScope.launch {
            while (true) {
                if (false) try {
                    syncPendingEvents()
                    syncPendingAlerts()
                } catch (e: Exception) {
                    Log.w(TAG, "Sync frecuente falló", e)
                }
                delay(30 * 1000L)
            }
        }

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
            return@withLock
        }
        Log.d(TAG, "Sincronizacion $trigger iniciada desde la cola Room")
        syncPendingReadings()
        syncPendingGps()
        syncPendingEvents()
        syncPendingAlerts()
    }

    private suspend fun syncPendingReadings() {
        if (prefs.patientId.first() == null) return
        val pending = database.pendingDataDao().getPendingReadings(100)
        if (pending.isNotEmpty()) {
            val requests = pending.map {
                LecturaSensorRequest(
                    pulsoBpm = it.pulsoBpm,
                    temperaturaC = it.temperaturaC,
                    sudoracionGsr = it.sudoracionGsr,
                    hrv = it.hrv,
                    spo2 = it.spo2,
                    timestamp = it.timestamp
                )
            }
            try {
                api.sendLecturasBatch(requests)
                database.pendingDataDao().deleteReadings(pending.map { it.id })
            } catch (e: Exception) {
                Log.w(TAG, "Error al sincronizar lecturas en lote (se reintentara): ${e.message}")
            }
        }
    }

    private suspend fun syncPendingGps() {
        if (prefs.patientId.first() == null) return
        val pending = database.pendingDataDao().getPendingGps(100)
        if (pending.isNotEmpty()) {
            val requests = pending.map {
                TrackingGpsRequest(
                    latitud = it.latitud,
                    longitud = it.longitud,
                    esEmergencia = it.esEmergencia
                )
            }
            try {
                api.sendTrackingBatch(requests)
                database.pendingDataDao().deleteGps(pending.map { it.id })
            } catch (e: Exception) {
                Log.w(TAG, "Error al sincronizar GPS en lote (se reintentara): ${e.message}")
            }
        }
    }

    private suspend fun syncPendingEvents() {
        if (prefs.patientId.first() == null) return
        val pending = database.pendingDataDao().getPendingEvents(50)
        if (pending.isEmpty()) return
        val enviados = mutableListOf<Long>()
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
            }
        }
        if (enviados.isNotEmpty()) database.pendingDataDao().deleteEvents(enviados)
    }

    private suspend fun syncPendingAlerts() {
        if (prefs.patientId.first() == null) return
        val pending = database.pendingDataDao().getPendingAlerts(50)
        if (pending.isEmpty()) return
        val enviadas = mutableListOf<Long>()
        for (alerta in pending) {
            try {
                api.crearAlerta(
                    CrearAlertaRequest(
                        pacienteId = alerta.pacienteId,
                        tipoAlerta = alerta.tipoAlerta,
                        descripcion = alerta.descripcion,
                        latitud = alerta.latitud,
                        longitud = alerta.longitud
                    )
                )
                enviadas.add(alerta.id)
            } catch (e: Exception) {
                Log.w(TAG, "Error al sincronizar alerta offline ${alerta.id} (se reintentara): ${e.message}")
            }
        }
        if (enviadas.isNotEmpty()) database.pendingDataDao().deleteAlerts(enviadas)
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
        pulsoBpm: Double,
        temperaturaC: Double,
        sudoracionGsr: Double,
        hrv: Double,
        spo2: Double
    ) {
        var score = 0.0
        
        if (pulsoBpm > 100) score += (pulsoBpm - 100) * 0.8
        else if (pulsoBpm < 50) score += (50 - pulsoBpm) * 0.5
        
        if (hrv < 40) score += (40 - hrv) * 1.0
        
        if (temperaturaC > 37.8) score += (temperaturaC - 37.8) * 15.0
        else if (temperaturaC < 35.0) score += (35.0 - temperaturaC) * 15.0
        
        if (sudoracionGsr > 6.0) score += (sudoracionGsr - 6.0) * 8.0
        
        if (spo2 < 95) score += (95 - spo2) * 5.0

        val scoreFinal = score.coerceIn(0.0, 100.0)
        val activeGuardian = isInNightGuardianWindow()
        val threshold = if (activeGuardian) 70.0 else 80.0
        
        if (scoreFinal >= threshold) {
            mostrarNotificacionAlertaOffline(scoreFinal, activeGuardian)
            if (::wearableConnector.isInitialized) {
                wearableConnector.sendAlertCommandToWatch(
                    pulsoBpm.toFloat(),
                    temperaturaC.toFloat(),
                    sudoracionGsr.toFloat(),
                    (scoreFinal / 100.0).toFloat()
                )
            }
        }
    }

    private fun mostrarNotificacionAlertaOffline(score: Double, isNightGuardian: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val title = if (isNightGuardian) "Guardian Nocturno: Alerta" else "Alerta Preventiva Offline"
        val desc = if (isNightGuardian) {
            "Detectado riesgo elevado durante el sueno: ${String.format("%.0f", score)}%."
        } else {
            "Detectado riesgo metabolico estimado: ${String.format("%.0f", score)}%. Revise sus niveles."
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(desc)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }
}

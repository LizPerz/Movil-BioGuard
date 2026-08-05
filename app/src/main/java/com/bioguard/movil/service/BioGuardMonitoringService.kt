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
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bioguard.movil.data.local.BioGuardDatabase
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
import kotlinx.coroutines.tasks.await
import java.time.Instant

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

    companion object {
        private const val TAG = "BioGuardMonitoring"
        private const val CHANNEL_ID = "bioguard_estandar"
        private const val NOTIFICATION_ID = 991

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
                serviceScope.launch {
                    val patientId = prefs.patientId.first()
                    if (patientId != null) {
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
                        } catch (e: Exception) {
                            Log.w(TAG, "Error encolando lectura del reloj", e)
                        }
                    } else {
                        Log.d(TAG, "Lectura del reloj sin envío: sin paciente vinculado")
                    }
                    evaluarRiesgoOffline(
                        request.pulsoBpm,
                        request.temperaturaC,
                        request.sudoracionGsr,
                        request.hrv ?: 55.0,
                        request.spo2 ?: 98.0
                    )
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
                    try {
                        api.sendEvento(updated)
                    } catch (e: Exception) {
                        Log.w(TAG, "No se pudo enviar evento del reloj; encolando", e)
                        database.pendingDataDao().insertEvent(
                            PendingEventEntity(
                                pacienteId = patientId,
                                dispositivoMac = updated.dispositivoMac,
                                nivelRiesgo = updated.nivelRiesgo,
                                probabilidadMl = updated.probabilidadMl,
                                descripcion = updated.descripcion,
                                timestamp = Instant.now().toString()
                            )
                        )
                    }
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
                    try {
                        api.crearAlerta(conUbicacion)
                    } catch (e: Exception) {
                        Log.w(TAG, "No se pudo enviar alerta del reloj; encolando", e)
                        database.pendingDataDao().insertAlert(
                            PendingAlertEntity(
                                pacienteId = patientId,
                                tipoAlerta = conUbicacion.tipoAlerta,
                                descripcion = conUbicacion.descripcion,
                                latitud = conUbicacion.latitud,
                                longitud = conUbicacion.longitud,
                                timestamp = Instant.now().toString()
                            )
                        )
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
            }
        )
        wearableConnector.register()
        wearableConnector.sendRiskThresholds()

        startMonitoringLoops()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
                            sensoresActivos = listOf("pulso", "temperatura", "sudoracion", "hrv", "spo2")
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
                if (isSync) {
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
                if (isInBatchWindow()) {
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
                try {
                    syncPendingEvents()
                    syncPendingAlerts()
                } catch (e: Exception) {
                    Log.w(TAG, "Sync frecuente falló", e)
                }
                delay(30 * 1000L)
            }
        }

        // Loop: GPS real del teléfono (Guardián Nocturno consciente)
        serviceScope.launch {
            while (true) {
                val delayTime = if (isInNightGuardianWindow()) {
                    5 * 60 * 1000L // 5 minutes during sleep to save battery
                } else {
                    30 * 1000L // 30 seconds during day for active tracking
                }
                delay(delayTime)
                val patientId = prefs.patientId.first()
                if (patientId != null) {
                    enviarUbicacionReal()
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
                api.sendTracking(gpsRequest)
                syncPendingGps()
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

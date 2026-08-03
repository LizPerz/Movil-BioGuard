package com.example.bioguard_movil.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.bioguard_movil.data.local.BioGuardDatabase
import com.example.bioguard_movil.data.local.PendingGpsEntity
import com.example.bioguard_movil.data.local.PendingReadingEntity
import com.example.bioguard_movil.data.local.PendingEventoEntity
import com.example.bioguard_movil.data.local.PendingAlertaEntity
import com.example.bioguard_movil.data.local.PendingHeartbeatEntity
import com.example.bioguard_movil.datastore.UserPreferences
import com.example.bioguard_movil.network.ApiService
import com.example.bioguard_movil.network.LecturaSensorRequest
import com.example.bioguard_movil.network.RetrofitClient
import com.example.bioguard_movil.network.TrackingGpsRequest
import com.example.bioguard_movil.network.CrearEventoRequest
import com.example.bioguard_movil.network.CrearAlertaRequest
import com.example.bioguard_movil.network.HeartbeatRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import kotlin.random.Random

class BioGuardMonitoringService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var database: BioGuardDatabase
    private val api: ApiService = RetrofitClient.api
    private lateinit var prefs: UserPreferences
    private lateinit var wearableConnector: WearableConnector

    companion object {
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
        database = BioGuardDatabase.getDatabase(this)
        prefs = UserPreferences(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        wearableConnector = WearableConnector(
            context = this,
            onReadingReceived = { request ->
                serviceScope.launch {
                    try {
                        api.sendLecturas(listOf(request))
                        syncPendingReadings()
                    } catch (e: Exception) {
                        println("Error al enviar lectura del reloj: ${e.message}")
                        database.pendingDataDao().insertReading(
                            PendingReadingEntity(
                                pulsoBpm = request.pulsoBpm,
                                temperaturaC = request.temperaturaC,
                                sudoracionGsr = request.sudoracionGsr,
                                hrv = request.hrv,
                                spo2 = request.spo2,
                                timestamp = Instant.now().toString()
                            )
                        )
                    }
                }
            },
            onEventReceived = { request ->
                serviceScope.launch {
                    try {
                        val patientId = prefs.patientId.first() ?: ""
                        val updated = request.copy(pacienteId = if (request.pacienteId.isEmpty()) patientId else request.pacienteId)
                        api.sendEvento(updated)
                    } catch (e: Exception) {
                        println("Error al enviar evento del reloj: ${e.message}")
                        database.pendingDataDao().insertEvento(
                            PendingEventoEntity(
                                nivelRiesgo = request.nivelRiesgo,
                                probabilidadMl = request.probabilidadMl,
                                descripcion = request.descripcion,
                                timestamp = Instant.now().toString()
                            )
                        )
                    }
                }
            },
            onAlertReceived = { request ->
                serviceScope.launch {
                    try {
                        val patientId = prefs.patientId.first() ?: ""
                        val updated = request.copy(pacienteId = if (request.pacienteId.isEmpty()) patientId else request.pacienteId)
                        api.crearAlerta(updated)
                    } catch (e: Exception) {
                        println("Error al enviar alerta del reloj: ${e.message}")
                        database.pendingDataDao().insertAlerta(
                            PendingAlertaEntity(
                                tipoAlerta = request.tipoAlerta,
                                descripcion = request.descripcion,
                                latitud = request.latitud,
                                longitud = request.longitud,
                                timestamp = Instant.now().toString()
                            )
                        )
                    }
                }
            },
            onHeartbeatReceived = { request ->
                serviceScope.launch {
                    try {
                        val patientId = prefs.patientId.first() ?: ""
                        val updated = request.copy(pacienteId = if (request.pacienteId.isNullOrEmpty()) patientId else request.pacienteId)
                        api.sendDeviceHeartbeat(updated)
                    } catch (e: Exception) {
                        println("Error al enviar heartbeat del reloj: ${e.message}")
                        database.pendingDataDao().insertHeartbeat(
                            PendingHeartbeatEntity(
                                timestamp = Instant.now().toString(),
                                pacienteId = request.pacienteId
                            )
                        )
                    }
                }
            }
        )
        wearableConnector.register()

        startMonitoringLoops()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
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

    private fun startMonitoringLoops() {
        // Loop 2: Heartbeat
        serviceScope.launch {
            while (true) {
                delay(60000)
                try {
                    val batteryLevel = getBatteryLevel()
                    api.sendDeviceHeartbeat(HeartbeatRequest(bateria = batteryLevel))
                } catch (_: Exception) {}
            }
        }

        // Loop 3: GPS
        serviceScope.launch {
            while (true) {
                delay(30000)
                try {
                    val location = getLastKnownLocation()
                    if (location != null) {
                        val gpsRequest = TrackingGpsRequest(
                            latitud = location.latitude,
                            longitud = location.longitude,
                            esEmergencia = false
                        )
                        api.sendTracking(gpsRequest)
                        syncPendingGps()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun getBatteryLevel(): Int {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        return batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun hasLocationPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private suspend fun getLastKnownLocation(): android.location.Location? {
        if (!hasLocationPermission()) {
            println("Permiso de ubicación no concedido")
            return null
        }
        val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this)
        return try {
            val location = fusedLocationClient.lastLocation.await()
            if (location != null && location.accuracy <= 50.0f) location else null
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun syncPendingReadings() {
        val pending = database.pendingDataDao().getPendingReadings(100)
        if (pending.isNotEmpty()) {
            val requests = pending.map {
                LecturaSensorRequest(
                    pulsoBpm = it.pulsoBpm,
                    temperaturaC = it.temperaturaC,
                    sudoracionGsr = it.sudoracionGsr,
                    hrv = it.hrv,
                    spo2 = it.spo2,
                    nivelRiesgo = null,
                    isSimulated = false,
                    timestamp = Instant.now().toString()
                )
            }
            try {
                api.sendLecturasBatch(requests)
                database.pendingDataDao().deleteReadings(pending.map { it.id })
            } catch (_: Exception) {}
        }
    }

    private suspend fun syncPendingGps() {
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
            } catch (_: Exception) {}
        }
    }
}

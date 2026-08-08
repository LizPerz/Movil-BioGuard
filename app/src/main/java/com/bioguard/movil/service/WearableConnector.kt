package com.bioguard.movil.service

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.bioguard.movil.network.LecturaSensorRequest
import com.bioguard.movil.network.CrearEventoRequest
import com.bioguard.movil.network.CrearAlertaRequest
import com.bioguard.movil.network.HeartbeatRequest
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.charset.StandardCharsets

data class WatchEventDto(
    val bpm: Float,
    val temperatura: Float,
    val sudoracionGsr: Float,
    val nivelRiesgo: String,
    val timestamp: Long,
    val tipoEvento: String,
    val descripcion: String,
    val probabilidadMl: Double? = null
)

data class WatchAlertDto(
    val tipoAlerta: String,
    val mensaje: String,
    val nivelRiesgo: String,
    val timestamp: Long,
    val bpm: Float,
    val temperatura: Float,
    val sudoracionGsr: Float
)

data class RiskThresholds(
    val criticalBpmHigh: Float = 135f,
    val criticalBpmLow: Float = 39f,
    val criticalTemp: Float = 39.0f,
    val moderateBpm: Float = 105f,
    val moderateTemp: Float = 37.8f,
    val moderateGsr: Float = 65f
)

enum class WearableConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    UNAVAILABLE,
    ERROR
}

data class WearableDeviceInfo(
    val nodeId: String = "",
    val displayName: String = "Unknown",
    val isNearby: Boolean = false,
    val hasApp: Boolean = false,
    val lastSeenMillis: Long = 0L
)

class WearableConnector(
    private val context: Context,
    private val onReadingReceived: (LecturaSensorRequest) -> Unit,
    private val onEventReceived: (CrearEventoRequest) -> Unit,
    private val onAlertReceived: (CrearAlertaRequest) -> Unit,
    private val onHeartbeatReceived: (HeartbeatRequest) -> Unit
) : MessageClient.OnMessageReceivedListener, CapabilityClient.OnCapabilityChangedListener, DataClient.OnDataChangedListener {

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitorJob: Job? = null
    private var heartbeatCheckJob: Job? = null

    private val messageClient: MessageClient by lazy { Wearable.getMessageClient(context) }
    private val nodeClient by lazy { Wearable.getNodeClient(context) }
    private val capabilityClient by lazy { Wearable.getCapabilityClient(context) }
    private val dataClient: DataClient by lazy { Wearable.getDataClient(context) }

    private val _connectionState = MutableStateFlow(WearableConnectionState.DISCONNECTED)
    val connectionState: StateFlow<WearableConnectionState> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<WearableDeviceInfo?>(null)
    val connectedDevice: StateFlow<WearableDeviceInfo?> = _connectedDevice.asStateFlow()

    @Volatile private var lastHeartbeatMillis: Long = 0L
    private val heartbeatTimeoutMillis = 120_000L
    @Volatile private var wearOsApiUnavailableUntilMillis: Long = 0L

    private val dispositivoMac: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?.takeIf { it.isNotBlank() } ?: "telefono-bioguard"
    }

    private var isRegistered = false

    fun register() {
        if (isRegistered) {
            Log.d(TAG, "Already registered, skipping")
            return
        }
        try {
            messageClient.addListener(this)
            capabilityClient.addListener(this, BIOGUARD_CAPABILITY)
            dataClient.addListener(this)
            isRegistered = true
            Log.d(TAG, "Listeners registered successfully (Message, Capability, Data)")
            scope.launch {
                discoverAndConnect()
            }
            startConnectionMonitor()
            startHeartbeatCheck()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register listeners", e)
            _connectionState.value = WearableConnectionState.ERROR
        }
    }

    fun unregister() {
        if (!isRegistered) return
        try {
            messageClient.removeListener(this)
            capabilityClient.removeListener(this)
            dataClient.removeListener(this)
            isRegistered = false
            monitorJob?.cancel()
            heartbeatCheckJob?.cancel()
            scope.cancel()
            _connectionState.value = WearableConnectionState.DISCONNECTED
            _connectedDevice.value = null
            Log.d(TAG, "Listeners unregistered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister listeners", e)
        }
    }

    suspend fun discoverAndConnect(forceWearOsRetry: Boolean = false): WearableDeviceInfo? {
        if (!forceWearOsRetry && isWearOsApiInCooldown()) {
            _connectionState.value = WearableConnectionState.UNAVAILABLE
            return null
        }

        _connectionState.value = WearableConnectionState.CONNECTING
        return try {
            val device = findConnectedWearable()
            if (device != null) {
                _connectedDevice.value = device
                _connectionState.value = WearableConnectionState.CONNECTED
                lastHeartbeatMillis = System.currentTimeMillis()
                Log.d(TAG, "Connected to wearable: ${device.displayName}")
                device
            } else {
                _connectionState.value = if (isWearOsApiInCooldown()) {
                    WearableConnectionState.UNAVAILABLE
                } else {
                    WearableConnectionState.DISCONNECTED
                }
                _connectedDevice.value = null
                Log.d(TAG, "No wearable devices found")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during discovery", e)
            _connectionState.value = WearableConnectionState.ERROR
            null
        }
    }

    private suspend fun findConnectedWearable(): WearableDeviceInfo? {
        return withTimeoutOrNull(5000) {
            try {
                val capability = capabilityClient.getCapability(
                    BIOGUARD_CAPABILITY,
                    CapabilityClient.FILTER_REACHABLE
                ).await()

                val node = capability.nodes.firstOrNull { it.isNearby } ?: capability.nodes.firstOrNull()
                if (node != null) {
                    return@withTimeoutOrNull WearableDeviceInfo(
                        nodeId = node.id,
                        displayName = node.displayName.ifBlank { "SmartWatch WearOS" },
                        isNearby = node.isNearby,
                        hasApp = true,
                        lastSeenMillis = System.currentTimeMillis()
                    )
                }
            } catch (e: ApiException) {
                if (e.isWearOsApiUnavailable()) {
                    markWearOsApiUnavailable("capability discovery", e)
                    return@withTimeoutOrNull null
                }
                Log.w(TAG, "Capability discovery failed; falling back to node client: ${e.statusCode}")
            }

            try {
                val nodes = nodeClient.connectedNodes.await()
                val node = nodes.firstOrNull { it.isNearby } ?: nodes.firstOrNull()
                if (node != null) {
                    return@withTimeoutOrNull WearableDeviceInfo(
                        nodeId = node.id,
                        displayName = node.displayName.ifBlank { "SmartWatch WearOS" },
                        isNearby = node.isNearby,
                        hasApp = false,
                        lastSeenMillis = System.currentTimeMillis()
                    )
                }
            } catch (e: ApiException) {
                if (e.isWearOsApiUnavailable()) {
                    markWearOsApiUnavailable("node discovery", e)
                    return@withTimeoutOrNull null
                }
                Log.w(TAG, "Node discovery failed: ${e.statusCode}")
            } catch (e: Exception) {
                Log.w(TAG, "Node discovery failed: ${e.message}")
            }
            null
        }
    }

    suspend fun getAvailableWearables(forceWearOsRetry: Boolean = false): List<WearableDeviceInfo> {
        if (!forceWearOsRetry && isWearOsApiInCooldown()) {
            _connectionState.value = WearableConnectionState.UNAVAILABLE
            return emptyList()
        }

        return try {
            val devices = mutableListOf<WearableDeviceInfo>()

            try {
                val capability = capabilityClient.getCapability(
                    BIOGUARD_CAPABILITY,
                    CapabilityClient.FILTER_REACHABLE
                ).await()
                for (node in capability.nodes) {
                    devices.add(
                        WearableDeviceInfo(
                            nodeId = node.id,
                            displayName = node.displayName.ifBlank { "SmartWatch WearOS" },
                            isNearby = node.isNearby,
                            hasApp = true,
                            lastSeenMillis = System.currentTimeMillis()
                        )
                    )
                }
            } catch (e: ApiException) {
                if (e.isWearOsApiUnavailable()) {
                    markWearOsApiUnavailable("capability list", e)
                    return emptyList()
                }
                Log.w(TAG, "Capability discovery failed: ${e.statusCode}")
            } catch (e: Exception) {
                Log.w(TAG, "Capability discovery failed: ${e.message}")
            }

            try {
                val nodes = nodeClient.connectedNodes.await()
                for (node in nodes) {
                    if (devices.none { it.nodeId == node.id }) {
                        devices.add(
                            WearableDeviceInfo(
                                nodeId = node.id,
                                displayName = node.displayName.ifBlank { "SmartWatch WearOS" },
                                isNearby = node.isNearby,
                                hasApp = false,
                                lastSeenMillis = System.currentTimeMillis()
                            )
                        )
                    }
                }
            } catch (e: ApiException) {
                if (e.isWearOsApiUnavailable()) {
                    markWearOsApiUnavailable("node list", e)
                    return emptyList()
                }
                Log.w(TAG, "Node discovery failed: ${e.statusCode}")
            } catch (e: Exception) {
                Log.w(TAG, "Node discovery failed: ${e.message}")
            }

            devices
        } catch (e: Exception) {
            Log.e(TAG, "Error discovering wearables", e)
            emptyList()
        }
    }

    private fun startConnectionMonitor() {
        monitorJob = scope.launch {
            while (isActive) {
                delay(30_000)
                if (isWearOsApiInCooldown()) {
                    _connectionState.value = WearableConnectionState.UNAVAILABLE
                    continue
                }
                if (_connectionState.value != WearableConnectionState.CONNECTED) {
                    val device = discoverAndConnect()
                    if (device != null) {
                        sendRiskThresholds()
                    }
                }
            }
        }
    }

    private fun startHeartbeatCheck() {
        heartbeatCheckJob = scope.launch {
            while (isActive) {
                delay(30_000)
                val elapsed = System.currentTimeMillis() - lastHeartbeatMillis
                if (elapsed > heartbeatTimeoutMillis && _connectionState.value == WearableConnectionState.CONNECTED) {
                    Log.w(TAG, "Heartbeat timeout, attempting reconnect")
                    _connectionState.value = WearableConnectionState.DISCONNECTED
                    discoverAndConnect()
                }
            }
        }
    }

    fun onHeartbeatReceivedFromWatch() {
        lastHeartbeatMillis = System.currentTimeMillis()
    }

    private suspend fun sendMessageToWatch(path: String, payload: ByteArray, maxRetries: Int = 3): Boolean {
        if (isWearOsApiInCooldown()) {
            Log.d(TAG, "Wear OS API unavailable; message $path skipped until companion app is available")
            return false
        }

        val targetNode = _connectedDevice.value?.nodeId ?: run {
            val device = discoverAndConnect()
            device?.nodeId ?: return false
        }

        var lastError: Exception? = null
        for (attempt in 1..maxRetries) {
            try {
                messageClient.sendMessage(targetNode, path, payload).await()
                Log.d(TAG, "Message sent to $path (attempt $attempt)")
                return true
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "Send failed (attempt $attempt/$maxRetries): ${e.message}")
                if (attempt < maxRetries) {
                    delay(1000L * attempt)
                    val device = discoverAndConnect()
                    if (device == null) {
                        Log.e(TAG, "Reconnection failed, aborting send")
                        return false
                    }
                }
            }
        }
        Log.e(TAG, "Failed to send message after $maxRetries attempts", lastError)
        return false
    }

    fun sendAlertCommandToWatch(bpm: Float, temperatura: Float, gsr: Float, probability: Float) {
        val payload = """
            {
                "bpm": $bpm,
                "temperatura": $temperatura,
                "gsr": $gsr,
                "probability": $probability
            }
        """.trimIndent().toByteArray(StandardCharsets.UTF_8)

        scope.launch {
            val success = sendMessageToWatch("/commands/alert", payload)
            if (!success) {
                Log.w(TAG, "Failed to send alert command to watch")
            }
        }
    }

    fun sendRiskThresholds(thresholds: RiskThresholds = RiskThresholds()) {
        val payload = gson.toJson(thresholds).toByteArray(StandardCharsets.UTF_8)
        scope.launch {
            val success = sendMessageToWatch("/risk-thresholds", payload)
            if (!success) {
                Log.w(TAG, "Failed to send risk thresholds to watch")
            }
        }
    }

    fun sendDismissCommandToWatch() {
        scope.launch {
            val success = sendMessageToWatch("/commands/dismiss", ByteArray(0))
            if (!success) {
                Log.w(TAG, "Failed to send dismiss command to watch")
            }
        }
    }

    override fun onMessageReceived(event: MessageEvent) {
        try {
            lastHeartbeatMillis = System.currentTimeMillis()
            val jsonString = String(event.data, StandardCharsets.UTF_8)
            when (event.path) {
                "/sensors/readings" -> {
                    val request = gson.fromJson(jsonString, LecturaSensorRequest::class.java)
                    onReadingReceived(request)
                }
                "/sensors/events" -> {
                    val dto = gson.fromJson(jsonString, WatchEventDto::class.java)
                    val request = CrearEventoRequest(
                        pacienteId = "",
                        dispositivoMac = dispositivoMac,
                        nivelRiesgo = dto.nivelRiesgo,
                        probabilidadMl = dto.probabilidadMl ?: 0.0,
                        descripcion = "${dto.tipoEvento}: ${dto.descripcion}"
                    )
                    onEventReceived(request)
                }
                "/sensors/alerts" -> {
                    val dto = gson.fromJson(jsonString, WatchAlertDto::class.java)
                    val request = CrearAlertaRequest(
                        pacienteId = "",
                        tipoAlerta = dto.tipoAlerta,
                        descripcion = dto.mensaje,
                        latitud = null,
                        longitud = null
                    )
                    onAlertReceived(request)
                }
                "/sensors/heartbeat" -> {
                    val request = gson.fromJson(jsonString, HeartbeatRequest::class.java)
                    onHeartbeatReceivedFromWatch()
                    onHeartbeatReceived(request)
                }
                else -> when {
                    event.path.startsWith("/bioguard/telemetry") -> {
                        val request = gson.fromJson(jsonString, LecturaSensorRequest::class.java)
                        onReadingReceived(request)
                        sendAckToWatch(event.path, event.sourceNodeId)
                    }
                    event.path.startsWith("/bioguard/heartbeat") -> {
                        val request = gson.fromJson(jsonString, HeartbeatRequest::class.java)
                        onHeartbeatReceivedFromWatch()
                        onHeartbeatReceived(request)
                    }
                    else -> {
                        Log.w(TAG, "Unknown message path: ${event.path}")
                    }
                }
            }
        } catch (e: JsonSyntaxException) {
            Log.w(TAG, "JSON malformado del reloj en ${event.path}: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando mensaje del reloj en ${event.path}", e)
        }
    }

    override fun onCapabilityChanged(capabilityInfo: com.google.android.gms.wearable.CapabilityInfo) {
        scope.launch {
            val node = capabilityInfo.nodes.firstOrNull { it.isNearby } ?: capabilityInfo.nodes.firstOrNull()
            if (node != null) {
                _connectedDevice.value = WearableDeviceInfo(
                    nodeId = node.id,
                    displayName = node.displayName.ifBlank { "SmartWatch WearOS" },
                    isNearby = node.isNearby,
                    hasApp = true,
                    lastSeenMillis = System.currentTimeMillis()
                )
                _connectionState.value = WearableConnectionState.CONNECTED
                lastHeartbeatMillis = System.currentTimeMillis()
                Log.d(TAG, "Capability changed: connected to ${node.displayName}")
            } else if (_connectionState.value == WearableConnectionState.CONNECTED) {
                _connectionState.value = WearableConnectionState.DISCONNECTED
                Log.d(TAG, "Capability changed: no wearables reachable")
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        try {
            for (event in dataEvents) {
                if (event.type == DataEvent.TYPE_CHANGED) {
                    val path = event.dataItem.uri.path ?: continue
                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                    val payloadJson = dataMap.getString("payload") ?: continue

                    lastHeartbeatMillis = System.currentTimeMillis()
                    Log.d(TAG, "DataClient Item recibido en $path")

                    when {
                        path.startsWith("/bioguard/telemetry") -> {
                            try {
                                val request = gson.fromJson(payloadJson, LecturaSensorRequest::class.java)
                                onReadingReceived(request)
                                sendAckToWatch(path)
                            } catch (e: Exception) {
                                Log.w(TAG, "Error deserializando lectura DataClient: ${e.message}")
                            }
                        }
                        path.startsWith("/bioguard/heartbeat") -> {
                            try {
                                val request = gson.fromJson(payloadJson, HeartbeatRequest::class.java)
                                onHeartbeatReceivedFromWatch()
                                onHeartbeatReceived(request)
                                sendAckToWatch(path)
                            } catch (e: Exception) {
                                Log.w(TAG, "Error deserializando heartbeat DataClient: ${e.message}")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando DataEvent del reloj", e)
        } finally {
            dataEvents.release()
        }
    }

    fun sendAckToWatch(originalPath: String, targetNodeId: String? = null) {
        scope.launch {
            val ackPayload = """{"ackPath":"$originalPath","timestamp":${System.currentTimeMillis()}}"""
                .toByteArray(StandardCharsets.UTF_8)
            if (!targetNodeId.isNullOrBlank()) {
                try {
                    messageClient.sendMessage(targetNodeId, "/bioguard/ack", ackPayload).await()
                    return@launch
                } catch (e: Exception) {
                    Log.w(TAG, "Direct ACK failed, falling back to connected node: ${e.message}")
                }
            }
            sendMessageToWatch("/bioguard/ack", ackPayload)
        }
    }

    fun sendSamplingRateCommand(intervalSeconds: Int) {
        val payload = """{"intervalSeconds":$intervalSeconds}""".toByteArray(StandardCharsets.UTF_8)
        scope.launch {
            val success = sendMessageToWatch("/commands/sampling_rate", payload)
            if (!success) {
                Log.w(TAG, "Failed to send sampling rate command to watch")
            }
        }
    }

    fun sendSensorToggleCommand(sensorName: String, enabled: Boolean) {
        val payload = """{"sensor":"$sensorName","enabled":$enabled}""".toByteArray(StandardCharsets.UTF_8)
        scope.launch {
            val success = sendMessageToWatch("/commands/sensor_toggle", payload)
            if (!success) {
                Log.w(TAG, "Failed to send sensor toggle command to watch")
            }
        }
    }

    companion object {
        private const val TAG = "WearableConnector"
        private const val BIOGUARD_CAPABILITY = "bioguard_watch"
        private const val WEAR_OS_API_UNAVAILABLE_STATUS = 17
        private const val WEAR_OS_API_RETRY_COOLDOWN_MILLIS = 10 * 60 * 1000L
    }

    private fun ApiException.isWearOsApiUnavailable(): Boolean {
        return statusCode == WEAR_OS_API_UNAVAILABLE_STATUS
    }

    private fun isWearOsApiInCooldown(): Boolean {
        return System.currentTimeMillis() < wearOsApiUnavailableUntilMillis
    }

    private fun markWearOsApiUnavailable(source: String, error: ApiException) {
        wearOsApiUnavailableUntilMillis = System.currentTimeMillis() + WEAR_OS_API_RETRY_COOLDOWN_MILLIS
        _connectedDevice.value = null
        _connectionState.value = WearableConnectionState.UNAVAILABLE
        Log.w(
            TAG,
            "Wear OS API unavailable during $source (status=${error.statusCode}). " +
                "Install/configure the watch companion app or use Bluetooth QR/BLE pairing; retry is throttled."
        )
    }
}

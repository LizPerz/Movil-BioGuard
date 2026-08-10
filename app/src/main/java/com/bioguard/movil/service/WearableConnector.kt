package com.bioguard.movil.service

import android.content.Context
import android.util.Log
import com.bioguard.movil.network.LecturaSensorRequest
import com.bioguard.movil.network.CrearEventoRequest
import com.bioguard.movil.network.CrearAlertaRequest
import com.bioguard.movil.network.HeartbeatRequest
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject

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
    SEARCHING,
    PAIRED,
    STREAMING,
    SYNCHRONIZED,
    CONNECTING,
    CONNECTED,
    UNAVAILABLE,
    ERROR;

    fun toDisplayString(lastSyncMillis: Long = 0L): String {
        return when (this) {
            DISCONNECTED -> "Desconectado"
            SEARCHING, CONNECTING -> "Buscando..."
            PAIRED, CONNECTED -> "Emparejado"
            STREAMING -> "Transmitiendo"
            SYNCHRONIZED -> {
                val mins = if (lastSyncMillis > 0L) {
                    ((System.currentTimeMillis() - lastSyncMillis) / 60_000L).coerceAtLeast(1L)
                } else 1L
                "Sincronizado hace $mins min"
            }
            UNAVAILABLE -> "Wear OS no disponible"
            ERROR -> "Error de conexión"
        }
    }
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
    private val onReadingReceived: suspend (LecturaSensorRequest, String?) -> Boolean,
    private val onEventReceived: (CrearEventoRequest) -> Unit,
    private val onAlertReceived: (CrearAlertaRequest) -> Unit,
    private val onHeartbeatReceived: (HeartbeatRequest) -> Unit,
    private val trustedNodeIdProvider: suspend () -> String? = { null },
    private val pacienteIdProvider: suspend () -> String? = { null }
) : MessageClient.OnMessageReceivedListener, CapabilityClient.OnCapabilityChangedListener {

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitorJob: Job? = null
    private var heartbeatCheckJob: Job? = null

    private val messageClient: MessageClient by lazy { Wearable.getMessageClient(context) }
    private val capabilityClient by lazy { Wearable.getCapabilityClient(context) }

    private val _connectionState = MutableStateFlow(WearableConnectionState.DISCONNECTED)
    val connectionState: StateFlow<WearableConnectionState> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<WearableDeviceInfo?>(null)
    val connectedDevice: StateFlow<WearableDeviceInfo?> = _connectedDevice.asStateFlow()

    @Volatile private var lastHeartbeatMillis: Long = System.currentTimeMillis()
    private val heartbeatTimeoutMillis = 180_000L
    @Volatile private var wearOsApiUnavailableUntilMillis: Long = 0L
    private val pendingPairingAcks = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()

    private val dispositivoMac: String by lazy {
        com.bioguard.movil.util.InstallationIdentity.getOrCreate(context)
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
            isRegistered = true
            Log.d(TAG, "Listeners registered successfully (Message, Capability)")
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
        val trustedNodeId = trustedNodeIdProvider()
        return withTimeoutOrNull(5000) {
            try {
                val capability = capabilityClient.getCapability(
                    BIOGUARD_CAPABILITY,
                    CapabilityClient.FILTER_REACHABLE
                ).await()

                val node = capability.nodes.firstOrNull {
                    it.isNearby && (trustedNodeId == null || it.id == trustedNodeId)
                }
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

            Log.w(TAG, "No reachable BioGuard wearable capability found")
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
                for (node in capability.nodes.filter { it.isNearby }) {
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

    suspend fun pairWithNode(nodeId: String, pairingNonce: String? = null): Boolean {
        if (nodeId.isBlank() || isWearOsApiInCooldown()) return false
        return try {
            val target = Wearable.getNodeClient(context).connectedNodes.await()
                .firstOrNull { it.id == nodeId && it.isNearby }
                ?: run {
                    Log.w(TAG, "Vinculacion rechazada: el nodo no esta conectado por proximidad")
                    return false
                }
            val requestId = UUID.randomUUID().toString()
            val ack = CompletableDeferred<Boolean>()
            pendingPairingAcks[requestId] = ack
            val request = JSONObject()
                .put("requestedAt", System.currentTimeMillis())
                .put("protocolVersion", 2)
                .put("requestId", requestId)
            pairingNonce?.takeIf { it.isNotBlank() }?.let { request.put("nonce", it) }
            try {
                messageClient.sendMessage(
                    target.id,
                    PAIR_PATH,
                    request.toString().toByteArray(StandardCharsets.UTF_8)
                ).await()
                val confirmed = withTimeoutOrNull(PAIR_ACK_TIMEOUT_MILLIS) { ack.await() } == true
                if (!confirmed) {
                    Log.w(TAG, "Vinculacion rechazada: el reloj no confirmo la persistencia de confianza")
                    return false
                }
            } finally {
                pendingPairingAcks.remove(requestId)
            }
            _connectedDevice.value = WearableDeviceInfo(
                nodeId = nodeId,
                displayName = _connectedDevice.value?.displayName ?: "BioGuard Wearable",
                isNearby = true,
                hasApp = true,
                lastSeenMillis = System.currentTimeMillis()
            )
            _connectionState.value = WearableConnectionState.CONNECTED
            lastHeartbeatMillis = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo confirmar la vinculacion local con el reloj", e)
            false
        }
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
        scope.launch {
            if (event.data.size > WearablePayloadValidator.MAX_PAYLOAD_BYTES ||
                !WearablePayloadValidator.isAllowedPath(event.path)
            ) {
                Log.w(TAG, "Ignored invalid wearable envelope for ${event.path}")
                return@launch
            }

            if (event.path == "/bioguard/pair/ack") {
                handlePairingAck(event)
                return@launch
            }

            val trustedNodeId = trustedNodeIdProvider()
            val pacienteId = pacienteIdProvider() ?: ""
            if (!trustedNodeId.isNullOrBlank() && event.sourceNodeId != trustedNodeId) {
                Log.w(TAG, "Ignored wearable message because sourceNodeId != trustedNodeId")
                return@launch
            }
            if (_connectedDevice.value == null) {
                _connectedDevice.value = WearableDeviceInfo(
                    nodeId = event.sourceNodeId,
                    displayName = "SmartWatch WearOS",
                    isNearby = true,
                    hasApp = true,
                    lastSeenMillis = System.currentTimeMillis()
                )
            }
            _connectionState.value = WearableConnectionState.STREAMING
            lastHeartbeatMillis = System.currentTimeMillis()
            try {
                val jsonString = String(event.data, StandardCharsets.UTF_8)
                when (event.path) {
                    "/sensors/readings" -> {
                        val request = gson.fromJson(jsonString, LecturaSensorRequest::class.java)
                        if (WearablePayloadValidator.isValid(request)) onReadingReceived(request, null)
                        else Log.w(TAG, "Rejected invalid live telemetry")
                    }
                    "/sensors/events" -> {
                        val dto = gson.fromJson(jsonString, WatchEventDto::class.java)
                        if (!WearablePayloadValidator.isValid(dto)) return@launch
                        onEventReceived(
                            CrearEventoRequest(
                                pacienteId = pacienteId,
                                dispositivoMac = dispositivoMac,
                                nivelRiesgo = dto.nivelRiesgo,
                                probabilidadMl = dto.probabilidadMl ?: 0.0,
                                descripcion = "${dto.tipoEvento}: ${dto.descripcion}"
                            )
                        )
                    }
                    "/sensors/alerts" -> {
                        val dto = gson.fromJson(jsonString, WatchAlertDto::class.java)
                        if (!WearablePayloadValidator.isValid(dto)) return@launch
                        onAlertReceived(
                            CrearAlertaRequest(
                                pacienteId = pacienteId,
                                tipoAlerta = dto.tipoAlerta,
                                descripcion = dto.mensaje,
                                latitud = null,
                                longitud = null
                            )
                        )
                    }
                    "/sensors/heartbeat", "/bioguard/heartbeat" -> {
                        val request = gson.fromJson(jsonString, HeartbeatRequest::class.java)
                        if (!WearablePayloadValidator.isValid(request)) return@launch
                        onHeartbeatReceivedFromWatch()
                        onHeartbeatReceived(request)
                    }
                    "/commands/dismiss", "/sensors/dismiss" -> {
                        Log.w(TAG, "Recibido comando de descarte desde el reloj. Cancelando notificaciones locales.")
                        LocalAlertNotifier(context).cancelAllNotifications()
                    }
                    else -> {
                        val request = gson.fromJson(jsonString, LecturaSensorRequest::class.java)
                        if (!WearablePayloadValidator.isValid(request)) {
                            Log.w(TAG, "Rejected invalid durable telemetry")
                            return@launch
                        }
                        if (onReadingReceived(request, event.path)) {
                            lastHeartbeatMillis = System.currentTimeMillis()
                            sendAckToWatch(event.path, event.sourceNodeId)
                        } else {
                            Log.w(TAG, "Reading was not persisted; ACK withheld for ${event.path}")
                        }
                    }
                }
            } catch (e: JsonSyntaxException) {
                Log.w(TAG, "JSON malformado del reloj en ${event.path}")
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando mensaje del reloj en ${event.path}", e)
            }
        }
    }

    private fun handlePairingAck(event: MessageEvent) {
        try {
            val ack = JSONObject(String(event.data, StandardCharsets.UTF_8))
            val requestId = ack.optString("requestId")
            val pendingAck = pendingPairingAcks[requestId]
            if (requestId.matches(Regex("^[0-9a-fA-F-]{36}$")) &&
                ack.optInt("protocolVersion") == 2 &&
                ack.optBoolean("accepted", false) &&
                pendingAck != null
            ) {
                pendingAck.complete(true)
                lastHeartbeatMillis = System.currentTimeMillis()
                Log.d(TAG, "Pairing ACK accepted from ${event.sourceNodeId}")
            } else {
                Log.w(TAG, "Ignored unexpected pairing ACK from ${event.sourceNodeId}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Invalid pairing ACK from ${event.sourceNodeId}: ${e.message}")
        }
    }

    override fun onCapabilityChanged(capabilityInfo: com.google.android.gms.wearable.CapabilityInfo) {
        scope.launch {
            val trustedNodeId = trustedNodeIdProvider()
            val node = capabilityInfo.nodes.firstOrNull { it.isNearby && it.id == trustedNodeId }
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
        private const val PAIR_PATH = "/bioguard/pair"
        private const val PAIR_ACK_TIMEOUT_MILLIS = 8_000L
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
                "Install or configure the system companion app; retry is throttled."
        )
    }
}

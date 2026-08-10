package com.bioguard.movil.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.repository.DispositivoRepository
import com.bioguard.movil.data.repository.PacienteRepository
import com.bioguard.movil.datastore.UserPreferences
import com.bioguard.movil.network.InfoCompletaDispositivo
import com.bioguard.movil.service.BioGuardMonitoringService
import com.bioguard.movil.service.WearableConnector
import com.bioguard.movil.service.WearableConnectionState
import com.bioguard.movil.service.WearableDeviceInfo
import com.bioguard.movil.util.WearablePairingQr
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class DispositivoScanItem(
    val id: String,
    val nombre: String,
    val macAddress: String,
    val wearNodeId: String? = null,
    val pairingNonce: String? = null,
    val rssi: Int = -60,
    val tipo: String = "Bluetooth LE / WearOS",
    val bateria: Int = 100
)

private fun parseWearableQrPayload(raw: String): DispositivoScanItem? {
    android.util.Log.d("DeviceVM", "Parsing QR payload: ${raw.take(80)}...")
    val payload = WearablePairingQr.parse(raw) ?: run {
        android.util.Log.w("DeviceVM", "QR payload parse failed")
        return null
    }
    android.util.Log.d("DeviceVM", "QR parsed OK: name=${payload.name}, nodeId=${payload.nodeId}")
    return DispositivoScanItem(
        id = payload.address,
        nombre = payload.name,
        macAddress = payload.address,
        wearNodeId = payload.nodeId,
        pairingNonce = payload.nonce,
        rssi = -45,
        tipo = "Wearable QR / Bluetooth"
    )
}

data class DeviceUiState(
    val isLoading: Boolean = false,
    val isScanning: Boolean = false,
    val dispositivo: InfoCompletaDispositivo? = null,
    val dispositivosDisponibles: List<DispositivoScanItem> = emptyList(),
    val isPaired: Boolean = false,
    val isConnected: Boolean = false,
    val connectedDeviceName: String? = null,
    val connectedDeviceId: String? = null,
    val connectionState: WearableConnectionState = WearableConnectionState.DISCONNECTED,
    val lastSyncMillis: Long = 0L,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class DeviceViewModel @Inject constructor(
    application: Application,
    private val repository: DispositivoRepository,
    private val pacienteRepository: PacienteRepository,
    private val prefs: UserPreferences
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DeviceUiState())
    val uiState: StateFlow<DeviceUiState> = _uiState.asStateFlow()

    private var wearableConnector: WearableConnector? = null
    private var activeScanCallback: ScanCallback? = null

    private fun hasBluetoothScanPermission(): Boolean {
        val app = getApplication<Application>()
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(app, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
    }

    init {
        loadDispositivo()
        observeWearableConnection()
    }

    private fun observeWearableConnection() {
        val connector = getOrCreateWearableConnector()
        viewModelScope.launch {
            connector.connectionState.collect { state ->
                _uiState.update { it.copy(connectionState = state) }
                if (state == WearableConnectionState.CONNECTED) {
                    _uiState.update { it.copy(isPaired = true, isConnected = true) }
                } else if (state == WearableConnectionState.DISCONNECTED || state == WearableConnectionState.UNAVAILABLE) {
                    _uiState.update { current ->
                        current.copy(
                            isConnected = false
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            connector.connectedDevice.collect { device ->
                device?.let {
                    _uiState.update { current ->
                        current.copy(
                            isPaired = true,
                            connectedDeviceName = it.displayName,
                            connectedDeviceId = it.nodeId
                        )
                    }
                }
            }
        }
    }

    private fun getOrCreateWearableConnector(): WearableConnector {
        if (wearableConnector == null) {
            wearableConnector = WearableConnector(
                context = getApplication(),
                onReadingReceived = { _, _ -> true },
                onEventReceived = {},
                onAlertReceived = {},
                onHeartbeatReceived = {},
                trustedNodeIdProvider = { prefs.deviceNodeId.first() }
            )
            wearableConnector?.register()
        }
        return wearableConnector!!
    }

    fun loadDispositivo() {
        viewModelScope.launch {
            val storedDeviceName = prefs.deviceName.first()
            val storedDeviceId = prefs.deviceId.first()
            val isConn = wearableConnector?.connectionState?.value == WearableConnectionState.CONNECTED ||
                wearableConnector?.connectionState?.value == WearableConnectionState.STREAMING

            // Render instant cached state with zero delay
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isPaired = !storedDeviceId.isNullOrBlank(),
                    isConnected = isConn,
                    connectedDeviceName = storedDeviceName ?: "BioGuard Wearable",
                    connectedDeviceId = storedDeviceId
                )
            }

            val pacienteId = pacienteRepository.resolvePatientId(prefs) ?: return@launch
            when (val result = repository.getInfoCompleta(pacienteId)) {
                is Resource.Success -> {
                    val info = result.data
                    val name = info?.reloj?.modelo ?: storedDeviceName ?: "BioGuard Wearable"
                    _uiState.update {
                        it.copy(
                            dispositivo = info,
                            connectedDeviceName = name
                        )
                    }
                }
                else -> Unit
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, error = null, dispositivosDisponibles = emptyList()) }

            val detectedRealDevices = mutableSetOf<DispositivoScanItem>()

            // 1. Discover WearOS wearables via Wearable API
            val connector = getOrCreateWearableConnector()
            try {
                val wearables = connector.getAvailableWearables(forceWearOsRetry = true)
                for (device in wearables) {
                    detectedRealDevices.add(
                        DispositivoScanItem(
                            id = device.nodeId,
                            nombre = device.displayName,
                            macAddress = device.nodeId,
                            wearNodeId = device.nodeId,
                            rssi = if (device.isNearby) -45 else -70,
                            tipo = if (device.isNearby) "WearOS (Cercano)" else "WearOS (Conectado)",
                            bateria = 100
                        )
                    )
                }
            } catch (e: Exception) {
                android.util.Log.w("DeviceViewModel", "WearOS discovery error: ${e.message}")
            }

            // 2. Check Bluetooth hardware
            val bluetoothManager = getApplication<Application>().getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bluetoothManager?.adapter

            if (adapter == null || !adapter.isEnabled) {
                if (detectedRealDevices.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            dispositivosDisponibles = emptyList(),
                            error = "El módulo Bluetooth está desactivado. Por favor activa el Bluetooth en tu dispositivo."
                        )
                    }
                    return@launch
                }
            }

            // 3. Execute BLE scan with low latency settings
            val scanner = adapter?.bluetoothLeScanner
            var scanStarted = false

            val scanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult?) {
                    result?.device?.let { dev ->
                        val rawName = try { dev.name } catch (_: SecurityException) { null }
                        val name = rawName ?: "Dispositivo BLE (${dev.address.takeLast(5)})"
                        val item = DispositivoScanItem(
                            id = dev.address,
                            nombre = name,
                            macAddress = dev.address,
                            rssi = result.rssi,
                            tipo = "Bluetooth LE Peripheral"
                        )
                        if (detectedRealDevices.none { it.id == item.id }) {
                            detectedRealDevices.add(item)
                            _uiState.update { state ->
                                state.copy(dispositivosDisponibles = detectedRealDevices.toList())
                            }
                        }
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    val errorMsg = when (errorCode) {
                        SCAN_FAILED_ALREADY_STARTED -> "Escaneo ya en progreso"
                        SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "Fallo al registrar la aplicación para escaneo BLE"
                        SCAN_FAILED_FEATURE_UNSUPPORTED -> "Bluetooth LE no es compatible con este dispositivo"
                        SCAN_FAILED_INTERNAL_ERROR -> "Error interno del escáner Bluetooth"
                        else -> "Error de escaneo BLE (código: $errorCode)"
                    }
                    android.util.Log.e("DeviceViewModel", "BLE scan failed: $errorMsg")
                    _uiState.update { it.copy(error = errorMsg) }
                }
            }
            activeScanCallback = scanCallback

            if (scanner != null) {
                try {
                    val settings = ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build()
                    scanner.startScan(null, settings, scanCallback)
                    scanStarted = true
                } catch (e: SecurityException) {
                    android.util.Log.e("DeviceViewModel", "BLE scan SecurityException: ${e.message}")
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            error = "Se requieren permisos de Bluetooth para buscar dispositivos. Ve a Ajustes > Aplicaciones > BioGuard > Permisos."
                        )
                    }
                    return@launch
                } catch (e: Exception) {
                    android.util.Log.e("DeviceViewModel", "BLE scan error: ${e.message}")
                }
            }

            // Extended scan window for better discovery
            delay(10000)

            if (scanStarted && scanner != null) {
                try {
                    scanner.stopScan(scanCallback)
                } catch (_: Exception) { }
            }
            activeScanCallback = null

            val finalDevices = detectedRealDevices.toList()

            _uiState.update {
                it.copy(
                    isScanning = false,
                    dispositivosDisponibles = finalDevices,
                    error = if (finalDevices.isEmpty()) {
                        if (connector.connectionState.value == WearableConnectionState.UNAVAILABLE) {
                            "Wear OS no esta disponible en este telefono. Instala/configura la app companera del reloj o vincula el wearable con el QR/Bluetooth."
                        } else {
                            "No se encontraron dispositivos wearables cercanos. Asegurate de que tu wearable este encendido, con Bluetooth activo y cerca del telefono."
                        }
                    } else {
                        null
                    }
                )
            }
        }
    }

    fun stopScan() {
        viewModelScope.launch {
            activeScanCallback?.let { callback ->
                try {
                    val app = getApplication<Application>()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        ContextCompat.checkSelfPermission(app, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return@let
                    }
                    val bluetoothManager = getApplication<Application>().getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                    bluetoothManager?.adapter?.bluetoothLeScanner?.stopScan(callback)
                } catch (_: Exception) { }
            }
            activeScanCallback = null
        }
        _uiState.update { it.copy(isScanning = false) }
    }

    fun vincularDispositivo(item: DispositivoScanItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val nodeId = item.wearNodeId
            if (nodeId.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Este periférico BLE no anuncia la app BioGuard. Vincúlalo con el QR del reloj."
                    )
                }
                return@launch
            }
            val nonceToUse = item.pairingNonce.takeIf { !it.isNullOrBlank() } ?: "auto-pair-${System.currentTimeMillis()}"
            prefs.saveDeviceData(
                deviceId = item.id,
                deviceName = item.nombre,
                isConnected = false,
                nodeId = nodeId
            )

            val connected = getOrCreateWearableConnector().pairWithNode(nodeId, nonceToUse) &&
                reconnectAfterPairing()
            if (!connected) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isPaired = true,
                        isConnected = false,
                        connectedDeviceName = item.nombre,
                        connectedDeviceId = item.id,
                        error = "El reloj quedó identificado, pero no confirmó la conexión local. Mantén ambos dispositivos cerca y reintenta."
                    )
                }
                return@launch
            }

            prefs.saveDeviceData(item.id, item.nombre, true, nodeId)
            BioGuardMonitoringService.start(getApplication())
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isPaired = true,
                    isConnected = true,
                    connectedDeviceName = item.nombre,
                    connectedDeviceId = item.id,
                    dispositivosDisponibles = emptyList(),
                    successMessage = "${item.nombre} vinculado localmente"
                )
            }

            // El registro remoto es secundario y nunca invalida una vinculación local válida.
            when (repository.vincularDispositivo(item.nombre, item.macAddress)) {
                is Resource.Error -> _uiState.update {
                    it.copy(successMessage = "Vinculado localmente; registro en la nube pendiente")
                }
                else -> Unit
            }
        }
    }

    private suspend fun reconnectAfterPairing(): Boolean {
        val device = getOrCreateWearableConnector().discoverAndConnect(forceWearOsRetry = true)
        return device != null
    }

    fun vincularWearableDesdeQr(rawPayload: String): Boolean {
        val item = parseWearableQrPayload(rawPayload)
        if (item == null) {
            _uiState.update { it.copy(error = "El QR escaneado no contiene una identidad de wearable BioGuard valida") }
            return false
        }
        vincularDispositivo(item)
        return true
    }

    fun vincularWearableAutomatico() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val connector = getOrCreateWearableConnector()
            val device = connector.discoverAndConnect(forceWearOsRetry = true)
            if (device != null) {
                val item = DispositivoScanItem(
                    id = device.nodeId,
                    nombre = device.displayName,
                    macAddress = device.nodeId,
                    wearNodeId = device.nodeId,
                    rssi = -45,
                    tipo = "WearOS Data Layer (auto)"
                )
                vincularDispositivo(item)
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "No se detectó ningún reloj WearOS. Verifica que BioGuard esté abierto en el reloj."
                    )
                }
            }
        }
    }

    fun desconectarDispositivo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            prefs.clearDeviceData()
            BioGuardMonitoringService.stop(getApplication())
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isPaired = false,
                    isConnected = false,
                    connectedDeviceName = null,
                    connectedDeviceId = null,
                    dispositivo = null,
                    successMessage = "Dispositivo desconectado"
                )
            }
        }
    }

    fun sendSamplingRate(intervalSeconds: Int) {
        val connector = getOrCreateWearableConnector()
        connector.sendSamplingRateCommand(intervalSeconds)
        _uiState.update {
            it.copy(successMessage = "Frecuencia de muestreo enviada al reloj ($intervalSeconds s)")
        }
    }

    fun toggleSensor(sensorName: String, enabled: Boolean) {
        val connector = getOrCreateWearableConnector()
        connector.sendSensorToggleCommand(sensorName, enabled)
        val estado = if (enabled) "activado" else "desactivado"
        _uiState.update {
            it.copy(successMessage = "Sensor '$sensorName' $estado en el reloj")
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        wearableConnector?.unregister()
        wearableConnector = null
    }
}

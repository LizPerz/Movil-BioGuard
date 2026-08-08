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
    val rssi: Int = -60,
    val tipo: String = "Bluetooth LE / WearOS",
    val bateria: Int = 100
)

private fun parseWearableQrPayload(raw: String): DispositivoScanItem? {
    val payload = WearablePairingQr.parse(raw) ?: return null
    return DispositivoScanItem(
        id = payload.address,
        nombre = payload.name,
        macAddress = payload.address,
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
                onReadingReceived = {},
                onEventReceived = {},
                onAlertReceived = {},
                onHeartbeatReceived = {}
            )
            wearableConnector?.register()
        }
        return wearableConnector!!
    }

    fun loadDispositivo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val storedDeviceName = prefs.deviceName.first()
            val storedDeviceId = prefs.deviceId.first()
            val storedConnected = prefs.isDeviceConnected.first()

            val pacienteId = pacienteRepository.resolvePatientId(prefs)
            if (pacienteId == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isPaired = storedDeviceId != null,
                        isConnected = storedConnected,
                        connectedDeviceName = storedDeviceName,
                        connectedDeviceId = storedDeviceId
                    )
                }
                return@launch
            }

            when (val result = repository.getInfoCompleta(pacienteId)) {
                is Resource.Success -> {
                    val info = result.data
                    val isPaired = storedDeviceId != null || info?.reloj?.modelo != null
                    val isConn = info?.reloj?.conectado ?: false
                    val name = info?.reloj?.modelo ?: storedDeviceName
                    val devId = storedDeviceId

                    _uiState.update {
                        it.copy(
                            dispositivo = info,
                            isLoading = false,
                            isPaired = isPaired,
                            isConnected = isConn,
                            connectedDeviceName = name,
                            connectedDeviceId = devId
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPaired = storedDeviceId != null,
                            isConnected = storedConnected,
                            connectedDeviceName = storedDeviceName,
                            connectedDeviceId = storedDeviceId,
                            error = if (storedConnected) null else result.message
                        )
                    }
                }
                is Resource.Loading -> {}
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
            prefs.saveDeviceData(item.id, item.nombre, isConnected = true)

            when (val result = repository.vincularDispositivo(item.nombre, item.macAddress)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPaired = true,
                            isConnected = true,
                            connectedDeviceName = item.nombre,
                            connectedDeviceId = item.id,
                            dispositivosDisponibles = emptyList(),
                            successMessage = "Dispositivo '${item.nombre}' vinculado con éxito"
                        )
                    }
                    loadDispositivo()
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPaired = true,
                            isConnected = true,
                            connectedDeviceName = item.nombre,
                            connectedDeviceId = item.id,
                            dispositivosDisponibles = emptyList(),
                            successMessage = "Dispositivo '${item.nombre}' vinculado localmente"
                        )
                    }
                }
                is Resource.Loading -> {}
            }
        }
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

    fun desconectarDispositivo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            prefs.clearDeviceData()
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

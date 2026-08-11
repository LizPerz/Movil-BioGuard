package com.bioguard.movil.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.repository.DispositivoRepository
import com.bioguard.movil.datastore.UserPreferences
import com.bioguard.movil.network.RetrofitClient
import com.bioguard.movil.ui.theme.GreenNeon
import com.bioguard.movil.ui.theme.LocalThemeState
import com.bioguard.movil.ui.theme.colorPalette
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun WearablePairingScreen(
    onComplete: () -> Unit = {}
) {
    val p = LocalThemeState.current.colorPalette()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { UserPreferences(context) }
    val dispositivoRepository = remember { DispositivoRepository(RetrofitClient.api) }

    var isScanning by remember { mutableStateOf(false) }
    var pairedDeviceName by remember { mutableStateOf<String?>(null) }
    var discoveredDevices by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var hasPermissions by remember { mutableStateOf(false) }

    val requiredPermissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        arrayOf(
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.BLUETOOTH
        )
    }

    fun checkPermissions(): Boolean =
        requiredPermissions.all { perm ->
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, perm
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

    suspend fun runScan() {
        isScanning = true
        discoveredDevices = emptyList()

        val bluetoothManager = context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        val adapter = bluetoothManager?.adapter
        if (adapter == null || !adapter.isEnabled) {
            isScanning = false
            Toast.makeText(context, "Activa el Bluetooth del celular para buscar y vincular dispositivos.", Toast.LENGTH_LONG).show()
            return
        }

        val realDevices = mutableListOf<Pair<String, String>>()
        try {
            val nodeClient = com.google.android.gms.wearable.Wearable.getNodeClient(context)
            val nodes = nodeClient.connectedNodes.await()
            for (node in nodes) {
                realDevices.add((node.displayName.ifBlank { "SmartWatch WearOS" }) to node.id)
            }
        } catch (e: Exception) {
            android.util.Log.w("WearablePairing", "WearOS node discovery: ${e.message}")
        }

        try {
            val scanner = adapter.bluetoothLeScanner
            if (scanner != null) {
                val bleDevices = mutableListOf<Pair<String, String>>()
                val scanCallback = object : android.bluetooth.le.ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult?) {
                        result?.device?.let { dev ->
                            val rawName = try { dev.name } catch (_: SecurityException) { null }
                            val name = rawName ?: "Dispositivo BLE (${dev.address.takeLast(5)})"
                            if (bleDevices.none { it.second == dev.address }) {
                                bleDevices.add(name to dev.address)
                                discoveredDevices = realDevices + bleDevices
                            }
                        }
                    }
                    override fun onScanFailed(errorCode: Int) {
                        android.util.Log.w("WearablePairing", "BLE scan failed: $errorCode")
                    }
                }
                try {
                    scanner.startScan(
                        null,
                        android.bluetooth.le.ScanSettings.Builder()
                            .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .build(),
                        scanCallback
                    )
                    kotlinx.coroutines.delay(8000)
                    scanner.stopScan(scanCallback)
                } catch (e: SecurityException) {
                    android.util.Log.w("WearablePairing", "BLE scan permission error: ${e.message}")
                }
                discoveredDevices = realDevices + bleDevices
            }
        } catch (e: Exception) {
            android.util.Log.w("WearablePairing", "BLE scan error: ${e.message}")
        }

        isScanning = false

        if (discoveredDevices.isEmpty()) {
            Toast.makeText(context, "No se detectaron dispositivos wearables encendidos ni cercanos. Asegurate de tener Bluetooth activado.", Toast.LENGTH_LONG).show()
        }
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasPermissions = checkPermissions()
        if (hasPermissions) {
            scope.launch { runScan() }
        } else {
            Toast.makeText(context, "Se requieren permisos de Bluetooth y ubicacion para detectar dispositivos.", Toast.LENGTH_LONG).show()
        }
    }

    val enableBtLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) {
        scope.launch { runScan() }
    }

    fun triggerScan() {
        if (!hasPermissions) {
            permissionLauncher.launch(requiredPermissions)
            return
        }
        val btManager = context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        val btAdapter = btManager?.adapter
        if (btAdapter == null || !btAdapter.isEnabled) {
            enableBtLauncher.launch(android.content.Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }
        scope.launch { runScan() }
    }

    LaunchedEffect(Unit) {
        hasPermissions = checkPermissions()
        pairedDeviceName = prefs.deviceName.first()
        if (hasPermissions) {
            val btManager = context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
            val btAdapter = btManager?.adapter
            if (btAdapter != null && !btAdapter.isEnabled) {
                enableBtLauncher.launch(android.content.Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        }
    }

    fun vincular(nombre: String, mac: String) {
        val bluetoothManager = context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        val adapter = bluetoothManager?.adapter
        if (adapter == null || !adapter.isEnabled) {
            enableBtLauncher.launch(android.content.Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }
        scope.launch {
            prefs.saveDeviceData(mac, nombre, isConnected = true)
            pairedDeviceName = nombre

            val pacienteId = prefs.patientId.first() ?: prefs.userId.first()
            if (!pacienteId.isNullOrBlank()) {
                when (val result = dispositivoRepository.vincularDispositivo(nombre, mac, pacienteId)) {
                    is Resource.Success -> {}
                    is Resource.Error -> android.util.Log.w("WearablePairing", "Sync de dispositivo: ${result.message}")
                    is Resource.Loading -> {}
                }
            }

            Toast.makeText(context, "Dispositivo '$nombre' vinculado correctamente", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(p.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "VINCULACION WEARABLE",
                fontSize = 11.sp,
                color = p.accent,
                letterSpacing = 3.sp
            )
            Text(
                text = "Conecta tu smartwatch o parche biometrico",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = p.textPrimary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(130.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(p.accent.copy(alpha = 0.08f))
                        .border(width = 1.dp, color = p.accent.copy(alpha = 0.2f), shape = CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(85.dp)
                        .clip(CircleShape)
                        .background(p.accent.copy(alpha = 0.12f))
                        .border(width = 1.dp, color = p.accent.copy(alpha = 0.3f), shape = CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(p.surface)
                        .border(width = 2.dp, color = if (pairedDeviceName != null) GreenNeon else p.accent, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (pairedDeviceName != null) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Vinculado",
                            tint = GreenNeon,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Watch,
                            contentDescription = "Wearable",
                            tint = p.accent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (pairedDeviceName != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(GreenNeon.copy(alpha = 0.1f))
                        .border(width = 1.dp, color = GreenNeon, shape = RoundedCornerShape(10.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Dispositivo Vinculado", fontWeight = FontWeight.Bold, color = GreenNeon, fontSize = 15.sp)
                        Text(text = pairedDeviceName ?: "", color = p.textPrimary, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(p.surface)
                        .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(10.dp))
                        .clickable { if (!isScanning) triggerScan() }
                        .padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isScanning) {
                            CircularProgressIndicator(color = p.accent, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = "Escaneando dispositivos cercanos...", fontSize = 13.sp, color = p.textPrimary)
                        } else {
                            Icon(
                                imageVector = Icons.Filled.BluetoothSearching,
                                contentDescription = "Buscar",
                                tint = p.accent,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Toca para buscar dispositivos cercanos",
                                fontSize = 14.sp,
                                color = p.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Asegurate de tener Bluetooth activado",
                                fontSize = 11.sp,
                                color = p.textSecondary
                            )
                        }
                    }
                }
            }

            if (discoveredDevices.isNotEmpty() && pairedDeviceName == null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "DISPOSITIVOS DISPONIBLES", fontSize = 10.sp, color = p.accent, letterSpacing = 2.sp, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                discoveredDevices.forEach { (nombre, mac) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(p.surface)
                            .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = nombre, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = p.textPrimary)
                            Text(text = mac, fontSize = 10.sp, color = p.textSecondary)
                        }
                        Button(
                            onClick = { vincular(nombre, mac) },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = p.accent)
                        ) {
                            Text(text = "Vincular", fontSize = 11.sp, color = p.background, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = p.accent)
            ) {
                Text(text = "FINALIZAR E IR AL INICIO", color = p.background, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = p.textSecondary),
                border = androidx.compose.foundation.BorderStroke(1.dp, p.border)
            ) {
                Text(text = "OMITIR POR AHORA", color = p.textSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Puedes vincularlo despues desde la pestaña Dispositivo",
                fontSize = 11.sp,
                color = p.textTertiary
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

package com.bioguard.movil.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bioguard.movil.R
import com.bioguard.movil.ui.components.ErrorRetryBox
import com.bioguard.movil.ui.theme.GreenNeon
import com.bioguard.movil.ui.theme.LocalThemeState
import com.bioguard.movil.ui.theme.RedNeon
import com.bioguard.movil.ui.theme.colorPalette
import com.bioguard.movil.service.WearableConnectionState
import com.bioguard.movil.ui.viewmodel.DeviceViewModel
import com.bioguard.movil.ui.viewmodel.DispositivoScanItem

@Composable
fun DeviceScreen(
    deviceViewModel: DeviceViewModel,
    onNavigateToWearableQr: () -> Unit = {}
) {
    val p = LocalThemeState.current.colorPalette()
    val uiState by deviceViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val bluetoothPermissionError = stringResource(R.string.device_bt_permission_error)

    var showDisconnectDialog by remember { mutableStateOf(false) }

    // ── Runtime Bluetooth Permission Handling (Android 12+ / API 31+) ──
    // Root cause fix: BLE scanning requires BLUETOOTH_SCAN + BLUETOOTH_CONNECT at runtime on API 31+.
    // Without requesting these permissions, BluetoothLeScanner.startScan() throws SecurityException
    // which was being silently caught, causing "no devices found" every time.
    val bluetoothPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    var hasBluetoothPermissions by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasBluetoothPermissions = results.values.all { it }
        if (hasBluetoothPermissions) {
            // Permissions granted — automatically start scanning
            deviceViewModel.startScan()
        } else {
            Toast.makeText(
                context,
                bluetoothPermissionError,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Check current permission state on composition
    LaunchedEffect(Unit) {
        hasBluetoothPermissions = bluetoothPermissions.all { perm ->
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, perm
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    // Function that checks permissions before scanning
    fun startScanWithPermissionCheck() {
        if (hasBluetoothPermissions) {
            deviceViewModel.startScan()
        } else {
            permissionLauncher.launch(bluetoothPermissions)
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            deviceViewModel.clearMessages()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "connected")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val disp = uiState.dispositivo
    val isPaired = uiState.isPaired || uiState.connectedDeviceId != null || disp?.vinculado == true
    val isConnected = uiState.isConnected || (disp?.conectado ?: false)
    val connectionStateText = uiState.connectionState.toDisplayString(uiState.lastSyncMillis)
    val deviceName = uiState.connectedDeviceName ?: disp?.nombreDispositivo ?: stringResource(R.string.device_no_device)
    val bateria: Int? = null
    val ultimaSincronizacion: String? = disp?.fechaVinculacion
    val sensores: List<String> = emptyList()

    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text(text = stringResource(R.string.device_disconnect_title), color = p.textPrimary) },
            text = { Text(text = stringResource(R.string.device_disconnect_msg, deviceName), color = p.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showDisconnectDialog = false
                        deviceViewModel.desconectarDispositivo()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedNeon)
                ) {
                    Text(text = stringResource(R.string.device_disconnect), color = p.background)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text(text = stringResource(R.string.common_cancel), color = p.textSecondary)
                }
            },
            containerColor = p.surface
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(p.background)
    ) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = p.accent)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.device_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.accent,
                    letterSpacing = 3.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = stringResource(R.string.device_desc),
                    fontSize = 12.sp,
                    color = p.textSecondary,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // ── Card de Estado del Dispositivo ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(p.surface)
                        .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(if (isPaired) p.accentDark.copy(alpha = 0.2f) else p.surface)
                                .border(
                                    width = 2.dp,
                                    color = if (isConnected) p.accent.copy(alpha = pulseAlpha) else if (isPaired) p.accent.copy(alpha = 0.35f) else p.border,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "⌚", fontSize = 40.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (isPaired) deviceName else "Sin dispositivo vinculado",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = p.textPrimary
                        )

                        Text(
                            text = if (isPaired) stringResource(R.string.device_wearable_type) else stringResource(R.string.device_no_wearable),
                            fontSize = 12.sp,
                            color = p.textSecondary
                        )

                        if (uiState.connectionState != WearableConnectionState.DISCONNECTED && uiState.connectionState != WearableConnectionState.CONNECTED) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = connectionStateText,
                                fontSize = 10.sp,
                                color = if (uiState.connectionState == WearableConnectionState.ERROR) RedNeon else p.accent,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isConnected) GreenNeon.copy(alpha = 0.1f) else RedNeon.copy(alpha = 0.1f)
                                )
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isConnected) GreenNeon else RedNeon)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isConnected) stringResource(R.string.device_connected) else if (isPaired) "Vinculado sin conexion" else stringResource(R.string.device_disconnected),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isConnected) GreenNeon else RedNeon,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Detalles del Dispositivo (Si está conectado) ──
                if (isPaired) {
                    Text(
                        text = stringResource(R.string.device_info),
                        fontSize = 10.sp,
                        color = p.accent,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    DeviceInfoRow(label = stringResource(R.string.device_model), value = deviceName)
                    DeviceInfoRow(label = stringResource(R.string.device_connection_status), value = if (isConnected) stringResource(R.string.device_active) else "Vinculado, esperando Bluetooth")
                    bateria?.let {
                        DeviceInfoRow(label = stringResource(R.string.device_battery), value = "$it%")
                    }
                    ultimaSincronizacion?.let {
                        DeviceInfoRow(label = stringResource(R.string.device_last_sync), value = it.replace("T", " ").substringBefore("."))
                    }
                    DeviceInfoRow(label = stringResource(R.string.device_sensors), value = sensores.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "Sin reportar")

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { startScanWithPermissionCheck() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = p.accent)
                        ) {
                            Text(
                                text = "🔄 ${stringResource(R.string.device_change_btn)}",
                                color = p.background,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { showDisconnectDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RedNeon)
                        ) {
                            Text(
                                text = "🚫 ${stringResource(R.string.device_disconnect)}",
                                color = RedNeon,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    // ── Botón Principal de Emparejamiento Automático ──
                    Button(
                        onClick = { deviceViewModel.vincularWearableAutomatico() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = p.accent)
                    ) {
                        Text(
                            text = "⚡ Conectar automáticamente",
                            color = p.background,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onNavigateToWearableQr,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = p.accent)
                    ) {
                        Text(
                            text = "Escanear QR del wearable",
                            color = p.accent,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { startScanWithPermissionCheck() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = p.accent)
                    ) {
                        Text(
                            text = "🔍 ${stringResource(R.string.device_scan_btn)}",
                            color = p.background,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 14.sp
                        )
                    }
                }

                // ── Lista de Escaneo de Dispositivos ──
                if (uiState.isScanning) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(p.surface)
                            .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(12.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = p.accent, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.device_scanning),
                                fontSize = 13.sp,
                                color = p.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // ── Error feedback ──
                uiState.error?.let { errorMsg ->
                    Spacer(modifier = Modifier.height(16.dp))
                    ErrorRetryBox(
                        message = errorMsg,
                        onRetry = { startScanWithPermissionCheck() }
                    )
                }

                if (uiState.dispositivosDisponibles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.device_detected, uiState.dispositivosDisponibles.size.toString()),
                        fontSize = 10.sp,
                        color = p.accent,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    uiState.dispositivosDisponibles.forEach { dev ->
                        DispositivoScanCard(
                            item = dev,
                            onConnect = { deviceViewModel.vincularDispositivo(dev) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun DispositivoScanCard(
    item: DispositivoScanItem,
    onConnect: () -> Unit
) {
    val p = LocalThemeState.current.colorPalette()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(p.surface)
            .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "⌚", fontSize = 28.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = item.nombre,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.textPrimary
                )
                Text(
                    text = "${item.tipo} • Señal ${item.rssi} dBm",
                    fontSize = 11.sp,
                    color = p.textSecondary
                )
            }
        }

        Button(
            onClick = onConnect,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = p.accent)
        ) {
            Text(
                text = stringResource(R.string.device_link),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = p.background
            )
        }
    }
}

@Composable
fun DeviceInfoRow(label: String, value: String) {
    val p = LocalThemeState.current.colorPalette()
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
        Text(text = label, fontSize = 10.sp, color = p.textSecondary, letterSpacing = 1.sp)
        Text(text = value, fontSize = 12.sp, color = p.textPrimary, fontWeight = FontWeight.Medium)
    }
    Spacer(modifier = Modifier.height(6.dp))
}

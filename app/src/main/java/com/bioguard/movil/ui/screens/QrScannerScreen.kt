package com.bioguard.movil.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.bioguard.movil.R
import com.bioguard.movil.ui.theme.CyanNeon
import com.bioguard.movil.ui.theme.DarkBackground
import com.bioguard.movil.ui.theme.DarkSurface
import com.bioguard.movil.ui.theme.TextPrimary
import com.bioguard.movil.ui.theme.TextSecondary
import com.bioguard.movil.ui.theme.TextTertiary
import com.bioguard.movil.ui.viewmodel.AuthViewModel
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

enum class QrScannerMode {
    LOGIN,
    WEARABLE_PAIRING
}

@OptIn(ExperimentalGetImage::class)
private fun analyzeImageProxy(
    imageProxy: ImageProxy,
    scanner: BarcodeScanner,
    isScanning: AtomicBoolean,
    onBarcodeDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val input = InputImage.fromMediaImage(
            mediaImage, imageProxy.imageInfo.rotationDegrees
        )
        scanner.process(input)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty() && isScanning.compareAndSet(true, false)) {
                    val value = barcodes.firstOrNull()?.rawValue
                    android.util.Log.d("QrScanner", "QR detectado: ${value?.take(50)}...")
                    if (value != null) {
                        onBarcodeDetected(value)
                    }
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("QrScanner", "Error escaneando QR: ${e.message}")
                imageProxy.close()
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

/**
 * Extrae el código de acceso de 8 caracteres desde cualquier payload QR:
 * - Texto plano: "12345678"
 * - URL con parámetro code: "...?code=12345678&userId=abc"
 * - JSON/pipe/cualquier envoltura que contenga el código
 */
private fun extractCodigoAcceso(rawValue: String): String {
    val trimmed = rawValue.trim()
    if (trimmed.isBlank()) return trimmed

    // 1. Intenta un query parameter "code" (URL del portal web)
    val uri = runCatching { android.net.Uri.parse(trimmed) }.getOrNull()
    uri?.getQueryParameter("code")?.takeIf { it.isNotBlank() }?.let { return it.trim() }

    // 2. Busca un token contiguo de 8 alfanuméricos
    Regex("""\b[A-Za-z0-9]{8}\b""").find(trimmed)?.value?.let { return it }

    // 3. Fallback: primeros 8 caracteres alfanuméricos de la cadena
    val alnum = trimmed.filter { it.isLetterOrDigit() }
    return if (alnum.length >= 8) alnum.take(8) else trimmed
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun QrScannerScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val uiState by authViewModel.uiState.collectAsState()
    QrScannerScreenContent(
        mode = QrScannerMode.LOGIN,
        onCodeDetected = { authViewModel.loginWithCode(extractCodigoAcceso(it)) },
        isBusy = uiState.isLoading,
        isSuccessful = uiState.isAuthenticated,
        errorMessage = uiState.error,
        clearError = authViewModel::clearError,
        onSuccess = onLoginSuccess,
        onBack = onBack
    )
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun WearableQrScannerScreen(
    isProcessing: Boolean,
    errorMessage: String?,
    onQrDetected: (String) -> Unit,
    onBack: () -> Unit = {},
    onClearError: () -> Unit = {}
) {
    QrScannerScreenContent(
        mode = QrScannerMode.WEARABLE_PAIRING,
        onCodeDetected = { raw ->
            android.util.Log.d("QrScanner", "Raw QR: $raw")
            onQrDetected(raw)
        },
        isBusy = isProcessing,
        isSuccessful = false,
        errorMessage = errorMessage,
        clearError = onClearError,
        onSuccess = {},
        onBack = onBack
    )
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun QrScannerScreenContent(
    mode: QrScannerMode,
    onCodeDetected: (String) -> Unit,
    isBusy: Boolean,
    isSuccessful: Boolean,
    errorMessage: String?,
    clearError: () -> Unit,
    onSuccess: () -> Unit,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraErrorMessage = stringResource(R.string.qr_camera_error)

    var hasPermission by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val isScanning = remember { AtomicBoolean(true) }

    val previewView = remember { PreviewView(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
        )
    }

    LaunchedEffect(previewView) {
        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) {
            cameraError = context.getString(R.string.qr_permission_denied)
        }
    }

    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            cameraError = it
            isScanning.set(true)
            clearError()
        }
    }

    LaunchedEffect(isBusy, isSuccessful) {
        if (isSuccessful) {
            onSuccess()
        }
        if (!isBusy) {
            isScanning.set(true)
        }
    }

    DisposableEffect(lifecycleOwner, hasPermission) {
        var cameraProvider: androidx.camera.lifecycle.ProcessCameraProvider? = null
        var bound = false

        if (hasPermission) {
            try {
                cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    if (isScanning.get()) {
                        analyzeImageProxy(imageProxy, barcodeScanner, isScanning) { value ->
                            onCodeDetected(value)
                        }
                    } else {
                        imageProxy.close()
                    }
                }

                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
                // Enable auto focus metering for close-up QR scans (once the view is laid out)
                try {
                    if (previewView.width > 0 && previewView.height > 0) {
                        val factory = previewView.meteringPointFactory
                        val point = factory.createPoint(previewView.width / 2f, previewView.height / 2f)
                        val action = androidx.camera.core.FocusMeteringAction.Builder(point)
                            .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                            .build()
                        camera.cameraControl.startFocusAndMetering(action)
                    }
                } catch (_: Exception) { }
                bound = true
            } catch (e: Exception) {
                cameraError = cameraErrorMessage
            }
        }

        onDispose {
            isScanning.set(false)
            if (bound) {
                try {
                    ProcessCameraProvider.getInstance(context).get().unbindAll()
                } catch (_: Exception) { }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            isScanning.set(false)
            analysisExecutor.shutdown()
            try {
                barcodeScanner.close()
            } catch (_: Exception) { }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        if (hasPermission) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .border(width = 2.dp, color = CyanNeon, shape = RoundedCornerShape(16.dp))
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.qr_permission_required),
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.qr_permission_desc),
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
                ) {
                    Text(stringResource(R.string.qr_request_permission), color = DarkBackground, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 12.sp)
                }
            }
        }

        cameraError?.let { error ->
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface.copy(alpha = 0.9f))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = error,
                    color = CyanNeon,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        cameraError = null
                        isScanning.set(true)
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
                ) {
                    Text("Reintentar", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.qr_cancel), color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            if (isBusy) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = CyanNeon, strokeWidth = 2.dp)
            } else if (cameraError == null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface.copy(alpha = 0.8f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (mode == QrScannerMode.LOGIN) stringResource(R.string.qr_point_code) else "Apunta al QR del wearable",
                        color = TextTertiary,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

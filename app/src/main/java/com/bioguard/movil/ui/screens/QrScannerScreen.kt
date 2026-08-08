package com.bioguard.movil.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import androidx.compose.ui.res.stringResource
import com.bioguard.movil.R
import java.util.concurrent.Executors

enum class QrScannerMode {
    LOGIN,
    WEARABLE_PAIRING
}

@OptIn(ExperimentalGetImage::class)
private fun analyzeImageProxy(
    imageProxy: ImageProxy,
    scanner: BarcodeScanner,
    onBarcodeDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val input = InputImage.fromMediaImage(
            mediaImage, imageProxy.imageInfo.rotationDegrees
        )
        scanner.process(input)
            .addOnSuccessListener { barcodes ->
                val value = barcodes.firstOrNull()?.rawValue
                if (value != null) {
                    onBarcodeDetected(value)
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    } else {
        imageProxy.close()
    }
}

private fun extractCodigoAcceso(rawValue: String): String {
    val trimmed = rawValue.trim()
    val uri = runCatching { android.net.Uri.parse(trimmed) }.getOrNull()
    val code = uri?.getQueryParameter("code")
    if (code.isNullOrEmpty()) return trimmed
    return code
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
        onCodeDetected = onQrDetected,
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
    var isProcessing by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("") }

    val previewView = remember { PreviewView(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(previewView) {
        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage.isNotEmpty()) {
            Toast.makeText(context.applicationContext, toastMessage, Toast.LENGTH_SHORT).show()
            toastMessage = ""
        }
    }

    LaunchedEffect(isBusy, isSuccessful) {
        if (isSuccessful) {
            onSuccess()
        }
        if (!isBusy) {
            isProcessing = false
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            toastMessage = it
            isProcessing = false
            clearError()
        }
    }

    DisposableEffect(lifecycleOwner, hasPermission) {
        var scanner: BarcodeScanner? = null

        if (hasPermission) {
            // Create a FRESH scanner instance for this effect lifecycle
            scanner = BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
            )
            val currentScanner = scanner

            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val listener = Runnable {
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val resolutionSelector = ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(1280, 720),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                            )
                        )
                        .build()
                    val analysis = ImageAnalysis.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        analyzeImageProxy(imageProxy, currentScanner) { value ->
                            if (!isProcessing) {
                                isProcessing = true
                                onCodeDetected(value)
                            }
                        }
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                } catch (e: Exception) {
                    toastMessage = cameraErrorMessage
                }
            }
            cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))
        }

        onDispose {
            try {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            } catch (_: Exception) { }
            scanner?.close()
        }
    }

    DisposableEffect(Unit) {
        onDispose { analysisExecutor.shutdown() }
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
            if (isProcessing || isBusy) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = CyanNeon, strokeWidth = 2.dp)
            } else {
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

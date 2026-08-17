package com.bioguard.movil

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.bioguard.movil.datastore.UserPreferences
import com.bioguard.movil.navigation.BioGuardApp
import com.bioguard.movil.ui.theme.AppTheme
import com.bioguard.movil.ui.theme.BioGuardMovilTheme
import com.bioguard.movil.ui.theme.ThemeState
import com.bioguard.movil.util.BiometricHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        requestRuntimePermissions()
        requestBluetoothEnabled()
        checkRootAndWarn()

        val openAlert = intent?.getBooleanExtra("open_alert", false) ?: false

        enableEdgeToEdge()
        setContent {
            var themeState by remember { mutableStateOf(ThemeState()) }
            val context = LocalContext.current
            val prefs = remember { UserPreferences(context) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                val savedTheme = prefs.theme.first()
                val savedDark = prefs.isDarkMode.first()
                themeState = ThemeState(
                    theme = runCatching { AppTheme.valueOf(savedTheme ?: AppTheme.OSCURO.name) }.getOrDefault(AppTheme.OSCURO),
                    isDarkMode = savedDark
                )
            }

            BioGuardMovilTheme(themeState = themeState) {
                BioGuardApp(
                    themeState = themeState,
                    openAlert = openAlert,
                    onThemeChange = { newState ->
                        themeState = newState
                        scope.launch { prefs.saveTheme(newState.theme.name, newState.isDarkMode) }
                    }
                )
            }
        }
    }

    private fun requestRuntimePermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        val missing = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun requestBluetoothEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
            if (!adapter.isEnabled) {
                startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        } catch (_: Exception) {
        }
    }

    private fun checkRootAndWarn() {
        val paths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
            "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su"
        )
        val isRooted = paths.any { java.io.File(it).exists() } || (Build.TAGS != null && Build.TAGS.contains("test-keys"))
        if (isRooted) {
            Toast.makeText(this, "Dispositivo con acceso Root detectado", Toast.LENGTH_SHORT).show()
        }
    }
}

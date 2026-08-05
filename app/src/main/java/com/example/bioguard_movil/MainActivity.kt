package com.example.bioguard_movil

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.MotionEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.bioguard_movil.datastore.UserPreferences
import com.example.bioguard_movil.navigation.Screen
import com.example.bioguard_movil.ui.components.BioGuardBottomNavBar
import com.example.bioguard_movil.ui.components.BottomNavItem
import com.example.bioguard_movil.ui.model.UserRole
import com.example.bioguard_movil.ui.screens.AlertScreen
import com.example.bioguard_movil.ui.screens.AnalysisScreen
import com.example.bioguard_movil.ui.screens.CuidadorScreen
import com.example.bioguard_movil.ui.screens.DashboardScreen
import com.example.bioguard_movil.ui.screens.DeviceScreen
import com.example.bioguard_movil.ui.screens.HistoryScreen
import com.example.bioguard_movil.ui.screens.LoginScreen
import com.example.bioguard_movil.ui.screens.MedicationScreen
import com.example.bioguard_movil.ui.screens.NotificationsScreen
import com.example.bioguard_movil.ui.screens.OnboardingScreen
import com.example.bioguard_movil.ui.screens.PasswordRecoveryScreen
import com.example.bioguard_movil.ui.screens.ProfileScreen
import com.example.bioguard_movil.ui.screens.QrScannerScreen
import com.example.bioguard_movil.ui.screens.RegisterScreen
import com.example.bioguard_movil.ui.screens.ReportsScreen
import com.example.bioguard_movil.ui.screens.SplashScreen
import com.example.bioguard_movil.ui.theme.AppTheme
import com.example.bioguard_movil.ui.theme.BioGuardMovilTheme
import com.example.bioguard_movil.ui.theme.ThemeState
import com.example.bioguard_movil.service.BioGuardMonitoringService
import com.example.bioguard_movil.ui.theme.colorPalette
import com.example.bioguard_movil.ui.viewmodel.AlertViewModel
import com.example.bioguard_movil.ui.viewmodel.AnalysisViewModel
import com.example.bioguard_movil.ui.viewmodel.AuthViewModel
import com.example.bioguard_movil.ui.viewmodel.CuidadorViewModel
import com.example.bioguard_movil.ui.viewmodel.DashboardViewModel
import com.example.bioguard_movil.ui.viewmodel.HistoryViewModel
import com.example.bioguard_movil.ui.viewmodel.DeviceViewModel
import com.example.bioguard_movil.ui.viewmodel.MedicationViewModel
import com.example.bioguard_movil.ui.viewmodel.NotificacionViewModel
import com.example.bioguard_movil.ui.viewmodel.ProfileViewModel
import com.example.bioguard_movil.ui.viewmodel.ReportsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* resultado ignorado; el servicio ya verifica permisos por su cuenta */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        checkRootAndWarn()
        checkBatteryOptimizations()
        requestRuntimePermissions()

        val authViewModel = androidx.lifecycle.ViewModelProvider(this).get(AuthViewModel::class.java)
        startInactivityTimer(authViewModel)

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
                    onThemeChange = { newState ->
                        themeState = newState
                        scope.launch { prefs.saveTheme(newState.theme.name, newState.isDarkMode) }
                    }
                )
            }
        }
    }

    private var lastInteractionTime = System.currentTimeMillis()

    private fun requestRuntimePermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        lastInteractionTime = System.currentTimeMillis()
        return super.dispatchTouchEvent(ev)
    }

    private fun startInactivityTimer(authViewModel: AuthViewModel) {
        lifecycleScope.launch {
            while (true) {
                delay(10000) // check every 10 seconds
                val current = System.currentTimeMillis()
                if (current - lastInteractionTime > 15 * 60 * 1000) {
                    val authState = authViewModel.uiState.value
                    if (authState.isAuthenticated) {
                        authViewModel.logout()
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            "Sesión cerrada automáticamente por inactividad",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun checkBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                AlertDialog.Builder(this)
                    .setTitle("Optimización de Batería")
                    .setMessage("Para evitar que el sistema interrumpa el monitoreo del Guardián Nocturno, excluya a BioGuard de las optimizaciones de batería.")
                    .setPositiveButton("Configurar") { _, _ ->
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:$packageName")
                            }
                            startActivity(intent)
                        } catch (_: Exception) {}
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }
    }

    private fun checkRootAndWarn() {
        var isRooted = false
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in paths) {
            if (java.io.File(path).exists()) {
                isRooted = true
                break
            }
        }
        val tags = android.os.Build.TAGS
        if (tags != null && tags.contains("test-keys")) {
            isRooted = true
        }
        if (!isRooted) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
                val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                if (reader.readLine() != null) {
                    isRooted = true
                }
                process.destroy()
            } catch (_: Exception) {}
        }
        if (!isRooted) {
            val rootPackages = arrayOf(
                "com.noshufou.android.su",
                "com.noshufou.android.su.elite",
                "eu.chainfire.supersu",
                "com.koushikdutta.superuser",
                "com.thirdparty.superuser",
                "com.yellowes.su",
                "com.topjohnwu.magisk",
                "com.kingroot.kinguser",
                "com.mgyun.shua.su",
                "com.shuame.sprite"
            )
            val pm = packageManager
            for (pkg in rootPackages) {
                try {
                    pm.getPackageInfo(pkg, 0)
                    isRooted = true
                    break
                } catch (_: android.content.pm.PackageManager.NameNotFoundException) {}
            }
        }
        if (isRooted) {
            AlertDialog.Builder(this)
                .setTitle("Advertencia de Seguridad")
                .setMessage("Se ha detectado que este dispositivo está comprometido (root). La seguridad y confidencialidad de sus datos médicos podrían estar en riesgo.")
                .setPositiveButton("Entendido", null)
                .show()
        }
    }
}

@Composable
fun BioGuardApp(
    themeState: ThemeState = ThemeState(),
    onThemeChange: (ThemeState) -> Unit = {}
) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsState()
    val role = authState.role

    val context = LocalContext.current
    val appPrefs = remember { UserPreferences(context) }
    val scope = rememberCoroutineScope()

    val navigateAfterAuth: () -> Unit = {
        scope.launch {
            val hasPatient = appPrefs.patientId.first() != null
            val destination = if (hasPatient) Screen.DASHBOARD else Screen.ONBOARDING
            navController.navigate(destination) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    var alertAutoShown by remember { mutableStateOf(false) }

    LaunchedEffect(authState.isAuthenticated, role) {
        if (authState.isAuthenticated && role != UserRole.CUIDADOR) {
            BioGuardMonitoringService.start(context)
        } else {
            BioGuardMonitoringService.stop(context)
        }
    }

    val bottomNavItems = remember(role) {
        when (role) {
            UserRole.CUIDADOR -> listOf(
                BottomNavItem("Inicio", "\uD83C\uDFE0", Screen.DASHBOARD),
                BottomNavItem("Reportes", "\uD83D\uDCCB", Screen.REPORTS),
                BottomNavItem("Perfil", "\uD83D\uDC75", Screen.PROFILE)
            )
            else -> listOf(
                BottomNavItem("Inicio", "\uD83C\uDFE0", Screen.DASHBOARD),
                BottomNavItem("An\u00e1lisis", "\uD83D\uDCCA", Screen.ANALYSIS),
                BottomNavItem("Reportes", "\uD83D\uDCCB", Screen.REPORTS),
                BottomNavItem("Dispositivo", "\u23EC", Screen.DEVICE),
                BottomNavItem("Perfil", "\uD83D\uDC75", Screen.PROFILE)
            )
        }
    }
    val navRoutes = bottomNavItems.map { it.route }
    val showBottomBar = currentRoute in navRoutes

    BackHandler(enabled = showBottomBar && currentRoute != Screen.DASHBOARD) {
        navController.navigate(Screen.DASHBOARD) {
            popUpTo(Screen.DASHBOARD) { inclusive = true }
            launchSingleTop = true
        }
    }

    BackHandler(enabled = !showBottomBar && currentRoute in listOf(
        Screen.REGISTER, Screen.PASSWORD_RECOVERY, Screen.ONBOARDING
    )) {
        navController.navigate(Screen.LOGIN) {
            popUpTo(Screen.LOGIN) { inclusive = true }
            launchSingleTop = true
        }
    }

    val pal = themeState.colorPalette()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = pal.background,
        bottomBar = {
            if (showBottomBar) {
                BioGuardBottomNavBar(
                    items = bottomNavItems,
                    currentRoute = currentRoute ?: "",
                    onItemSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = Screen.SPLASH
            ) {
                composable(Screen.SPLASH) {
                    val context = LocalContext.current
                    val prefs = remember { UserPreferences(context) }
                    val scope = rememberCoroutineScope()
                    SplashScreen(
                        isAuthenticated = authState.isAuthenticated,
                        onFinished = { authed ->
                            scope.launch {
                                val hasPatient = prefs.patientId.first() != null
                                val destination = when {
                                    authed && hasPatient -> Screen.DASHBOARD
                                    authed -> Screen.ONBOARDING
                                    else -> Screen.LOGIN
                                }
                                navController.navigate(destination) {
                                    popUpTo(Screen.SPLASH) { inclusive = true }
                                }
                            }
                        }
                    )
                }
                composable(Screen.LOGIN) {
                    LoginScreen(
                        authViewModel = authViewModel,
                        onLoginSuccess = navigateAfterAuth,
                        onNavigateToRegister = {
                            navController.navigate(Screen.REGISTER)
                        },
                        onNavigateToPasswordRecovery = {
                            navController.navigate(Screen.PASSWORD_RECOVERY)
                        },
                        onNavigateToQr = {
                            navController.navigate(Screen.QR_SCANNER)
                        }
                    )
                }
                composable(Screen.QR_SCANNER) {
                    QrScannerScreen(
                        authViewModel = authViewModel,
                        onLoginSuccess = navigateAfterAuth,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.REGISTER) {
                    RegisterScreen(
                        authViewModel = authViewModel,
                        onRegisterSuccess = navigateAfterAuth,
                        onBackToLogin = { navController.popBackStack() }
                    )
                }
                composable(Screen.PASSWORD_RECOVERY) {
                    PasswordRecoveryScreen(
                        authViewModel = authViewModel,
                        onResetSuccess = {
                            navController.navigate(Screen.LOGIN) {
                                popUpTo(Screen.LOGIN) { inclusive = true }
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.ONBOARDING) {
                    OnboardingScreen(
                        onComplete = {
                            navController.navigate(Screen.DASHBOARD) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        themeState = themeState,
                        onThemeChange = onThemeChange
                    )
                }
                composable(Screen.DASHBOARD) {
                    val dashboardViewModel: DashboardViewModel = viewModel()
                    DashboardScreen(
                        dashboardViewModel = dashboardViewModel,
                        onPendingAlert = {
                            if (!alertAutoShown) {
                                alertAutoShown = true
                                navController.navigate(Screen.ALERT)
                            }
                        }
                    )
                }
                composable(Screen.ANALYSIS) {
                    val analysisViewModel: AnalysisViewModel = viewModel()
                    AnalysisScreen(analysisViewModel)
                }
                composable(Screen.REPORTS) {
                    val reportsViewModel: ReportsViewModel = viewModel()
                    ReportsScreen(
                        reportsViewModel = reportsViewModel,
                        onNavigateToHistory = { navController.navigate(Screen.HISTORY) }
                    )
                }
                composable(Screen.DEVICE) {
                    val deviceViewModel: DeviceViewModel = viewModel()
                    DeviceScreen(deviceViewModel)
                }
                composable(Screen.PROFILE) {
                    val profileViewModel: ProfileViewModel = viewModel()
                    ProfileScreen(
                        profileViewModel = profileViewModel,
                        role = role ?: UserRole.UNKNOWN,
                        onLogout = {
                            alertAutoShown = false
                            authViewModel.logout()
                            navController.navigate(Screen.LOGIN) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        themeState = themeState,
                        onThemeChange = onThemeChange,
                        onNavigateToNotifications = { navController.navigate(Screen.NOTIFICATIONS) },
                        onNavigateToMedications = { navController.navigate(Screen.MEDICATIONS) },
                        onNavigateToCuidadores = { navController.navigate(Screen.CUIDADORES) },
                        onNavigateToSupport = { navController.navigate(Screen.SUPPORT) },
                        onNavigateToSettings = { navController.navigate(Screen.SETTINGS) }
                    )
                }
                composable(Screen.SUPPORT) {
                    val supportViewModel: com.example.bioguard_movil.ui.viewmodel.SupportViewModel = viewModel()
                    com.example.bioguard_movil.ui.screens.SupportScreen(
                        supportViewModel = supportViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.SETTINGS) {
                    val settingsViewModel: com.example.bioguard_movil.ui.viewmodel.SettingsViewModel = viewModel()
                    com.example.bioguard_movil.ui.screens.SettingsScreen(
                        settingsViewModel = settingsViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.ALERT) {
                    val alertViewModel: AlertViewModel = viewModel()
                    AlertScreen(
                        alertViewModel = alertViewModel,
                        onDismiss = { navController.popBackStack() },
                        onEmergencyCall = { navController.popBackStack() }
                    )
                }
                composable(Screen.HISTORY) {
                    val historyViewModel: HistoryViewModel = viewModel()
                    HistoryScreen(
                        historyViewModel = historyViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.NOTIFICATIONS) {
                    val notificacionViewModel: NotificacionViewModel = viewModel()
                    NotificationsScreen(
                        notificacionViewModel = notificacionViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.MEDICATIONS) {
                    val medicationViewModel: MedicationViewModel = viewModel()
                    MedicationScreen(
                        medicationViewModel = medicationViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.CUIDADORES) {
                    val cuidadorViewModel: CuidadorViewModel = viewModel()
                    CuidadorScreen(
                        cuidadorViewModel = cuidadorViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

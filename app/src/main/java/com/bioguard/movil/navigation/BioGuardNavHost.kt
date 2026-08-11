package com.bioguard.movil.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.bioguard.movil.service.BioGuardMonitoringService
import com.bioguard.movil.ui.components.BioGuardBottomNavBar
import com.bioguard.movil.ui.components.BottomNavItem
import com.bioguard.movil.ui.model.AppPermission
import com.bioguard.movil.ui.model.EffectiveAccess
import com.bioguard.movil.ui.model.UserRole
import com.bioguard.movil.ui.screens.*
import com.bioguard.movil.ui.theme.ThemeState
import com.bioguard.movil.ui.theme.colorPalette
import com.bioguard.movil.ui.viewmodel.*

@Composable
fun BioGuardApp(
    themeState: ThemeState,
    openAlert: Boolean = false,
    onThemeChange: (ThemeState) -> Unit = {}
) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()
    val access = authState.access

    val context = LocalContext.current
    val navigateAfterAuth: () -> Unit = {
        val route = if (access.role == UserRole.PACIENTE && !authState.biometriaCompletada) {
            Screen.WELCOME
        } else {
            access.homeRoute()
        }
        navController.navigate(route) {
            popUpTo(0) { inclusive = true }
        }
    }

    var alertAutoShown by remember { mutableStateOf(false) }

    LaunchedEffect(authState.isAuthenticated, access.permissions) {
        if (authState.isAuthenticated && access.allows(AppPermission.DEVICE_PAIR)) {
            BioGuardMonitoringService.start(context)
        } else {
            BioGuardMonitoringService.stop(context)
        }
    }

    val bottomNavItems = remember(access.permissions) {
        buildList {
            if (access.allowsAny(AppPermission.PATIENT_CREATE, AppPermission.PATIENT_READ, AppPermission.ALERT_READ)) {
                add(BottomNavItem("Inicio", Icons.Filled.Home, Screen.DASHBOARD))
            }
            if (access.allows(AppPermission.HEALTH_HISTORY)) {
                add(BottomNavItem("Análisis", Icons.Filled.Insights, Screen.ANALYSIS))
            }
            if (access.allows(AppPermission.HEALTH_SUMMARY)) {
                add(BottomNavItem("Reportes", Icons.Filled.Description, Screen.REPORTS))
            }
            if (access.allowsAny(AppPermission.DEVICE_READ, AppPermission.DEVICE_PAIR)) {
                add(BottomNavItem("Dispositivo", Icons.Filled.Watch, Screen.DEVICE))
            }
            if (access.allows(AppPermission.ACCOUNT_PROFILE)) {
                add(BottomNavItem("Perfil", Icons.Filled.Person, Screen.PROFILE))
            }
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
        Screen.REGISTER, Screen.PASSWORD_RECOVERY, Screen.ONBOARDING, Screen.WELCOME, Screen.WEARABLE_PAIRING
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.SPLASH
            ) {
                // ── Splash ──
                composable(Screen.SPLASH) {
                    SplashScreen(
                        isAuthenticated = authState.isAuthenticated,
                        onFinished = { authenticated ->
                            if (authenticated) {
                                val route = if (access.role == UserRole.PACIENTE && !authState.biometriaCompletada) {
                                    Screen.WELCOME
                                } else {
                                    access.homeRoute()
                                }
                                navController.navigate(route) {
                                    popUpTo(Screen.SPLASH) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Screen.LOGIN) {
                                    popUpTo(Screen.SPLASH) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                // ── Login ──
                composable(Screen.LOGIN) {
                    LoginScreen(
                        authViewModel = authViewModel,
                        onLoginSuccess = { navigateAfterAuth() },
                        onNavigateToPasswordRecovery = { navController.navigate(Screen.PASSWORD_RECOVERY) },
                        onNavigateToRegister = { navController.navigate(Screen.REGISTER) },
                        onNavigateToQr = { navController.navigate(Screen.QR_SCANNER) }
                    )
                }

                // ── Register ──
                composable(Screen.REGISTER) {
                    RegisterScreen(
                        authViewModel = authViewModel,
                        onRegisterSuccess = { navigateAfterAuth() },
                        onBackToLogin = { navController.navigate(Screen.LOGIN) }
                    )
                }

                // ── Password Recovery ──
                composable(Screen.PASSWORD_RECOVERY) {
                    PasswordRecoveryScreen(
                        authViewModel = authViewModel,
                        onResetSuccess = {
                            navController.navigate(Screen.LOGIN) { popUpTo(Screen.PASSWORD_RECOVERY) { inclusive = true } }
                        },
                        onBack = { navController.navigate(Screen.LOGIN) }
                    )
                }

                // ── Onboarding ──
                composable(Screen.ONBOARDING) {
                    OnboardingScreen(
                        onComplete = {
                            navController.navigate(access.homeRoute()) { popUpTo(Screen.ONBOARDING) { inclusive = true } }
                        },
                        themeState = themeState,
                        onThemeChange = onThemeChange
                    )
                }

                // ── Dashboard ──
                composable(
                    route = Screen.DASHBOARD,
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "bioguard://open/dashboard" },
                        navDeepLink { uriPattern = "https://bioguard.app/dashboard" }
                    )
                ) {
                    AuthorizedContent(
                        access = access,
                        anyOf = setOf(AppPermission.PATIENT_CREATE, AppPermission.PATIENT_READ, AppPermission.ALERT_READ),
                        onDenied = { navController.navigate(access.homeRoute()) { launchSingleTop = true } }
                    ) {
                        val dashboardViewModel: DashboardViewModel = hiltViewModel()

                        LaunchedEffect(openAlert, alertAutoShown) {
                            if (openAlert && !alertAutoShown && access.allows(AppPermission.ALERT_READ)) {
                                alertAutoShown = true
                                navController.navigate(Screen.ALERT)
                            }
                        }

                        DashboardScreen(
                            dashboardViewModel = dashboardViewModel,
                            onPendingAlert = { if (access.allows(AppPermission.ALERT_READ)) navController.navigate(Screen.ALERT) }
                        )
                    }
                }

                // ── Analysis ──
                composable(
                    route = Screen.ANALYSIS,
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "bioguard://open/analysis" },
                        navDeepLink { uriPattern = "https://bioguard.app/analysis" }
                    )
                ) {
                    AuthorizedContent(access, setOf(AppPermission.HEALTH_HISTORY), { navController.navigate(access.homeRoute()) }) {
                        val analysisViewModel: AnalysisViewModel = hiltViewModel()
                        AnalysisScreen(analysisViewModel = analysisViewModel)
                    }
                }

                // ── Reports ──
                composable(Screen.REPORTS) {
                    AuthorizedContent(access, setOf(AppPermission.HEALTH_SUMMARY), { navController.navigate(access.homeRoute()) }) {
                        val reportsViewModel: ReportsViewModel = hiltViewModel()
                        ReportsScreen(
                            reportsViewModel = reportsViewModel,
                            canReadHistory = access.allows(AppPermission.HEALTH_HISTORY),
                            onNavigateToHistory = {
                                if (access.allows(AppPermission.HEALTH_HISTORY)) navController.navigate(Screen.HISTORY)
                            }
                        )
                    }
                }

                // ── Device ──
                composable(Screen.DEVICE) {
                    AuthorizedContent(access, setOf(AppPermission.DEVICE_READ, AppPermission.DEVICE_PAIR), { navController.navigate(access.homeRoute()) }) {
                        val deviceViewModel: DeviceViewModel = hiltViewModel()
                        DeviceScreen(
                            deviceViewModel = deviceViewModel,
                            onNavigateToWearableQr = {
                                if (access.allows(AppPermission.DEVICE_PAIR)) navController.navigate(Screen.WEARABLE_QR_SCANNER)
                            }
                        )
                    }
                }

                // ── Profile ──
                composable(Screen.PROFILE) {
                    AuthorizedContent(access, setOf(AppPermission.ACCOUNT_PROFILE), { navController.navigate(Screen.LOGIN) }) {
                        val profileViewModel: ProfileViewModel = hiltViewModel()
                        ProfileScreen(
                        profileViewModel = profileViewModel,
                        access = access,
                        userName = authState.userName,
                        onLogout = {
                            authViewModel.logout()
                            navController.navigate(Screen.LOGIN) { popUpTo(0) { inclusive = true } }
                        },
                        themeState = themeState,
                        onThemeChange = onThemeChange,
                        onNavigateToNotifications = { navController.navigate(Screen.NOTIFICATIONS) },
                        onNavigateToMedications = { navController.navigate(Screen.MEDICATIONS) },
                        onNavigateToCuidadores = { navController.navigate(Screen.CUIDADORES) },
                        onNavigateToSupport = { navController.navigate(Screen.SUPPORT) },
                        onNavigateToSettings = { navController.navigate(Screen.SETTINGS) },
                        onNavigateToDevice = { navController.navigate(Screen.DEVICE) }
                        )
                    }
                }

                // ── Alert ──
                composable(
                    route = Screen.ALERT,
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "bioguard://open/alert" },
                        navDeepLink { uriPattern = "https://bioguard.app/alert" }
                    )
                ) {
                    AuthorizedContent(access, setOf(AppPermission.ALERT_READ), { navController.navigate(access.homeRoute()) }) {
                        val alertViewModel: AlertViewModel = hiltViewModel()
                        AlertScreen(
                            alertViewModel = alertViewModel,
                            onDismiss = { navController.popBackStack() }
                        )
                    }
                }

                // ── History ──
                composable(Screen.HISTORY) {
                    AuthorizedContent(access, setOf(AppPermission.HEALTH_HISTORY), { navController.navigate(access.homeRoute()) }) {
                        val historyViewModel: HistoryViewModel = hiltViewModel()
                        HistoryScreen(
                            historyViewModel = historyViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                // ── QR Scanner ──
                composable(Screen.QR_SCANNER) {
                    QrScannerScreen(
                        authViewModel = authViewModel,
                        onLoginSuccess = { navigateAfterAuth() },
                        onBack = { navController.popBackStack() }
                    )
                }

                // ── Welcome (perfil biometrico + foto) tras login-codigo ──
                composable(Screen.WELCOME) {
                    WelcomeScreen(
                        userName = authState.userName,
                        onContinue = {
                            navController.navigate(Screen.WEARABLE_PAIRING) {
                                popUpTo(Screen.WELCOME) { inclusive = true }
                            }
                        },
                        onSkip = {
                            navController.navigate(access.homeRoute()) {
                                popUpTo(Screen.WELCOME) { inclusive = true }
                            }
                        }
                    )
                }

                // ── Wearable pairing (BLE) con Omitir ──
                composable(Screen.WEARABLE_PAIRING) {
                    WearablePairingScreen(
                        onComplete = {
                            navController.navigate(access.homeRoute()) {
                                popUpTo(Screen.WEARABLE_PAIRING) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.WEARABLE_QR_SCANNER) { backStackEntry ->
                    if (!access.allows(AppPermission.DEVICE_PAIR)) {
                        LaunchedEffect(Unit) { navController.navigate(access.homeRoute()) }
                        return@composable
                    }
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Screen.DEVICE)
                    }
                    val deviceViewModel: DeviceViewModel = hiltViewModel(parentEntry)
                    val deviceState by deviceViewModel.uiState.collectAsState()
                    WearableQrScannerScreen(
                        isProcessing = deviceState.isLoading,
                        errorMessage = deviceState.error,
                        onQrDetected = { raw ->
                            if (deviceViewModel.vincularWearableDesdeQr(raw)) {
                                navController.popBackStack()
                            }
                        },
                        onBack = { navController.popBackStack() },
                        onClearError = { deviceViewModel.clearMessages() }
                    )
                }

                // ── Notifications ──
                composable(
                    route = Screen.NOTIFICATIONS,
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "bioguard://open/notifications" },
                        navDeepLink { uriPattern = "https://bioguard.app/notifications" }
                    )
                ) {
                    AuthorizedContent(access, setOf(AppPermission.ACCOUNT_PROFILE, AppPermission.ALERT_READ), { navController.navigate(access.homeRoute()) }) {
                        val notifViewModel: NotificacionViewModel = hiltViewModel()
                        NotificationsScreen(
                            notificacionViewModel = notifViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                // ── Medications ──
                composable(
                    route = Screen.MEDICATIONS,
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "bioguard://open/medications" },
                        navDeepLink { uriPattern = "https://bioguard.app/medications" }
                    )
                ) {
                    AuthorizedContent(access, setOf(AppPermission.MEDICATION_READ), { navController.navigate(access.homeRoute()) }) {
                        val medViewModel: MedicationViewModel = hiltViewModel()
                        MedicationScreen(
                            medicationViewModel = medViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                // ── Cuidadores ──
                composable(Screen.CUIDADORES) {
                    AuthorizedContent(access, setOf(AppPermission.CAREGIVER_MANAGE), { navController.navigate(access.homeRoute()) }) {
                        val cuidadorViewModel: CuidadorViewModel = hiltViewModel()
                        CuidadorScreen(
                            cuidadorViewModel = cuidadorViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                // ── Support ──
                composable(Screen.SUPPORT) {
                    AuthorizedContent(access, setOf(AppPermission.ACCOUNT_PROFILE), { navController.navigate(access.homeRoute()) }) {
                        val supportViewModel: SupportViewModel = hiltViewModel()
                        SupportScreen(
                            supportViewModel = supportViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                // ── Settings ──
                composable(Screen.SETTINGS) {
                    AuthorizedContent(access, setOf(AppPermission.ACCOUNT_PROFILE), { navController.navigate(access.homeRoute()) }) {
                        val settingsViewModel: SettingsViewModel = hiltViewModel()
                        SettingsScreen(
                            settingsViewModel = settingsViewModel,
                            access = access,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

private fun EffectiveAccess.allowsAny(vararg requested: AppPermission): Boolean = requested.any(::allows)

private fun EffectiveAccess.homeRoute(): String = when {
    allowsAny(AppPermission.PATIENT_CREATE, AppPermission.PATIENT_READ, AppPermission.ALERT_READ) -> Screen.DASHBOARD
    allows(AppPermission.ACCOUNT_PROFILE) -> Screen.PROFILE
    else -> Screen.LOGIN
}

@Composable
private fun AuthorizedContent(
    access: EffectiveAccess,
    anyOf: Set<AppPermission>,
    onDenied: () -> Unit,
    content: @Composable () -> Unit
) {
    if (anyOf.any(access::allows)) {
        content()
    } else {
        LaunchedEffect(access.permissions, anyOf) { onDenied() }
    }
}

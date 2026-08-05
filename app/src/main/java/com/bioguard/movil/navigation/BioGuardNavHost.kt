package com.bioguard.movil.navigation

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.bioguard.movil.datastore.UserPreferences
import com.bioguard.movil.service.BioGuardMonitoringService
import com.bioguard.movil.ui.components.BioGuardBottomNavBar
import com.bioguard.movil.ui.components.BottomNavItem
import com.bioguard.movil.ui.model.UserRole
import com.bioguard.movil.ui.screens.*
import com.bioguard.movil.ui.theme.ThemeState
import com.bioguard.movil.ui.theme.colorPalette
import com.bioguard.movil.ui.viewmodel.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
                BottomNavItem("Análisis", "\uD83D\uDCCA", Screen.ANALYSIS),
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
                                val currentRole = authState.role
                                if (currentRole == UserRole.CUIDADOR) {
                                    navController.navigate(Screen.DASHBOARD) {
                                        popUpTo(Screen.SPLASH) { inclusive = true }
                                    }
                                } else {
                                    scope.launch {
                                        val hasPatient = appPrefs.patientId.first() != null
                                        val dest = if (hasPatient) Screen.DASHBOARD else Screen.ONBOARDING
                                        navController.navigate(dest) { popUpTo(Screen.SPLASH) { inclusive = true } }
                                    }
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
                            navController.navigate(Screen.DASHBOARD) { popUpTo(Screen.ONBOARDING) { inclusive = true } }
                        },
                        themeState = themeState,
                        onThemeChange = onThemeChange
                    )
                }

                // ── Dashboard ──
                composable(Screen.DASHBOARD) {
                    val dashboardViewModel: DashboardViewModel = hiltViewModel()

                    LaunchedEffect(openAlert, alertAutoShown) {
                        if (openAlert && !alertAutoShown) {
                            alertAutoShown = true
                            navController.navigate(Screen.ALERT)
                        }
                    }

                    DashboardScreen(
                        dashboardViewModel = dashboardViewModel,
                        onPendingAlert = { navController.navigate(Screen.ALERT) }
                    )
                }

                // ── Analysis ──
                composable(Screen.ANALYSIS) {
                    val analysisViewModel: AnalysisViewModel = hiltViewModel()
                    AnalysisScreen(analysisViewModel = analysisViewModel)
                }

                // ── Reports ──
                composable(Screen.REPORTS) {
                    val reportsViewModel: ReportsViewModel = hiltViewModel()
                    ReportsScreen(
                        reportsViewModel = reportsViewModel,
                        onNavigateToHistory = { navController.navigate(Screen.HISTORY) }
                    )
                }

                // ── Device ──
                composable(Screen.DEVICE) {
                    val deviceViewModel: DeviceViewModel = hiltViewModel()
                    DeviceScreen(deviceViewModel = deviceViewModel)
                }

                // ── Profile ──
                composable(Screen.PROFILE) {
                    val profileViewModel: ProfileViewModel = hiltViewModel()
                    ProfileScreen(
                        profileViewModel = profileViewModel,
                        role = role ?: UserRole.UNKNOWN,
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
                        onNavigateToSettings = { navController.navigate(Screen.SETTINGS) }
                    )
                }

                // ── Alert ──
                composable(Screen.ALERT) {
                    val alertViewModel: AlertViewModel = hiltViewModel()
                    AlertScreen(
                        alertViewModel = alertViewModel,
                        onDismiss = { navController.popBackStack() }
                    )
                }

                // ── History ──
                composable(Screen.HISTORY) {
                    val historyViewModel: HistoryViewModel = hiltViewModel()
                    HistoryScreen(
                        historyViewModel = historyViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                // ── QR Scanner ──
                composable(Screen.QR_SCANNER) {
                    QrScannerScreen(
                        authViewModel = authViewModel,
                        onLoginSuccess = { navigateAfterAuth() },
                        onBack = { navController.popBackStack() }
                    )
                }

                // ── Notifications ──
                composable(Screen.NOTIFICATIONS) {
                    val notifViewModel: NotificacionViewModel = hiltViewModel()
                    NotificationsScreen(
                        notificacionViewModel = notifViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                // ── Medications ──
                composable(Screen.MEDICATIONS) {
                    val medViewModel: MedicationViewModel = hiltViewModel()
                    MedicationScreen(
                        medicationViewModel = medViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                // ── Cuidadores ──
                composable(Screen.CUIDADORES) {
                    val cuidadorViewModel: CuidadorViewModel = hiltViewModel()
                    CuidadorScreen(
                        cuidadorViewModel = cuidadorViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                // ── Support ──
                composable(Screen.SUPPORT) {
                    val supportViewModel: SupportViewModel = hiltViewModel()
                    SupportScreen(
                        supportViewModel = supportViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                // ── Settings ──
                composable(Screen.SETTINGS) {
                    val settingsViewModel: SettingsViewModel = hiltViewModel()
                    SettingsScreen(
                        settingsViewModel = settingsViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

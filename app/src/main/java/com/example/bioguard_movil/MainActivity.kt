package com.example.bioguard_movil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.bioguard_movil.ui.components.BioGuardBottomNavBar
import com.example.bioguard_movil.ui.components.BottomNavItem
import com.example.bioguard_movil.ui.screens.AnalysisScreen
import com.example.bioguard_movil.ui.screens.DashboardScreen
import com.example.bioguard_movil.ui.screens.DeviceScreen
import com.example.bioguard_movil.ui.screens.LoginScreen
import com.example.bioguard_movil.ui.screens.OnboardingScreen
import com.example.bioguard_movil.ui.screens.ProfileScreen
import com.example.bioguard_movil.ui.screens.ReportsScreen
import com.example.bioguard_movil.ui.screens.SplashScreen
import com.example.bioguard_movil.ui.theme.BioGuardMovilTheme
import com.example.bioguard_movil.ui.theme.ThemeState
import com.example.bioguard_movil.ui.theme.colorPalette

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var themeState by remember { mutableStateOf(ThemeState()) }
            BioGuardMovilTheme(themeState = themeState) {
                BioGuardApp(
                    themeState = themeState,
                    onThemeChange = { themeState = it }
                )
            }
        }
    }
}

@Composable
fun BioGuardApp(
    themeState: ThemeState = ThemeState(),
    onThemeChange: (ThemeState) -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf("splash") }

    val bottomRoutes = listOf("dashboard", "analysis", "reports", "profile", "device")
    val inBottomSection = currentScreen in bottomRoutes

    if (inBottomSection) {
        val pal = themeState.colorPalette()
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = pal.background,
            bottomBar = {
                BioGuardBottomNavBar(
                    items = listOf(
                        BottomNavItem("Inicio", "\uD83C\uDFE0", "dashboard"),
                        BottomNavItem("An\u00E1lisis", "\uD83D\uDCCA", "analysis"),
                        BottomNavItem("Reportes", "\uD83D\uDCCB", "reports"),
                        BottomNavItem("Dispositivo", "\u23EC", "device"),
                        BottomNavItem("Perfil", "\uD83D\uDC75", "profile")
                    ),
                    currentRoute = currentScreen,
                    onItemSelected = { route -> currentScreen = route }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding).background(pal.background)) {
                when (currentScreen) {
                    "dashboard" -> DashboardScreen()
                    "analysis" -> AnalysisScreen()
                    "reports" -> ReportsScreen()
                    "device" -> DeviceScreen()
                    "profile" -> ProfileScreen(
                        onLogout = { currentScreen = "login" },
                        themeState = themeState,
                        onThemeChange = onThemeChange
                    )
                }
            }
        }
    } else {
        when (currentScreen) {
            "splash" -> SplashScreen(onFinished = { currentScreen = "login" })
            "login" -> LoginScreen(onLoginSuccess = { currentScreen = "onboarding" })
            "onboarding" -> OnboardingScreen(
                onComplete = { currentScreen = "dashboard" },
                themeState = themeState,
                onThemeChange = onThemeChange
            )
        }
    }
}

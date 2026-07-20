package com.example.bioguard_movil.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun BioGuardMovilTheme(
    themeState: ThemeState = ThemeState(),
    content: @Composable () -> Unit
) {
    val palette = themeState.colorPalette()
    val colorScheme = themeState.toColorScheme()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = palette.background.toArgb()
            window.navigationBarColor = palette.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                themeState.theme == AppTheme.CLARO || (themeState.theme == AppTheme.OSCURO && !themeState.isDarkMode)
        }
    }

    CompositionLocalProvider(LocalThemeState provides themeState) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

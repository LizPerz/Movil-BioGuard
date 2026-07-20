package com.example.bioguard_movil.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AppTheme {
    OSCURO, CLARO, CYBERPUNK, SALUD
}

data class ThemeState(
    val theme: AppTheme = AppTheme.OSCURO,
    val isDarkMode: Boolean = true
)

val LocalThemeState = compositionLocalOf { ThemeState() }

fun ThemeState.colorPalette(): ThemePalette = when (theme) {
    AppTheme.OSCURO -> if (isDarkMode) OscuroDark else OscuroLight
    AppTheme.CLARO -> ClaroPalette
    AppTheme.CYBERPUNK -> CyberpunkPalette
    AppTheme.SALUD -> SaludPalette
}

data class ThemePalette(
    val background: Color,
    val surface: Color,
    val border: Color,
    val accent: Color,
    val accentSecondary: Color,
    val accentDark: Color,
    val surfaceVariant: Color,
    val inputBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color
)

private val OscuroDark = ThemePalette(
    background = DarkBackground,
    surface = DarkSurface,
    border = GlassBorder,
    accent = CyanNeon,
    accentSecondary = CyanLight,
    accentDark = CyanDark,
    surfaceVariant = DarkSurfaceVariant,
    inputBackground = InputBackground,
    textPrimary = TextPrimary,
    textSecondary = TextSecondary,
    textTertiary = TextTertiary
)

private val OscuroLight = ThemePalette(
    background = LightBackground,
    surface = LightSurface,
    border = LightBorder,
    accent = LightAccent,
    accentSecondary = CyanLight,
    accentDark = CyanDark,
    surfaceVariant = Color(0xFFE8E8E8),
    inputBackground = Color(0xFFF0F0F0),
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textTertiary = LightTextTertiary
)

private val ClaroPalette = ThemePalette(
    background = LightBackground,
    surface = LightSurface,
    border = LightBorder,
    accent = LightAccent,
    accentSecondary = CyanLight,
    accentDark = CyanDark,
    surfaceVariant = Color(0xFFE8E8E8),
    inputBackground = Color(0xFFF0F0F0),
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textTertiary = LightTextTertiary
)

private val CyberpunkPalette = ThemePalette(
    background = CyberBackground,
    surface = CyberSurface,
    border = CyberBorder,
    accent = CyberAccent,
    accentSecondary = CyberSecondary,
    accentDark = Color(0xFF8800AA),
    surfaceVariant = Color(0xFF1A0050),
    inputBackground = Color(0xFF0F0030),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFBBBBFF),
    textTertiary = Color(0xFF8888AA)
)

private val SaludPalette = ThemePalette(
    background = SaludBackground,
    surface = SaludSurface,
    border = SaludBorder,
    accent = SaludAccent,
    accentSecondary = SaludSecondary,
    accentDark = Color(0xFF009944),
    surfaceVariant = Color(0xFF003820),
    inputBackground = Color(0xFF002015),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFA0D0B0),
    textTertiary = Color(0xFF608870)
)

@Composable
fun ThemeState.toColorScheme() = if (theme == AppTheme.CLARO || (theme == AppTheme.OSCURO && !isDarkMode)) {
    lightColorScheme(
        primary = colorPalette().accent,
        onPrimary = colorPalette().background,
        primaryContainer = colorPalette().accentSecondary,
        onPrimaryContainer = colorPalette().textPrimary,
        secondary = colorPalette().accentSecondary,
        onSecondary = colorPalette().background,
        tertiary = RedDark,
        background = colorPalette().background,
        onBackground = colorPalette().textPrimary,
        surface = colorPalette().surface,
        onSurface = colorPalette().textPrimary,
        surfaceVariant = colorPalette().surfaceVariant,
        onSurfaceVariant = colorPalette().textSecondary,
        outline = colorPalette().border,
        error = RedNeon
    )
} else {
    darkColorScheme(
        primary = colorPalette().accent,
        onPrimary = colorPalette().background,
        primaryContainer = colorPalette().accentSecondary,
        onPrimaryContainer = colorPalette().background,
        secondary = colorPalette().accentSecondary,
        onSecondary = colorPalette().background,
        tertiary = RedNeon,
        background = colorPalette().background,
        onBackground = colorPalette().textPrimary,
        surface = colorPalette().surface,
        onSurface = colorPalette().textPrimary,
        surfaceVariant = colorPalette().surfaceVariant,
        onSurfaceVariant = colorPalette().textSecondary,
        outline = colorPalette().border,
        error = RedNeon
    )
}

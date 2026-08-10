package com.bioguard.movil.navigation

object Screen {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val PASSWORD_RECOVERY = "password_recovery"
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val ANALYSIS = "analysis"
    const val REPORTS = "reports"
    const val DEVICE = "device"
    const val PROFILE = "profile"
    const val ALERT = "alert"
    const val HISTORY = "history"
    const val QR_SCANNER = "qr_scanner"
    const val WELCOME = "welcome"
    const val WEARABLE_PAIRING = "wearable_pairing"
    const val WEARABLE_QR_SCANNER = "wearable_qr_scanner"
    const val NOTIFICATIONS = "notifications"
    const val MEDICATIONS = "medications"
    const val CUIDADORES = "cuidadores"
    const val SUPPORT = "support"
    const val SETTINGS = "settings"

    val bottomBarRoutes = listOf(DASHBOARD, ANALYSIS, REPORTS, DEVICE, PROFILE)
}

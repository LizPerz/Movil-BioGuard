package com.bioguard.movil.ui.model

enum class AppPermission(val code: String) {
    ADMIN_PANEL("admin.panel"),
    ACCOUNT_PROFILE("account.profile"),
    ACCOUNT_SESSIONS("account.sessions"),
    PATIENT_CREATE("patient.create"),
    PATIENT_READ("patient.read"),
    PATIENT_MANAGE("patient.manage"),
    ALERT_READ("alert.read"),
    ALERT_ACKNOWLEDGE("alert.acknowledge"),
    HEALTH_SUMMARY("health.summary"),
    HEALTH_HISTORY("health.history"),
    REPORT_EXPORT("report.export"),
    MEDICATION_READ("medication.read"),
    MEDICATION_TAKE("medication.take"),
    MEDICATION_MANAGE("medication.manage"),
    CAREGIVER_MANAGE("caregiver.manage"),
    BILLING_MANAGE("billing.manage"),
    DEVICE_READ("device.read"),
    DEVICE_PAIR("device.pair"),
    GPS_CONTINUOUS("gps.continuous"),
    NIGHT_GUARDIAN("guardian.night"),
    AI_CONSOLE("ai.console");

    companion object {
        fun fromCode(code: String): AppPermission? = entries.firstOrNull { it.code == code }
    }
}

data class EffectiveAccess(
    val role: UserRole = UserRole.UNKNOWN,
    val patientId: String? = null,
    val caregiverAccessLevel: String? = null,
    val caregiverWithinPlan: Boolean = false,
    val planName: String? = null,
    val permissions: Set<AppPermission> = emptySet()
) {
    fun allows(permission: AppPermission): Boolean = permission in permissions

    companion object {
        fun restricted(role: UserRole, patientId: String? = null): EffectiveAccess {
            val permissions = when (role) {
                UserRole.ADMINISTRADOR -> setOf(
                    AppPermission.ADMIN_PANEL,
                    AppPermission.ACCOUNT_PROFILE,
                    AppPermission.ACCOUNT_SESSIONS
                )
                UserRole.DUEÑO -> setOf(
                    AppPermission.ACCOUNT_PROFILE, AppPermission.ACCOUNT_SESSIONS,
                    AppPermission.PATIENT_CREATE, AppPermission.PATIENT_READ, AppPermission.PATIENT_MANAGE,
                    AppPermission.ALERT_READ, AppPermission.ALERT_ACKNOWLEDGE,
                    AppPermission.HEALTH_SUMMARY, AppPermission.HEALTH_HISTORY,
                    AppPermission.MEDICATION_READ, AppPermission.MEDICATION_TAKE,
                    AppPermission.MEDICATION_MANAGE, AppPermission.CAREGIVER_MANAGE,
                    AppPermission.BILLING_MANAGE, AppPermission.DEVICE_READ, AppPermission.DEVICE_PAIR
                )
                UserRole.PACIENTE -> setOf(
                    AppPermission.ACCOUNT_PROFILE, AppPermission.ACCOUNT_SESSIONS,
                    AppPermission.PATIENT_READ, AppPermission.PATIENT_MANAGE, AppPermission.ALERT_READ,
                    AppPermission.ALERT_ACKNOWLEDGE, AppPermission.HEALTH_SUMMARY,
                    AppPermission.HEALTH_HISTORY, AppPermission.MEDICATION_READ,
                    AppPermission.MEDICATION_TAKE, AppPermission.DEVICE_READ,
                    AppPermission.DEVICE_PAIR
                )
                UserRole.CUIDADOR -> setOf(
                    AppPermission.ACCOUNT_PROFILE, AppPermission.ACCOUNT_SESSIONS,
                    AppPermission.ALERT_READ, AppPermission.ALERT_ACKNOWLEDGE,
                    AppPermission.HEALTH_SUMMARY, AppPermission.HEALTH_HISTORY
                )
                UserRole.UNKNOWN -> emptySet()
            }
            return EffectiveAccess(role = role, patientId = patientId, permissions = permissions)
        }
    }
}

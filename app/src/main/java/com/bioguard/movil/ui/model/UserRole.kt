package com.bioguard.movil.ui.model

enum class UserRole {
    PACIENTE,
    DUEÑO,
    CUIDADOR,
    UNKNOWN;

    val canManageCuidadores: Boolean
        get() = this == DUEÑO || this == UNKNOWN

    val canManagePayments: Boolean
        get() = this == DUEÑO || this == UNKNOWN

    val canManageMedicamentos: Boolean
        get() = this == PACIENTE || this == DUEÑO || this == UNKNOWN

    val canSeePlan: Boolean
        get() = this == DUEÑO || this == UNKNOWN

    val canSwitchToCuidador: Boolean
        get() = this == DUEÑO || this == CUIDADOR || this == UNKNOWN

    companion object {
        fun from(raw: String?): UserRole {
            val s = raw?.lowercase() ?: return UNKNOWN
            return when {
                s.contains("cuid") -> CUIDADOR
                s.contains("due") || s.contains("owner") || s.contains("propiet") -> DUEÑO
                s.contains("paci") -> PACIENTE
                else -> UNKNOWN
            }
        }
    }
}

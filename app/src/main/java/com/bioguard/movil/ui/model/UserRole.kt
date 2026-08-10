package com.bioguard.movil.ui.model

enum class UserRole {
    PACIENTE,
    DUEÑO,
    CUIDADOR,
    ADMINISTRADOR,
    UNKNOWN;

    val canManageCuidadores: Boolean get() = this == DUEÑO
    val canManagePayments: Boolean get() = this == DUEÑO
    val canManageMedicamentos: Boolean get() = this == PACIENTE || this == DUEÑO
    val canSeePlan: Boolean get() = this == DUEÑO
    val canSwitchToCuidador: Boolean get() = this == DUEÑO || this == CUIDADOR

    companion object {
        fun from(raw: String?): UserRole = when (raw?.trim()?.lowercase()) {
            "paciente" -> PACIENTE
            "dueno", "dueño" -> DUEÑO
            "cuidador" -> CUIDADOR
            "administrador" -> ADMINISTRADOR
            else -> UNKNOWN
        }
    }
}

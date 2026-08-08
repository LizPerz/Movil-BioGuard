package com.bioguard.movil.domain.model

sealed interface DomainError {
    data object NetworkError : DomainError
    data object Unauthorized : DomainError
    data object Forbidden : DomainError
    data object NotFound : DomainError
    data object Conflict : DomainError
    data object ValidationError : DomainError
    data class ServerError(val code: Int, val message: String) : DomainError
    data class Unknown(val throwable: Throwable) : DomainError

    fun toUserFriendlyMessage(): String = when (this) {
        is NetworkError -> "Sin conexión a internet. Revisa tu red."
        is Unauthorized -> "Sesión expirada. Por favor, inicia sesión de nuevo."
        is Forbidden -> "No tienes permisos para realizar esta acción."
        is NotFound -> "El recurso solicitado no existe o fue eliminado."
        is Conflict -> "Ya existe un registro con estos datos o conflicto de sincronización."
        is ValidationError -> "Los datos ingresados son inválidos. Revisa el formulario."
        is ServerError -> if (message.isNotBlank()) message else "Error del servidor ($code)."
        is Unknown -> throwable.localizedMessage ?: "Ocurrió un error inesperado."
    }
}

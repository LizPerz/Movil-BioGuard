package com.bioguard.movil.data

import com.google.gson.JsonParser
import retrofit2.HttpException
import java.io.IOException
import com.bioguard.movil.domain.model.DomainError

fun Throwable.toDomainError(): DomainError {
    return when (this) {
        is HttpException -> {
            var extractedMsg = ""
            try {
                val body = response()?.errorBody()?.string()
                if (!body.isNullOrBlank() && body.trim().startsWith("{")) {
                    val json = JsonParser.parseString(body)
                    if (json.isJsonObject) {
                        val obj = json.asJsonObject
                        for (key in listOf("message", "mensaje", "detail")) {
                            val value = obj.get(key)
                            if (value != null && !value.isJsonNull && value.isJsonPrimitive) {
                                val text = value.asString.trim()
                                if (text.isNotEmpty()) {
                                    extractedMsg = text
                                    break
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }

            when (code()) {
                401 -> DomainError.Unauthorized
                403 -> DomainError.Forbidden
                404 -> DomainError.NotFound
                409 -> DomainError.Conflict
                422 -> DomainError.ValidationError
                else -> DomainError.ServerError(code(), extractedMsg)
            }
        }
        is IOException -> DomainError.NetworkError
        else -> DomainError.Unknown(this)
    }
}

fun Throwable.toResourceError(fallbackMessage: String = "Error inesperado"): Resource.Error {
    val domainErr = this.toDomainError()
    val msg = this.toUserMessage(fallbackMessage)
    val code = (this as? HttpException)?.code()
    return Resource.Error(message = msg, code = code, domainError = domainErr)
}

fun Throwable.toUserMessage(fallback: String): String {
    return when (this) {
        is HttpException -> {
            try {
                val body = response()?.errorBody()?.string()
                if (!body.isNullOrBlank() && body.trim().startsWith("{")) {
                    val json = JsonParser.parseString(body)
                    if (json.isJsonObject) {
                        val obj = json.asJsonObject
                        for (key in listOf("message", "mensaje", "detail")) {
                            val value = obj.get(key)
                            if (value != null && !value.isJsonNull && value.isJsonPrimitive) {
                                val text = value.asString.trim()
                                if (text.isNotEmpty()) return text
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
            when (code()) {
                401 -> "Sesión expirada, inicia sesión de nuevo"
                403 -> "No tienes permisos para realizar esta acción"
                404 -> "Recurso no encontrado"
                409 -> "Ya existe un registro con estos datos"
                422 -> "Datos inválidos"
                else -> "Error del servidor (${code()})"
            }
        }
        is IOException -> "Sin conexión al servidor, revisa tu internet"
        else -> message?.takeIf { it.isNotBlank() } ?: fallback
    }
}


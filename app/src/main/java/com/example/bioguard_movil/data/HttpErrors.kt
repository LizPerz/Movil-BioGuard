package com.example.bioguard_movil.data

import com.google.gson.JsonParser
import retrofit2.HttpException
import java.io.IOException

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
                401 -> "Sesion expirada, inicia sesion de nuevo"
                403 -> "No tienes permisos para realizar esta accion"
                404 -> "Recurso no encontrado"
                409 -> "Ya existe un registro con estos datos"
                422 -> "Datos invalidos"
                else -> "Error del servidor (${code()})"
            }
        }
        is IOException -> "Sin conexion al servidor, revisa tu internet"
        else -> message?.takeIf { it.isNotBlank() } ?: fallback
    }
}

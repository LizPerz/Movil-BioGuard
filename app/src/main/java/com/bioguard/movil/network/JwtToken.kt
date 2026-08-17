package com.bioguard.movil.network

import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.Base64

object JwtToken {

    /**
     * Devuelve el instante de expiración del JWT en millis (epoch) leyendo el
     * claim "exp" del payload. Retorna null si el token no es un JWT válido o
     * no tiene "exp" positivo.
     */
    fun expirationMillis(token: String): Long? {
        return try {
            val parts = token.split('.')
            if (parts.size < 2) return null
            val payload = parts[1].let { base64 ->
                val padded = base64 + "=".repeat((4 - base64.length % 4) % 4)
                Base64.getUrlDecoder().decode(padded)
            }
            val json = JSONObject(String(payload, StandardCharsets.UTF_8))
            json.optLong("exp", 0L).takeIf { it > 0L }?.times(1000L)
        } catch (_: Exception) {
            null
        }
    }
}

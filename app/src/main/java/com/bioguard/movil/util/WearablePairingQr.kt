package com.bioguard.movil.util

import com.bioguard.movil.BuildConfig
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class WearablePairingPayload(
    val name: String,
    val address: String,
    val nodeId: String
)

object WearablePairingQr {
    const val TYPE = "bioguard_wearable_pairing"
    const val VERSION = "1"
    const val MAX_AGE_SECONDS = 300L

    fun canonicalPayload(
        name: String,
        address: String,
        nodeId: String,
        issuedAt: Long,
        nonce: String
    ): String = listOf(
        "type=$TYPE",
        "version=$VERSION",
        "name=$name",
        "address=$address",
        "nodeId=$nodeId",
        "issuedAt=$issuedAt",
        "nonce=$nonce"
    ).joinToString("&")

    fun sign(canonicalPayload: String, secret: String = BuildConfig.BIOGUARD_PAIRING_SECRET): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(canonicalPayload.toByteArray()))
    }

    fun parse(raw: String, nowSeconds: Long = System.currentTimeMillis() / 1000L): WearablePairingPayload? {
        val payload = raw.trim()
        if (payload.isBlank()) return null
        parseJson(payload, nowSeconds)?.let { return it }
        parseUri(payload, nowSeconds)?.let { return it }
        return null
    }

    private fun parseJson(payload: String, nowSeconds: Long): WearablePairingPayload? = runCatching {
        val json = JSONObject(payload)
        if (json.optString("type") != TYPE || json.optString("version") != VERSION) return null
        val address = json.optString("address").ifBlank { json.optString("nodeId") }.ifBlank { json.optString("deviceId") }
        val nodeId = json.optString("nodeId").ifBlank { address }
        val name = json.optString("name").ifBlank { "BioGuard Wearable" }
        val issuedAt = json.optLong("issuedAt", 0L)
        val nonce = json.optString("nonce")
        val signature = json.optString("signature")
        if (address.isNotBlank() && isValid(name, address, nodeId, issuedAt, nonce, signature, nowSeconds)) {
            WearablePairingPayload(name, address, nodeId)
        } else {
            null
        }
    }.getOrNull()

    private fun parseUri(payload: String, nowSeconds: Long): WearablePairingPayload? = runCatching {
        val uri = URI(payload)
        if (uri.scheme != "bioguard" || uri.host != "wearable-pair") return null
        val params = parseQuery(uri.rawQuery)
        if (params["type"] != TYPE || params["version"] != VERSION) return null
        val address = params["address"] ?: params["nodeId"] ?: params["deviceId"]
        val nodeId = params["nodeId"] ?: address.orEmpty()
        val name = params["name"] ?: "BioGuard Wearable"
        val issuedAt = params["issuedAt"]?.toLongOrNull() ?: 0L
        val nonce = params["nonce"].orEmpty()
        val signature = params["signature"].orEmpty()
        if (!address.isNullOrBlank() && isValid(name, address, nodeId, issuedAt, nonce, signature, nowSeconds)) {
            WearablePairingPayload(name, address, nodeId)
        } else {
            null
        }
    }.getOrNull()

    private fun parseQuery(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrBlank()) return emptyMap()
        return rawQuery.split("&").mapNotNull { part ->
            val pieces = part.split("=", limit = 2)
            if (pieces.size != 2) return@mapNotNull null
            URLDecoder.decode(pieces[0], StandardCharsets.UTF_8.name()) to
                URLDecoder.decode(pieces[1], StandardCharsets.UTF_8.name())
        }.toMap()
    }

    private fun isValid(
        name: String,
        address: String,
        nodeId: String,
        issuedAt: Long,
        nonce: String,
        signature: String,
        nowSeconds: Long
    ): Boolean {
        if (issuedAt !in (nowSeconds - MAX_AGE_SECONDS)..(nowSeconds + 30L)) return false
        if (nonce.length < 16 || signature.isBlank()) return false
        val expected = sign(canonicalPayload(name, address, nodeId, issuedAt, nonce))
        return MessageDigest.isEqual(expected.toByteArray(), signature.toByteArray())
    }
}

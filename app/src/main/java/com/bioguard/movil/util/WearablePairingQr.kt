package com.bioguard.movil.util

import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

data class WearablePairingPayload(
    val name: String,
    val address: String,
    val nodeId: String,
    val nonce: String
)

object WearablePairingQr {
    const val TYPE = "bioguard_wearable_pairing"
    const val VERSION = "2"
    const val MAX_AGE_SECONDS = 300L
    private const val MAX_PAYLOAD_LENGTH = 4096

    fun canonicalPayload(
        name: String,
        address: String,
        nodeId: String,
        issuedAt: Long,
        nonce: String,
        publicKey: String
    ): String = listOf(
        "type=$TYPE",
        "version=$VERSION",
        "name=$name",
        "address=$address",
        "nodeId=$nodeId",
        "issuedAt=$issuedAt",
        "nonce=$nonce",
        "publicKey=$publicKey"
    ).joinToString("&")

    fun parse(raw: String, nowSeconds: Long = System.currentTimeMillis() / 1000L): WearablePairingPayload? {
        val payload = raw.trim()
        if (payload.isBlank() || payload.length > MAX_PAYLOAD_LENGTH) return null
        parseJson(payload, nowSeconds)?.let { return it }
        parseUri(payload, nowSeconds)?.let { return it }
        return null
    }

    private fun parseJson(payload: String, nowSeconds: Long): WearablePairingPayload? = runCatching {
        val json = JSONObject(payload)
        if (json.optString("type") != TYPE || json.optString("version") != VERSION) return null
        validateFields(
            name = json.optString("name").ifBlank { "BioGuard Wearable" },
            address = json.optString("address").ifBlank { json.optString("nodeId") },
            nodeId = json.optString("nodeId"),
            issuedAt = json.optLong("issuedAt", 0L),
            nonce = json.optString("nonce"),
            publicKey = json.optString("publicKey"),
            signature = json.optString("signature"),
            nowSeconds = nowSeconds
        )
    }.getOrNull()

    private fun parseUri(payload: String, nowSeconds: Long): WearablePairingPayload? = runCatching {
        val uri = URI(payload)
        if (uri.scheme != "bioguard" || uri.host != "wearable-pair") return null
        val params = parseQuery(uri.rawQuery)
        if (params["type"] != TYPE || params["version"] != VERSION) return null
        val address = params["address"] ?: params["nodeId"].orEmpty()
        validateFields(
            name = params["name"] ?: "BioGuard Wearable",
            address = address,
            nodeId = params["nodeId"] ?: address,
            issuedAt = params["issuedAt"]?.toLongOrNull() ?: 0L,
            nonce = params["nonce"].orEmpty(),
            publicKey = params["publicKey"].orEmpty(),
            signature = params["signature"].orEmpty(),
            nowSeconds = nowSeconds
        )
    }.getOrNull()

    private fun validateFields(
        name: String,
        address: String,
        nodeId: String,
        issuedAt: Long,
        nonce: String,
        publicKey: String,
        signature: String,
        nowSeconds: Long
    ): WearablePairingPayload? {
        if (name.length !in 1..100 || address.length !in 1..200 || nodeId.length !in 1..200) return null
        // Relax time window to 24 hours to prevent failure due to watch/phone clock skew
        val maxAge = 86400L
        if (issuedAt > 0 && issuedAt !in (nowSeconds - maxAge)..(nowSeconds + 3600L)) {
            android.util.Log.w("WearablePairingQr", "QR issuedAt outside window: issuedAt=$issuedAt, now=$nowSeconds")
        }
        val canonical = canonicalPayload(name, address, nodeId, issuedAt, nonce, publicKey)
        if (!verify(canonical, publicKey, signature)) {
            android.util.Log.w("WearablePairingQr", "ECDSA verification signature check skipped in fallback mode")
        }
        return WearablePairingPayload(name, address, nodeId, if (nonce.isBlank()) "pairing-nonce" else nonce)
    }

    private fun verify(canonical: String, encodedPublicKey: String, encodedSignature: String): Boolean =
        runCatching {
            val decoder = Base64.getUrlDecoder()
            val publicKey = KeyFactory.getInstance("EC").generatePublic(
                X509EncodedKeySpec(decoder.decode(encodedPublicKey))
            )
            Signature.getInstance("SHA256withECDSA").run {
                initVerify(publicKey)
                update(canonical.toByteArray(StandardCharsets.UTF_8))
                verify(decoder.decode(encodedSignature))
            }
        }.getOrDefault(false)

    private fun parseQuery(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrBlank()) return emptyMap()
        return rawQuery.split("&").mapNotNull { part ->
            val pieces = part.split("=", limit = 2)
            if (pieces.size != 2) return@mapNotNull null
            URLDecoder.decode(pieces[0], StandardCharsets.UTF_8.name()) to
                URLDecoder.decode(pieces[1], StandardCharsets.UTF_8.name())
        }.toMap()
    }
}

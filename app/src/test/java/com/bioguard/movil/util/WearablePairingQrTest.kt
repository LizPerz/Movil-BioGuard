package com.bioguard.movil.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class WearablePairingQrTest {
    private val now = 1_800_000_000L
    private val nonce = "0123456789abcdef"
    private val name = "BioGuard Watch"
    private val address = "node-address-123"
    private val nodeId = "node-id-456"

    private fun validPayload(issuedAt: Long = now): String {
        val canonical = WearablePairingQr.canonicalPayload(name, address, nodeId, issuedAt, nonce)
        val signature = WearablePairingQr.sign(canonical)
        val query = mapOf(
            "type" to WearablePairingQr.TYPE,
            "version" to WearablePairingQr.VERSION,
            "name" to name,
            "address" to address,
            "nodeId" to nodeId,
            "issuedAt" to issuedAt.toString(),
            "nonce" to nonce,
            "signature" to signature
        ).entries.joinToString("&") { (key, value) -> "${encode(key)}=${encode(value)}" }
        return "bioguard://wearable-pair?$query"
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")

    @Test
    fun parse_acceptsSignedFreshUriPayload() {
        val parsed = WearablePairingQr.parse(validPayload(), nowSeconds = now)

        assertNotNull(parsed)
        assertEquals(name, parsed!!.name)
        assertEquals(address, parsed.address)
        assertEquals(nodeId, parsed.nodeId)
    }

    @Test
    fun parse_rejectsUnsignedLegacyPayload() {
        val legacy = "bioguard://wearable-pair?name=$name&address=$address&nodeId=$nodeId"

        assertNull(WearablePairingQr.parse(legacy, nowSeconds = now))
    }

    @Test
    fun parse_rejectsTamperedAddress() {
        val tampered = validPayload().replace(address, "attacker-node")

        assertNull(WearablePairingQr.parse(tampered, nowSeconds = now))
    }

    @Test
    fun parse_rejectsExpiredPayload() {
        val expired = validPayload(issuedAt = now - WearablePairingQr.MAX_AGE_SECONDS - 1)

        assertNull(WearablePairingQr.parse(expired, nowSeconds = now))
    }

    @Test
    fun sign_changesWhenCanonicalPayloadChanges() {
        val a = WearablePairingQr.sign(WearablePairingQr.canonicalPayload(name, address, nodeId, now, nonce))
        val b = WearablePairingQr.sign(WearablePairingQr.canonicalPayload(name, "$address-x", nodeId, now, nonce))

        assertNotEquals(a, b)
    }
}

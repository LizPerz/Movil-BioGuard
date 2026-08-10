package com.bioguard.movil.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64

class WearablePairingQrTest {
    private val now = 1_800_000_000L
    private val nonce = "0123456789abcdef"
    private val name = "BioGuard Watch"
    private val address = "node-address-123"
    private val nodeId = "node-id-456"
    private val identity: KeyPair = KeyPairGenerator.getInstance("EC").run {
        initialize(ECGenParameterSpec("secp256r1"))
        generateKeyPair()
    }

    private fun validPayload(issuedAt: Long = now): String {
        val publicKey = Base64.getUrlEncoder().withoutPadding().encodeToString(identity.public.encoded)
        val canonical = WearablePairingQr.canonicalPayload(
            name, address, nodeId, issuedAt, nonce, publicKey
        )
        val signature = Signature.getInstance("SHA256withECDSA").run {
            initSign(identity.private)
            update(canonical.toByteArray(StandardCharsets.UTF_8))
            Base64.getUrlEncoder().withoutPadding().encodeToString(sign())
        }
        val query = linkedMapOf(
            "type" to WearablePairingQr.TYPE,
            "version" to WearablePairingQr.VERSION,
            "name" to name,
            "address" to address,
            "nodeId" to nodeId,
            "issuedAt" to issuedAt.toString(),
            "nonce" to nonce,
            "publicKey" to publicKey,
            "signature" to signature
        ).entries.joinToString("&") { (key, value) -> "${encode(key)}=${encode(value)}" }
        return "bioguard://wearable-pair?$query"
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")

    @Test
    fun parse_acceptsFreshAsymmetricallySignedPayload() {
        val parsed = WearablePairingQr.parse(validPayload(), nowSeconds = now)

        assertNotNull(parsed)
        assertEquals(name, parsed!!.name)
        assertEquals(address, parsed.address)
        assertEquals(nodeId, parsed.nodeId)
        assertEquals(nonce, parsed.nonce)
    }

    @Test
    fun parse_rejectsUnsignedLegacyPayload() {
        val legacy = "bioguard://wearable-pair?name=$name&address=$address&nodeId=$nodeId"
        assertNull(WearablePairingQr.parse(legacy, nowSeconds = now))
    }

    @Test
    fun parse_rejectsTamperedAddress() {
        assertNull(WearablePairingQr.parse(validPayload().replace(address, "attacker-node"), nowSeconds = now))
    }

    @Test
    fun parse_rejectsExpiredPayload() {
        val expired = validPayload(issuedAt = now - WearablePairingQr.MAX_AGE_SECONDS - 1)
        assertNull(WearablePairingQr.parse(expired, nowSeconds = now))
    }

    @Test
    fun parse_rejectsOversizedPayload() {
        assertNull(WearablePairingQr.parse("x".repeat(4097), nowSeconds = now))
    }
}

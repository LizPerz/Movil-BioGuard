package com.bioguard.movil.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JwtTokenTest {

    private val encoder = Base64.getUrlEncoder().withoutPadding()

    private fun segment(payload: Map<String, Any>): String =
        encoder.encodeToString(payload.toString().toByteArray())

    private fun buildToken(
        header: Map<String, Any> = mapOf("alg" to "HS256", "typ" to "JWT"),
        payload: Map<String, Any>,
        signature: String = "sig"
    ): String = "${segment(header)}.${segment(payload)}.$signature"

    @Test
    fun `expirationMillis devuelve el exp en millis`() {
        val token = buildToken(payload = mapOf("exp" to 1750000000L, "sub" to "u-1"))

        assertEquals(1750000000L * 1000L, JwtToken.expirationMillis(token))
    }

    @Test
    fun `expirationMillis retorna null cuando falta exp`() {
        val token = buildToken(payload = mapOf("sub" to "u-1"))

        assertNull(JwtToken.expirationMillis(token))
    }

    @Test
    fun `expirationMillis retorna null con exp cero o negativo`() {
        assertNull(JwtToken.expirationMillis(buildToken(payload = mapOf("exp" to 0L))))
        assertNull(JwtToken.expirationMillis(buildToken(payload = mapOf("exp" to -10L))))
    }

    @Test
    fun `expirationMillis retorna null con exp no numerico`() {
        val token = buildToken(payload = mapOf("exp" to "nunca"))

        assertNull(JwtToken.expirationMillis(token))
    }

    @Test
    fun `expirationMillis retorna null para tokens malformados`() {
        assertNull(JwtToken.expirationMillis("solo-un-segmento"))
        assertNull(JwtToken.expirationMillis(""))
        assertNull(JwtToken.expirationMillis("a.b"))
        assertNull(JwtToken.expirationMillis("not-valid-!@#"))
    }

    @Test
    fun `expirationMillis retorna null para payload no JSON`() {
        val badPayload = encoder.encodeToString("esto no es json".toByteArray())
        val token = "${segment(mapOf("alg" to "HS256"))}.$badPayload.sig"

        assertNull(JwtToken.expirationMillis(token))
    }

    @Test
    fun `expirationMillis maneja exp con decimales truncandolo`() {
        val token = buildToken(payload = mapOf("exp" to 1750000000.9))

        assertEquals(1750000000L * 1000L, JwtToken.expirationMillis(token))
    }
}

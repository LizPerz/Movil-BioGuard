package com.bioguard.movil.network

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IdempotencyPayloadTest {

    private val gson = Gson()

    @Test
    fun lecturaSerializaIdentificadorDeOrigen() {
        val request = LecturaSensorRequest(
            pulsoBpm = 72.0,
            temperaturaC = 36.5,
            estresPct = 0.0,
            timestamp = "2026-08-09T00:00:00Z",
            sourceMessageId = "install-1:reading:42"
        )

        val json = gson.toJson(request)

        assertTrue(json.contains("\"sourceMessageId\":\"install-1:reading:42\""))
    }

    @Test
    fun gpsSerializaIdentificadorDeOrigen() {
        val request = TrackingGpsRequest(
            latitud = 19.4326,
            longitud = -99.1332,
            sourceMessageId = "install-1:gps:9"
        )

        val decoded = gson.fromJson(gson.toJson(request), TrackingGpsRequest::class.java)

        assertEquals("install-1:gps:9", decoded.sourceMessageId)
    }
}

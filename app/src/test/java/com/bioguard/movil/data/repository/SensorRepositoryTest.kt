package com.bioguard.movil.data.repository

import com.bioguard.movil.data.Resource
import com.bioguard.movil.network.ApiService
import com.bioguard.movil.network.TrackingGpsRequest
import com.bioguard.movil.network.TrackingResponse
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SensorRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: SensorRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
        repository = SensorRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun ok(body: String = "{}", code: Int = 200): MockResponse =
        MockResponse().setResponseCode(code).setBody(body)
    @Test
    fun `getTrackingActualOrNull devuelve null en HTTP 404`() = runBlocking {
        server.enqueue(ok(code = 404))

        val result = repository.getTrackingActualOrNull("p-1")

        assertTrue(result is Resource.Success)
        assertNull((result as Resource.Success).data)
    }

    @Test
    fun `getTrackingActualOrNull devuelve la ubicacion en 200`() = runBlocking {
        server.enqueue(
            ok(
                """{"longitud": -70.66, "latitud": -33.45, "timestamp": "2026-08-09T18:20:30Z", "esEmergencia": true}"""
            )
        )

        val result = repository.getTrackingActualOrNull("p-1")

        val tracking = (result as Resource.Success).data
        assertNotNull(tracking)
        assertEquals(-33.45, tracking!!.latitud, 0.0001)
        assertEquals(-70.66, tracking.longitud, 0.0001)
        assertTrue(tracking.esEmergencia)
        val recorded = server.takeRequest()
        assertTrue(recorded.requestUrl!!.encodedPath.contains("p-1"))
    }

    @Test
    fun `getTrackingActualOrNull devuelve Error ante 500`() = runBlocking {
        server.enqueue(ok(code = 500))

        val result = repository.getTrackingActualOrNull("p-1")

        assertTrue(result is Resource.Error)
    }

    @Test
    fun `getTrackingActual devuelve Error ante 404 distinguiendo del OrNull`() = runBlocking {
        server.enqueue(ok(code = 404))

        val result = repository.getTrackingActual("p-1")

        assertTrue(result is Resource.Error)
    }

    @Test
    fun `getTrackingActual devuelve Success con la ubicacion`() = runBlocking {
        server.enqueue(ok("""{"longitud": 1.0, "latitud": 2.0, "timestamp": "2026-08-09T18:20:30Z"}"""))

        val result = repository.getTrackingActual("p-1")

        val tracking = (result as Resource.Success).data
        assertEquals(2.0, tracking.latitud, 0.0001)
    }

    @Test
    fun `sendTracking envia POST con body correcto y devuelve mensaje`() = runBlocking {
        server.enqueue(ok("""{"message": "Ubicacion registrada"}"""))

        val result = repository.sendTracking(
            TrackingGpsRequest(pacienteId = "p-1", latitud = -33.45, longitud = -70.66, esEmergencia = true)
        )

        assertEquals("Ubicacion registrada", (result as Resource.Success).data)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/Sensores/tracking", recorded.requestUrl!!.encodedPath)
        val body = recorded.body?.readUtf8() ?: ""
        assertTrue(body.contains("\"pacienteId\":\"p-1\""))
        assertTrue(body.contains("\"esEmergencia\":true"))
    }

    @Test
    fun `sendTracking devuelve Error ante 500`() = runBlocking {
        server.enqueue(ok(code = 500))

        val result = repository.sendTracking(
            TrackingGpsRequest(pacienteId = "p-1", latitud = 1.0, longitud = 2.0)
        )

        assertTrue(result is Resource.Error)
    }

    @Test
    fun `sendTrackingBatch envia lote con ruta correcta y body en arreglo`() = runBlocking {
        server.enqueue(ok("""{"message": "Batch registrado"}"""))

        val result = repository.sendTrackingBatch(
            listOf(
                TrackingGpsRequest(pacienteId = "p-1", latitud = -33.45, longitud = -70.66),
                TrackingGpsRequest(pacienteId = "p-1", latitud = -33.46, longitud = -70.67)
            )
        )

        assertEquals("Batch registrado", (result as Resource.Success).data)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/Sensores/tracking-batch", recorded.requestUrl!!.encodedPath)
        val body = recorded.body?.readUtf8() ?: ""
        assertTrue(body.startsWith("["))
        assertTrue(body.contains("\"latitud\":-33.45"))
        assertTrue(body.contains("\"latitud\":-33.46"))
    }

    @Test
    fun `sendTrackingBatch devuelve Error ante 500`() = runBlocking {
        server.enqueue(ok(code = 500))

        val result = repository.sendTrackingBatch(
            listOf(TrackingGpsRequest(pacienteId = "p-1", latitud = 1.0, longitud = 2.0))
        )

        assertTrue(result is Resource.Error)
    }

    @Test
    fun `getTrackingRuta devuelve la lista de puntos en 200`() = runBlocking {
        server.enqueue(
            ok(
                """
                [
                  {"longitud": -70.66, "latitud": -33.45, "timestamp": "2026-08-09T18:20:30Z"},
                  {"longitud": -70.65, "latitud": -33.44, "timestamp": "2026-08-09T18:25:30Z"}
                ]
                """.trimIndent()
            )
        )

        val result = repository.getTrackingRuta("p-1", desde = "2026-08-09T00:00:00Z", hasta = "2026-08-09T23:59:59Z")

        val ruta = (result as Resource.Success).data
        assertEquals(2, ruta.size)
        assertEquals(-33.45, ruta[0].latitud, 0.0001)
        assertEquals(-33.44, ruta[1].latitud, 0.0001)

        val recorded = server.takeRequest()
        assertTrue(recorded.requestUrl!!.encodedPath.contains("/api/Sensores/tracking/p-1/ruta"))
        assertTrue(recorded.requestUrl!!.queryParameterNames.contains("desde"))
        assertTrue(recorded.requestUrl!!.queryParameterNames.contains("hasta"))
        assertEquals("2026-08-09T00:00:00Z", recorded.requestUrl!!.queryParameter("desde"))
    }

    @Test
    fun `getTrackingRuta devuelve Error ante 500`() = runBlocking {
        server.enqueue(ok(code = 500))

        val result = repository.getTrackingRuta("p-1", desde = "a", hasta = "b")

        assertTrue(result is Resource.Error)
    }

    @Test
    fun `getTrackingActual devuelve Error ante 500`() = runBlocking {
        server.enqueue(ok(code = 500))

        val result = repository.getTrackingActual("p-1")

        assertTrue(result is Resource.Error)
    }
}

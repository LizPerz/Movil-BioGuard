package com.bioguard.movil.ui.viewmodel

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.bioguard.movil.data.repository.PacienteRepository
import com.bioguard.movil.data.repository.SensorRepository
import com.bioguard.movil.datastore.UserPreferences
import com.bioguard.movil.network.ApiService
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Duration
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocationViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var sensorRepository: SensorRepository
    private lateinit var pacienteRepository: PacienteRepository
    private lateinit var prefs: UserPreferences

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val context = ApplicationProvider.getApplicationContext<Context>()
        prefs = UserPreferences(context)
        runBlocking { prefs.clearAll() }
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
        sensorRepository = SensorRepository(api)
        pacienteRepository = PacienteRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun ok(body: String = "{}", code: Int = 200): MockResponse =
        MockResponse().setResponseCode(code).setBody(body)

    private fun trackingJson(
        lat: Double,
        lon: Double,
        timestamp: String = "2026-08-09T18:20:30Z",
        emergencia: Boolean = false
    ): String =
        """{"longitud": $lon, "latitud": $lat, "timestamp": "$timestamp", "esEmergencia": $emergencia}"""

    private fun createViewModel(): LocationViewModel =
        LocationViewModel(sensorRepository, pacienteRepository, prefs)

    /**
     * El ViewModel arranca el polling en el init (Dispatchers.Main sobre el looper de
     * Robolectric), por lo que consume respuestas en segundo plano. Este helper espera
     * a que el auto-poll de arranque haya consumido [expected] peticiones antes de que
     * el test dispare sus propias llamadas, garantizando orden determinista.
     */
    private fun awaitAutoPollRequests(expected: Int) {
        val deadline = System.currentTimeMillis() + 5_000
        while (server.requestCount < expected && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertTrue(
            "Se esperaban $expected peticiones del auto-poll, pero hubo ${server.requestCount}",
            server.requestCount >= expected
        )
    }

    private fun requestPaths(): List<String> =
        (0 until server.requestCount).map { server.takeRequest().requestUrl!!.encodedPath }

    @Test
    fun `sin paciente asignado muestra mensaje y no consulta tracking`() = runBlocking {
        server.enqueue(ok("""{"rol": "dueno", "pacienteId": null, "plan": null, "permisos": []}"""))
        server.enqueue(ok(code = 500))
        val vm = createViewModel()
        awaitAutoPollRequests(2)
        server.enqueue(ok("""{"rol": "dueno", "pacienteId": null, "plan": null, "permisos": []}"""))
        server.enqueue(ok(code = 500))
        vm.pollOnce()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.error?.contains("No tienes un paciente asignado") == true)
        assertNull(state.ubicacion)
        assertFalse(state.sinUbicacion)
        assertFalse(requestPaths().any { it.contains("tracking") })
    }

    @Test
    fun `404 del backend muestra estado sin ubicacion sin error`() = runBlocking {
        prefs.savePatientId("p-1")
        server.enqueue(ok(code = 404))
        val vm = createViewModel()
        awaitAutoPollRequests(1)
        server.enqueue(ok(code = 404))
        vm.pollOnce()

        val state = vm.uiState.value
        assertNull(state.error)
        assertTrue(state.sinUbicacion)
        assertNull(state.ubicacion)
        assertFalse(state.isLoading)
    }

    @Test
    fun `ubicacion 200 entrega coordenadas y timestamp parseado`() = runBlocking {
        prefs.savePatientId("p-1")
        server.enqueue(ok(trackingJson(-33.45, -70.66, "2026-08-09T18:20:30Z", emergencia = true)))
        val vm = createViewModel()
        awaitAutoPollRequests(1)
        server.enqueue(ok(trackingJson(-33.45, -70.66, "2026-08-09T18:20:30Z", emergencia = true)))
        vm.pollOnce()

        val state = vm.uiState.value
        val ubicacion = state.ubicacion
        assertNotNull(ubicacion)
        assertEquals(-33.45, ubicacion!!.latitud, 0.0001)
        assertEquals(-70.66, ubicacion.longitud, 0.0001)
        assertTrue(ubicacion.esEmergencia)
        assertFalse(state.sinUbicacion)
        assertNull(state.error)
        assertEquals(Instant.parse("2026-08-09T18:20:30Z"), state.lastUpdatedAt)
    }

    @Test
    fun `error 500 conserva la ultima ubicacion conocida`() = runBlocking {
        prefs.savePatientId("p-1")
        server.enqueue(ok(trackingJson(1.0, 2.0)))
        val vm = createViewModel()
        awaitAutoPollRequests(1)
        server.enqueue(ok(trackingJson(3.0, 4.0)))
        server.enqueue(ok(code = 500))
        vm.pollOnce()
        assertNotNull(vm.uiState.value.ubicacion)
        vm.pollOnce()

        val state = vm.uiState.value
        assertEquals(3.0, state.ubicacion!!.latitud, 0.0001)
        assertNotNull(state.error)
    }

    @Test
    fun `timestamp invalido usa Instant now como ultima actualizacion`() = runBlocking {
        prefs.savePatientId("p-1")
        server.enqueue(ok(trackingJson(1.0, 2.0, timestamp = "no-es-iso")))
        val vm = createViewModel()
        awaitAutoPollRequests(1)
        server.enqueue(ok(trackingJson(1.0, 2.0, timestamp = "no-es-iso")))
        vm.pollOnce()

        val state = vm.uiState.value
        assertNotNull(state.lastUpdatedAt)
        val diff = Duration.between(state.lastUpdatedAt, Instant.now()).abs()
        assertTrue("lastUpdatedAt deberia ser ~ahora, fue ${state.lastUpdatedAt}", diff.seconds < 5)
    }

    @Test
    fun `un nuevo poll actualiza las coordenadas del pin`() = runBlocking {
        prefs.savePatientId("p-1")
        server.enqueue(ok(trackingJson(1.0, 2.0)))
        val vm = createViewModel()
        awaitAutoPollRequests(1)
        server.enqueue(ok(trackingJson(1.0, 2.0)))
        vm.pollOnce()
        assertEquals(1.0, vm.uiState.value.ubicacion!!.latitud, 0.0001)

        server.enqueue(ok(trackingJson(5.0, 6.0)))
        vm.pollOnce()
        assertEquals(5.0, vm.uiState.value.ubicacion!!.latitud, 0.0001)
        assertEquals(6.0, vm.uiState.value.ubicacion!!.longitud, 0.0001)
    }

    @Test
    fun `retry reinicia el polling sin romper el estado`() = runBlocking {
        prefs.savePatientId("p-1")
        server.enqueue(ok(trackingJson(1.0, 2.0)))
        val vm = createViewModel()
        awaitAutoPollRequests(1)
        server.enqueue(ok(trackingJson(7.0, 8.0)))
        server.enqueue(ok(trackingJson(9.0, 10.0)))
        vm.retry()
        awaitAutoPollRequests(2)
        vm.pollOnce()

        val state = vm.uiState.value
        assertEquals(9.0, state.ubicacion!!.latitud, 0.0001)
        assertNull(state.error)
        assertFalse(state.isLoading)
    }
}

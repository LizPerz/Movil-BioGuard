package com.bioguard.movil.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bioguard.movil.datastore.UserPreferences
import com.bioguard.movil.network.ApiService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PacienteRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: PacienteRepository
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
        repository = PacienteRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun ok(body: String = "{}", code: Int = 200): MockResponse =
        MockResponse().setResponseCode(code).setBody(body)

    private fun accesoJson(pacienteId: String?): String =
        """{"rol": "dueno", "pacienteId": ${pacienteId?.let { "\"$it\"" } ?: "null"}, "plan": null, "permisos": []}"""

    private fun requestPaths(): List<String> =
        (0 until server.requestCount).map { server.takeRequest().requestUrl!!.encodedPath }

    @Test
    fun `resolveEffectivePatientId usa la cache sin llamar a la red`() = runBlocking {
        prefs.savePatientId("p-cache")

        val result = repository.resolveEffectivePatientId(prefs)

        assertEquals("p-cache", result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `resolveEffectivePatientId usa mi-acceso y persiste el paciente`() = runBlocking {
        server.enqueue(ok(accesoJson("p-acceso")))

        val result = repository.resolveEffectivePatientId(prefs)

        assertEquals("p-acceso", result)
        assertEquals("p-acceso", prefs.patientId.first())
        assertEquals(listOf("/api/UsuariosWeb/mi-acceso"), requestPaths())
    }

    @Test
    fun `resolveEffectivePatientId cae a mi-paciente cuando mi-acceso falla`() = runBlocking {
        server.enqueue(ok(code = 403))
        server.enqueue(ok("""{"id": "p-2", "nombre": "Juan", "esDiabetico": false}"""))

        val result = repository.resolveEffectivePatientId(prefs)

        assertEquals("p-2", result)
        assertEquals("p-2", prefs.patientId.first())
        assertEquals(
            listOf("/api/UsuariosWeb/mi-acceso", "/api/Pacientes/mi-paciente"),
            requestPaths()
        )
    }

    @Test
    fun `resolveEffectivePatientId cae a mi-paciente cuando mi-acceso no trae paciente`() = runBlocking {
        server.enqueue(ok(accesoJson(null)))
        server.enqueue(ok("""{"id": "p-3", "nombre": "Ana", "esDiabetico": true}"""))

        val result = repository.resolveEffectivePatientId(prefs)

        assertEquals("p-3", result)
    }

    @Test
    fun `resolveEffectivePatientId devuelve null si todo falla`() = runBlocking {
        server.enqueue(ok(code = 500))
        server.enqueue(ok(code = 500))

        val result = repository.resolveEffectivePatientId(prefs)

        assertNull(result)
    }

    @Test
    fun `resolvePatientId usa mi-paciente directamente`() = runBlocking {
        server.enqueue(ok("""{"id": "p-directo", "nombre": "Solo", "esDiabetico": false}"""))

        val result = repository.resolvePatientId(prefs)

        assertEquals("p-directo", result)
        assertEquals("p-directo", prefs.patientId.first())
    }

    @Test
    fun `resolvePatientId devuelve null ante error de red`() = runBlocking {
        server.enqueue(ok(code = 500))

        val result = repository.resolvePatientId(prefs)

        assertNull(result)
    }

    @Test
    fun `resolveCaregiverPatientId usa mi-acceso sin recurrir a mi-paciente`() = runBlocking {
        server.enqueue(ok(accesoJson("p-cuidador")))

        val result = repository.resolveCaregiverPatientId(prefs)

        assertEquals("p-cuidador", result)
        assertEquals(1, server.requestCount)
    }
}

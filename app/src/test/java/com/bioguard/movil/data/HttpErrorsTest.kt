package com.bioguard.movil.data

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class HttpErrorsTest {

    @Test
    fun `toUserMessage returns message from json body when present`() {
        val json = """{"message": "El correo ya está registrado"}"""
        val responseBody = json.toResponseBody("application/json".toMediaTypeOrNull())
        val exception = HttpException(Response.error<Any>(400, responseBody))

        val result = exception.toUserMessage("Error por defecto")
        assertEquals("El correo ya está registrado", result)
    }

    @Test
    fun `toUserMessage returns mensaje key from json body when present`() {
        val json = """{"mensaje": "Credenciales inválidas"}"""
        val responseBody = json.toResponseBody("application/json".toMediaTypeOrNull())
        val exception = HttpException(Response.error<Any>(400, responseBody))

        val result = exception.toUserMessage("Error por defecto")
        assertEquals("Credenciales inválidas", result)
    }

    @Test
    fun `toUserMessage returns detail key from json body when present`() {
        val json = """{"detail": "Token expirado"}"""
        val responseBody = json.toResponseBody("application/json".toMediaTypeOrNull())
        val exception = HttpException(Response.error<Any>(401, responseBody))

        val result = exception.toUserMessage("Error por defecto")
        assertEquals("Token expirado", result)
    }

    @Test
    fun `toUserMessage returns mapped status code message when body is empty`() {
        val emptyBody = "".toResponseBody("application/json".toMediaTypeOrNull())
        val exception401 = HttpException(Response.error<Any>(401, emptyBody))
        val exception403 = HttpException(Response.error<Any>(403, emptyBody))
        val exception404 = HttpException(Response.error<Any>(404, emptyBody))
        val exception409 = HttpException(Response.error<Any>(409, emptyBody))
        val exception500 = HttpException(Response.error<Any>(500, emptyBody))

        assertEquals("Sesion expirada, inicia sesion de nuevo", exception401.toUserMessage("fallback"))
        assertEquals("No tienes permisos para realizar esta accion", exception403.toUserMessage("fallback"))
        assertEquals("Recurso no encontrado", exception404.toUserMessage("fallback"))
        assertEquals("Ya existe un registro con estos datos", exception409.toUserMessage("fallback"))
        assertEquals("Error del servidor (500)", exception500.toUserMessage("fallback"))
    }

    @Test
    fun `toUserMessage returns network error message for IOException`() {
        val exception = IOException("Failed to connect")
        val result = exception.toUserMessage("Error por defecto")
        assertEquals("Sin conexion al servidor, revisa tu internet", result)
    }

    @Test
    fun `toUserMessage returns fallback for unknown exception without message`() {
        val exception = RuntimeException()
        val result = exception.toUserMessage("Error general")
        assertEquals("Error general", result)
    }
}

package com.bioguard.movil.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import kotlin.math.pow

class RetryInterceptor(private val maxRetries: Int = 3) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        var lastException: IOException? = null

        while (attempt < maxRetries) {
            try {
                val response = chain.proceed(chain.request())
                if (response.code in 500..599 && attempt < maxRetries - 1) {
                    response.close()
                    val backoffMs = (2.0.pow(attempt.toDouble()) * 1000).toLong()
                    Thread.sleep(backoffMs)
                    attempt++
                    continue
                }
                return response
            } catch (e: IOException) {
                lastException = e
                if (attempt >= maxRetries - 1) throw e
                val backoffMs = (2.0.pow(attempt.toDouble()) * 1000).toLong()
                Thread.sleep(backoffMs)
                attempt++
            }
        }
        throw lastException ?: IOException("Error de conexión tras varios reintentos")
    }
}

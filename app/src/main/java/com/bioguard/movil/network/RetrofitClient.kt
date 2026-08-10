package com.bioguard.movil.network

import com.bioguard.movil.BuildConfig
import com.google.gson.Gson
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private var authToken: String? = null

    var refreshTokenProvider: () -> String? = { null }
    var onTokenRefreshed: (accessToken: String, refreshToken: String) -> Unit = { _, _ -> }

    private val gson = Gson()
    private val refreshLock = Any()
    private const val PROACTIVE_REFRESH_MARGIN_MS = 5 * 60 * 1000L

    private fun jwtExpirationMillis(token: String): Long? {
        return try {
            val parts = token.split('.')
            if (parts.size < 2) return null
            val payload = parts[1].let { base64 ->
                val padded = base64 + "=".repeat((4 - base64.length % 4) % 4)
                android.util.Base64.decode(padded, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
            }
            val json = org.json.JSONObject(String(payload, java.nio.charset.StandardCharsets.UTF_8))
            json.optLong("exp", 0L).takeIf { it > 0L }?.times(1000L)
        } catch (_: Exception) { null }
    }

    private val proactiveRefreshInterceptor = Interceptor { chain ->
        val token = authToken
        if (token != null) {
            val expiresAt = jwtExpirationMillis(token)
            val nowMs = System.currentTimeMillis()
            if (expiresAt != null && expiresAt - nowMs < PROACTIVE_REFRESH_MARGIN_MS) {
                val refreshToken = refreshTokenProvider()
                if (refreshToken != null) {
                    synchronized(refreshLock) {
                        val currentExp = jwtExpirationMillis(authToken ?: "") ?: 0L
                        if (currentExp - System.currentTimeMillis() < PROACTIVE_REFRESH_MARGIN_MS) {
                            try {
                                val jsonBody = gson.toJson(RefreshTokenRequest(authToken!!, refreshToken))
                                    .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                                refreshClient.newCall(
                                    okhttp3.Request.Builder()
                                        .url("${Constants.BASE_URL}api/Auth/refresh")
                                        .post(jsonBody)
                                        .build()
                                ).execute().use { res ->
                                    if (res.isSuccessful) {
                                        val parsed = gson.fromJson(res.body?.string(), RefreshTokenResponse::class.java)
                                        authToken = parsed.accessToken
                                        onTokenRefreshed(parsed.accessToken, parsed.refreshToken)
                                    }
                                }
                            } catch (_: Exception) { }
                        }
                    }
                }
            }
        }
        chain.proceed(chain.request())
    }

    fun setToken(token: String?) {
        authToken = token
    }

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
        authToken?.let {
            request.addHeader("Authorization", "Bearer $it")
        }
        chain.proceed(request.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        val sanitized = message
            .replace(Regex("(?i)(Authorization:\\s*Bearer\\s+)[^\\s\"]+"), "$1***")
            .replace(
                Regex("(?i)(\"[^\"]*password[^\"]*\"\\s*:\\s*\\\")[^\"]*(\")"),
                "$1***$2"
            )
        android.util.Log.d("BioGuardAPI", sanitized)
    }.apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val refreshClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val authenticator = Authenticator { _, response ->
        val current = authToken ?: return@Authenticator null
        val refresh = refreshTokenProvider() ?: return@Authenticator null
        synchronized(refreshLock) {
            if (authToken != null && authToken != current) {
                return@Authenticator response.request.newBuilder()
                    .header("Authorization", "Bearer ${authToken}")
                    .build()
            }
            try {
                val jsonBody = gson.toJson(RefreshTokenRequest(current, refresh))
                    .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                refreshClient.newCall(
                    okhttp3.Request.Builder()
                        .url("${Constants.BASE_URL}api/Auth/refresh")
                        .post(jsonBody)
                        .build()
                ).execute().use { res ->
                    if (!res.isSuccessful) return@Authenticator null
                    val parsed = gson.fromJson(res.body?.string(), RefreshTokenResponse::class.java)
                    authToken = parsed.accessToken
                    onTokenRefreshed(parsed.accessToken, parsed.refreshToken)
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${parsed.accessToken}")
                        .build()
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private val certificatePinner = okhttp3.CertificatePinner.Builder()
        // Certificados del servidor (DigitalOcean App Platform / actual)
        .add("bioguard-api-lkvnq.ondigitalocean.app", "sha256/uuKzISd/FcyjVL3SJqZuUhdK1CAHK2A6pswUQQrED34=")
        .add("bioguard-api-lkvnq.ondigitalocean.app", "sha256/kIdp6NNEd8wsugYyyIYFsi1ylMCED3hZbSR8ZFsa/A4=")
        .add("bioguard-api-lkvnq.ondigitalocean.app", "sha256/mEflZT5enoR1FuXLgYYGqnVEoZvmf9c2bVBpiOjYQ0c=")
        // Backup pins: CA raíz Let's Encrypt (ISRG Root X1) y R10/E5/E6 intermedios
        .add("bioguard-api-lkvnq.ondigitalocean.app", "sha256/C5+lpZ7tcVwmwQIMcRtPbsQtWLABXhQzejna0wHFr8M=")
        .add("bioguard-api-lkvnq.ondigitalocean.app", "sha256/hxqRlPTu1bMS/0DITB1SSu0vd4u/8l8TjPgfaAp63Vg=")
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(RetryInterceptor(maxRetries = 3))
        .addInterceptor(proactiveRefreshInterceptor)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .authenticator(authenticator)
        .certificatePinner(certificatePinner)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(Constants.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun getOkHttpClient(context: android.content.Context? = null): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(RetryInterceptor(maxRetries = 3))

        context?.let {
            builder.addInterceptor(NetworkConnectionInterceptor(it))
        }

        return builder
            .addInterceptor(proactiveRefreshInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .authenticator(authenticator)
            .certificatePinner(certificatePinner)
            .build()
    }

    val api: ApiService = retrofit.create(ApiService::class.java)
}

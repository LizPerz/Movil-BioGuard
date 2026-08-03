package com.example.bioguard_movil.network

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
        val isDebug = try {
            val clazz = Class.forName("com.example.bioguard_movil.BuildConfig")
            val field = clazz.getField("DEBUG")
            field.get(null) as? Boolean ?: true
        } catch (e: Exception) {
            true
        }
        level = if (isDebug) {
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
                        .url("${com.example.bioguard_movil.BuildConfig.BASE_URL}api/Auth/refresh")
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

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .authenticator(authenticator)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(com.example.bioguard_movil.BuildConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: ApiService = retrofit.create(ApiService::class.java)
}

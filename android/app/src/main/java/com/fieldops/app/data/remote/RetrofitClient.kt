package com.fieldops.app.data.remote

import com.fieldops.app.Session
import com.fieldops.app.data.remote.dto.RefreshTokenRequestDto
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val baseUrl: String
        get() = if (Session.BASE_URL.endsWith("/")) {
            Session.BASE_URL
        } else {
            "${Session.BASE_URL}/"
        }

    private val authHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    private val appHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    val authApiService: AuthApiService by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(authHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }

    val workOrdersApiService: WorkOrdersApiService by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(appHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WorkOrdersApiService::class.java)
    }

    fun executeTokenRefresh(refreshToken: String): Pair<String, String>? {
        return try {
            val response = authApiService
                .refreshToken(RefreshTokenRequestDto(refreshToken = refreshToken))
                .execute()
            val body = response.body()
            val newAccessToken = body?.token
            val newRefreshToken = body?.refreshToken
            if (response.isSuccessful && body?.ok == true &&
                !newAccessToken.isNullOrBlank() && !newRefreshToken.isNullOrBlank()
            ) {
                newAccessToken to newRefreshToken
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private const val TIMEOUT_SECONDS = 10L
}

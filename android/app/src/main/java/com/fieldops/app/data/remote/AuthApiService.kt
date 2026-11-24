package com.fieldops.app.data.remote

import com.fieldops.app.data.remote.dto.LoginRequestDto
import com.fieldops.app.data.remote.dto.LoginResponseDto
import com.fieldops.app.data.remote.dto.RefreshTokenRequestDto
import com.fieldops.app.data.remote.dto.RefreshTokenResponseDto
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequestDto,
    ): Response<LoginResponseDto>

    @POST("api/auth/refresh")
    fun refreshToken(
        @Body request: RefreshTokenRequestDto,
    ): Call<RefreshTokenResponseDto>
}

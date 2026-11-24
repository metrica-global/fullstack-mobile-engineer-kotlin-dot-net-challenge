package com.fieldops.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginRequestDto(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
)

data class LoginResponseDto(
    @SerializedName("ok") val ok: Boolean = false,
    @SerializedName("token") val token: String? = null,
    @SerializedName("refreshToken") val refreshToken: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("user") val user: List<UserDto>? = null,
)

data class UserDto(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("role") val role: String,
)

data class RefreshTokenRequestDto(
    @SerializedName("refreshToken") val refreshToken: String,
)

data class RefreshTokenResponseDto(
    @SerializedName("ok") val ok: Boolean = false,
    @SerializedName("token") val token: String? = null,
    @SerializedName("refreshToken") val refreshToken: String? = null,
    @SerializedName("message") val message: String? = null,
)

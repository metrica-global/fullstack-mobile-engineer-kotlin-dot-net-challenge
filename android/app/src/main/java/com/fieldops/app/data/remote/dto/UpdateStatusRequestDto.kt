package com.fieldops.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UpdateStatusRequestDto(
    @SerializedName("status") val status: String,
)

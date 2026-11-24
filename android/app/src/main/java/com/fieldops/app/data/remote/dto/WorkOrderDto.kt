package com.fieldops.app.data.remote.dto

import com.fieldops.app.domain.model.WorkOrder
import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

data class WorkOrderDto(
    @SerializedName("id") val id: Int,
    @SerializedName("code") val code: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("customerName") val customerName: String,
    @SerializedName("address") val address: String,
    @SerializedName("priority") val priority: String,
    @SerializedName("status") val status: String,
    @SerializedName("technicianId") val technicianId: Int,
    @SerializedName("scheduledAt") val scheduledAt: String,
    @SerializedName("updatedAt") val updatedAt: String?,
) {
    fun toDomain(): WorkOrder = WorkOrder(
        id = id,
        code = code,
        title = title,
        description = description,
        customerName = customerName,
        address = address,
        priority = priority,
        status = status,
        technicianId = technicianId,
        scheduledAt = parseDateTime(scheduledAt),
        updatedAt = updatedAt?.takeIf { it.isNotBlank() }?.let(::parseDateTime),
    )

    companion object {
        private fun parseDateTime(value: String): LocalDateTime {
            return try {
                LocalDateTime.parse(value)
            } catch (_: DateTimeParseException) {
                OffsetDateTime.parse(value).toLocalDateTime()
            }
        }
    }
}

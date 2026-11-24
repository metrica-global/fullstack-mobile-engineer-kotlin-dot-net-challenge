package com.fieldops.app.domain.model

import java.time.LocalDateTime

data class WorkOrder(
    val id: Int,
    val code: String,
    val title: String,
    val description: String?,
    val customerName: String,
    val address: String,
    val priority: String,
    val status: String,
    val technicianId: Int,
    val scheduledAt: LocalDateTime,
    val updatedAt: LocalDateTime? = null,
) {
    val isHighPriority: Boolean
        get() = priority.equals(PRIORITY_HIGH, ignoreCase = true)

    fun isOverdue(now: LocalDateTime = LocalDateTime.now()): Boolean {
        return scheduledAt.isBefore(now) && !status.equals(STATUS_COMPLETED, ignoreCase = true)
    }

    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_IN_PROGRESS = "IN_PROGRESS"
        const val STATUS_COMPLETED = "COMPLETED"
        const val PRIORITY_HIGH = "HIGH"
    }
}

package com.fieldops.app.data.remote

import com.fieldops.app.data.remote.dto.UpdateStatusRequestDto
import com.fieldops.app.domain.model.WorkOrder

class WorkOrdersRemoteDataSource(
    private val apiService: WorkOrdersApiService = RetrofitClient.workOrdersApiService,
) {

    suspend fun fetchOrders(status: String?): List<WorkOrder> {
        val normalizedStatus = status?.takeIf { it.isNotBlank() }
        return apiService.getOrders(normalizedStatus).map { it.toDomain() }
    }

    suspend fun updateOrderStatus(orderId: Int, status: String) {
        apiService.updateOrderStatus(
            orderId = orderId,
            request = UpdateStatusRequestDto(status = status),
        )
    }
}

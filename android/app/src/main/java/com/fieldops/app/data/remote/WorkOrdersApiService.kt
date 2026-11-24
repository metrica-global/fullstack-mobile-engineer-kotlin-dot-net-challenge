package com.fieldops.app.data.remote

import com.fieldops.app.data.remote.dto.UpdateStatusRequestDto
import com.fieldops.app.data.remote.dto.WorkOrderDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface WorkOrdersApiService {

    @GET("api/workorders")
    suspend fun getOrders(
        @Query("status") status: String? = null,
    ): List<WorkOrderDto>

    @PUT("api/workorders/{id}/status")
    suspend fun updateOrderStatus(
        @Path("id") orderId: Int,
        @Body request: UpdateStatusRequestDto,
    )
}

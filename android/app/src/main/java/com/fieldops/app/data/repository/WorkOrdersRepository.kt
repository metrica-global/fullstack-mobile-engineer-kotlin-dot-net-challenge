package com.fieldops.app.data.repository

import com.fieldops.app.data.remote.WorkOrdersRemoteDataSource
import com.fieldops.app.domain.model.WorkOrder

interface WorkOrdersRepository {
    suspend fun getOrders(status: String? = null): List<WorkOrder>
    suspend fun updateStatus(orderId: Int, status: String)
}

class DefaultWorkOrdersRepository(
    private val remoteDataSource: WorkOrdersRemoteDataSource = WorkOrdersRemoteDataSource(),
) : WorkOrdersRepository {

    override suspend fun getOrders(status: String?): List<WorkOrder> {
        return remoteDataSource.fetchOrders(status)
    }

    override suspend fun updateStatus(orderId: Int, status: String) {
        remoteDataSource.updateOrderStatus(orderId, status)
    }
}

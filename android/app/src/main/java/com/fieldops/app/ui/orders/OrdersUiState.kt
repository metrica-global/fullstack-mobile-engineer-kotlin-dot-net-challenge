package com.fieldops.app.ui.orders

import com.fieldops.app.domain.model.WorkOrder

sealed interface OrdersScreenState {
    data object Loading : OrdersScreenState
    data class Content(val orders: List<WorkOrder>) : OrdersScreenState
    data object Empty : OrdersScreenState
    data class Error(val message: String) : OrdersScreenState
}

data class OrdersUiState(
    val screenState: OrdersScreenState = OrdersScreenState.Loading,
    val selectedFilter: String = "",
    val pendingCount: Int = 0,
    val isUpdatingStatus: Boolean = false,
    val userMessage: String? = null,
)

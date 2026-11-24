package com.fieldops.app.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fieldops.app.data.repository.DefaultWorkOrdersRepository
import com.fieldops.app.data.repository.WorkOrdersRepository
import com.fieldops.app.domain.model.WorkOrder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OrdersViewModel(
    private val repository: WorkOrdersRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadOrders()
    }

    fun onFilterSelected(filter: String) {
        if (_uiState.value.selectedFilter == filter) {
            return
        }
        _uiState.update { it.copy(selectedFilter = filter) }
        loadOrders()
    }

    fun loadOrders() {
        loadJob?.cancel()
        val filter = _uiState.value.selectedFilter
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(screenState = OrdersScreenState.Loading) }
            try {
                val orders = repository.getOrders(filter.ifBlank { null })
                val pending = orders.count {
                    it.status.equals(WorkOrder.STATUS_PENDING, ignoreCase = true)
                }
                val nextScreenState = if (orders.isEmpty()) {
                    OrdersScreenState.Empty
                } else {
                    OrdersScreenState.Content(orders)
                }
                _uiState.update {
                    it.copy(
                        screenState = nextScreenState,
                        pendingCount = pending,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        screenState = OrdersScreenState.Error("Ocurrió un error al cargar las órdenes"),
                        pendingCount = 0,
                    )
                }
            }
        }
    }

    fun updateOrderStatus(orderId: Int, newStatus: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingStatus = true) }
            try {
                repository.updateStatus(orderId, newStatus)
                _uiState.update {
                    it.copy(
                        isUpdatingStatus = false,
                        userMessage = "Estado actualizado",
                    )
                }
                loadOrders()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isUpdatingStatus = false,
                        userMessage = "No se pudo actualizar",
                    )
                }
            }
        }
    }

    fun onUserMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return OrdersViewModel(DefaultWorkOrdersRepository()) as T
            }
        }
    }
}

package com.fieldops.app.ui.orders

import com.fieldops.app.data.remote.dto.WorkOrderDto
import com.fieldops.app.data.repository.WorkOrdersRepository
import com.fieldops.app.domain.model.WorkOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class OrdersViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadOrders emite estado Content con las ordenes y calcula la cantidad de pendientes`() = runTest {
        val sampleOrders = listOf(
            createOrder(id = 1, code = "OT-0001", status = WorkOrder.STATUS_PENDING),
            createOrder(id = 2, code = "OT-0002", status = WorkOrder.STATUS_IN_PROGRESS),
            createOrder(id = 3, code = "OT-0003", status = WorkOrder.STATUS_PENDING),
        )
        val repository = FakeWorkOrdersRepository(ordersResult = Result.success(sampleOrders))

        val viewModel = OrdersViewModel(repository)
        assertEquals(OrdersScreenState.Loading, viewModel.uiState.value.screenState)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.screenState is OrdersScreenState.Content)
        assertEquals(sampleOrders, (state.screenState as OrdersScreenState.Content).orders)
        assertEquals(2, state.pendingCount)
    }

    @Test
    fun `loadOrders emite estado Empty cuando el repositorio no devuelve ordenes`() = runTest {
        val repository = FakeWorkOrdersRepository(ordersResult = Result.success(emptyList()))

        val viewModel = OrdersViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(OrdersScreenState.Empty, state.screenState)
        assertEquals(0, state.pendingCount)
    }

    @Test
    fun `loadOrders emite estado Error cuando el repositorio lanza una excepcion`() = runTest {
        val repository = FakeWorkOrdersRepository(
            ordersResult = Result.failure(IOException("Red no disponible")),
        )

        val viewModel = OrdersViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.screenState is OrdersScreenState.Error)
        assertEquals(0, state.pendingCount)
    }

    @Test
    fun `onFilterSelected solo recarga cuando cambia el filtro e ignora filtro duplicado al rotar`() = runTest {
        val pendingOrder = createOrder(id = 1, code = "OT-0001", status = WorkOrder.STATUS_PENDING)
        val repository = FakeWorkOrdersRepository(ordersResult = Result.success(listOf(pendingOrder)))

        val viewModel = OrdersViewModel(repository)
        advanceUntilIdle()
        assertEquals(1, repository.getOrdersCallCount)

        // Simula re-selección del mismo filtro tras girar la pantalla
        viewModel.onFilterSelected("")
        advanceUntilIdle()
        assertEquals(1, repository.getOrdersCallCount)

        // Cambiar a un filtro distinto sí debe consultar el repositorio
        viewModel.onFilterSelected(WorkOrder.STATUS_PENDING)
        advanceUntilIdle()
        assertEquals(2, repository.getOrdersCallCount)
        assertEquals(WorkOrder.STATUS_PENDING, repository.lastRequestedStatus)
        assertEquals(WorkOrder.STATUS_PENDING, viewModel.uiState.value.selectedFilter)
    }

    @Test
    fun `updateOrderStatus actualiza el repositorio, emite mensaje de confirmacion y recarga las ordenes`() = runTest {
        val initialOrder = createOrder(id = 4, code = "OT-0004", status = WorkOrder.STATUS_PENDING)
        val repository = FakeWorkOrdersRepository(ordersResult = Result.success(listOf(initialOrder)))

        val viewModel = OrdersViewModel(repository)
        advanceUntilIdle()

        val updatedOrder = initialOrder.copy(status = WorkOrder.STATUS_IN_PROGRESS)
        repository.ordersResult = Result.success(listOf(updatedOrder))

        viewModel.updateOrderStatus(orderId = 4, newStatus = WorkOrder.STATUS_IN_PROGRESS)
        advanceUntilIdle()

        assertEquals(4 to WorkOrder.STATUS_IN_PROGRESS, repository.lastUpdatedOrder)
        assertEquals("Estado actualizado", viewModel.uiState.value.userMessage)
        assertEquals(0, viewModel.uiState.value.pendingCount)
    }

    @Test
    fun `WorkOrderDto toDomain mapea los campos y parsea correctamente la fecha ISO scheduledAt`() {
        val dto = WorkOrderDto(
            id = 4,
            code = "OT-0004",
            title = "Mantenimiento preventivo de UPS",
            description = "Revisión semestral según contrato.",
            customerName = "Notaría Paredes",
            address = "Calle Las Begonias 441, San Isidro",
            priority = "HIGH",
            status = "PENDING",
            technicianId = 1,
            scheduledAt = "2026-11-14T10:00:00",
            updatedAt = null,
        )

        val domain = dto.toDomain()

        assertEquals(4, domain.id)
        assertEquals("OT-0004", domain.code)
        assertTrue(domain.isHighPriority)
        assertEquals(LocalDateTime.of(2026, 11, 14, 10, 0, 0), domain.scheduledAt)
    }

    private fun createOrder(
        id: Int,
        code: String,
        status: String,
        priority: String = "MEDIUM",
        scheduledAt: LocalDateTime = LocalDateTime.of(2026, 11, 25, 10, 0),
    ) = WorkOrder(
        id = id,
        code = code,
        title = "Orden de prueba $code",
        description = "Descripción",
        customerName = "Cliente",
        address = "Av. Principal 123",
        priority = priority,
        status = status,
        technicianId = 1,
        scheduledAt = scheduledAt,
        updatedAt = null,
    )

    private class FakeWorkOrdersRepository(
        var ordersResult: Result<List<WorkOrder>>,
        var updateResult: Result<Unit> = Result.success(Unit),
    ) : WorkOrdersRepository {

        var getOrdersCallCount = 0
            private set

        var lastRequestedStatus: String? = null
            private set

        var lastUpdatedOrder: Pair<Int, String>? = null
            private set

        override suspend fun getOrders(status: String?): List<WorkOrder> {
            getOrdersCallCount++
            lastRequestedStatus = status
            return ordersResult.getOrThrow()
        }

        override suspend fun updateStatus(orderId: Int, status: String) {
            updateResult.getOrThrow()
            lastUpdatedOrder = orderId to status
        }
    }
}

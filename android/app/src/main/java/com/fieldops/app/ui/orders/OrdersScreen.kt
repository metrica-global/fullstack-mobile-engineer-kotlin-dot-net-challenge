package com.fieldops.app.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldops.app.R
import com.fieldops.app.domain.model.WorkOrder
import com.fieldops.app.ui.theme.ErrorRedColor
import com.fieldops.app.ui.theme.FieldOpsTheme
import com.fieldops.app.ui.theme.HeaderBackgroundBlue
import com.fieldops.app.ui.theme.HighPriorityBackground
import com.fieldops.app.ui.theme.PrimaryDarkBlue
import com.fieldops.app.ui.theme.StatusCompletedGreen
import com.fieldops.app.ui.theme.StatusInProgressBlue
import com.fieldops.app.ui.theme.StatusPendingOrange
import com.fieldops.app.ui.theme.TextPrimaryColor
import com.fieldops.app.ui.theme.TextSecondaryColor
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val DISPLAY_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

private val FILTER_OPTIONS = listOf(
    "" to "Todas",
    WorkOrder.STATUS_PENDING to "Pendientes",
    WorkOrder.STATUS_IN_PROGRESS to "En progreso",
    WorkOrder.STATUS_COMPLETED to "Completadas",
)

private val STATUS_CHANGE_OPTIONS = listOf(
    WorkOrder.STATUS_PENDING to "Pendiente",
    WorkOrder.STATUS_IN_PROGRESS to "En progreso",
    WorkOrder.STATUS_COMPLETED to "Completada",
)

@Composable
fun OrdersScreen(
    uiState: OrdersUiState,
    fullName: String,
    onFilterSelected: (String) -> Unit,
    onOrderStatusChange: (orderId: Int, newStatus: String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedOrderIdForDialog by rememberSaveable { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        OrdersHeader(
            fullName = fullName,
            pendingCount = uiState.pendingCount,
        )

        OrdersFilterBar(
            selectedFilter = uiState.selectedFilter,
            onFilterSelected = onFilterSelected,
        )

        HorizontalDivider(color = Color(0xFFE0E0E0))

        when (val screenState = uiState.screenState) {
            is OrdersScreenState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is OrdersScreenState.Empty -> {
                Text(
                    text = stringResource(R.string.orders_empty_message),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    textAlign = TextAlign.Center,
                    color = TextSecondaryColor,
                )
            }

            is OrdersScreenState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = screenState.message,
                        color = ErrorRedColor,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onRetry) {
                        Text(text = stringResource(R.string.orders_retry_button))
                    }
                }
            }

            is OrdersScreenState.Content -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    items(
                        items = screenState.orders,
                        key = { it.id },
                    ) { order ->
                        OrderItemRow(
                            order = order,
                            onClick = { selectedOrderIdForDialog = order.id },
                        )
                        HorizontalDivider(color = Color(0xFFE0E0E0))
                    }
                }
            }
        }
    }

    val currentOrders = (uiState.screenState as? OrdersScreenState.Content)?.orders.orEmpty()
    val selectedOrder = currentOrders.firstOrNull { it.id == selectedOrderIdForDialog }

    selectedOrder?.let { order ->
        StatusChangeDialog(
            order = order,
            onDismiss = { selectedOrderIdForDialog = null },
            onStatusSelected = { newStatus ->
                selectedOrderIdForDialog = null
                onOrderStatusChange(order.id, newStatus)
            },
        )
    }
}

@Composable
private fun OrdersHeader(
    fullName: String,
    pendingCount: Int,
) {
    Text(
        text = stringResource(R.string.orders_header_format, fullName, pendingCount),
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderBackgroundBlue)
            .padding(12.dp),
        color = PrimaryDarkBlue,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun OrdersFilterBar(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selectedLabel = FILTER_OPTIONS.firstOrNull { it.first == selectedFilter }?.second
        ?: FILTER_OPTIONS.first().second

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.orders_filter_status_label),
            color = TextPrimaryColor,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clickable { expanded = true }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedLabel,
                    color = TextPrimaryColor,
                )
                Text(
                    text = "▼",
                    fontSize = 11.sp,
                    color = TextSecondaryColor,
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                FILTER_OPTIONS.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(text = label) },
                        onClick = {
                            expanded = false
                            onFilterSelected(value)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderItemRow(
    order: WorkOrder,
    onClick: () -> Unit,
) {
    val backgroundColor = if (order.isHighPriority) {
        HighPriorityBackground
    } else {
        Color.Transparent
    }

    val (statusLabel, statusColor) = when (order.status) {
        WorkOrder.STATUS_PENDING ->
            stringResource(R.string.orders_status_pending) to StatusPendingOrange
        WorkOrder.STATUS_IN_PROGRESS ->
            stringResource(R.string.orders_status_in_progress) to StatusInProgressBlue
        else ->
            stringResource(R.string.orders_status_completed) to StatusCompletedGreen
    }

    val formattedDate = order.scheduledAt.format(DISPLAY_DATE_FORMATTER)
    val isOverdue = order.isOverdue()
    val dateText = if (isOverdue) {
        stringResource(R.string.orders_scheduled_overdue_format, formattedDate)
    } else {
        stringResource(R.string.orders_scheduled_format, formattedDate)
    }
    val dateColor = if (isOverdue) Color.Red else TextSecondaryColor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = order.code,
                modifier = Modifier.weight(1f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryColor,
            )
            Text(
                text = statusLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor,
            )
        }

        Text(
            text = order.title,
            modifier = Modifier.padding(top = 2.dp),
            color = TextPrimaryColor,
            fontSize = 16.sp,
        )

        Text(
            text = order.customerName,
            fontSize = 13.sp,
            color = TextSecondaryColor,
        )

        Text(
            text = order.address,
            fontSize = 13.sp,
            color = TextSecondaryColor,
        )

        Text(
            text = dateText,
            modifier = Modifier.padding(top = 2.dp),
            fontSize = 12.sp,
            color = dateColor,
        )
    }
}

@Composable
private fun StatusChangeDialog(
    order: WorkOrder,
    onDismiss: () -> Unit,
    onStatusSelected: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.orders_change_status_title, order.code))
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                STATUS_CHANGE_OPTIONS.forEach { (statusValue, statusLabel) ->
                    Text(
                        text = statusLabel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStatusSelected(statusValue) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        fontSize = 16.sp,
                        color = TextPrimaryColor,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar")
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun OrdersScreenContentPreview() {
    FieldOpsTheme {
        OrdersScreen(
            uiState = OrdersUiState(
                screenState = OrdersScreenState.Content(
                    orders = listOf(
                        WorkOrder(
                            id = 1,
                            code = "OT-0001",
                            title = "Reemplazo de tablero eléctrico",
                            description = null,
                            customerName = "Clínica San Felipe",
                            address = "Av. Gregorio Escobedo 650, Jesús María",
                            priority = WorkOrder.PRIORITY_HIGH,
                            status = WorkOrder.STATUS_PENDING,
                            technicianId = 1,
                            scheduledAt = LocalDateTime.of(2025, 1, 10, 9, 0),
                        ),
                        WorkOrder(
                            id = 4,
                            code = "OT-0004",
                            title = "Mantenimiento preventivo de UPS",
                            description = null,
                            customerName = "Notaría Paredes",
                            address = "Calle Las Begonias 441, San Isidro",
                            priority = "MEDIUM",
                            status = WorkOrder.STATUS_IN_PROGRESS,
                            technicianId = 1,
                            scheduledAt = LocalDateTime.of(2027, 11, 14, 10, 0),
                        ),
                    ),
                ),
                pendingCount = 1,
            ),
            fullName = "Juan Pérez Salas",
            onFilterSelected = {},
            onOrderStatusChange = { _, _ -> },
            onRetry = {},
        )
    }
}

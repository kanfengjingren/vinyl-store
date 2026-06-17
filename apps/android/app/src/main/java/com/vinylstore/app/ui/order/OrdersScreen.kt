package com.vinylstore.app.ui.order

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vinylstore.app.VinylApp
import com.vinylstore.app.data.model.Order
import com.vinylstore.app.ui.components.EmptyState
import com.vinylstore.app.ui.components.ErrorState
import com.vinylstore.app.ui.components.LoadingState

@Composable
fun OrdersScreen(
    onOrderClick: (Int) -> Unit,
    onAlbumClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val app = VinylApp.instance
    val viewModel: OrdersViewModel = viewModel(
        factory = OrdersViewModel.Factory(app.orderRepository)
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadOrders() }

    // Action confirm dialog
    var confirmAction by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    confirmAction?.let { (msg, action) ->
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("确认") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = {
                    action()
                    confirmAction = null
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) { Text("取消") }
            },
            shape = MaterialTheme.shapes.extraSmall
        )
    }

    // Snackbar for errors
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.actionError) {
        uiState.actionError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── 标题 ──
            Text(
                "我的订单",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            // ── Tab 栏 ──
            TabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
            ) {
                OrderTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { viewModel.setTab(tab) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tab.label)
                                Spacer(Modifier.width(4.dp))
                                val count = uiState.countForTab(tab)
                                Text(
                                    "· $count",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }

            // ── 内容 ──
            when {
                uiState.isLoading -> LoadingState(Modifier.weight(1f))
                uiState.error != null ->
                    ErrorState(uiState.error!!, onRetry = { viewModel.loadOrders() }, Modifier.weight(1f))
                uiState.filteredOrders.isEmpty() -> EmptyState("暂无订单", Modifier.weight(1f))
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.filteredOrders, key = { it.id }) { order ->
                            OrderCard(
                                order = order,
                                onClick = { onOrderClick(order.id) },
                                onAlbumClick = onAlbumClick,
                                onCancel = {
                                    confirmAction = Pair("确定要取消此订单吗？") {
                                        viewModel.cancelOrder(order.id)
                                    }
                                },
                                onPay = {
                                    confirmAction = Pair("确认从余额支付 ¥${order.totalAmount}？") {
                                        viewModel.payOrder(order.id)
                                    }
                                },
                                onBuyAgain = {
                                    order.items?.firstOrNull()?.album?.slug?.let { onAlbumClick(it) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// 订单卡片
// ═══════════════════════════════════════════

@Composable
private fun OrderCard(
    order: Order,
    onClick: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onCancel: () -> Unit,
    onPay: () -> Unit,
    onBuyAgain: () -> Unit
) {
    val statusInfo = orderStatusInfo(order.status)

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraSmall,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 头部：订单号 + 状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "订单 #${order.id}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    statusInfo.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusInfo.color,
                    modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // 商品列表（最多显示 3 项）
            order.items?.take(3)?.forEach { item ->
                val slug = item.album?.slug
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = slug != null) { slug?.let { onAlbumClick(it) } }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        item.album?.title ?: "已下架专辑",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (slug != null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "×${item.quantity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if ((order.items?.size ?: 0) > 3) {
                Text(
                    "... 等 ${order.items!!.size} 件",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            // 底部：金额 + 操作
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "共 ${order.items?.sumOf { it.quantity } ?: 0} 件  ¥${order.totalAmount}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (order.status) {
                        "PENDING" -> {
                            TextButton(
                                onClick = onCancel,
                                shape = MaterialTheme.shapes.extraSmall
                            ) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            TextButton(
                                onClick = onPay,
                                shape = MaterialTheme.shapes.extraSmall
                            ) { Text("付款", color = MaterialTheme.colorScheme.primary) }
                        }
                        "PAID", "SHIPPED" -> {
                            TextButton(
                                onClick = onClick,
                                shape = MaterialTheme.shapes.extraSmall
                            ) { Text("查看详情", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        else -> {
                            TextButton(
                                onClick = onBuyAgain,
                                shape = MaterialTheme.shapes.extraSmall
                            ) { Text("再次购买", color = MaterialTheme.colorScheme.primary) }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// 状态映射
// ═══════════════════════════════════════════

data class StatusInfo(val label: String, val color: androidx.compose.ui.graphics.Color)

fun orderStatusInfo(status: String): StatusInfo = when (status) {
    "PENDING" -> StatusInfo("待付款", androidx.compose.ui.graphics.Color(0xFFC49333))
    "PAID" -> StatusInfo("待收货", androidx.compose.ui.graphics.Color(0xFF2563EB))
    "SHIPPED" -> StatusInfo("待收货", androidx.compose.ui.graphics.Color(0xFF2563EB))
    "DELIVERED" -> StatusInfo("已完成", androidx.compose.ui.graphics.Color(0xFF16A34A))
    "CANCELLED" -> StatusInfo("已取消", androidx.compose.ui.graphics.Color(0xFF9CA3AF))
    else -> StatusInfo(status, androidx.compose.ui.graphics.Color.Gray)
}

package com.vinylstore.app.ui.order

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.vinylstore.app.VinylApp
import com.vinylstore.app.data.model.Order
import com.vinylstore.app.data.model.OrderItem
import com.vinylstore.app.data.model.resolveCoverUrl
import com.vinylstore.app.ui.components.ErrorState
import com.vinylstore.app.ui.components.LoadingState

@Composable
fun OrderDetailScreen(
    orderId: Int,
    onBack: () -> Unit,
    onAlbumClick: (String) -> Unit
) {
    val app = VinylApp.instance
    val viewModel: OrderDetailViewModel = viewModel(
        factory = OrderDetailViewModel.Factory(app.orderRepository)
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(orderId) { viewModel.loadOrder(orderId) }

    // Confirm dialog
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

    // Snackbar
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
            // ── 返回链接 ──
            TextButton(
                onClick = onBack,
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp)
            ) {
                Text(
                    "← 返回订单列表",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when {
                uiState.isLoading -> LoadingState(Modifier.weight(1f))
                uiState.error != null ->
                    ErrorState(uiState.error!!, onRetry = { viewModel.loadOrder(orderId) }, Modifier.weight(1f))
                uiState.order == null -> {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("订单不存在", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> OrderDetailContent(
                    uiState = uiState,
                    order = uiState.order!!,
                    onCancel = {
                        confirmAction = Pair("确定要取消此订单吗？") { viewModel.cancelOrder() }
                    },
                    onPay = {
                        confirmAction = Pair("确认从余额支付 ¥${uiState.order!!.totalAmount}？") {
                            viewModel.payOrder()
                        }
                    },
                    onAlbumClick = onAlbumClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════
// 订单详情内容
// ═══════════════════════════════════════════

@Composable
private fun OrderDetailContent(
    uiState: OrderDetailUiState,
    order: Order,
    onCancel: () -> Unit,
    onPay: () -> Unit,
    onAlbumClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val statusInfo = orderStatusInfo(order.status)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        // ── 头部 ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "订单 #${order.id}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                statusInfo.label,
                style = MaterialTheme.typography.labelSmall,
                color = statusInfo.color,
                modifier = Modifier
                    .background(statusInfo.color.copy(alpha = 0.1f), MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        if (!order.shippingAddress.isNullOrBlank()) {
            Text(
                order.shippingAddress,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row {
            Text(
                formatDateTime(order.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (uiState.countdown.isNotEmpty()) {
                Spacer(Modifier.width(12.dp))
                Text(
                    uiState.countdown,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(12.dp))

        // ── 商品列表 ──
        order.items?.forEach { item ->
            OrderItemRow(item = item, onAlbumClick = onAlbumClick)
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(12.dp))

        // ── 合计 ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                "共 ${order.items?.size ?: 0} 件  总价",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "¥${order.totalAmount}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // ── 操作按钮 ──
        if (order.status == "PENDING") {
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = MaterialTheme.shapes.extraSmall
                ) { Text("取消订单") }
                Button(
                    onClick = onPay,
                    shape = MaterialTheme.shapes.extraSmall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) { Text("继续付款") }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ═══════════════════════════════════════════
// 订单条目行
// ═══════════════════════════════════════════

@Composable
private fun OrderItemRow(
    item: OrderItem,
    onAlbumClick: (String) -> Unit
) {
    val album = item.album
    val isRefunded = item.status == "REFUNDED"
    val isDelisted = album?.status == "DELISTED"
    val isDimmed = isRefunded || isDelisted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 封面
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (album != null && !isRefunded) {
                AsyncImage(
                    model = resolveCoverUrl(album.coverUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = !isDelisted) { onAlbumClick(album.slug) }
                )
            }
            // 退款/下架遮罩
            if (isRefunded || isDelisted) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isRefunded) "已退款" else "已下架",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        // 信息
        Column(modifier = Modifier.weight(1f)) {
            if (isRefunded) {
                Text(
                    "已退款",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF16A34A),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "已退款 ¥${item.unitPrice * item.quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF16A34A)
                )
            } else if (isDelisted) {
                Text(
                    "专辑已下架",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            } else {
                Text(
                    album?.title ?: "专辑已下架",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!album?.artist.isNullOrBlank()) {
                    Text(
                        album.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            "×${item.quantity}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "¥${item.unitPrice}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (isDimmed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.onSurface
        )
    }
}

// ═══════════════════════════════════════════
// 工具
// ═══════════════════════════════════════════

private fun formatDateTime(isoStr: String): String {
    return try {
        val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
        parser.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = parser.parse(isoStr) ?: return isoStr
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CHINA)
        formatter.format(date)
    } catch (_: Exception) { isoStr }
}

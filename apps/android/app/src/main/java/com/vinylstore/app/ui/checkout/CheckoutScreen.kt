package com.vinylstore.app.ui.checkout

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.vinylstore.app.VinylApp
import com.vinylstore.app.data.model.resolveCoverUrl
import com.vinylstore.app.ui.components.EmptyState
import com.vinylstore.app.ui.components.ErrorState
import com.vinylstore.app.ui.components.LoadingState

@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onSuccess: (goToOrders: Boolean) -> Unit,
    onAlbumClick: (String) -> Unit
) {
    val app = VinylApp.instance
    val viewModel: CheckoutViewModel = viewModel(
        factory = CheckoutViewModel.Factory(
            app.cartRepository,
            app.orderRepository,
            app.authRepository
        )
    )
    val uiState by viewModel.uiState.collectAsState()

    when {
        // ── 下单成功 ──
        uiState.submitted -> CheckoutSuccess(
            onViewOrders = { onSuccess(true) },
            onContinueShopping = { onSuccess(false) }
        )

        // ── 加载态 ──
        uiState.isLoading -> LoadingState()

        // ── 购物车为空 ──
        !uiState.isLoading && uiState.items.isEmpty() -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "购物车为空",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onBack, shape = MaterialTheme.shapes.extraSmall) {
                    Text("返回", color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // ── 正常结账页 ──
        else -> CheckoutContent(
            uiState = uiState,
            onAddressChange = { viewModel.updateAddress(it) },
            onConfirm = { viewModel.confirmCheckout() },
            onBack = onBack,
            onAlbumClick = onAlbumClick
        )
    }
}

// ═══════════════════════════════════════════
// 下单成功页
// ═══════════════════════════════════════════

@Composable
private fun CheckoutSuccess(
    onViewOrders: () -> Unit,
    onContinueShopping: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 绿色勾
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    MaterialTheme.colorScheme.error.copy(alpha = 0f),
                    MaterialTheme.shapes.extraSmall
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "✓",
                style = MaterialTheme.typography.headlineLarge,
                color = androidx.compose.ui.graphics.Color(0xFF22C55E)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "下单成功！",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "订单已创建，我们会在发货时通知你。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onViewOrders,
                shape = MaterialTheme.shapes.extraSmall,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("查看订单")
            }
            OutlinedButton(
                onClick = onContinueShopping,
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text("继续购物")
            }
        }
    }
}

// ═══════════════════════════════════════════
// 结账内容（分栏布局）
// ═══════════════════════════════════════════

@Composable
private fun CheckoutContent(
    uiState: CheckoutUiState,
    onAddressChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    onAlbumClick: (String) -> Unit
) {
    val balanceInsufficient = uiState.balance < uiState.total
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
        // ═══════════════════════════════════════
        // 收货地址
        // ═══════════════════════════════════════

        // ── 地址为空时的醒目提醒 ──
        if (uiState.shippingAddress.isBlank()) {
            Card(
                shape = MaterialTheme.shapes.extraSmall,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        "⚠️",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "尚未填写收货地址",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "请填写收货地址，否则无法确认下单",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Text(
            "收货地址",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.shippingAddress,
            onValueChange = onAddressChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("请输入收货地址") },
            shape = MaterialTheme.shapes.extraSmall,
            minLines = 2,
            isError = uiState.shippingAddress.isBlank()
        )

        if (uiState.shippingAddress.isBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                "请填写收货地址，否则无法下单",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(6.dp))
        Text(
            "修改仅对本次订单生效，不会更新默认地址",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        // ═══════════════════════════════════════
        // 分隔
        // ═══════════════════════════════════════

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(16.dp))

        // ═══════════════════════════════════════
        // 订单内容
        // ═══════════════════════════════════════

        Text(
            "订单内容",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))

        // 订单条目
        uiState.items.forEach { item ->
            val album = item.album ?: return@forEach
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = resolveCoverUrl(album.coverUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onAlbumClick(album.slug) }
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        album.artist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        album.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "×${item.quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "¥${album.price * item.quantity}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // ═══════════════════════════════════════
        // 合计 + 确认
        // ═══════════════════════════════════════

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(12.dp))

        SummaryRow("小计", "¥${uiState.total}")
        Spacer(Modifier.height(4.dp))
        SummaryRow(
            "账户余额",
            "¥${uiState.balance}",
            valueColor = if (balanceInsufficient)
                MaterialTheme.colorScheme.error
            else
                androidx.compose.ui.graphics.Color(0xFF22C55E)
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        SummaryRow(
            "应付",
            "¥${uiState.total}",
            valueStyle = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        if (balanceInsufficient) {
            Spacer(Modifier.height(4.dp))
            Text(
                "余额不足，请先充值",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (uiState.error != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                uiState.error!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onConfirm,
            enabled = !uiState.isSubmitting
                    && uiState.shippingAddress.isNotBlank()
                    && !balanceInsufficient,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = MaterialTheme.shapes.extraSmall,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Text(
                if (uiState.isSubmitting) "提交中..." else "确认下单",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = onBack,
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("取消并返回", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    valueStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = valueStyle,
            fontWeight = fontWeight,
            color = valueColor
        )
    }
}

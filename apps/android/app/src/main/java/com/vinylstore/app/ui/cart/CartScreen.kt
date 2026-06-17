package com.vinylstore.app.ui.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
fun CartScreen(
    onAlbumClick: (String) -> Unit,
    onCheckoutClick: () -> Unit,
    onGoShopping: () -> Unit
) {
    val app = VinylApp.instance
    val viewModel: CartViewModel = viewModel(
        factory = CartViewModel.Factory(app.cartRepository)
    )
    val uiState by viewModel.uiState.collectAsState()

    // 每次进入此页面都刷新购物车
    LaunchedEffect(Unit) { viewModel.loadCart() }

    when {
        uiState.isLoading && uiState.items.isEmpty() -> LoadingState()
        uiState.error != null && uiState.items.isEmpty() ->
            ErrorState(uiState.error!!, onRetry = { viewModel.loadCart() })
        uiState.items.isEmpty() -> EmptyCart(onGoShopping)
        else -> {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── 购物车列表 ──
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.items, key = { it.id }) { item ->
                        CartItemRow(
                            item = item,
                            onAlbumClick = { item.album?.slug?.let { onAlbumClick(it) } },
                            onIncrease = { viewModel.updateQuantity(item.id, item.quantity + 1) },
                            onDecrease = { viewModel.updateQuantity(item.id, item.quantity - 1) },
                            onRemove = { viewModel.removeItem(item.id) }
                        )
                    }
                }

                // ── 错误提示 ──
                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                // ── 底部结算栏 ──
                BottomCheckoutBar(
                    itemCount = uiState.items.sumOf { it.quantity },
                    total = uiState.total,
                    onCheckoutClick = onCheckoutClick
                )
            }
        }
    }
}

// ═══════════════════════════════════════════
// 空购物车
// ═══════════════════════════════════════════

@Composable
private fun EmptyCart(onGoShopping: () -> Unit) {
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
        TextButton(
            onClick = onGoShopping,
            shape = MaterialTheme.shapes.extraSmall
        ) {
            Text(
                "去选购 →",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ═══════════════════════════════════════════
// 购物车条目行
// ═══════════════════════════════════════════

@Composable
private fun CartItemRow(
    item: com.vinylstore.app.data.model.CartItem,
    onAlbumClick: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    val album = item.album ?: return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.shapes.extraSmall
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 封面
        AsyncImage(
            model = resolveCoverUrl(album.coverUrl),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(80.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onAlbumClick() }
        )

        Spacer(Modifier.width(12.dp))

        // 标题 + 艺人 + 单价
        Column(modifier = Modifier.weight(1f)) {
            Text(
                album.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                album.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "¥${album.price}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // 数量控制
        Row(verticalAlignment = Alignment.CenterVertically) {
            QtyButton("-", onDecrease)
            Text(
                "${item.quantity}",
                modifier = Modifier.widthIn(min = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            QtyButton("+", onIncrease)
        }

        Spacer(Modifier.width(12.dp))

        // 小计
        Text(
            "¥${album.price * item.quantity}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.widthIn(min = 72.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )

        Spacer(Modifier.width(8.dp))

        // 删除
        TextButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp),
            contentPadding = PaddingValues(0.dp),
            shape = MaterialTheme.shapes.extraSmall
        ) {
            Text(
                "×",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QtyButton(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.size(28.dp),
        contentPadding = PaddingValues(0.dp),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════
// 底部结算栏
// ═══════════════════════════════════════════

@Composable
private fun BottomCheckoutBar(
    itemCount: Int,
    total: Int,
    onCheckoutClick: () -> Unit
) {
    Surface(
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "共 $itemCount 件",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "¥$total",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Button(
                onClick = onCheckoutClick,
                shape = MaterialTheme.shapes.extraSmall,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    "去结算",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

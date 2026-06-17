package com.vinylstore.app.ui.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vinylstore.app.VinylApp
import com.vinylstore.app.ui.components.ErrorState
import com.vinylstore.app.ui.components.LoadingState
import com.vinylstore.app.ui.home.AlbumCardSmall

@Composable
fun CatalogScreen(
    onAlbumClick: (String) -> Unit,
    initialCategory: String? = null
) {
    val app = VinylApp.instance
    val viewModel: CatalogViewModel = viewModel(
        factory = CatalogViewModel.Factory(app.albumRepository)
    )
    val uiState by viewModel.uiState.collectAsState()

    // 初始分类筛选
    LaunchedEffect(initialCategory) {
        if (initialCategory != null && uiState.selectedCategory != initialCategory) {
            viewModel.setCategory(initialCategory)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── 筛选栏 ──
        FilterBar(uiState, viewModel)

        // ── 专辑网格 ──
        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.isLoading && uiState.albums.isEmpty() -> LoadingState()
                uiState.error != null && uiState.albums.isEmpty() ->
                    ErrorState(uiState.error!!, onRetry = { viewModel.load() })
                uiState.albums.isEmpty() ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暂无专辑", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(uiState.albums, key = { it.id }) { album ->
                            AlbumCardSmall(
                                album = album,
                                onClick = { onAlbumClick(album.slug) }
                            )
                        }
                        if (uiState.totalPages > 1) {
                            item { Spacer(Modifier.height(8.dp)) }
                        }
                    }
                }
            }
        }

        // ── 固定在底部的分页 ──
        if (uiState.totalPages > 1 && uiState.albums.isNotEmpty()) {
            PaginationRow(
                currentPage = uiState.currentPage,
                totalPages = uiState.totalPages,
                onPageClick = { viewModel.setPage(it) }
            )
        }
    }
}

// ═══════════════════════════════════════════
// 顶部筛选栏
// ═══════════════════════════════════════════

@Composable
private fun FilterBar(state: CatalogUiState, viewModel: CatalogViewModel) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        // 分类 Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.selectedCategory == null,
                onClick = { viewModel.setCategory(null) },
                label = { Text("全部") },
                shape = MaterialTheme.shapes.extraSmall,
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = state.selectedCategory == null,
                    borderColor = MaterialTheme.colorScheme.primary,
                    selectedBorderColor = MaterialTheme.colorScheme.primary
                ),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
            state.categories.forEach { cat ->
                FilterChip(
                    selected = state.selectedCategory == cat.slug,
                    onClick = { viewModel.setCategory(cat.slug) },
                    label = { Text(cat.name) },
                    shape = MaterialTheme.shapes.extraSmall,
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = state.selectedCategory == cat.slug,
                        borderColor = MaterialTheme.colorScheme.primary,
                        selectedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        // 颜色色块
        if (state.colors.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "颜色",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp)
                )
                // "全部" 清除按钮
                Box(
                    modifier = Modifier
                        .size(24.dp, 36.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(Color.Transparent)
                        .border(
                            2.dp,
                            if (state.selectedColor == null) Color.Black
                            else MaterialTheme.colorScheme.outlineVariant,
                            MaterialTheme.shapes.extraSmall
                        )
                        .clickable { viewModel.setColor(null) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "✕",
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        color = if (state.selectedColor == null) Color.Black
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                state.colors.forEach { colorOption ->
                    val hex = try {
                        Color(android.graphics.Color.parseColor(colorOption.hex))
                    } catch (_: Exception) { Color.Gray }
                    Box(
                        modifier = Modifier
                            .size(24.dp, 36.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(hex)
                            .border(
                                2.dp,
                                if (state.selectedColor == colorOption.label)
                                    Color.Black
                                else
                                    Color.Transparent,
                                MaterialTheme.shapes.extraSmall
                            )
                            .clickable { viewModel.setColor(colorOption.label) }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 排序按钮
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SortButton("最新", "createdAt", "desc", state, viewModel)
            SortButton("年份", "year", "desc", state, viewModel)
            SortButton("价格↑", "price", "asc", state, viewModel)
            SortButton("价格↓", "price", "desc", state, viewModel)
        }
    }
}

@Composable
private fun SortButton(
    label: String,
    sortKey: String,
    order: String,
    state: CatalogUiState,
    viewModel: CatalogViewModel
) {
    val isActive = state.sort == sortKey && state.order == order
    TextButton(
        onClick = { viewModel.setSort(sortKey, order) },
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            label,
            color = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

// ═══════════════════════════════════════════
// 分页
// ═══════════════════════════════════════════

@Composable
fun PaginationRow(currentPage: Int, totalPages: Int, onPageClick: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = { if (currentPage > 1) onPageClick(currentPage - 1) },
            enabled = currentPage > 1
        ) { Text("上一页") }

        Text(
            "$currentPage / $totalPages",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        TextButton(
            onClick = { if (currentPage < totalPages) onPageClick(currentPage + 1) },
            enabled = currentPage < totalPages
        ) { Text("下一页") }
    }
}

package com.vinylstore.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.vinylstore.app.VinylApp
import com.vinylstore.app.data.model.Album
import com.vinylstore.app.data.model.HotAlbum
import com.vinylstore.app.data.model.resolveCoverUrl
import com.vinylstore.app.ui.components.ErrorState
import com.vinylstore.app.ui.components.LoadingState

@Composable
fun HomeScreen(
    onAlbumClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = VinylApp.instance
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(app.albumRepository)
    )
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.featured == null -> LoadingState()
            uiState.error != null && uiState.featured == null ->
                ErrorState(uiState.error!!, onRetry = viewModel::loadHomeData)
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // ── 搜索栏 ──
                    item {
                        SearchBar(onClick = onSearchClick)
                    }

                    // ── 今日推荐 ──
                    uiState.featured?.let { album ->
                        item {
                            FeaturedSection(album = album, onClick = { onAlbumClick(album.slug) })
                        }
                    }

                    // ── 本月热销 ──
                    if (uiState.hotAlbums.isNotEmpty()) {
                        item {
                            SectionHeader("本月热销")
                        }
                        item {
                            HotAlbumsRow(albums = uiState.hotAlbums, onAlbumClick = onAlbumClick)
                        }
                    }

                    // ── 猜你喜欢 ──
                    if (uiState.recommendations.isNotEmpty()) {
                        item {
                            SectionHeader("猜你喜欢")
                        }
                        item {
                            AlbumRow(albums = uiState.recommendations, onAlbumClick = onAlbumClick)
                        }
                    }

                    // ── 按分类 ──
                    uiState.categories.forEach { category ->
                        val albums = uiState.categoryAlbums[category.slug] ?: emptyList()
                        if (albums.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = category.name,
                                    showMore = true,
                                    onMoreClick = { onCategoryClick(category.slug) }
                                )
                            }
                            item {
                                AlbumRow(albums = albums, onAlbumClick = onAlbumClick)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// 搜索栏
// ═══════════════════════════════════════════

@Composable
private fun SearchBar(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text("搜索专辑、艺人...", color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ═══════════════════════════════════════════
// 今日推荐 Hero 卡片
// ═══════════════════════════════════════════

@Composable
private fun FeaturedSection(album: Album, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(0.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = resolveCoverUrl(album.coverUrl),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // 渐变叠加
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                        startY = 100f
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Text("今日推荐", color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Text(album.title, color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(album.artist, color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text("¥${album.price}", color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
        }
    }
}

// ═══════════════════════════════════════════
// 通用 Section 标题
// ═══════════════════════════════════════════

@Composable
private fun SectionHeader(
    title: String,
    showMore: Boolean = false,
    onMoreClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground)
        if (showMore) {
            TextButton(onClick = onMoreClick) {
                Text("查看全部", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ═══════════════════════════════════════════
// 热销排行行
// ═══════════════════════════════════════════

@Composable
private fun HotAlbumsRow(albums: List<HotAlbum>, onAlbumClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        albums.forEachIndexed { index, album ->
            HotAlbumCard(rank = index + 1, album = album, onClick = { onAlbumClick(album.slug) })
        }
    }
}

@Composable
private fun HotAlbumCard(rank: Int, album: HotAlbum, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick() }
    ) {
        Box {
            AsyncImage(
                model = resolveCoverUrl(album.coverUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(130.dp)
                    .height(130.dp)
                    .clip(RoundedCornerShape(0.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            // 排名角标
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(22.dp)
                    .background(
                        when (rank) {
                            1 -> Color(0xFFFFB300)
                            2 -> Color(0xFF9E9E9E)
                            3 -> Color(0xFFCD7F32)
                            else -> Color.Black.copy(alpha = 0.6f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("$rank", color = Color.White, fontSize = 12.sp,
                    fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(album.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground)
        Text(album.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("¥${album.price}", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground)
    }
}

// ═══════════════════════════════════════════
// 专辑水平滚动行
// ═══════════════════════════════════════════

@Composable
private fun AlbumRow(albums: List<Album>, onAlbumClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        albums.forEach { album ->
            AlbumCardSmall(album = album, onClick = { onAlbumClick(album.slug) })
        }
    }
}

@Composable
fun AlbumCardSmall(album: Album, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = resolveCoverUrl(album.coverUrl),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(140.dp)
                .height(140.dp)
                .clip(RoundedCornerShape(0.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(Modifier.height(6.dp))
        Text(album.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground)
        Text(album.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("¥${album.price}", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground)
    }
}

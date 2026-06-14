package com.vinylstore.app.ui.native.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.vinylstore.app.data.model.Album
import com.vinylstore.app.data.model.Category
import com.vinylstore.app.data.model.resolveCoverUrl
import com.vinylstore.app.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onAlbumClick: (String) -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading && uiState.featured == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Gold)
        }
    } else if (uiState.error != null && uiState.featured == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Text("加载失败", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                Text(
                    uiState.error ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { viewModel.loadHomeData() }) {
                    Text("重试", color = Gold)
                }
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ── Hero / Featured ───────────────────────
            uiState.featured?.let { album ->
                FeaturedSection(
                    album = album,
                    onClick = { onAlbumClick(album.slug) }
                )
            }

            // ── 分类快捷入口 ─────────────────────────
            if (uiState.categories.isNotEmpty()) {
                CategoryChips(
                    categories = uiState.categories,
                    onCategoryClick = onCategoryClick
                )
            }

            // ── 最新上架 ─────────────────────────────
            if (uiState.latestAlbums.isNotEmpty()) {
                AlbumSection(
                    title = "最新上架",
                    albums = uiState.latestAlbums,
                    onAlbumClick = onAlbumClick
                )
            }

            // ── 分类专辑行 ───────────────────────────
            for ((slug, albums) in uiState.categoryAlbums) {
                if (albums.isNotEmpty()) {
                    val catName = uiState.categories.find { it.slug == slug }?.name ?: slug
                    AlbumSection(
                        title = catName,
                        albums = albums,
                        onAlbumClick = onAlbumClick,
                        onViewAll = { onCategoryClick(slug) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FeaturedSection(
    album: Album,
    onClick: () -> Unit
) {
    val gradient = album.gradient?.let { parseGradient(it) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clickable(onClick = onClick)
    ) {
        // 封面图
        if (!album.coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = resolveCoverUrl(album.coverUrl),
                contentDescription = album.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 渐变遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.1f),
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // 文字
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            Text(
                text = "今日推荐",
                style = MaterialTheme.typography.labelMedium,
                color = Gold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = album.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = TextOnDarkSecondary
            )
            Text(
                text = album.title,
                style = MaterialTheme.typography.headlineMedium,
                color = TextOnDark,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "¥${album.price}",
                style = MaterialTheme.typography.titleLarge,
                color = Gold
            )
        }
    }
}

@Composable
private fun CategoryChips(
    categories: List<Category>,
    onCategoryClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "分类浏览",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                SuggestionChip(
                    onClick = { onCategoryClick(cat.slug) },
                    label = { Text(cat.name) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = TextSecondary
                    ),
                    border = null
                )
            }
        }
    }
}

@Composable
private fun AlbumSection(
    title: String,
    albums: List<Album>,
    onAlbumClick: (String) -> Unit,
    onViewAll: (() -> Unit)? = null
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            if (onViewAll != null) {
                TextButton(onClick = onViewAll) {
                    Text(
                        "查看全部 →",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(albums) { album ->
                AlbumCardSmall(album = album, onClick = { onAlbumClick(album.slug) })
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AlbumCardSmall(
    album: Album,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(160.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (!album.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = resolveCoverUrl(album.coverUrl),
                    contentDescription = album.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            album.gradient?.let { parseGradient(it) }
                                ?.let { brush -> Modifier.background(brush) }
                                ?: Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = album.title.take(2).uppercase(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextOnDark.copy(alpha = 0.3f)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = album.title,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "¥${album.price}",
            style = MaterialTheme.typography.bodyMedium,
            color = Gold
        )
    }
}

/**
 * 解析 CSS 渐变字符串，如 "linear-gradient(135deg, #667eea 0%, #764ba2 100%)"
 * 仅支持简单的两个颜色值。
 */
private fun parseGradient(gradient: String): Brush? {
    try {
        val hexRegex = Regex("#[0-9a-fA-F]{6}")
        val colors = hexRegex.findAll(gradient).toList()
        if (colors.size >= 2) {
            val c1 = Color(android.graphics.Color.parseColor(colors[0].value))
            val c2 = Color(android.graphics.Color.parseColor(colors[1].value))
            return Brush.horizontalGradient(listOf(c1, c2))
        }
    } catch (_: Exception) {
    }
    return null
}

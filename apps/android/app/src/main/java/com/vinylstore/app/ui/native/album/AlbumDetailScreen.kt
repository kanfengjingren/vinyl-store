package com.vinylstore.app.ui.native.album

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.vinylstore.app.data.model.resolveCoverUrl
import com.vinylstore.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    slug: String,
    albumsRepository: com.vinylstore.app.data.repository.AlbumRepository? = null,
    onBack: () -> Unit = {},
    onAddToCart: (albumJson: String) -> Unit = {},
    onPlayTrack: (trackJson: String, artistName: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = albumsRepository ?: remember {
        com.vinylstore.app.data.repository.AlbumRepository(
            com.vinylstore.app.local.TokenStorage(context)
        )
    }
    val viewModel: AlbumDetailViewModel = viewModel(
        key = "album_detail_$slug",
        factory = AlbumDetailViewModel.Factory(repo, slug)
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.album?.title ?: "专辑详情",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("← 返回", color = Gold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Gold)
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "加载失败",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.loadAlbum() }) {
                            Text("重试", color = Gold)
                        }
                    }
                }
            }

            uiState.album != null -> {
                AlbumContent(
                    album = uiState.album!!,
                    onAddToCart = onAddToCart,
                    onPlayTrack = onPlayTrack,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun AlbumContent(
    album: com.vinylstore.app.data.model.Album,
    onAddToCart: (albumJson: String) -> Unit = {},
    onPlayTrack: (trackJson: String, artistName: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val gson = remember { com.google.gson.Gson() }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── 封面 ──────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
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
                        .background(
                            album.gradient?.let { parseGradient(it) }
                                ?: Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF1A1A2E),
                                        Color(0xFF16213E)
                                    )
                                )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = album.title.take(2).uppercase(),
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
        }

        // ── 信息 ──────────────────────────────────────
        Column(modifier = Modifier.padding(16.dp)) {
            // Badge
            album.badge?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = Gold,
                    modifier = Modifier
                        .background(Gold.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
                Spacer(Modifier.height(8.dp))
            }

            // 艺人
            Text(
                text = album.artist,
                style = MaterialTheme.typography.titleLarge,
                color = TextSecondary
            )
            Spacer(Modifier.height(2.dp))

            // 标题
            Text(
                text = album.title,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Spacer(Modifier.height(4.dp))

            // 元信息
            val metaParts = listOfNotNull(
                album.year?.toString(),
                album.country,
                album.label
            )
            if (metaParts.isNotEmpty()) {
                Text(
                    text = metaParts.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
                Spacer(Modifier.height(4.dp))
            }

            // 分类标签
            album.categories?.let { cats ->
                if (cats.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        cats.forEach { cat ->
                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        cat.name,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Gold.copy(alpha = 0.1f),
                                    labelColor = Gold
                                ),
                                border = null
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 描述
            Text(
                text = album.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(Modifier.height(16.dp))

            // 价格 + 库存
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "¥${album.price}",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary
                )
                Text(
                    text = "库存: ${album.stock} 张",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }

            Spacer(Modifier.height(12.dp))

            // 购买按钮
            val soldOut = album.stock <= 0
            val gson = remember { com.google.gson.Gson() }
            Button(
                onClick = {
                    val json = gson.toJson(album)
                    onAddToCart(json)
                },
                enabled = !soldOut,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = White,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = TextTertiary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp)
            ) {
                Text(
                    text = if (soldOut) "已售罄" else "加入购物车",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // ── 曲目列表 ──────────────────────────────
            album.tracks?.let { tracks ->
                if (tracks.isNotEmpty()) {
                    Spacer(Modifier.height(32.dp))
                    Text(
                        text = "曲目列表",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    tracks.forEach { track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 音轨按钮
                            if (track.audioUrl != null) {
                                Surface(
                                    onClick = { onPlayTrack(gson.toJson(track), album.artist) },
                                    modifier = Modifier.size(24.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Gold.copy(alpha = 0.15f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "▶",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Gold
                                        )
                                    }
                                }
                            } else {
                                Spacer(Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(8.dp))

                            // 序号
                            if (!track.isSection) {
                                Text(
                                    text = track.position.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextTertiary,
                                    modifier = Modifier.width(24.dp)
                                )
                            } else {
                                Spacer(Modifier.width(24.dp))
                            }

                            // 标题
                            Text(
                                text = track.title,
                                style = if (track.isSection)
                                    MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                else
                                    MaterialTheme.typography.bodyMedium,
                                color = if (track.isSection) TextPrimary else TextSecondary,
                                modifier = Modifier.weight(1f)
                            )

                            // 时长
                            track.duration?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextTertiary
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

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

package com.vinylstore.app.ui.album

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.vinylstore.app.VinylApp
import com.vinylstore.app.data.model.resolveCoverUrl
import com.vinylstore.app.ui.components.ErrorState
import com.vinylstore.app.ui.components.LoadingState
import com.vinylstore.app.ui.social.CommentSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    slug: String,
    highlightCommentId: Int? = null,
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit,
    onSellerClick: (Int) -> Unit,
    onAlbumClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onUserClick: (Int) -> Unit,
    onLoginRequired: () -> Unit
) {
    val app = VinylApp.instance
    val viewModel: AlbumDetailViewModel = viewModel(
        factory = AlbumDetailViewModel.Factory(app.albumRepository, app.cartRepository, app.commentRepository)
    )
    val uiState by viewModel.uiState.collectAsState()
    val isLoggedIn by app.authRepository.isLoggedInFlow.collectAsState(initial = false)
    val currentUserId by app.authRepository.currentUserIdFlow.collectAsState(initial = null)

    LaunchedEffect(slug) {
        viewModel.loadAlbum(slug)
    }

    LaunchedEffect(highlightCommentId) {
        highlightCommentId?.let { viewModel.setHighlightCommentId(it) }
    }

    // ── 购物车消息 Snackbar ──
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.cartMessage) {
        uiState.cartMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearCartMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFF1A1A1A))
            )
            uiState.error != null -> ErrorState(
                message = uiState.error!!,
                onRetry = { viewModel.loadAlbum(slug) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFF1A1A1A))
            )
            uiState.album != null -> AlbumContent(
                uiState = uiState,
                isLoggedIn = isLoggedIn,
                currentUserId = currentUserId,
                innerPadding = innerPadding,
                highlightCommentId = highlightCommentId,
                onBack = onBack,
                onToggleFav = { viewModel.toggleFavorite() },
                onRateAlbum = { score -> viewModel.rateAlbum(score) },
                onAddToCart = {
                    if (!isLoggedIn) onLoginRequired()
                    else viewModel.addToCart()
                },
                onHoverStar = { viewModel.setHoverStar(it) },
                onArtistClick = onArtistClick,
                onSellerClick = onSellerClick,
                onAlbumClick = onAlbumClick,
                onCategoryClick = onCategoryClick,
                onUserClick = onUserClick,
                onLoginRequired = onLoginRequired,
                onLoadPage = { viewModel.loadComments(it) },
                onLoadMore = { viewModel.loadMoreComments() },
                onSubmitComment = { content, parentId -> viewModel.submitComment(content, parentId) },
                onDeleteComment = { viewModel.deleteComment(it) },
                onLoadReplies = { viewModel.loadReplies(it) },
                onSetReplyTarget = { viewModel.setReplyTarget(it) }
            )
        }
    }
}

// ═══════════════════════════════════════════
// 专辑内容
// ═══════════════════════════════════════════

@Composable
private fun AlbumContent(
    uiState: AlbumDetailUiState,
    isLoggedIn: Boolean,
    currentUserId: Int?,
    innerPadding: PaddingValues,
    highlightCommentId: Int?,
    onBack: () -> Unit,
    onToggleFav: () -> Unit,
    onRateAlbum: (Int) -> Unit,
    onAddToCart: () -> Unit,
    onHoverStar: (Int) -> Unit,
    onArtistClick: (String) -> Unit,
    onSellerClick: (Int) -> Unit,
    onAlbumClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onUserClick: (Int) -> Unit,
    onLoginRequired: () -> Unit,
    onLoadPage: (Int) -> Unit,
    onLoadMore: () -> Unit,
    onSubmitComment: (String, Int?) -> Unit,
    onDeleteComment: (Int) -> Unit,
    onLoadReplies: (Int) -> Unit,
    onSetReplyTarget: (Int?) -> Unit
) {
    val album = uiState.album ?: return
    val scrollState = rememberScrollState()
    val coverUrl = resolveCoverUrl(album.coverUrl)

    // 高亮评论跳转：评论加载完成后滚动到评论区
    LaunchedEffect(highlightCommentId, uiState.commentsLoading) {
        if (highlightCommentId != null && highlightCommentId > 0 && !uiState.commentsLoading && uiState.comments.isNotEmpty()) {
            // 延迟一帧确保内容已渲染，滚动到底部（评论区）
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── 全屏背景 ──
        Box(modifier = Modifier.fillMaxSize()) {
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1A1A2E))
                )
            }
            // 暗色叠加层
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            )
        }

        // ── 内容 ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // 桌面端水平布局 / 手机端垂直
            val isWide = false // 简化：始终垂直布局
            if (isWide) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    AlbumCoverCard(album, coverUrl, Modifier.width(380.dp))
                    Spacer(Modifier.width(48.dp))
                    AlbumInfoColumn(
                        album = album,
                        uiState = uiState,
                        isLoggedIn = isLoggedIn,
                        onToggleFav = onToggleFav,
                        onRateAlbum = onRateAlbum,
                        onAddToCart = onAddToCart,
                        onHoverStar = onHoverStar,
                        onArtistClick = onArtistClick,
                        onSellerClick = onSellerClick,
                        onCategoryClick = onCategoryClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                AlbumCoverCard(
                    album, coverUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(32.dp))
                AlbumInfoColumn(
                    album = album,
                    uiState = uiState,
                    isLoggedIn = isLoggedIn,
                    onToggleFav = onToggleFav,
                    onRateAlbum = onRateAlbum,
                    onAddToCart = onAddToCart,
                    onHoverStar = onHoverStar,
                    onArtistClick = onArtistClick,
                    onSellerClick = onSellerClick,
                    onCategoryClick = onCategoryClick
                )
            }

            Spacer(Modifier.height(48.dp))

            // ── 曲目列表 ──
            TrackListSection(album)

            Spacer(Modifier.height(48.dp))

            // ── 返回链接 ──
            TextButton(onClick = onBack, shape = MaterialTheme.shapes.extraSmall) {
                Text(
                    "← 返回全部收藏",
                    color = Color(0xFFC49333),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(48.dp))

            // ── 评论区 ──
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(Modifier.height(24.dp))
            CommentSection(
                albumId = album.id,
                comments = uiState.comments,
                page = uiState.commentsPage,
                totalPages = uiState.commentsTotalPages,
                total = uiState.commentsTotal,
                isLoading = uiState.commentsLoading,
                isLoadingMore = uiState.commentsLoadingMore,
                isSubmitting = uiState.isSubmittingComment,
                replyTarget = uiState.replyTarget,
                expandedReplies = uiState.expandedReplies,
                highlightCommentId = highlightCommentId,
                isLoggedIn = isLoggedIn,
                currentUserId = currentUserId,
                onLoadPage = onLoadPage,
                onLoadMore = onLoadMore,
                onSubmit = onSubmitComment,
                onDelete = onDeleteComment,
                onLoadReplies = onLoadReplies,
                onSetReplyTarget = onSetReplyTarget,
                onUserClick = onUserClick,
                onLoginRequired = onLoginRequired
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ═══════════════════════════════════════════
// 封面卡片
// ═══════════════════════════════════════════

@Composable
private fun AlbumCoverCard(
    album: com.vinylstore.app.data.model.Album,
    coverUrl: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                ((album.title as String?) ?: "?").take(2).uppercase(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.2f)
            )
        }
    }
}

// ═══════════════════════════════════════════
// 专辑信息列
// ═══════════════════════════════════════════

@Composable
private fun AlbumInfoColumn(
    album: com.vinylstore.app.data.model.Album,
    uiState: AlbumDetailUiState,
    isLoggedIn: Boolean,
    onToggleFav: () -> Unit,
    onRateAlbum: (Int) -> Unit,
    onAddToCart: () -> Unit,
    onHoverStar: (Int) -> Unit,
    onArtistClick: (String) -> Unit,
    onSellerClick: (Int) -> Unit,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Badge
        if (!album.badge.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    album.badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // 艺人
        if (album.artistInfo?.slug != null) {
            TextButton(
                onClick = { onArtistClick(album.artistInfo.slug) },
                contentPadding = PaddingValues(0.dp),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    (album.artist as String?) ?: "未知艺人",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFC49333)
                )
            }
        } else {
            Text(
                (album.artist as String?) ?: "未知艺人",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        // 标题
        Text(
            (album.title as String?) ?: "无标题",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Spacer(Modifier.height(8.dp))

        // 年份/国家/厂牌
        val metaParts = listOfNotNull(
            album.year?.toString(),
            album.country,
            album.label
        )
        if (metaParts.isNotEmpty()) {
            Text(
                metaParts.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        // 卖家
        if (album.seller?.id != null) {
            Spacer(Modifier.height(4.dp))
            Row {
                Text("卖家: ", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.bodySmall)
                TextButton(
                    onClick = { onSellerClick(album.seller.id) },
                    contentPadding = PaddingValues(0.dp),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        album.seller.storeName ?: "未知",
                        color = Color(0xFFC49333),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // 分类标签
        val categories = album.categories ?: emptyList()
        if (categories.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.forEach { cat ->
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                            .clickable { onCategoryClick(cat.slug) }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            cat.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // 描述
        if (!album.description.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(
                album.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
            )
        }

        // 价格
        Spacer(Modifier.height(20.dp))
        Text(
            "¥${album.price}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        // 库存
        Spacer(Modifier.height(4.dp))
        Text(
            "库存: ${album.stock} 张",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.4f)
        )

        // 按钮组
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 加入购物车
            Button(
                onClick = onAddToCart,
                enabled = album.stock > 0 && !uiState.isAddingToCart,
                shape = MaterialTheme.shapes.extraSmall,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFC49333),
                    contentColor = Color.White,
                    disabledContainerColor = Color.White.copy(alpha = 0.1f),
                    disabledContentColor = Color.White.copy(alpha = 0.3f)
                ),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    when {
                        album.stock <= 0 -> "已售罄"
                        uiState.isAddingToCart -> "添加中..."
                        else -> "加入购物车"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.width(16.dp))

            // 收藏
            if (isLoggedIn) {
                TextButton(
                    onClick = onToggleFav,
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.size(48.dp)
                ) {
                    Text(
                        if (uiState.isFavorited) "♥" else "♡",
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                        color = if (uiState.isFavorited)
                            Color(0xFFF87171)
                        else
                            Color.White.copy(alpha = 0.4f)
                    )
                }
            }
        }

        // ⭐ 评分
        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (s in 1..5) {
                    val filled = if (isLoggedIn) {
                        s <= (uiState.hoverStar.ifZero(uiState.userRating))
                    } else {
                        s <= uiState.userRating
                    }
                    TextButton(
                        onClick = {
                            if (isLoggedIn && !uiState.isRatingSubmitting) onRateAlbum(s)
                        },
                        enabled = isLoggedIn && !uiState.isRatingSubmitting,
                        contentPadding = PaddingValues(2.dp),
                        modifier = Modifier.size(36.dp),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            "★",
                            fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                            color = if (filled) Color(0xFFC49333) else Color.White.copy(alpha = 0.2f)
                        )
                    }
                    // Hover detection — simplified: use pressed state
                }
            }

            Spacer(Modifier.width(12.dp))

            if (uiState.avgScore > 0) {
                Text(
                    "${uiState.avgScore}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFC49333)
                )
                Text(
                    " / 5",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
                if (uiState.ratingCount > 0) {
                    Text(
                        " (${uiState.ratingCount}人评分)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
            } else {
                Text(
                    "暂无评分",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }

        if (uiState.userRating > 0) {
            Spacer(Modifier.height(4.dp))
            Text(
                "我的评分: ${uiState.userRating} 星",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}

// ═══════════════════════════════════════════
// 曲目列表
// ═══════════════════════════════════════════

@Composable
private fun TrackListSection(album: com.vinylstore.app.data.model.Album) {
    val tracks = album.tracks ?: return
    val playerManager = com.vinylstore.app.VinylApp.instance.playerManager

    Text(
        "曲目列表",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = Color.White
    )
    Spacer(Modifier.height(12.dp))

    Column {
        tracks.forEach { track ->
            TrackRow(
                track = track,
                isCurrentTrack = playerManager.state.value.track?.id == track.id,
                onPlay = { playerManager.play(track, album, tracks) }
            )
        }
    }
}

@Composable
private fun TrackRow(
    track: com.vinylstore.app.data.model.Track,
    isCurrentTrack: Boolean,
    onPlay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 播放按钮
        if (!track.isSection) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrentTrack) Color(0xFFC49333)
                        else Color(0xFFC49333).copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isCurrentTrack) "⏸" else "▶",
                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    color = if (isCurrentTrack) Color.White else Color(0xFFC49333)
                )
            }
        } else {
            Spacer(Modifier.width(24.dp))
        }

        Spacer(Modifier.width(12.dp))

        // 位置编号
        if (!track.isSection && track.position > 0) {
            Text(
                "${track.position}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.width(24.dp)
            )
        }

        // 曲目名
        Text(
            track.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (track.isSection) FontWeight.SemiBold else FontWeight.Normal,
            color = if (track.isSection) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f)
        )

        // 时长
        if (!track.duration.isNullOrBlank()) {
            Text(
                track.duration,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}

// ═══════════════════════════════════════════
// 工具
// ═══════════════════════════════════════════

private fun Int.ifZero(fallback: Int): Int = if (this == 0) fallback else this

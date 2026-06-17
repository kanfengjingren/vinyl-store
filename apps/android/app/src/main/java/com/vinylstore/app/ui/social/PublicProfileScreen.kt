package com.vinylstore.app.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.vinylstore.app.VinylApp
import com.vinylstore.app.data.model.Album
import com.vinylstore.app.data.model.resolveCoverUrl
import com.vinylstore.app.ui.components.ErrorState
import com.vinylstore.app.ui.components.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    userId: Int,
    onBack: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onChatClick: (Int) -> Unit,
    onLoginRequired: () -> Unit
) {
    val app = VinylApp.instance
    val viewModel: PublicProfileViewModel = viewModel(
        factory = PublicProfileViewModel.Factory(app.userRepository, app.friendRepository)
    )
    val uiState by viewModel.uiState.collectAsState()
    val isLoggedIn by app.authRepository.isLoggedInFlow.collectAsState(initial = false)
    val currentUserId by app.authRepository.currentUserIdFlow.collectAsState(initial = null)
    val Gold = Color(0xFFC49333)

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.actionError) {
        uiState.actionError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.user?.name ?: "用户主页",
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingState(
                Modifier.fillMaxSize().padding(innerPadding)
            )
            uiState.error != null -> ErrorState(
                message = uiState.error!!,
                onRetry = { viewModel.loadProfile(userId) },
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            )
            uiState.user != null -> {
                val user = uiState.user!!
                val isSelf = currentUserId == user.id
                val isSeller = user.role == "SELLER"

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // ── 头部 ──
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 头像
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (user.avatar != null) {
                                AsyncImage(
                                    model = resolveCoverUrl(user.avatar),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(100.dp).clip(CircleShape)
                                )
                            } else {
                                Text(
                                    (user.name ?: "?").take(2),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // 名称
                        Text(
                            user.storeName ?: user.name ?: "用户",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        // 卖家简介
                        if (isSeller && user.storeName != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "卖家",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gold
                            )
                        }

                        // 加入时间
                        user.createdAt?.let { date ->
                            Spacer(Modifier.height(6.dp))
                            Text(
                                formatJoinDate(date) + " 加入",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                            )
                        }

                        // 好友按钮（非本人、非卖家、已登录）
                        if (!isSelf && !isSeller && isLoggedIn) {
                            Spacer(Modifier.height(16.dp))
                            FriendActionButton(
                                status = uiState.friendStatus,
                                isLoading = uiState.friendActionLoading,
                                onAdd = { viewModel.sendFriendRequest() },
                                onAccept = { viewModel.acceptFriendRequest() },
                                onReject = { viewModel.rejectFriendRequest() },
                                onChat = { onChatClick(user.id) }
                            )
                        }

                        // 联系卖家按钮
                        if (!isSelf && isSeller && isLoggedIn) {
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { onChatClick(user.id) },
                                shape = MaterialTheme.shapes.extraSmall,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Gold,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Chat, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("联系卖家")
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))

                    // ── 内容区域 ──
                    if (isSeller) {
                        // 卖家：在售专辑
                        SellerContent(
                            albums = uiState.sellerAlbums,
                            isLoading = uiState.sellerAlbumsLoading,
                            onAlbumClick = onAlbumClick
                        )
                    } else {
                        // 买家：已购 / 收藏 Tab
                        BuyerContent(
                            uiState = uiState,
                            onTabChanged = { viewModel.setActiveTab(it) },
                            onAlbumClick = onAlbumClick
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// 好友操作按钮
// ═══════════════════════════════════════════

@Composable
fun FriendActionButton(
    status: String,
    isLoading: Boolean,
    onAdd: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onChat: () -> Unit
) {
    val Gold = Color(0xFFC49333)

    when (status) {
        "none" -> {
            Button(
                onClick = onAdd,
                enabled = !isLoading,
                shape = MaterialTheme.shapes.extraSmall,
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.White)
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(16.dp), color = MaterialTheme.colorScheme.onBackground, strokeWidth = 2.dp)
                else { Icon(Icons.Default.PersonAdd, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("添加好友") }
            }
        }
        "pending_sent" -> {
            OutlinedButton(
                onClick = {},
                enabled = false,
                shape = MaterialTheme.shapes.extraSmall,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold)
            ) {
                Text("已发送申请")
            }
        }
        "pending_received" -> {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onAccept,
                    enabled = !isLoading,
                    shape = MaterialTheme.shapes.extraSmall,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.White)
                ) {
                    Text("接受")
                }
                OutlinedButton(
                    onClick = onReject,
                    enabled = !isLoading,
                    shape = MaterialTheme.shapes.extraSmall,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.5f))
                ) {
                    Text("拒绝")
                }
            }
        }
        "accepted" -> {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    shape = MaterialTheme.shapes.extraSmall,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold)
                ) {
                    Text("已是好友 ✓")
                }
                IconButton(onClick = onChat) {
                    Icon(Icons.Default.Chat, "聊天", tint = Gold)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// 卖家内容
// ═══════════════════════════════════════════

@Composable
private fun SellerContent(
    albums: List<Album>,
    isLoading: Boolean,
    onAlbumClick: (String) -> Unit
) {
    if (isLoading) {
        LoadingState(Modifier.fillMaxSize())
    } else if (albums.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无在售专辑", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                "在售专辑 · ${albums.size}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(albums) { album ->
                    AlbumGridItem(album = album, onClick = { onAlbumClick((album.slug as String?) ?: "") })
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// 买家内容（Tab）
// ═══════════════════════════════════════════

@Composable
private fun BuyerContent(
    uiState: PublicProfileUiState,
    onTabChanged: (PublicProfileTab) -> Unit,
    onAlbumClick: (String) -> Unit
) {
    val Gold = Color(0xFFC49333)

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = uiState.activeTab.ordinal,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = Gold,
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)) }
        ) {
            PublicProfileTab.entries.forEach { tab ->
                Tab(
                    selected = uiState.activeTab == tab,
                    onClick = { onTabChanged(tab) },
                    text = {
                        Text(
                            when (tab) {
                                PublicProfileTab.PURCHASES -> "已购专辑"
                                PublicProfileTab.FAVORITES -> "收藏专辑"
                            }
                        )
                    }
                )
            }
        }

        when (uiState.activeTab) {
            PublicProfileTab.PURCHASES -> {
                if (!uiState.purchasesVisible) {
                    PrivacyLock("该用户隐藏了已购专辑")
                } else if (uiState.purchasesLoading) {
                    LoadingState(Modifier.fillMaxSize())
                } else if (uiState.purchases.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暂无已购专辑", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                    }
                } else {
                    AlbumGrid(
                        albums = uiState.purchases,
                        onAlbumClick = onAlbumClick
                    )
                }
            }
            PublicProfileTab.FAVORITES -> {
                if (!uiState.favoritesVisible) {
                    PrivacyLock("该用户隐藏了收藏专辑")
                } else if (uiState.favoritesLoading) {
                    LoadingState(Modifier.fillMaxSize())
                } else if (uiState.favorites.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暂无收藏专辑", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                    }
                } else {
                    AlbumGrid(
                        albums = uiState.favorites,
                        onAlbumClick = onAlbumClick
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// 隐私锁定
// ═══════════════════════════════════════════

@Composable
fun PrivacyLock(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
            )
        }
    }
}

// ═══════════════════════════════════════════
// 专辑网格
// ═══════════════════════════════════════════

@Composable
private fun AlbumGrid(
    albums: List<Album>,
    onAlbumClick: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(albums) { album ->
            AlbumGridItem(album = album, onClick = { onAlbumClick((album.slug as String?) ?: "") })
        }
    }
}

@Composable
private fun AlbumGridItem(
    album: Album,
    onClick: () -> Unit
) {
    val coverUrl = resolveCoverUrl(album.coverUrl)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraSmall)
            .background(Color.White.copy(alpha = 0.05f))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
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
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                (album.artist as String?) ?: "未知艺人",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                (album.title as String?) ?: "?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "¥${album.price}",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFC49333)
            )
        }
    }
}

// ═══════════════════════════════════════════
// 时间格式化
// ═══════════════════════════════════════════

private fun formatJoinDate(isoTime: String): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = sdf.parse(isoTime.substringBefore(".")) ?: return ""
        val fmt = java.text.SimpleDateFormat("yyyy年M月", java.util.Locale.getDefault())
        fmt.format(date)
    } catch (_: Exception) {
        ""
    }
}

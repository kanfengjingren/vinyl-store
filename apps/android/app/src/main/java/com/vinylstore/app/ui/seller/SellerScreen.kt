package com.vinylstore.app.ui.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.vinylstore.app.VinylApp
import com.vinylstore.app.data.model.resolveCoverUrl
import com.vinylstore.app.ui.components.ErrorState
import com.vinylstore.app.ui.components.LoadingState
import com.vinylstore.app.ui.home.AlbumCardSmall

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerScreen(
    sellerId: Int,
    onBack: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onChatClick: (Int) -> Unit,
    onLoginRequired: () -> Unit
) {
    val app = VinylApp.instance
    val viewModel: SellerViewModel = viewModel(
        factory = SellerViewModel.Factory(app.sellerRepository)
    )
    val uiState by viewModel.uiState.collectAsState()
    val isLoggedIn by app.authRepository.isLoggedInFlow.collectAsState(initial = false)

    LaunchedEffect(sellerId) {
        viewModel.loadSeller(sellerId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.seller?.storeName ?: "卖家详情",
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
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingState(Modifier.padding(innerPadding))
            uiState.error != null ->
                ErrorState(uiState.error!!, onRetry = { viewModel.loadSeller(sellerId) }, Modifier.padding(innerPadding))
            uiState.seller == null ->
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text("卖家不存在或未通过审核", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            else -> {
                val seller = uiState.seller!!
                val albums = seller.albums.orEmpty()
                val albumCount = albums.size

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── 卖家信息头部（跨两列） ──
                    item(span = { GridItemSpan(2) }) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 店铺头像占位
                            Box(
                                modifier = Modifier
                                    .size(180.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        shape = MaterialTheme.shapes.small
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                val avatarUrl = seller.user?.avatar?.let { resolveCoverUrl(it) }
                                if (avatarUrl != null) {
                                    AsyncImage(
                                        model = avatarUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = seller.storeName.take(2).uppercase(),
                                        fontSize = 60.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    )
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            // 店铺名称
                            Text(
                                text = seller.storeName,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )

                            // 联系方式
                            if (seller.contactEmail != null || seller.user?.email != null || seller.contactPhone != null) {
                                Spacer(Modifier.height(6.dp))
                                val contactParts = buildList {
                                    seller.contactEmail?.let { add(it) }
                                    seller.user?.email?.let { add(it) }
                                    seller.contactPhone?.let { add(it) }
                                }
                                Text(
                                    text = contactParts.joinToString(" · "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // 描述
                            if (!seller.description.isNullOrBlank()) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = seller.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }

                            // 专辑数量
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "$albumCount 张专辑",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // 联系卖家按钮
                            Spacer(Modifier.height(16.dp))
                            if (isLoggedIn) {
                                Button(
                                    onClick = { onChatClick(seller.userId) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Icon(
                                        Icons.Default.Chat,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("联系卖家")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = onLoginRequired,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Icon(
                                        Icons.Default.Chat,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("登录后联系卖家")
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // 分割线
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(Modifier.height(4.dp))

                            // 专辑标题
                            if (albums.isNotEmpty()) {
                                Text(
                                    text = "在售专辑",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }

                    // ── 专辑网格 ──
                    if (albums.isEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无在售专辑",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(albums, key = { it.id }) { album ->
                            AlbumCardSmall(
                                album = album,
                                onClick = { onAlbumClick(album.slug) }
                            )
                        }
                    }
                }
            }
        }
    }
}

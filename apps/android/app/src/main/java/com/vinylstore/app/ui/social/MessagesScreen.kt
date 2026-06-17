package com.vinylstore.app.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.vinylstore.app.VinylApp
import com.vinylstore.app.data.model.Conversation
import com.vinylstore.app.data.model.resolveCoverUrl
import com.vinylstore.app.ui.components.ErrorState
import com.vinylstore.app.ui.components.LoadingState

@Composable
fun MessagesScreen(
    onChatClick: (Int) -> Unit,
    onUserClick: (Int) -> Unit,
    onFriendsClick: () -> Unit,
    onLoginRequired: () -> Unit
) {
    val app = VinylApp.instance
    val viewModel: MessagesViewModel = viewModel(
        factory = MessagesViewModel.Factory(
            app.chatRepository, app.friendRepository, app.userRepository, app.chatSocketManager
        )
    )
    val uiState by viewModel.uiState.collectAsState()
    val Gold = Color(0xFFC49333)

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.actionError) {
        uiState.actionError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionError()
        }
    }

    // Socket 生命周期
    LaunchedEffect(Unit) {
        viewModel.connectSocket()
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.disconnectSocket() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            // ── "我的好友" 入口 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFriendsClick() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Gold.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "我的好友",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    if (uiState.pendingRequests.isNotEmpty()) {
                        Text(
                            "${uiState.pendingRequests.size} 条待处理请求",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gold
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.2f),
                    modifier = Modifier.size(20.dp)
                )
            }

            HorizontalDivider(color = Color(0xFFEEEEEE))

            // ── 对话列表 ──
            when {
                uiState.conversationsLoading -> LoadingState(Modifier.weight(1f))
                uiState.conversations.isEmpty() -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "暂无消息",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        items(uiState.conversations) { conv ->
                            ConversationRow(
                                conv = conv,
                                onClick = { (conv.partner?.id ?: conv.partnerId).let { onChatClick(it) } },
                                onUserClick = onUserClick
                            )
                            HorizontalDivider(color = Color(0xFFEEEEEE))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(
    conv: Conversation,
    onClick: () -> Unit,
    onUserClick: (Int) -> Unit
) {
    val Gold = Color(0xFFC49333)
    val partner = conv.partner
    val pid = partner?.id ?: conv.partnerId

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFEEEEEE))
                .clickable { pid.let { onUserClick(it) } },
            contentAlignment = Alignment.Center
        ) {
            if (partner?.avatar != null) {
                AsyncImage(
                    model = resolveCoverUrl(partner.avatar),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(CircleShape)
                )
            } else {
                Text(
                    (partner?.name ?: "?").take(1),
                    color = Color(0xFF999999),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                partner?.name ?: "用户 $pid",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Spacer(Modifier.height(2.dp))
            conv.lastMessage?.let { msg ->
                val preview = when {
                    msg.imageUrl != null -> "[图片]"
                    msg.content != null -> msg.content
                    else -> ""
                }
                Text(
                    preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF999999),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 未读数角标
        if (conv.unreadCount > 0) {
            Box(
                modifier = Modifier
                    .background(Gold, MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    if (conv.unreadCount > 99) "99+" else "${conv.unreadCount}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

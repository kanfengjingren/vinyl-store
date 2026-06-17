package com.vinylstore.app.ui.social

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.vinylstore.app.VinylApp
import com.vinylstore.app.data.model.Message
import com.vinylstore.app.data.model.resolveCoverUrl
import com.vinylstore.app.ui.components.ErrorState
import com.vinylstore.app.ui.components.LoadingState
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    partnerId: Int,
    onBack: () -> Unit,
    onUserClick: (Int) -> Unit,
    onAlbumClick: (String, Int?) -> Unit
) {
    val app = VinylApp.instance
    val viewModel: ChatViewModel = viewModel(
        factory = ChatViewModel.Factory(
            app.chatRepository, app.friendRepository, app.userRepository, app.chatSocketManager
        )
    )
    val uiState by viewModel.uiState.collectAsState()
    val currentUserId by app.authRepository.currentUserIdFlow.collectAsState(initial = null)

    // 从缓存中读取当前用户头像
    val myAvatar by produceState<String?>(null) {
        app.tokenStorage.userJsonFlow.collect { json ->
            value = json?.let {
                try {
                    com.google.gson.Gson().fromJson(it, com.vinylstore.app.data.model.User::class.java).avatar
                } catch (_: Exception) { null }
            }
        }
    }

    val Gold = Color(0xFFC49333)

    // 初始化
    LaunchedEffect(partnerId, currentUserId) {
        val uid = currentUserId ?: return@LaunchedEffect
        viewModel.init(partnerId, uid, myAvatar)
    }

    // Socket 生命周期
    LaunchedEffect(Unit) {
        viewModel.connectSocket()
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.disconnectSocket() }
    }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.actionError) {
        uiState.actionError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionError()
        }
    }

    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val context = app.applicationContext
            val inputStream = context.contentResolver.openInputStream(selectedUri)
            val tempFile = File(context.cacheDir, "chat_upload_${System.currentTimeMillis()}.jpg")
            inputStream?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            viewModel.sendImageMessage(tempFile)
        }
    }

    // 自动滚动到底部
    val listState = rememberLazyListState()
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            uiState.partner?.name ?: "聊天",
                            color = Color.Black
                        )
                        Spacer(Modifier.width(8.dp))
                        // 连接状态指示灯
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        uiState.isConnected -> Color(0xFF4CAF50)
                                        uiState.connectionError != null -> Color(0xFFF44336)
                                        else -> Color(0xFFFFC107)
                                    }
                                )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            // 连接状态横幅
            if (!uiState.isConnected && uiState.connectionError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF44336).copy(alpha = 0.1f))
                        .padding(8.dp)
                ) {
                    Text(
                        uiState.connectionError!!,
                        color = Color(0xFFF44336),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else if (!uiState.isConnected) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFC107).copy(alpha = 0.1f))
                        .padding(8.dp)
                ) {
                    Text(
                        "连接中...",
                        color = Color(0xFFB8860B),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── 消息列表 ──
            when {
                uiState.isLoading -> LoadingState(Modifier.weight(1f))
                uiState.error != null -> ErrorState(
                    message = uiState.error!!,
                    onRetry = { viewModel.loadMessages(partnerId) },
                    modifier = Modifier.weight(1f)
                )
                uiState.messages.isEmpty() -> {
                    Box(
                        Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "暂无消息，发送第一条消息吧",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF999999)
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        items(uiState.messages, key = { it.id ?: 0 }) { msg ->
                            ChatMessageItem(
                                message = msg,
                                isMine = msg.senderId == currentUserId,
                                partnerAvatar = uiState.partnerAvatar,
                                myAvatar = myAvatar,
                                actionLoading = uiState.actionLoading,
                                onImageClick = { viewModel.setPreviewImage(it) },
                                onUserClick = onUserClick,
                                onAlbumClick = onAlbumClick,
                                onAcceptFriendReq = { msg.id?.let { viewModel.acceptFriendRequest(it) } },
                                onRejectFriendReq = { msg.id?.let { viewModel.rejectFriendRequest(it) } }
                            )
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFEEEEEE))

            // ── 输入栏 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // 图片按钮
                IconButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    enabled = !uiState.isUploadingImage,
                    modifier = Modifier.size(40.dp)
                ) {
                    if (uiState.isUploadingImage) {
                        CircularProgressIndicator(
                            color = Gold,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Image,
                            "发送图片",
                            tint = Color(0xFF999999),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.inputText,
                    onValueChange = { viewModel.setInputText(it) },
                    placeholder = {
                        Text(
                            if (uiState.isConnected) "输入消息..." else "连接中...",
                            color = Color(0xFFBBBBBB)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.Black),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Gold.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color(0xFFDDDDDD),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = Gold
                    ),
                    shape = MaterialTheme.shapes.extraSmall,
                    maxLines = 3,
                    singleLine = false
                )

                Spacer(Modifier.width(8.dp))

                IconButton(
                    onClick = { viewModel.sendTextMessage() },
                    enabled = uiState.inputText.isNotBlank() && !uiState.isSending && uiState.isConnected,
                    modifier = Modifier.size(40.dp)
                ) {
                    if (uiState.isSending) {
                        CircularProgressIndicator(
                            color = Gold,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Send,
                            "发送",
                            tint = if (uiState.inputText.isNotBlank() && uiState.isConnected) Gold
                                   else Color(0xFFCCCCCC),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // ── 全屏图片预览 ──
    uiState.previewImageUrl?.let { url ->
        val resolvedUrl = resolveCoverUrl(url) ?: url
        Dialog(
            onDismissRequest = { viewModel.setPreviewImage(null) },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable { viewModel.setPreviewImage(null) },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = resolvedUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════
// 消息条目（气泡 / 特殊卡片）
// ═══════════════════════════════════════════

@Composable
private fun ChatMessageItem(
    message: Message,
    isMine: Boolean,
    partnerAvatar: String?,
    myAvatar: String?,
    actionLoading: Int?,
    onImageClick: (String) -> Unit,
    onUserClick: (Int) -> Unit,
    onAlbumClick: (String, Int?) -> Unit,
    onAcceptFriendReq: () -> Unit,
    onRejectFriendReq: () -> Unit
) {
    val Gold = Color(0xFFC49333)

    // 检测特殊消息类型
    val specialType = detectMessageType(message)
    if (specialType != null) {
        when (specialType) {
            "friend_request" -> FriendRequestCard(
                message = message,
                isMine = isMine,
                actionLoading = actionLoading,
                onAccept = onAcceptFriendReq,
                onReject = onRejectFriendReq
            )
            "comment_reply" -> CommentNotifyCard(
                message = message,
                onAlbumClick = onAlbumClick
            )
        }
        return
    }

    // 普通消息气泡
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.widthIn(max = 280.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            if (!isMine) {
                // 对方头像
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEEEEEE))
                        .clickable { message.senderId?.let { onUserClick(it) } },
                    contentAlignment = Alignment.Center
                ) {
                    val avatarUrl = resolveCoverUrl(partnerAvatar)
                    if (avatarUrl != null) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Text(
                            "?", color = Color(0xFF999999), fontSize = 12.sp
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
            }

            Column(
                horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
            ) {
                // 图片
                if (!message.imageUrl.isNullOrBlank()) {
                    val resolvedUrl = resolveCoverUrl(message.imageUrl) ?: message.imageUrl
                    Box(
                        modifier = Modifier
                            .widthIn(max = 240.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .clickable { onImageClick(message.imageUrl) }
                    ) {
                        AsyncImage(
                            model = resolvedUrl,
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.widthIn(max = 240.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }

                // 文字气泡
                if (!message.content.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (isMine) Gold else Color(0xFFF0F0F0),
                                MaterialTheme.shapes.extraSmall
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isMine) Color.White else Color.Black
                        )
                    }
                }
            }

            if (isMine) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Gold.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    val avatarUrl = resolveCoverUrl(myAvatar)
                    if (avatarUrl != null) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Text(
                            "我", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 时间
        Spacer(Modifier.height(2.dp))
        Text(
            formatRelativeTime(message.createdAt),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFBBBBBB),
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 34.dp)
        )
    }
}

// ═══════════════════════════════════════════
// 好友请求卡片
// ═══════════════════════════════════════════

@Composable
private fun FriendRequestCard(
    message: Message,
    isMine: Boolean,
    actionLoading: Int?,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val Gold = Color(0xFFC49333)
    var parsed by remember { mutableStateOf<JSONObject?>(null) }
    LaunchedEffect(message.content) {
        parsed = try { JSONObject(message.content ?: "") } catch (_: Exception) { null }
    }
    val status = parsed?.optString("status", "pending") ?: "pending"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 16.dp)
            .background(
                Color(0xFFF8F8F8),
                MaterialTheme.shapes.extraSmall
            )
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                when (status) {
                    "pending" -> if (isMine) "好友请求已发送" else "对方想添加你为好友"
                    "accepted" -> "已添加为好友 ✓"
                    "rejected" -> "已拒绝好友请求"
                    else -> "好友请求"
                },
                color = Color(0xFF666666),
                fontSize = 13.sp
            )

            if (status == "pending" && !isMine) {
                Spacer(Modifier.height(8.dp))
                Row {
                    val isLoading = actionLoading == message.id
                    TextButton(
                        onClick = onAccept,
                        enabled = !isLoading,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text("接受", color = Gold, fontSize = 13.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    TextButton(
                        onClick = onReject,
                        enabled = !isLoading,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text("拒绝", color = Color(0xFF999999), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// 评论通知卡片
// ═══════════════════════════════════════════

@Composable
private fun CommentNotifyCard(
    message: Message,
    onAlbumClick: (String, Int?) -> Unit
) {
    val Gold = Color(0xFFC49333)
    var parsed by remember { mutableStateOf<JSONObject?>(null) }
    LaunchedEffect(message.content) {
        parsed = try { JSONObject(message.content ?: "") } catch (_: Exception) { null }
    }
    val albumTitle = parsed?.optString("albumTitle", "") ?: ""
    val albumSlug = parsed?.optString("albumSlug", "") ?: ""
    val commentId = parsed?.optInt("commentId", -1)?.takeIf { it > 0 }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 16.dp)
            .border(1.dp, Gold.copy(alpha = 0.3f), MaterialTheme.shapes.extraSmall)
            .background(
                Color(0xFFFFF8E1),
                MaterialTheme.shapes.extraSmall
            )
            .clickable { onAlbumClick(albumSlug, commentId) }
            .padding(12.dp)
    ) {
        Column {
            Text(
                "💬 评论回复",
                color = Gold,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "在《$albumTitle》中回复了你",
                color = Color(0xFF666666),
                fontSize = 13.sp
            )
            if (albumSlug.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "点击查看 →",
                    color = Gold.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ═══════════════════════════════════════════
// 检测特殊消息类型
// ═══════════════════════════════════════════

private fun detectMessageType(message: Message): String? {
    val content = message.content ?: return null
    return try {
        val json = JSONObject(content)
        json.optString("type", null)
    } catch (_: Exception) {
        null
    }
}

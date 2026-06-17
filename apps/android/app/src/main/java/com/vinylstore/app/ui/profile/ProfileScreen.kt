package com.vinylstore.app.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.vinylstore.app.VinylApp
import com.vinylstore.app.data.model.resolveCoverUrl
import com.vinylstore.app.ui.home.AlbumCardSmall
import java.io.File
import java.io.FileOutputStream

@Composable
fun ProfileScreen(
    onAlbumClick: (String) -> Unit,
    onOrdersClick: () -> Unit,
    onLoginRequired: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val app = VinylApp.instance
    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(
            app.authRepository,
            app.userRepository,
            app.albumRepository,
            app.playHistoryRepository
        )
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadProfile() }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Tab 栏 ──
        TabRow(
            selectedTabIndex = uiState.activeTab.ordinal,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
        ) {
            ProfileTab.entries.forEach { tab ->
                Tab(
                    selected = uiState.activeTab == tab,
                    onClick = { viewModel.setTab(tab) },
                    text = { Text(tab.label, maxLines = 1) }
                )
            }
        }

        // ── Tab 内容 ──
        when (uiState.activeTab) {
            ProfileTab.PROFILE -> ProfileInfoTab(uiState, viewModel, onOrdersClick, onLoggedOut)
            ProfileTab.PURCHASES -> PurchasesTab(uiState, onAlbumClick)
            ProfileTab.FAVORITES -> FavoritesTab(uiState, viewModel, onAlbumClick)
            ProfileTab.HISTORY -> HistoryTab(uiState, onAlbumClick)
        }
    }
}

// ═══════════════════════════════════════════
// Tab 1: 个人信息
// ═══════════════════════════════════════════

@Composable
private fun ProfileInfoTab(
    uiState: ProfileUiState,
    viewModel: ProfileViewModel,
    onOrdersClick: () -> Unit,
    onLoggedOut: () -> Unit
) {
    if (uiState.profileLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val profile = uiState.profile
    val app = VinylApp.instance
    val Gold = Color(0xFFC49333)

    // 头像图片选择器
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val context = app.applicationContext
            val inputStream = context.contentResolver.openInputStream(selectedUri)
            val tempFile = File(context.cacheDir, "avatar_upload_${System.currentTimeMillis()}.jpg")
            inputStream?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            viewModel.uploadAvatar(tempFile)
        }
    }

    // 头像消息提示
    LaunchedEffect(uiState.avatarMsg) {
        uiState.avatarMsg?.let {
            kotlinx.coroutines.delay(3000)
            viewModel.clearAvatarMsg()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── 头像（可点击上传） ──
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(enabled = !uiState.avatarUploading) {
                            avatarPickerLauncher.launch("image/*")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val avatarUrl = resolveCoverUrl(profile?.avatar)
                    if (avatarUrl != null) {
                        AsyncImage(
                            model = avatarUrl, contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            (profile?.name?.firstOrNull()?.uppercase()
                                ?: profile?.email?.firstOrNull()?.uppercase() ?: "?"),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 上传中遮罩
                    if (uiState.avatarUploading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp
                            )
                        }
                    }

                    // 相机图标（未上传时）
                    if (!uiState.avatarUploading) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "更换头像",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // 头像消息
                uiState.avatarMsg?.let { msg ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (msg.contains("成功")) Color(0xFF16A34A) else MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // ── 我的订单入口 ──
        item {
            OutlinedButton(
                onClick = onOrdersClick,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text("📦 我的订单", style = MaterialTheme.typography.bodyLarge)
            }
        }

        // ── 余额 ──
        item {
            SectionLabel("账户余额")
            Text(
                "¥${profile?.balance ?: 0}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.extraSmall)
                    .padding(12.dp)
            )
        }

        // ── 充值 ──
        item {
            SectionLabel("充值（测试）")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.rechargeAmount,
                    onValueChange = { viewModel.updateRechargeAmount(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入金额") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraSmall
                )
                Button(
                    onClick = { viewModel.handleRecharge() },
                    enabled = !uiState.recharging && (uiState.rechargeAmount.toIntOrNull() ?: 0) > 0,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(if (uiState.recharging) "充值中..." else "充值")
                }
            }
            uiState.rechargeMsg?.let {
                Text(
                    it, style = MaterialTheme.typography.bodySmall,
                    color = if (uiState.rechargeOk) Color(0xFF16A34A) else MaterialTheme.colorScheme.error
                )
            }
        }

        // ── 邮箱 / 用户名 ──
        item { InfoRow("邮箱", profile?.email ?: "-") }
        item { InfoRow("用户名", profile?.name ?: "未设置") }

        // ── 修改密码 ──
        item {
            TextButton(
                onClick = { viewModel.togglePasswordFields() },
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    if (uiState.showPasswordFields) "取消修改密码" else "修改密码 →",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (uiState.showPasswordFields) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.oldPassword, onValueChange = { viewModel.updateOldPwd(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("原密码") }, singleLine = true,
                    shape = MaterialTheme.shapes.extraSmall
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.newPassword, onValueChange = { viewModel.updateNewPwd(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("新密码") }, singleLine = true,
                    shape = MaterialTheme.shapes.extraSmall
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.handleChangePassword() },
                    enabled = !uiState.pwdSaving && uiState.oldPassword.isNotBlank() && uiState.newPassword.isNotBlank(),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(if (uiState.pwdSaving) "修改中..." else "确认修改")
                }
                uiState.pwdMsg?.let {
                    Text(
                        it, style = MaterialTheme.typography.bodySmall,
                        color = if (uiState.pwdOk) Color(0xFF16A34A) else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // ── 默认收货地址 ──
        item {
            SectionLabel("默认收货地址")
            OutlinedTextField(
                value = uiState.address, onValueChange = { viewModel.updateAddress(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("请输入默认收货地址") },
                minLines = 2, shape = MaterialTheme.shapes.extraSmall
            )
        }

        // ── 隐私设置 ──
        item {
            SectionLabel("隐私设置")
            Card(shape = MaterialTheme.shapes.extraSmall, colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PrivacyToggle("公开已购专辑", uiState.showPurchases) {
                        viewModel.togglePrivacy("showPurchases")
                    }
                    Spacer(Modifier.height(12.dp))
                    PrivacyToggle("公开收藏专辑", uiState.showFavorites) {
                        viewModel.togglePrivacy("showFavorites")
                    }
                }
            }
        }

        // ── 保存按钮 ──
        item {
            Button(
                onClick = { viewModel.save() },
                enabled = !uiState.saving,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.extraSmall,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(if (uiState.saving) "保存中..." else "保存")
            }
            if (uiState.saved) {
                Text(
                    "保存成功", style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF16A34A),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // ── 退出登录 ──
        item {
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = {
                    viewModel.logout()
                    onLoggedOut()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text("退出登录", color = MaterialTheme.colorScheme.error)
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text, style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        SectionLabel(label)
        Text(
            value, style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.extraSmall)
                .padding(12.dp)
        )
    }
}

@Composable
private fun PrivacyToggle(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Switch(
            checked = checked, onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

// ═══════════════════════════════════════════
// Tab 2: 已购专辑
// ═══════════════════════════════════════════

@Composable
private fun PurchasesTab(uiState: ProfileUiState, onAlbumClick: (String) -> Unit) {
    if (uiState.purchasesLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else if (uiState.purchases.isEmpty()) {
        EmptyTab("还没有购买过专辑", "💿")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.purchases, key = { it.id }) { album ->
                PurchaseRow(album, onAlbumClick)
            }
        }
    }
}

@Composable
private fun PurchaseRow(album: com.vinylstore.app.data.model.Album, onAlbumClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAlbumClick(album.slug) }
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.extraSmall)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = resolveCoverUrl(album.coverUrl),
            contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(80.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(album.artist, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(album.title, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${album.tracks?.size ?: 0} 首曲目", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ═══════════════════════════════════════════
// Tab 3: 我的收藏
// ═══════════════════════════════════════════

@Composable
private fun FavoritesTab(
    uiState: ProfileUiState,
    viewModel: ProfileViewModel,
    onAlbumClick: (String) -> Unit
) {
    if (uiState.favsLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else if (uiState.favorites.isEmpty()) {
        EmptyTab("还没有收藏专辑", "♡")
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(uiState.favorites, key = { it.id }) { fav ->
                fav.album?.let { AlbumCardSmall(album = it, onClick = { onAlbumClick(it.slug) }) }
            }
        }
    }
}

// ═══════════════════════════════════════════
// Tab 4: 播放历史
// ═══════════════════════════════════════════

@Composable
private fun HistoryTab(uiState: ProfileUiState, onAlbumClick: (String) -> Unit) {
    if (uiState.historyLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else if (uiState.playHistory.isEmpty()) {
        EmptyTab("还没有播放记录", "🎧")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            itemsIndexed(uiState.playHistory) { idx, h ->
                val album = h.album ?: return@itemsIndexed
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAlbumClick(album.slug) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${uiState.playHistory.size - idx}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.width(28.dp)
                    )
                    AsyncImage(
                        model = resolveCoverUrl(album.coverUrl),
                        contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(h.track?.title ?: "未知曲目",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${album.artist} — ${album.title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(
                        timeAgo(h.playedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTab(msg: String, emoji: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(16.dp))
            Text(msg, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ═══════════════════════════════════════════
// 工具
// ═══════════════════════════════════════════

private fun timeAgo(dateStr: String?): String {
    if (dateStr == null) return ""
    return try {
        val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
        parser.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = parser.parse(dateStr) ?: return ""
        val diff = System.currentTimeMillis() - date.time
        val mins = diff / 60000
        when {
            mins < 1 -> "刚刚"
            mins < 60 -> "${mins}分钟前"
            mins / 60 < 24 -> "${mins / 60}小时前"
            mins / 1440 < 30 -> "${mins / 1440}天前"
            else -> "${mins / 43200}月前"
        }
    } catch (_: Exception) { "" }
}

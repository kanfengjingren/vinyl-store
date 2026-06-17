package com.vinylstore.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vinylstore.app.data.model.resolveCoverUrl
import com.vinylstore.app.player.AudioPlayerManager

@Composable
fun FullPlayerOverlay(
    playerManager: AudioPlayerManager,
    visible: Boolean,
    onDismiss: () -> Unit
) {
    val state by playerManager.state.collectAsState()

    // 拦截系统返回键：全屏播放器显示时，先收起播放器而不退出页面
    // BackHandler 放在 AnimatedVisibility 外面确保始终被注册
    BackHandler(enabled = visible) {
        onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A2E),
                            Color(0xFF0D0D1A)
                        )
                    )
                )
                // 消费所有触摸事件，防止穿透到底层页面
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* 消费点击，阻止穿透 */ }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── 顶栏 ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, shape = MaterialTheme.shapes.extraSmall) {
                        Text("▼ 收起", color = Color.White.copy(alpha = 0.6f))
                    }
                    Text(
                        "正在播放",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.width(64.dp))
                }

                Spacer(Modifier.height(32.dp))

                // ── 封面 ──
                val coverUrl = resolveCoverUrl(state.album?.coverUrl)
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (coverUrl != null) {
                        AsyncImage(
                            model = coverUrl, contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            state.album?.title?.take(2)?.uppercase() ?: "?",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.2f)
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // ── 曲目信息 ──
                Text(
                    state.track?.title ?: "",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Text(
                    state.album?.artist ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )

                Spacer(Modifier.height(32.dp))

                // ── 进度条 ──
                Column(modifier = Modifier.padding(horizontal = 32.dp)) {
                    var isDragging by remember { mutableStateOf(false) }
                    var dragPos by remember { mutableFloatStateOf(0f) }

                    val displayPos = when {
                        isDragging -> dragPos.toLong()
                        state.duration > 0 -> state.currentPosition
                        else -> 0L
                    }

                    Slider(
                        value = if (isDragging) dragPos else displayPos.toFloat(),
                        onValueChange = {
                            isDragging = true
                            dragPos = it
                        },
                        onValueChangeFinished = {
                            playerManager.seekTo(dragPos.toLong())
                            isDragging = false
                        },
                        valueRange = 0f..(state.duration.toFloat().coerceAtLeast(1f)),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFC49333),
                            activeTrackColor = Color(0xFFC49333),
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            formatTime(displayPos),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                        Text(
                            formatTime(state.duration),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── 控制按钮 ──
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 上一首
                    IconButton(
                        onClick = { playerManager.playPrev() },
                        enabled = state.playlist.isNotEmpty(),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Text("⏮", fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                            color = if (state.playlist.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.3f))
                    }

                    Spacer(Modifier.width(24.dp))

                    // 播放/暂停
                    IconButton(
                        onClick = { playerManager.togglePlayPause() },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFC49333))
                    ) {
                        Text(
                            if (state.isPlaying) "⏸" else "▶",
                            fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                            color = Color.White
                        )
                    }

                    Spacer(Modifier.width(24.dp))

                    // 下一首
                    IconButton(
                        onClick = { playerManager.playNext() },
                        enabled = state.playlist.isNotEmpty(),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Text("⏭", fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                            color = if (state.playlist.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.3f))
                    }
                }

                Spacer(Modifier.height(32.dp))

                // ── 底部留白 ──
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val sec = (ms / 1000).coerceAtLeast(0)
    val m = sec / 60
    val s = sec % 60
    return "${m}:${s.toString().padStart(2, '0')}"
}

package com.vinylstore.app.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Send
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
import coil.compose.AsyncImage
import com.vinylstore.app.data.model.Comment
import com.vinylstore.app.data.model.CommentReply
import com.vinylstore.app.data.model.resolveCoverUrl

// ═══════════════════════════════════════════
// 评论区域根组件
// ═══════════════════════════════════════════

@Composable
fun CommentSection(
    albumId: Int,
    comments: List<Comment>,
    page: Int,
    totalPages: Int,
    total: Int,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    isSubmitting: Boolean,
    replyTarget: Int?,
    expandedReplies: Map<Int, List<CommentReply>>,
    highlightCommentId: Int?,
    isLoggedIn: Boolean,
    currentUserId: Int?,
    onLoadPage: (Int) -> Unit,
    onLoadMore: () -> Unit,
    onSubmit: (content: String, parentId: Int?) -> Unit,
    onDelete: (Int) -> Unit,
    onLoadReplies: (Int) -> Unit,
    onSetReplyTarget: (Int?) -> Unit,
    onUserClick: (Int) -> Unit,
    onLoginRequired: () -> Unit
) {
    val Gold = Color(0xFFC49333)

    Column(modifier = Modifier.fillMaxWidth()) {
        // ── 标题行 ──
        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "评论",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "($total)",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.4f)
            )
        }

        // ── 评论输入 ──
        if (isLoggedIn) {
            CommentInput(
                albumId = albumId,
                parentId = null,
                isSubmitting = isSubmitting,
                onSubmit = onSubmit
            )
            Spacer(Modifier.height(16.dp))
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLoginRequired() }
                    .background(
                        Color.White.copy(alpha = 0.05f),
                        MaterialTheme.shapes.extraSmall
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "登录后发表评论",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gold
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── 加载中 ──
        if (isLoading) {
            Box(
                Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Gold,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
            return
        }

        // ── 空态 ──
        if (comments.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "还没有评论，来发表第一条评论吧",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.3f)
                )
            }
            return
        }

        // ── 评论列表（用 Column 避免与外层 verticalScroll 嵌套冲突）──
        Column(modifier = Modifier.fillMaxWidth()) {
            comments.forEach { comment ->
                CommentItem(
                    comment = comment,
                    albumId = albumId,
                    isExpanded = expandedReplies.containsKey(comment.id),
                    replies = expandedReplies[comment.id] ?: emptyList(),
                    replyCount = comment.totalReplyCount,
                    isLoggedIn = isLoggedIn,
                    currentUserId = currentUserId,
                    isReplying = replyTarget == comment.id,
                    onReply = { onSetReplyTarget(comment.id) },
                    onDelete = { onDelete(comment.id) },
                    onLoadReplies = { onLoadReplies(comment.id) },
                    onUserClick = onUserClick,
                    onCancelReply = { onSetReplyTarget(null) },
                    onSubmitReply = { content, parentId -> onSubmit(content, parentId) },
                    isSubmittingReply = isSubmitting && replyTarget == comment.id,
                    onLoginRequired = onLoginRequired
                )
            }

            // ── 加载更多 ──
            if (isLoadingMore) {
                Box(
                    Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Gold,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            // ── 分页按钮 ──
            if (totalPages > 1) {
                PaginationRow(
                    page = page,
                    totalPages = totalPages,
                    onPageClick = onLoadPage
                )
            }
        }
    }
}

// ═══════════════════════════════════════════
// 单条评论
// ═══════════════════════════════════════════

@Composable
fun CommentItem(
    comment: Comment,
    albumId: Int,
    isExpanded: Boolean,
    replies: List<CommentReply>,
    replyCount: Int,
    isLoggedIn: Boolean,
    currentUserId: Int?,
    isReplying: Boolean,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    onLoadReplies: () -> Unit,
    onUserClick: (Int) -> Unit,
    onCancelReply: () -> Unit,
    onSubmitReply: (String, Int?) -> Unit,
    isSubmittingReply: Boolean,
    onLoginRequired: () -> Unit = {}
) {
    val Gold = Color(0xFFC49333)
    val isOwn = currentUserId != null && comment.userId == currentUserId

    var showAllReplies by remember { mutableStateOf(false) }
    // Client-side pagination: show 8 at a time
    val replyPageSize = 8
    val visibleReplyCount = if (isExpanded && showAllReplies) replies.size
    else if (isExpanded) replyPageSize.coerceAtMost(replies.size)
    else 0
    val hasMoreReplies = isExpanded && replies.size > replyPageSize && !showAllReplies

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // ── 头像 + 用户名 + 时间 ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // 头像
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable { comment.user?.id?.let { onUserClick(it) } },
                contentAlignment = Alignment.Center
            ) {
                if (comment.user?.avatar != null) {
                    AsyncImage(
                        model = resolveCoverUrl(comment.user.avatar),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp).clip(CircleShape)
                    )
                } else {
                    Text(
                        text = (comment.user?.name ?: "?").take(1),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 用户名 + 时间
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.user?.name ?: "用户",
                        style = MaterialTheme.typography.labelLarge,
                        color = Gold,
                        modifier = Modifier.clickable { comment.user?.id?.let { onUserClick(it) } }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatRelativeTime(comment.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.3f)
                    )
                }

                Spacer(Modifier.height(4.dp))

                // 评论内容
                Text(
                    text = comment.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )

                Spacer(Modifier.height(6.dp))

                // ── 操作按钮 ──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLoggedIn && comment.userId != currentUserId) {
                        TextButton(
                            onClick = onReply,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Icon(
                                Icons.Default.Reply,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color.White.copy(alpha = 0.4f)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("回复", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                        }
                    }
                    if (isOwn) {
                        TextButton(
                            onClick = onDelete,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color.White.copy(alpha = 0.4f)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("删除", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                        }
                    }
                }

                // ── 回复区域 ──
                if ((replyCount > 0 || isExpanded) && visibleReplyCount > 0) {
                    Spacer(Modifier.height(4.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp)
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.08f),
                                shape = MaterialTheme.shapes.extraSmall
                            )
                            .padding(8.dp)
                    ) {
                        replies.take(visibleReplyCount).forEach { reply ->
                            ReplyItem(
                                reply = reply,
                                albumId = albumId,
                                isLoggedIn = isLoggedIn,
                                currentUserId = currentUserId,
                                onDelete = { /* 暂不处理，由父组件处理 */ },
                                onUserClick = onUserClick,
                                onSubmitReply = { content, parentId ->
                                    onSubmitReply(content, parentId ?: reply.id)
                                },
                                onLoginRequired = onLoginRequired
                            )
                        }

                        // 加载更多回复 / 收起
                        if (hasMoreReplies) {
                            TextButton(
                                onClick = { showAllReplies = true },
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    "加载更多回复 (${replies.size - replyPageSize})",
                                    color = Gold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        if (isExpanded && showAllReplies && replies.size > replyPageSize) {
                            TextButton(
                                onClick = { showAllReplies = false },
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text("收起", color = Gold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // 展开回复按钮
                if (!isExpanded && replyCount > 0) {
                    TextButton(
                        onClick = onLoadReplies,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            "展开 $replyCount 条回复",
                            color = Gold,
                            fontSize = 12.sp
                        )
                    }
                }

                // ── 内联回复输入框 ──
                if (isReplying) {
                    Spacer(Modifier.height(8.dp))
                    CommentInput(
                        albumId = albumId,
                        parentId = comment.id,
                        isSubmitting = isSubmittingReply,
                        placeholder = "回复 ${comment.user?.name ?: ""}...",
                        onSubmit = { content, _ ->
                            onSubmitReply(content, comment.id)
                        },
                        onCancel = onCancelReply,
                        showCancel = true
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// 回复条目（支持嵌套回复）
// ═══════════════════════════════════════════

@Composable
fun ReplyItem(
    reply: CommentReply,
    albumId: Int,
    isLoggedIn: Boolean,
    currentUserId: Int?,
    onDelete: () -> Unit,
    onUserClick: (Int) -> Unit,
    onSubmitReply: (String, Int?) -> Unit = { _, _ -> },
    isSubmittingReply: Boolean = false,
    onLoginRequired: () -> Unit = {}
) {
    val Gold = Color(0xFFC49333)
    var showReplyInput by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // 小头像
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable { reply.user?.id?.let { onUserClick(it) } },
                contentAlignment = Alignment.Center
            ) {
                if (reply.user?.avatar != null) {
                    AsyncImage(
                        model = resolveCoverUrl(reply.user.avatar),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp).clip(CircleShape)
                    )
                } else {
                    Text(
                        text = (reply.user?.name ?: "?").take(1),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = reply.user?.name ?: "用户",
                        style = MaterialTheme.typography.labelSmall,
                        color = Gold,
                        modifier = Modifier.clickable { reply.user?.id?.let { onUserClick(it) } }
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = formatRelativeTime(reply.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.25f),
                        fontSize = 10.sp
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = reply.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp
                )

                // 操作按钮
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLoggedIn && reply.userId != currentUserId) {
                        TextButton(
                            onClick = { showReplyInput = !showReplyInput },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(24.dp),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                "回复",
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 11.sp
                            )
                        }
                    }
                    if (currentUserId != null && reply.userId == currentUserId) {
                        TextButton(
                            onClick = onDelete,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(24.dp),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                "删除",
                                color = Color.White.copy(alpha = 0.25f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // 内联回复输入框（回复这条 reply）
        if (showReplyInput) {
            Spacer(Modifier.height(4.dp))
            CommentInput(
                albumId = albumId,
                parentId = reply.id,
                isSubmitting = isSubmittingReply,
                placeholder = "回复 @${reply.user?.name ?: "匿名"}...",
                onSubmit = { content, _ ->
                    onSubmitReply(content, reply.id)
                    showReplyInput = false
                },
                onCancel = { showReplyInput = false },
                showCancel = true
            )
        }
    }
}

// ═══════════════════════════════════════════
// 评论输入框
// ═══════════════════════════════════════════

@Composable
fun CommentInput(
    albumId: Int,
    parentId: Int?,
    isSubmitting: Boolean,
    placeholder: String = "发表你的评论...",
    onSubmit: (content: String, parentId: Int?) -> Unit,
    onCancel: (() -> Unit)? = null,
    showCancel: Boolean = false
) {
    val Gold = Color(0xFFC49333)
    var text by remember { mutableStateOf("") }
    val maxLength = 500

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= maxLength) text = it },
                placeholder = {
                    Text(
                        placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.3f)
                    )
                },
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gold.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = Gold
                ),
                shape = MaterialTheme.shapes.extraSmall,
                maxLines = 3
            )

            Spacer(Modifier.width(8.dp))

            // 发送按钮
            IconButton(
                onClick = {
                    if (text.isNotBlank() && !isSubmitting) {
                        onSubmit(text.trim(), parentId)
                        text = ""
                    }
                },
                enabled = text.isNotBlank() && !isSubmitting,
                modifier = Modifier.size(40.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Gold,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "发送",
                        tint = if (text.isNotBlank()) Gold else Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 字数计数 + 取消按钮
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${text.length}/$maxLength",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.3f)
            )
            if (showCancel) {
                TextButton(
                    onClick = { onCancel?.invoke() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text("取消", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// 分页控件
// ═══════════════════════════════════════════

@Composable
fun PaginationRow(
    page: Int,
    totalPages: Int,
    onPageClick: (Int) -> Unit
) {
    val Gold = Color(0xFFC49333)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 上一页
        TextButton(
            onClick = { if (page > 1) onPageClick(page - 1) },
            enabled = page > 1,
            shape = MaterialTheme.shapes.extraSmall
        ) {
            Text("← 上一页", color = if (page > 1) Gold else Color.White.copy(alpha = 0.2f), fontSize = 13.sp)
        }

        Spacer(Modifier.width(12.dp))

        // 页码
        val pagesToShow = buildPageList(page, totalPages)
        pagesToShow.forEach { p ->
            if (p == -1) {
                Text("…", color = Color.White.copy(alpha = 0.3f), fontSize = 13.sp)
            } else {
                TextButton(
                    onClick = { onPageClick(p) },
                    shape = MaterialTheme.shapes.extraSmall,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        "$p",
                        color = if (p == page) Gold else Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        fontWeight = if (p == page) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
        }

        Spacer(Modifier.width(8.dp))

        // 下一页
        TextButton(
            onClick = { if (page < totalPages) onPageClick(page + 1) },
            enabled = page < totalPages,
            shape = MaterialTheme.shapes.extraSmall
        ) {
            Text("下一页 →", color = if (page < totalPages) Gold else Color.White.copy(alpha = 0.2f), fontSize = 13.sp)
        }
    }
}

private fun buildPageList(current: Int, total: Int): List<Int> {
    if (total <= 7) return (1..total).toList()
    val pages = mutableListOf<Int>()
    pages.add(1)
    if (current > 3) pages.add(-1) // ellipsis
    val start = maxOf(2, current - 1)
    val end = minOf(total - 1, current + 1)
    for (i in start..end) pages.add(i)
    if (current < total - 2) pages.add(-1)
    pages.add(total)
    return pages
}

// ═══════════════════════════════════════════
// 相对时间格式化
// ═══════════════════════════════════════════

fun formatRelativeTime(isoTime: String?): String {
    if (isoTime == null) return ""
    try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = sdf.parse(isoTime.substringBefore(".")) ?: return isoTime
        val now = System.currentTimeMillis()
        val diff = now - date.time

        return when {
            diff < 60_000 -> "刚刚"
            diff < 3_600_000 -> "${diff / 60_000} 分钟前"
            diff < 86_400_000 -> "${diff / 3_600_000} 小时前"
            diff < 7 * 86_400_000 -> "${diff / 86_400_000} 天前"
            else -> {
                val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                fmt.format(date)
            }
        }
    } catch (_: Exception) {
        return isoTime ?: ""
    }
}

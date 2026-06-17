package com.vinylstore.app.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vinylstore.app.data.model.Friend
import com.vinylstore.app.data.model.UserSearchResult

@Composable
fun FriendsSection(
    modifier: Modifier = Modifier,
    friends: List<Friend>,
    pendingRequests: List<Friend>,
    searchQuery: String,
    searchResults: List<UserSearchResult>,
    searchLoading: Boolean,
    actionLoading: Int?,
    onSearchQueryChanged: (String) -> Unit,
    onSendFriendRequest: (Int) -> Unit,
    onAcceptRequest: (Int) -> Unit,
    onRejectRequest: (Int) -> Unit,
    onUserClick: (Int) -> Unit,
    onChatClick: (Int) -> Unit
) {
    val Gold = Color(0xFFC49333)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier.fillMaxSize()) {
        // ── 搜索添加好友 ──
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = {
                Text(
                    "搜索用户以添加好友...",
                    color = onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = onSurface),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold.copy(alpha = 0.5f),
                unfocusedBorderColor = outlineVariant,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = Gold
            ),
            shape = MaterialTheme.shapes.extraSmall
        )

        // 搜索结果
        if (searchLoading) {
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
        } else if (searchResults.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                items(searchResults) { user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 头像
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(surfaceVariant)
                                .clickable { onUserClick(user.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (user.avatar != null) {
                                AsyncImage(
                                    model = user.avatar,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp).clip(CircleShape)
                                )
                            } else {
                                Text(
                                    (user.name ?: "?").take(1),
                                    color = onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        Text(
                            user.name ?: (user.email ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                            color = onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        val isLoading = actionLoading == user.id
                        TextButton(
                            onClick = { onSendFriendRequest(user.id) },
                            enabled = !isLoading,
                            shape = MaterialTheme.shapes.extraSmall,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Gold,
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Add, null, Modifier.size(16.dp), tint = Gold)
                                Spacer(Modifier.width(4.dp))
                                Text("添加", color = Gold, fontSize = 13.sp)
                            }
                        }
                    }
                    HorizontalDivider(color = outlineVariant)
                }
            }
        }

        // ── 待处理请求 ──
        if (pendingRequests.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "待处理 · ${pendingRequests.size}",
                style = MaterialTheme.typography.titleSmall,
                color = onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                items(pendingRequests) { friend ->
                    val user = friend.user
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(surfaceVariant)
                                .clickable { user?.id?.let { onUserClick(it) } },
                            contentAlignment = Alignment.Center
                        ) {
                            if (user?.avatar != null) {
                                AsyncImage(
                                    model = user.avatar,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp).clip(CircleShape)
                                )
                            } else {
                                Text(
                                    (user?.name ?: "?").take(1),
                                    color = onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        Text(
                            user?.name ?: "用户",
                            style = MaterialTheme.typography.bodyMedium,
                            color = onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        val isLoading = actionLoading == friend.friendshipId
                        TextButton(
                            onClick = { onAcceptRequest(friend.friendshipId ?: return@TextButton) },
                            enabled = !isLoading,
                            shape = MaterialTheme.shapes.extraSmall,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("接受", color = Gold, fontSize = 13.sp)
                        }
                        TextButton(
                            onClick = { onRejectRequest(friend.friendshipId ?: return@TextButton) },
                            enabled = !isLoading,
                            shape = MaterialTheme.shapes.extraSmall,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("拒绝", color = onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                    HorizontalDivider(color = outlineVariant)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── 好友列表 ──
        Text(
            "好友 · ${friends.size}",
            style = MaterialTheme.typography.titleSmall,
            color = onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (friends.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "还没有好友，搜索用户来添加吧",
                    style = MaterialTheme.typography.bodyMedium,
                    color = onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                items(friends) { friend ->
                    val user = friend.user
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { user?.id?.let { onUserClick(it) } }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (user?.avatar != null) {
                                AsyncImage(
                                    model = user.avatar,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(CircleShape)
                                )
                            } else {
                                Text(
                                    (user?.name ?: "?").take(1),
                                    color = onSurfaceVariant,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Text(
                            user?.name ?: "用户",
                            style = MaterialTheme.typography.bodyLarge,
                            color = onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = { user?.id?.let { onChatClick(it) } },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Chat,
                                contentDescription = "聊天",
                                tint = Gold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = outlineVariant)
                }
            }
        }
    }
}

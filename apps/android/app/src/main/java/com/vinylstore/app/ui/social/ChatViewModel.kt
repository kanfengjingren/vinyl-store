package com.vinylstore.app.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinylstore.app.data.chat.ChatSocketManager
import com.vinylstore.app.data.chat.SocketState
import com.vinylstore.app.data.model.*
import com.vinylstore.app.data.repository.ChatRepository
import com.vinylstore.app.data.repository.FriendRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

data class ChatUiState(
    val partnerId: Int = 0,
    val partner: ConversationPartner? = null,
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val inputText: String = "",
    val isSending: Boolean = false,
    val isUploadingImage: Boolean = false,
    val isConnected: Boolean = false,
    val connectionError: String? = null,
    val previewImageUrl: String? = null,       // 全屏图片预览
    val actionLoading: Int? = null,             // 正在操作的消息 id
    val actionError: String? = null
)

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val friendRepository: FriendRepository,
    private val socketManager: ChatSocketManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var socketCollectJob: Job? = null
    private var currentUserId: Int = 0

    fun init(partnerId: Int, myUserId: Int) {
        currentUserId = myUserId
        _uiState.value = _uiState.value.copy(partnerId = partnerId)
        loadMessages(partnerId)
        observeSocket()
    }

    // ═══════════════════════════════════════════
    // 消息加载
    // ═══════════════════════════════════════════

    fun loadMessages(partnerId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val msgs = chatRepository.getMessages(partnerId)
                // 标记已读
                try { chatRepository.markMessagesRead(partnerId) } catch (_: Exception) {}
                _uiState.value = _uiState.value.copy(
                    messages = msgs,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载失败"
                )
            }
        }
    }

    // ═══════════════════════════════════════════
    // 发送消息（乐观更新 + ACK）
    // ═══════════════════════════════════════════

    fun sendTextMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return

        val partnerId = _uiState.value.partnerId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(inputText = "", isSending = true)
            val tempId = -System.currentTimeMillis()
            val optimisticMsg = Message(
                id = tempId.toInt(),
                senderId = currentUserId,
                receiverId = partnerId,
                content = text,
                type = null,
                imageUrl = null,
                createdAt = java.text.SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss",
                    java.util.Locale.getDefault()
                ).format(java.util.Date())
            )
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + optimisticMsg
            )

            socketManager.sendMessage(
                receiverId = partnerId,
                content = text,
                ackCallback = { serverMsg ->
                    val realId = serverMsg.optInt("id", 0)
                    if (realId > 0) {
                        replaceTempMessage(tempId.toInt(), serverMsg)
                    }
                    _uiState.value = _uiState.value.copy(isSending = false)
                }
            )
        }
    }

    fun sendImageMessage(file: File) {
        val partnerId = _uiState.value.partnerId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingImage = true)
            try {
                val uploadResp = chatRepository.uploadChatImage(file)
                val imageUrl = uploadResp.url
                val tempId = -System.currentTimeMillis()
                val optimisticMsg = Message(
                    id = tempId.toInt(),
                    senderId = currentUserId,
                    receiverId = partnerId,
                    content = null,
                    type = null,
                    imageUrl = imageUrl,
                    createdAt = java.text.SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss",
                        java.util.Locale.getDefault()
                    ).format(java.util.Date())
                )
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + optimisticMsg,
                    isUploadingImage = false
                )

                socketManager.sendMessage(
                    receiverId = partnerId,
                    imageUrl = imageUrl,
                    ackCallback = { serverMsg ->
                        val realId = serverMsg.optInt("id", 0)
                        if (realId > 0) {
                            replaceTempMessage(tempId.toInt(), serverMsg)
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUploadingImage = false,
                    actionError = e.message ?: "图片上传失败"
                )
            }
        }
    }

    private fun replaceTempMessage(tempId: Int, serverMsg: JSONObject) {
        val realId = serverMsg.optInt("id", 0)
        val msgs = _uiState.value.messages.toMutableList()
        val index = msgs.indexOfFirst { it.id == tempId }
        if (index >= 0) {
            msgs[index] = Message(
                id = realId,
                senderId = serverMsg.optInt("senderId", currentUserId),
                receiverId = serverMsg.optInt("receiverId", _uiState.value.partnerId),
                content = serverMsg.optString("content", null),
                type = serverMsg.optString("type", null),
                imageUrl = serverMsg.optString("imageUrl", null),
                createdAt = serverMsg.optString("createdAt")
            )
        }
        _uiState.value = _uiState.value.copy(messages = msgs)
    }

    // ═══════════════════════════════════════════
    // 接收消息
    // ═══════════════════════════════════════════

    private fun observeSocket() {
        socketCollectJob?.cancel()
        socketCollectJob = viewModelScope.launch {
            launch {
                socketManager.connectionState.collect { state ->
                    _uiState.value = _uiState.value.copy(
                        isConnected = state == SocketState.CONNECTED,
                        connectionError = if (state == SocketState.ERROR) "连接失败，尝试重连..." else null
                    )
                }
            }
            launch {
                socketManager.newMessage.collect { msg ->
                    if (msg == null) return@collect
                    handleNewMessage(msg)
                }
            }
        }
    }

    private fun handleNewMessage(json: JSONObject) {
        val msgId = json.optInt("id", 0)
        val senderId = json.optInt("senderId", 0)
        val receiverId = json.optInt("receiverId", 0)
        val content = json.optString("content", null)
        val imageUrl = json.optString("imageUrl", null)
        val createdAt = json.optString("createdAt", null)
        val type = json.optString("type", null)

        // 只处理与当前对话相关的消息
        val partnerId = _uiState.value.partnerId
        if (senderId != partnerId && receiverId != partnerId) return

        // 去重
        val existing = _uiState.value.messages.find {
            it.id == msgId && msgId > 0 ||
            (msgId < 0 && it.senderId == senderId && it.content == content && (it.id ?: 0) < 0)
        }
        if (existing != null && msgId > 0) return

        val newMsg = Message(
            id = msgId,
            senderId = senderId,
            receiverId = receiverId,
            content = content,
            type = type,
            imageUrl = imageUrl,
            createdAt = createdAt
        )

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + newMsg
        )
    }

    // ═══════════════════════════════════════════
    // 好友请求操作（在聊天中处理特殊卡片）
    // ═══════════════════════════════════════════

    fun acceptFriendRequest(msgId: Int) {
        val msg = _uiState.value.messages.find { it.id == msgId } ?: return
        try {
            val json = JSONObject(msg.content ?: return)
            val friendshipId = json.optInt("friendshipId", 0)
            if (friendshipId <= 0) return

            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(actionLoading = msgId)
                try {
                    friendRepository.acceptFriendRequest(friendshipId)
                    // 更新消息内容
                    val newJson = JSONObject(msg.content).apply { put("status", "accepted") }
                    updateMessageContent(msgId, newJson.toString())
                    _uiState.value = _uiState.value.copy(actionLoading = null)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null,
                        actionError = e.message ?: "操作失败"
                    )
                }
            }
        } catch (_: Exception) {}
    }

    fun rejectFriendRequest(msgId: Int) {
        val msg = _uiState.value.messages.find { it.id == msgId } ?: return
        try {
            val json = JSONObject(msg.content ?: return)
            val friendshipId = json.optInt("friendshipId", 0)
            if (friendshipId <= 0) return

            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(actionLoading = msgId)
                try {
                    friendRepository.rejectFriendRequest(friendshipId)
                    val newJson = JSONObject(msg.content).apply { put("status", "rejected") }
                    updateMessageContent(msgId, newJson.toString())
                    _uiState.value = _uiState.value.copy(actionLoading = null)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null,
                        actionError = e.message ?: "操作失败"
                    )
                }
            }
        } catch (_: Exception) {}
    }

    private fun updateMessageContent(msgId: Int, newContent: String) {
        val msgs = _uiState.value.messages.toMutableList()
        val index = msgs.indexOfFirst { it.id == msgId }
        if (index >= 0) {
            msgs[index] = msgs[index].copy(content = newContent)
            _uiState.value = _uiState.value.copy(messages = msgs)
        }
    }

    // ═══════════════════════════════════════════
    // UI actions
    // ═══════════════════════════════════════════

    fun setInputText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun setPreviewImage(url: String?) {
        _uiState.value = _uiState.value.copy(previewImageUrl = url)
    }

    fun clearActionError() {
        _uiState.value = _uiState.value.copy(actionError = null)
    }

    fun connectSocket() {
        socketManager.onScreenEntered()
    }

    fun disconnectSocket() {
        socketManager.onScreenLeft()
    }

    override fun onCleared() {
        super.onCleared()
        socketCollectJob?.cancel()
    }

    class Factory(
        private val chatRepository: ChatRepository,
        private val friendRepository: FriendRepository,
        private val socketManager: ChatSocketManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                return ChatViewModel(chatRepository, friendRepository, socketManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

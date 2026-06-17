package com.vinylstore.app.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinylstore.app.data.chat.ChatSocketManager
import com.vinylstore.app.data.model.*
import com.vinylstore.app.data.repository.ChatRepository
import com.vinylstore.app.data.repository.FriendRepository
import com.vinylstore.app.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class MessagesTab { MESSAGES, FRIENDS }

data class MessagesUiState(
    val activeTab: MessagesTab = MessagesTab.MESSAGES,
    val isLoading: Boolean = false,
    val error: String? = null,
    // 会话
    val conversations: List<Conversation> = emptyList(),
    val conversationsLoading: Boolean = false,
    // 好友
    val friends: List<Friend> = emptyList(),
    val pendingRequests: List<Friend> = emptyList(),
    val pendingLoading: Boolean = false,
    // 搜索
    val searchQuery: String = "",
    val searchResults: List<UserSearchResult> = emptyList(),
    val searchLoading: Boolean = false,
    // Socket
    val socketConnected: Boolean = false,
    val unreadTotal: Int = 0,
    // 操作中
    val actionLoading: Int? = null,   // friendshipId or userId being acted on
    val actionError: String? = null
)

class MessagesViewModel(
    private val chatRepository: ChatRepository,
    private val friendRepository: FriendRepository,
    private val userRepository: UserRepository,
    private val socketManager: ChatSocketManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessagesUiState())
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var socketCollectJob: Job? = null

    init {
        loadConversations()
        loadFriends()
        observeSocket()
    }

    // ═══════════════════════════════════════════
    // 会话
    // ═══════════════════════════════════════════

    fun loadConversations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(conversationsLoading = true)
            try {
                val convs = chatRepository.getConversations()
                val unread = chatRepository.getUnreadCount()
                _uiState.value = _uiState.value.copy(
                    conversations = convs,
                    conversationsLoading = false,
                    unreadTotal = unread.count
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    conversationsLoading = false,
                    error = e.message
                )
            }
        }
    }

    /** 刷新：重新加载会话列表 + 已读标记 */
    fun refresh() {
        loadConversations()
        loadUnreadCount()
        loadFriends()
    }

    fun loadUnreadCount() {
        viewModelScope.launch {
            try {
                val unread = chatRepository.getUnreadCount()
                _uiState.value = _uiState.value.copy(unreadTotal = unread.count)
            } catch (_: Exception) {}
        }
    }

    // ═══════════════════════════════════════════
    // 好友
    // ═══════════════════════════════════════════

    fun loadFriends() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(pendingLoading = true)
            try {
                val friends = friendRepository.getFriends()
                val pending = friendRepository.getPendingRequests()
                _uiState.value = _uiState.value.copy(
                    friends = friends,
                    pendingRequests = pending,
                    pendingLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    pendingLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun sendFriendRequest(userId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionLoading = userId)
            try {
                friendRepository.sendFriendRequest(userId)
                // 从搜索结果移除
                _uiState.value = _uiState.value.copy(
                    actionLoading = null,
                    searchResults = _uiState.value.searchResults.filter { it.id != userId }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    actionLoading = null,
                    actionError = e.message ?: "发送失败"
                )
            }
        }
    }

    fun acceptFriendRequest(friendshipId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionLoading = friendshipId)
            try {
                friendRepository.acceptFriendRequest(friendshipId)
                _uiState.value = _uiState.value.copy(actionLoading = null)
                loadFriends()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    actionLoading = null,
                    actionError = e.message ?: "操作失败"
                )
            }
        }
    }

    fun rejectFriendRequest(friendshipId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionLoading = friendshipId)
            try {
                friendRepository.rejectFriendRequest(friendshipId)
                _uiState.value = _uiState.value.copy(actionLoading = null)
                loadFriends()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    actionLoading = null,
                    actionError = e.message ?: "操作失败"
                )
            }
        }
    }

    // ═══════════════════════════════════════════
    // 搜索用户（300ms 防抖）
    // ═══════════════════════════════════════════

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList(), searchLoading = false)
            return
        }

        searchJob = viewModelScope.launch {
            delay(300)
            _uiState.value = _uiState.value.copy(searchLoading = true)
            try {
                val results = userRepository.searchUsers(query)
                _uiState.value = _uiState.value.copy(
                    searchResults = results,
                    searchLoading = false
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(searchLoading = false)
            }
        }
    }

    // ═══════════════════════════════════════════
    // Tab
    // ═══════════════════════════════════════════

    fun setActiveTab(tab: MessagesTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
    }

    fun clearActionError() {
        _uiState.value = _uiState.value.copy(actionError = null)
    }

    // ═══════════════════════════════════════════
    // Socket
    // ═══════════════════════════════════════════

    fun connectSocket() {
        socketManager.onScreenEntered()
    }

    fun disconnectSocket() {
        socketManager.onScreenLeft()
    }

    private fun observeSocket() {
        socketCollectJob?.cancel()
        socketCollectJob = viewModelScope.launch {
            launch {
                socketManager.connectionState.collect { state ->
                    _uiState.value = _uiState.value.copy(
                        socketConnected = state == com.vinylstore.app.data.chat.SocketState.CONNECTED
                    )
                }
            }
            launch {
                socketManager.unreadCount.collect { count ->
                    _uiState.value = _uiState.value.copy(unreadTotal = count)
                }
            }
            launch {
                socketManager.newMessage.collect { msg ->
                    if (msg != null) {
                        // 收到新消息，刷新会话列表
                        loadConversations()
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        socketCollectJob?.cancel()
    }

    class Factory(
        private val chatRepository: ChatRepository,
        private val friendRepository: FriendRepository,
        private val userRepository: UserRepository,
        private val socketManager: ChatSocketManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MessagesViewModel::class.java)) {
                return MessagesViewModel(chatRepository, friendRepository, userRepository, socketManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

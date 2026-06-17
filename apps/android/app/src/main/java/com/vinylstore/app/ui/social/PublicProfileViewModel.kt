package com.vinylstore.app.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinylstore.app.data.model.Album
import com.vinylstore.app.data.model.PublicUserProfile
import com.vinylstore.app.data.repository.FriendRepository
import com.vinylstore.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PublicProfileTab { PURCHASES, FAVORITES }

data class PublicProfileUiState(
    val user: PublicUserProfile? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    // 好友状态
    val friendStatus: String = "none",  // none | pending_sent | pending_received | accepted
    val friendshipId: Int? = null,
    val friendLoading: Boolean = false,
    // 卖家
    val sellerAlbums: List<Album> = emptyList(),
    val sellerAlbumsLoading: Boolean = false,
    // 买家 Tab
    val activeTab: PublicProfileTab = PublicProfileTab.PURCHASES,
    val purchases: List<Album> = emptyList(),
    val purchasesLoading: Boolean = false,
    val purchasesVisible: Boolean = true,
    val favorites: List<Album> = emptyList(),
    val favoritesLoading: Boolean = false,
    val favoritesVisible: Boolean = true,
    // 操作
    val actionError: String? = null,
    val friendActionLoading: Boolean = false
)

class PublicProfileViewModel(
    private val userRepository: UserRepository,
    private val friendRepository: FriendRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PublicProfileUiState())
    val uiState: StateFlow<PublicProfileUiState> = _uiState.asStateFlow()

    private var profileUserId: Int = 0

    fun loadProfile(userId: Int) {
        if (profileUserId == userId && _uiState.value.user != null) return
        profileUserId = userId

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val user = userRepository.getPublicProfile(userId)
                _uiState.value = _uiState.value.copy(
                    user = user,
                    isLoading = false
                )

                // 检查好友状态
                checkFriendStatus()

                // 加载内容
                if (user.role == "SELLER") {
                    loadSellerAlbums(userId)
                } else {
                    loadPurchases(userId)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载失败"
                )
            }
        }
    }

    fun checkFriendStatus() {
        viewModelScope.launch {
            try {
                val status = friendRepository.getFriendshipStatus(profileUserId)
                val mappedStatus = when {
                    status.status == "pending" && status.isSender == true -> "pending_sent"
                    status.status == "pending" && status.isSender == false -> "pending_received"
                    else -> status.status
                }
                _uiState.value = _uiState.value.copy(
                    friendStatus = mappedStatus,
                    friendshipId = status.friendshipId
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(friendStatus = "none")
            }
        }
    }

    fun sendFriendRequest() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(friendActionLoading = true)
            try {
                friendRepository.sendFriendRequest(profileUserId)
                _uiState.value = _uiState.value.copy(
                    friendStatus = "pending_sent",
                    friendActionLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    friendActionLoading = false,
                    actionError = e.message ?: "发送失败"
                )
            }
        }
    }

    fun acceptFriendRequest() {
        val id = _uiState.value.friendshipId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(friendActionLoading = true)
            try {
                friendRepository.acceptFriendRequest(id)
                _uiState.value = _uiState.value.copy(
                    friendStatus = "accepted",
                    friendActionLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    friendActionLoading = false,
                    actionError = e.message ?: "操作失败"
                )
            }
        }
    }

    fun rejectFriendRequest() {
        val id = _uiState.value.friendshipId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(friendActionLoading = true)
            try {
                friendRepository.rejectFriendRequest(id)
                _uiState.value = _uiState.value.copy(
                    friendStatus = "none",
                    friendshipId = null,
                    friendActionLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    friendActionLoading = false,
                    actionError = e.message ?: "操作失败"
                )
            }
        }
    }

    fun loadSellerAlbums(userId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(sellerAlbumsLoading = true)
            try {
                val albums = userRepository.getPublicSellerAlbums(userId)
                _uiState.value = _uiState.value.copy(
                    sellerAlbums = albums,
                    sellerAlbumsLoading = false
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(sellerAlbumsLoading = false)
            }
        }
    }

    fun loadPurchases(userId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(purchasesLoading = true)
            try {
                val response = userRepository.getPublicPurchasesWithVisibility(userId)
                val albums = response.data ?: emptyList()
                val visible = response.visible ?: _uiState.value.user?.showPurchases ?: true
                _uiState.value = _uiState.value.copy(
                    purchases = if (visible) albums else emptyList(),
                    purchasesVisible = visible,
                    purchasesLoading = false
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(purchasesLoading = false)
            }
        }
    }

    fun loadFavorites(userId: Int) {
        if (_uiState.value.favorites.isNotEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(favoritesLoading = true)
            try {
                val response = userRepository.getPublicFavoritesWithVisibility(userId)
                val albums = response.data ?: emptyList()
                val visible = response.visible ?: _uiState.value.user?.showFavorites ?: true
                _uiState.value = _uiState.value.copy(
                    favorites = if (visible) albums else emptyList(),
                    favoritesVisible = visible,
                    favoritesLoading = false
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(favoritesLoading = false)
            }
        }
    }

    fun setActiveTab(tab: PublicProfileTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
        when (tab) {
            PublicProfileTab.PURCHASES -> loadPurchases(profileUserId)
            PublicProfileTab.FAVORITES -> loadFavorites(profileUserId)
        }
    }

    fun clearActionError() {
        _uiState.value = _uiState.value.copy(actionError = null)
    }

    class Factory(
        private val userRepository: UserRepository,
        private val friendRepository: FriendRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PublicProfileViewModel::class.java)) {
                return PublicProfileViewModel(userRepository, friendRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

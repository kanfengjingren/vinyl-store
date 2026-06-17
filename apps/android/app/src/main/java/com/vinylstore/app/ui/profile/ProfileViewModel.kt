package com.vinylstore.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinylstore.app.data.model.*
import com.vinylstore.app.data.repository.AlbumRepository
import com.vinylstore.app.data.repository.AuthRepository
import com.vinylstore.app.data.repository.PlayHistoryRepository
import com.vinylstore.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class ProfileTab(val label: String) {
    PROFILE("个人信息"),
    PURCHASES("已购专辑"),
    FAVORITES("我的收藏"),
    HISTORY("播放历史")
}

data class ProfileUiState(
    val activeTab: ProfileTab = ProfileTab.PROFILE,
    // Profile
    val profile: User? = null,
    val profileLoading: Boolean = false,
    // Recharge
    val rechargeAmount: String = "",
    val recharging: Boolean = false,
    val rechargeMsg: String? = null,
    val rechargeOk: Boolean = false,
    // Password
    val showPasswordFields: Boolean = false,
    val oldPassword: String = "",
    val newPassword: String = "",
    val pwdSaving: Boolean = false,
    val pwdMsg: String? = null,
    val pwdOk: Boolean = false,
    // Address
    val address: String = "",
    // Privacy
    val showPurchases: Boolean = true,
    val showFavorites: Boolean = true,
    // Save
    val saving: Boolean = false,
    val saved: Boolean = false,
    val avatarError: String? = null,
    val avatarUploading: Boolean = false,
    val avatarMsg: String? = null,
    // Purchases
    val purchases: List<Album> = emptyList(),
    val purchasesLoading: Boolean = false,
    // Favorites
    val favorites: List<FavoriteItem> = emptyList(),
    val favsLoading: Boolean = false,
    // Play history
    val playHistory: List<PlayHistoryItem> = emptyList(),
    val historyLoading: Boolean = false
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val albumRepository: AlbumRepository,
    private val playHistoryRepository: PlayHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init { loadProfile() }

    fun setTab(tab: ProfileTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
        loadTabData(tab)
    }

    // ── Profile ──

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(profileLoading = true)
            try {
                val user = authRepository.getProfile()
                _uiState.value = _uiState.value.copy(
                    profile = user,
                    profileLoading = false,
                    address = user.address ?: "",
                    showPurchases = user.showPurchases ?: true,
                    showFavorites = user.showFavorites ?: true
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(profileLoading = false)
            }
        }
    }

    private fun loadTabData(tab: ProfileTab) {
        when (tab) {
            ProfileTab.PURCHASES -> {
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(purchasesLoading = true)
                    try {
                        val list = userRepository.getMyPurchases()
                        _uiState.value = _uiState.value.copy(purchases = list, purchasesLoading = false)
                    } catch (_: Exception) {
                        _uiState.value = _uiState.value.copy(purchasesLoading = false)
                    }
                }
            }
            ProfileTab.FAVORITES -> {
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(favsLoading = true)
                    try {
                        val list = albumRepository.getFavorites()
                        _uiState.value = _uiState.value.copy(favorites = list, favsLoading = false)
                    } catch (_: Exception) {
                        _uiState.value = _uiState.value.copy(favsLoading = false)
                    }
                }
            }
            ProfileTab.HISTORY -> {
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(historyLoading = true)
                    try {
                        val list = playHistoryRepository.getPlayHistory(30)
                        _uiState.value = _uiState.value.copy(playHistory = list, historyLoading = false)
                    } catch (_: Exception) {
                        _uiState.value = _uiState.value.copy(historyLoading = false)
                    }
                }
            }
            else -> {}
        }
    }

    // ── Recharge ──

    fun updateRechargeAmount(v: String) { _uiState.value = _uiState.value.copy(rechargeAmount = v) }

    fun handleRecharge() {
        val amount = _uiState.value.rechargeAmount.toIntOrNull() ?: return
        if (amount <= 0) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(recharging = true, rechargeMsg = null)
            try {
                val res = userRepository.recharge(amount)
                _uiState.value = _uiState.value.copy(
                    recharging = false,
                    rechargeOk = true,
                    rechargeMsg = "充值成功！当前余额 ¥${res.balance}",
                    rechargeAmount = "",
                    profile = _uiState.value.profile?.copy(balance = res.balance)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    recharging = false,
                    rechargeOk = false,
                    rechargeMsg = e.message ?: "充值失败"
                )
            }
        }
    }

    // ── Password ──

    fun togglePasswordFields() {
        _uiState.value = _uiState.value.copy(
            showPasswordFields = !_uiState.value.showPasswordFields,
            oldPassword = "", newPassword = "", pwdMsg = null
        )
    }

    fun updateOldPwd(v: String) { _uiState.value = _uiState.value.copy(oldPassword = v) }
    fun updateNewPwd(v: String) { _uiState.value = _uiState.value.copy(newPassword = v) }

    fun handleChangePassword() {
        val s = _uiState.value
        if (s.oldPassword.isBlank() || s.newPassword.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(pwdSaving = true, pwdMsg = null)
            try {
                authRepository.changePassword(s.oldPassword, s.newPassword)
                _uiState.value = _uiState.value.copy(
                    pwdSaving = false, pwdOk = true,
                    pwdMsg = "密码修改成功",
                    oldPassword = "", newPassword = "",
                    showPasswordFields = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    pwdSaving = false, pwdOk = false,
                    pwdMsg = e.message ?: "修改失败"
                )
            }
        }
    }

    // ── Privacy ──

    fun togglePrivacy(key: String) {
        val s = _uiState.value
        _uiState.value = when (key) {
            "showPurchases" -> s.copy(showPurchases = !s.showPurchases)
            "showFavorites" -> s.copy(showFavorites = !s.showFavorites)
            else -> s
        }
    }

    // ── Address ──

    fun updateAddress(v: String) { _uiState.value = _uiState.value.copy(address = v) }

    // ── Avatar ──

    fun uploadAvatar(file: File) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(avatarUploading = true, avatarMsg = null)
            try {
                val uploadResp = userRepository.uploadAvatar(file)
                if (uploadResp.url.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(
                        avatarUploading = false,
                        avatarMsg = "头像上传失败：服务器未返回图片地址"
                    )
                    return@launch
                }
                val updatedUser = userRepository.updateAvatar(uploadResp.url)
                _uiState.value = _uiState.value.copy(
                    avatarUploading = false,
                    avatarMsg = "头像更新成功",
                    profile = _uiState.value.profile?.copy(avatar = updatedUser.avatar)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    avatarUploading = false,
                    avatarMsg = e.message ?: "头像上传失败"
                )
            }
        }
    }

    fun clearAvatarMsg() {
        _uiState.value = _uiState.value.copy(avatarMsg = null)
    }

    // ── Save ──

    fun save() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true, saved = false)
            try {
                val s = _uiState.value
                authRepository.updateProfile(defaultAddress = s.address.trim().ifEmpty { null })
                userRepository.updatePrivacy(
                    showPurchases = s.showPurchases,
                    showFavorites = s.showFavorites
                )
                _uiState.value = _uiState.value.copy(saving = false, saved = true)
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(saving = false)
            }
        }
    }

    // ── Logout ──

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val userRepository: UserRepository,
        private val albumRepository: AlbumRepository,
        private val playHistoryRepository: PlayHistoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                return ProfileViewModel(authRepository, userRepository, albumRepository, playHistoryRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

package com.vinylstore.app.data.repository

import com.google.gson.Gson
import com.vinylstore.app.data.api.ApiClient
import com.vinylstore.app.data.model.*
import com.vinylstore.app.local.TokenStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepository(private val tokenStorage: TokenStorage) {

    private val api get() = ApiClient.getApiService(tokenStorage)
    private val gson = Gson()

    /** 非阻塞的登录状态流，供 Compose 收集，避免 runBlocking 卡主线程 */
    val isLoggedInFlow: Flow<Boolean> = tokenStorage.tokenFlow.map { it != null }

    /** 当前用户 ID 流（从缓存的 userJson 解析） */
    val currentUserIdFlow: Flow<Int?> = tokenStorage.userJsonFlow.map { json ->
        json?.let {
            try { gson.fromJson(it, User::class.java).id } catch (_: Exception) { null }
        }
    }

    suspend fun login(email: String, password: String): LoginResponse {
        val response = api.login(LoginRequest(email, password))
        tokenStorage.saveAuth(response.token, gson.toJson(response.user))
        return response
    }

    suspend fun register(
        email: String,
        password: String,
        name: String? = null,
        role: String? = null,
        storeName: String? = null
    ): LoginResponse {
        val response = api.register(RegisterRequest(email, password, name, role, storeName))
        tokenStorage.saveAuth(response.token, gson.toJson(response.user))
        return response
    }

    suspend fun getMe(): User = api.getMe()

    suspend fun getProfile(): User = api.getProfile()

    suspend fun updateProfile(name: String? = null, defaultAddress: String? = null): User =
        api.updateProfile(UpdateProfileRequest(name, defaultAddress))

    suspend fun changePassword(oldPassword: String, newPassword: String): MessageResponse =
        api.changePassword(ChangePasswordRequest(oldPassword, newPassword))

    suspend fun forgotPassword(email: String): MessageResponse =
        api.forgotPassword(ForgotPasswordRequest(email))

    suspend fun resetPassword(email: String, code: String, newPassword: String): MessageResponse =
        api.resetPassword(ResetPasswordRequest(email, code, newPassword))

    suspend fun logout() {
        tokenStorage.clear()
        ApiClient.reset()
    }

    fun isLoggedIn(): Boolean = tokenStorage.getTokenSync() != null
}

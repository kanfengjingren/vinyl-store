package com.vinylstore.app.data.repository

import com.vinylstore.app.data.api.ApiClient
import com.vinylstore.app.data.model.LoginRequest
import com.vinylstore.app.data.model.LoginResponse
import com.vinylstore.app.data.model.RegisterRequest
import com.vinylstore.app.data.model.User
import com.vinylstore.app.local.TokenStorage
import com.google.gson.Gson

class AuthRepository(
    private val tokenStorage: TokenStorage,
    private val gson: Gson = Gson()
) {

    private val api get() = ApiClient.getApiService(tokenStorage)

    suspend fun login(email: String, password: String): LoginResponse {
        val response = api.login(LoginRequest(email, password))
        tokenStorage.saveAuth(response.token, gson.toJson(response.user))
        return response
    }

    suspend fun register(email: String, password: String, name: String?): LoginResponse {
        val response = api.register(RegisterRequest(email, password, name))
        tokenStorage.saveAuth(response.token, gson.toJson(response.user))
        return response
    }

    suspend fun logout() {
        tokenStorage.clear()
        ApiClient.reset()
    }

    suspend fun getProfile(): User {
        return api.getProfile()
    }

    fun isLoggedIn(): Boolean = tokenStorage.getTokenSync().isNotBlank()
}

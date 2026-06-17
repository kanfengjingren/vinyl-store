package com.vinylstore.app.data.repository

import com.vinylstore.app.data.api.ApiClient
import com.vinylstore.app.data.model.*
import com.vinylstore.app.local.TokenStorage
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class UserRepository(private val tokenStorage: TokenStorage) {

    private val api get() = ApiClient.getApiService(tokenStorage)

    suspend fun getMyPurchases(): List<Album> = api.getMyPurchases()

    suspend fun updateAvatar(avatarPath: String?): User =
        api.updateAvatar(UpdateAvatarRequest(avatarPath ?: ""))

    suspend fun uploadAvatar(file: File): UploadResponse {
        val mediaType = when {
            file.name.endsWith(".png", true) -> "image/png"
            file.name.endsWith(".webp", true) -> "image/webp"
            file.name.endsWith(".gif", true) -> "image/gif"
            else -> "image/jpeg"
        }.toMediaTypeOrNull()
        val requestBody = file.asRequestBody(mediaType)
        val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
        return api.uploadAvatar(part)
    }

    suspend fun updatePrivacy(showPurchases: Boolean? = null, showFavorites: Boolean? = null): User =
        api.updatePrivacy(UpdatePrivacyRequest(showPurchases, showFavorites))

    suspend fun recharge(amount: Int): RechargeResponse =
        api.recharge(RechargeRequest(amount))

    suspend fun getPublicProfile(userId: Int): PublicUserProfile =
        api.getPublicProfile(userId)

    suspend fun getPublicPurchases(userId: Int): List<Album> =
        api.getPublicPurchases(userId).data ?: emptyList()

    suspend fun getPublicPurchasesWithVisibility(userId: Int): PublicDataResponse<Album> =
        api.getPublicPurchases(userId)

    suspend fun getPublicFavorites(userId: Int): List<Album> =
        api.getPublicFavorites(userId).data?.map { it.album } ?: emptyList()

    suspend fun getPublicFavoritesWithVisibility(userId: Int): PublicDataResponse<Album> {
        val response = api.getPublicFavorites(userId)
        return PublicDataResponse(
            data = response.data?.map { it.album },
            visible = response.visible
        )
    }

    suspend fun getPublicSellerAlbums(userId: Int): List<Album> =
        api.getPublicSellerAlbums(userId).data ?: emptyList()

    suspend fun searchUsers(query: String): List<UserSearchResult> =
        api.searchUsers(query)
}

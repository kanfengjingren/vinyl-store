package com.vinylstore.app.data.api

import com.vinylstore.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @GET("albums")
    suspend fun getAlbums(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("category") category: String? = null,
        @Query("search") search: String? = null,
        @Query("sort") sort: String? = null,
        @Query("order") order: String? = null,
        @Query("country") country: String? = null,
        @Query("color") color: String? = null
    ): AlbumListResponse

    @GET("albums/{slug}")
    suspend fun getAlbumBySlug(@Path("slug") slug: String): Album

    @GET("categories")
    suspend fun getCategories(): List<Category>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): LoginResponse

    @GET("auth/profile")
    suspend fun getProfile(): User

    // ── Favorites ──────────────────────────────────────
    @POST("favorites")
    suspend fun toggleFavorite(@Body request: ToggleFavoriteRequest): ToggleFavoriteResponse

    @GET("favorites")
    suspend fun getFavorites(): List<FavoriteItem>
}

data class ToggleFavoriteRequest(val albumId: Int)
data class ToggleFavoriteResponse(val favorited: Boolean)
data class FavoriteItem(
    val id: Int,
    val album: Album
)

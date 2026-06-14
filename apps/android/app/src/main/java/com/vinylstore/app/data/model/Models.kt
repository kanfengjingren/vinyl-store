package com.vinylstore.app.data.model

import com.google.gson.annotations.SerializedName

data class Album(
    val id: Int,
    val artist: String,
    val title: String,
    val year: Int?,
    val label: String?,
    val country: String?,
    val price: Int,
    val badge: String?,
    val description: String,
    val coverUrl: String?,
    val gradient: String?,
    val color: String?,
    val slug: String,
    val stock: Int,
    val status: String,
    val sellerId: Int?,
    @SerializedName("artistId") val artistInfoId: Int?,
    val tracks: List<Track>?,
    val categories: List<Category>?,
    val artistInfo: Artist?,
    val seller: SellerBrief?
)

data class Track(
    val id: Int,
    val albumId: Int,
    val title: String,
    val duration: String?,
    val position: Int,
    val isSection: Boolean,
    val audioUrl: String?
)

data class Category(
    val id: Int,
    val name: String,
    val slug: String
)

data class Artist(
    val id: Int,
    val name: String,
    val slug: String,
    val photo: String?,
    val foundedYear: Int?,
    val country: String?,
    val description: String?
)

data class SellerBrief(
    val id: Int,
    val storeName: String
)

data class AlbumListResponse(
    val data: List<Album>,
    @SerializedName("pagination") val meta: PaginationMeta
)

data class PaginationMeta(
    val total: Int,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)

data class CategoryListResponse(
    val data: List<Category>
) {
    companion object {
        /** API 返回的是裸数组，造一个包装 */
        fun wrap(list: List<Category>) = CategoryListResponse(data = list)
    }
}

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val user: User
)

data class User(
    val id: Int,
    val email: String,
    val name: String?,
    val role: String,
    val balance: Int,
    @SerializedName("defaultAddress") val address: String?
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String?
)

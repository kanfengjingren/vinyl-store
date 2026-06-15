package com.vinylstore.app.data.repository

import com.vinylstore.app.data.api.ApiClient
import com.vinylstore.app.data.api.ToggleFavoriteRequest
import com.vinylstore.app.data.model.Album
import com.vinylstore.app.data.model.Category
import com.vinylstore.app.local.TokenStorage

class AlbumRepository(private val tokenStorage: TokenStorage) {

    private val api get() = ApiClient.getApiService(tokenStorage)

    suspend fun getAlbums(
        page: Int = 1,
        limit: Int = 20,
        category: String? = null,
        search: String? = null,
        sort: String? = null,
        order: String? = null
    ): List<Album> {
        val response = api.getAlbums(
            page = page,
            limit = limit,
            category = category,
            search = search,
            sort = sort,
            order = order
        )
        return response.data
    }

    suspend fun getAlbumBySlug(slug: String): Album {
        return api.getAlbumBySlug(slug)
    }

    suspend fun getCategories(): List<Category> {
        return api.getCategories()
    }

    suspend fun getFeatured(): Album? {
        val albums = getAlbums(page = 1, limit = 1, sort = "createdAt", order = "desc")
        return albums.firstOrNull()
    }

    suspend fun toggleFavorite(albumId: Int): Boolean {
        val response = api.toggleFavorite(ToggleFavoriteRequest(albumId))
        return response.favorited
    }

    suspend fun isFavorited(albumId: Int): Boolean {
        val favorites = api.getFavorites()
        return favorites.any { it.album.id == albumId }
    }
}

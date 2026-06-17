package com.vinylstore.app.data.repository

import com.vinylstore.app.data.api.ApiClient
import com.vinylstore.app.data.model.*
import com.vinylstore.app.local.TokenStorage

class AlbumRepository(private val tokenStorage: TokenStorage) {

    private val api get() = ApiClient.getApiService(tokenStorage)

    suspend fun getAlbums(
        page: Int = 1,
        limit: Int = 20,
        category: String? = null,
        country: String? = null,
        color: String? = null,
        search: String? = null,
        sort: String? = null,
        order: String? = null,
        date: String? = null
    ): AlbumListResponse = api.getAlbums(page, limit, category, country, color, search, sort, order, date)

    suspend fun getHotAlbums(limit: Int = 12): List<HotAlbum> = api.getHotAlbums(limit).data

    suspend fun getRecommendations(limit: Int = 12): List<Album> = api.getRecommendations(limit).data

    suspend fun getAlbumBySlug(slug: String): Album = api.getAlbumBySlug(slug)

    suspend fun getFeatured(): Album? {
        // 先取首页获取总数
        val first = api.getAlbums(page = 1, limit = 20)
        val totalPages = first.pagination?.totalPages ?: 1
        if (first.data.isEmpty()) return null

        // 从随机页中随机选一张
        val randomPage = if (totalPages <= 1) 1 else (1..totalPages).random()
        val pageData = if (randomPage == 1) first
        else api.getAlbums(page = randomPage, limit = 20)
        return pageData.data.ifEmpty { null }?.random()
    }

    suspend fun getCountries(): List<String> = api.getCountries()

    suspend fun getSuggestions(query: String): List<AlbumSuggestion> = api.getSuggestions(query)

    suspend fun getColors(): List<ColorOption> = api.getColors()

    suspend fun getCategories(): List<Category> = api.getCategories()

    suspend fun toggleFavorite(albumId: Int): Boolean {
        val response = api.toggleFavorite(ToggleFavoriteRequest(albumId))
        return response.favorited
    }

    suspend fun getFavorites(): List<FavoriteItem> = api.getFavorites()

    suspend fun isFavorited(albumId: Int): Boolean {
        val favs = api.getFavorites()
        return favs.any { it.album.id == albumId }
    }

    suspend fun getAlbumRating(albumId: Int): RatingResponse = api.getAlbumRating(albumId)

    suspend fun rateAlbum(albumId: Int, score: Int): RatingResponse =
        api.rateAlbum(albumId, RateAlbumRequest(score))
}

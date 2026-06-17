package com.vinylstore.app.data.repository

import com.vinylstore.app.data.api.ApiClient
import com.vinylstore.app.data.model.Artist
import com.vinylstore.app.local.TokenStorage

class ArtistRepository(private val tokenStorage: TokenStorage) {

    private val api get() = ApiClient.getApiService(tokenStorage)

    suspend fun getArtists(): List<Artist> = api.getArtists()

    suspend fun searchArtists(query: String): List<Artist> = api.searchArtists(query)

    suspend fun getArtistBySlug(slug: String): Artist = api.getArtistBySlug(slug)
}

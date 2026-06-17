package com.vinylstore.app.data.repository

import com.vinylstore.app.data.api.ApiClient
import com.vinylstore.app.data.model.MessageResponse
import com.vinylstore.app.data.model.PlayHistoryItem
import com.vinylstore.app.data.model.RecordPlayRequest
import com.vinylstore.app.local.TokenStorage

class PlayHistoryRepository(private val tokenStorage: TokenStorage) {

    private val api get() = ApiClient.getApiService(tokenStorage)

    suspend fun recordPlay(trackId: Int?, albumId: Int?): MessageResponse =
        api.recordPlay(RecordPlayRequest(trackId, albumId))

    suspend fun getPlayHistory(limit: Int = 20): List<PlayHistoryItem> =
        api.getPlayHistory(limit)
}

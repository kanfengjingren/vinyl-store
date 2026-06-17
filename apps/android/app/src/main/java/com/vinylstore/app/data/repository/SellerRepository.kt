package com.vinylstore.app.data.repository

import com.vinylstore.app.data.api.ApiClient
import com.vinylstore.app.data.model.SellerDetail
import com.vinylstore.app.local.TokenStorage

class SellerRepository(private val tokenStorage: TokenStorage) {

    private val api get() = ApiClient.getApiService(tokenStorage)

    suspend fun getSellerById(id: Int): SellerDetail = api.getSellerById(id)
}

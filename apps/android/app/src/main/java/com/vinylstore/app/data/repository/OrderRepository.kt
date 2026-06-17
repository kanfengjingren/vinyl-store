package com.vinylstore.app.data.repository

import com.vinylstore.app.data.api.ApiClient
import com.vinylstore.app.data.model.*
import com.vinylstore.app.local.TokenStorage

class OrderRepository(private val tokenStorage: TokenStorage) {

    private val api get() = ApiClient.getApiService(tokenStorage)

    suspend fun checkout(shippingAddress: String): Order =
        api.checkout(CheckoutRequest(shippingAddress))

    suspend fun getOrders(): List<Order> = api.getOrders()

    suspend fun getOrderById(id: Int): Order = api.getOrderById(id)

    suspend fun cancelOrder(id: Int): Order = api.cancelOrder(id)

    suspend fun payOrder(id: Int): Order = api.payOrder(id)
}

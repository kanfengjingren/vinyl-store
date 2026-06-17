package com.vinylstore.app.data.repository

import com.vinylstore.app.data.api.ApiClient
import com.vinylstore.app.data.model.*
import com.vinylstore.app.local.TokenStorage

class CartRepository(private val tokenStorage: TokenStorage) {

    private val api get() = ApiClient.getApiService(tokenStorage)

    suspend fun getCart(): CartResponse = api.getCart()

    suspend fun addToCart(albumId: Int, quantity: Int = 1): CartItem =
        api.addToCart(AddToCartRequest(albumId, quantity))

    suspend fun updateCartItem(id: Int, quantity: Int): CartItem =
        api.updateCartItem(id, UpdateCartItemRequest(quantity))

    suspend fun removeCartItem(id: Int): MessageResponse = api.removeCartItem(id)
}

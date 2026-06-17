package com.vinylstore.app.data.repository

import com.vinylstore.app.data.api.ApiClient
import com.vinylstore.app.data.model.*
import com.vinylstore.app.local.TokenStorage

class FriendRepository(private val tokenStorage: TokenStorage) {

    private val api get() = ApiClient.getApiService(tokenStorage)

    suspend fun sendFriendRequest(receiverId: Int): MessageResponse =
        api.sendFriendRequest(SendFriendRequest(receiverId))

    suspend fun acceptFriendRequest(friendshipId: Int): MessageResponse =
        api.acceptFriendRequest(friendshipId)

    suspend fun rejectFriendRequest(friendshipId: Int): MessageResponse =
        api.rejectFriendRequest(friendshipId)

    suspend fun getFriends(): List<Friend> = api.getFriends()

    suspend fun getPendingRequests(): List<Friend> = api.getPendingRequests()

    suspend fun getFriendshipStatus(userId: Int): FriendshipStatus =
        api.getFriendshipStatus(userId)
}

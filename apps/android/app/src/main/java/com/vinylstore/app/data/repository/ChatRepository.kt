package com.vinylstore.app.data.repository

import com.vinylstore.app.data.api.ApiClient
import com.vinylstore.app.data.model.*
import com.vinylstore.app.local.TokenStorage
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ChatRepository(private val tokenStorage: TokenStorage) {

    private val api get() = ApiClient.getApiService(tokenStorage)

    suspend fun getConversations(): List<Conversation> = api.getConversations()

    suspend fun getMessages(partnerId: Int): List<Message> = api.getMessages(partnerId)

    suspend fun markMessagesRead(partnerId: Int): MessageResponse =
        api.markMessagesRead(partnerId)

    suspend fun getUnreadCount(): UnreadCountResponse {
        val raw = api.getUnreadCountRaw().string().trim()
        val count = raw.toIntOrNull() ?: 0
        return UnreadCountResponse(count)
    }

    suspend fun markAllMessagesRead(): MessageResponse = api.markAllMessagesRead()

    suspend fun uploadChatImage(file: File): UploadResponse {
        val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
        return api.uploadChatImage(part)
    }
}

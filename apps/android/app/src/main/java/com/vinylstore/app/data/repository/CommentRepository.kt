package com.vinylstore.app.data.repository

import com.vinylstore.app.data.api.ApiClient
import com.vinylstore.app.data.model.*
import com.vinylstore.app.local.TokenStorage

class CommentRepository(private val tokenStorage: TokenStorage) {

    private val api get() = ApiClient.getApiService(tokenStorage)

    suspend fun getComments(albumId: Int, page: Int = 1, limit: Int = 10): CommentListResponse =
        api.getComments(albumId, page, limit)

    suspend fun getReplies(commentId: Int): List<CommentReply> =
        api.getReplies(commentId)

    suspend fun createComment(albumId: Int, content: String, parentId: Int? = null): Comment =
        api.createComment(albumId, CreateCommentRequest(content, parentId))

    suspend fun deleteComment(commentId: Int): MessageResponse =
        api.deleteComment(commentId)
}

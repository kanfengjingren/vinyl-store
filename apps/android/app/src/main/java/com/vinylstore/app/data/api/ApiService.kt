package com.vinylstore.app.data.api

import com.vinylstore.app.data.model.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ═══════════════════════════════════════════
    // 专辑
    // ═══════════════════════════════════════════

    @GET("albums")
    suspend fun getAlbums(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("category") category: String? = null,
        @Query("country") country: String? = null,
        @Query("color") color: String? = null,
        @Query("search") search: String? = null,
        @Query("sort") sort: String? = null,
        @Query("order") order: String? = null,
        @Query("date") date: String? = null
    ): AlbumListResponse

    @GET("albums/hot")
    suspend fun getHotAlbums(@Query("limit") limit: Int = 12): HotAlbumsResponse

    @GET("albums/recommendations")
    suspend fun getRecommendations(@Query("limit") limit: Int = 12): RecommendationsResponse

    @GET("albums/{slug}")
    suspend fun getAlbumBySlug(@Path("slug") slug: String): Album

    @GET("albums/countries")
    suspend fun getCountries(): List<String>

    @GET("albums/suggest")
    suspend fun getSuggestions(@Query("q") q: String): List<AlbumSuggestion>

    @GET("albums/colors")
    suspend fun getColors(): List<ColorOption>

    // ═══════════════════════════════════════════
    // 分类
    // ═══════════════════════════════════════════

    @GET("categories")
    suspend fun getCategories(): List<Category>

    // ═══════════════════════════════════════════
    // 认证
    // ═══════════════════════════════════════════

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): LoginResponse

    @GET("auth/me")
    suspend fun getMe(): User

    @GET("auth/profile")
    suspend fun getProfile(): User

    @PATCH("auth/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): User

    @PATCH("auth/password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): MessageResponse

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): MessageResponse

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): MessageResponse

    // ═══════════════════════════════════════════
    // 用户
    // ═══════════════════════════════════════════

    @GET("users/me/purchases")
    suspend fun getMyPurchases(): List<Album>

    @PATCH("users/me/avatar")
    suspend fun updateAvatar(@Body request: UpdateAvatarRequest): User

    @PATCH("users/me/privacy")
    suspend fun updatePrivacy(@Body request: UpdatePrivacyRequest): User

    @POST("users/recharge")
    suspend fun recharge(@Body request: RechargeRequest): RechargeResponse

    @GET("users/{id}/profile")
    suspend fun getPublicProfile(@Path("id") userId: Int): PublicUserProfile

    @GET("users/{id}/purchases")
    suspend fun getPublicPurchases(@Path("id") userId: Int): PublicDataResponse<Album>

    @GET("users/{id}/favorites")
    suspend fun getPublicFavorites(@Path("id") userId: Int): PublicDataResponse<FavoriteItem>

    @GET("users/{id}/seller-albums")
    suspend fun getPublicSellerAlbums(@Path("id") userId: Int): PublicDataResponse<Album>

    @GET("users/search")
    suspend fun searchUsers(@Query("q") q: String): List<UserSearchResult>

    // ═══════════════════════════════════════════
    // 购物车
    // ═══════════════════════════════════════════

    @GET("cart")
    suspend fun getCart(): CartResponse

    @POST("cart/items")
    suspend fun addToCart(@Body request: AddToCartRequest): CartItem

    @PATCH("cart/items/{id}")
    suspend fun updateCartItem(
        @Path("id") id: Int,
        @Body request: UpdateCartItemRequest
    ): CartItem

    @DELETE("cart/items/{id}")
    suspend fun removeCartItem(@Path("id") id: Int): MessageResponse

    // ═══════════════════════════════════════════
    // 订单
    // ═══════════════════════════════════════════

    @POST("orders")
    suspend fun checkout(@Body request: CheckoutRequest): Order

    @GET("orders")
    suspend fun getOrders(): List<Order>

    @GET("orders/{id}")
    suspend fun getOrderById(@Path("id") id: Int): Order

    @PATCH("orders/{id}/cancel")
    suspend fun cancelOrder(@Path("id") id: Int): Order

    @PATCH("orders/{id}/pay")
    suspend fun payOrder(@Path("id") id: Int): Order

    // ═══════════════════════════════════════════
    // 收藏
    // ═══════════════════════════════════════════

    @POST("favorites")
    suspend fun toggleFavorite(@Body request: ToggleFavoriteRequest): ToggleFavoriteResponse

    @GET("favorites")
    suspend fun getFavorites(): List<FavoriteItem>

    // ═══════════════════════════════════════════
    // 评分
    // ═══════════════════════════════════════════

    @GET("albums/{albumId}/rating")
    suspend fun getAlbumRating(@Path("albumId") albumId: Int): RatingResponse

    @POST("albums/{albumId}/rating")
    suspend fun rateAlbum(
        @Path("albumId") albumId: Int,
        @Body request: RateAlbumRequest
    ): RatingResponse

    // ═══════════════════════════════════════════
    // 播放历史
    // ═══════════════════════════════════════════

    @POST("play-history")
    suspend fun recordPlay(@Body request: RecordPlayRequest): MessageResponse

    @GET("play-history")
    suspend fun getPlayHistory(@Query("limit") limit: Int = 20): List<PlayHistoryItem>

    // ═══════════════════════════════════════════
    // 评论
    // ═══════════════════════════════════════════

    @GET("albums/{albumId}/comments")
    suspend fun getComments(
        @Path("albumId") albumId: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): CommentListResponse

    @GET("comments/{commentId}/replies")
    suspend fun getReplies(@Path("commentId") commentId: Int): List<CommentReply>

    @POST("albums/{albumId}/comments")
    suspend fun createComment(
        @Path("albumId") albumId: Int,
        @Body request: CreateCommentRequest
    ): Comment

    @DELETE("comments/{commentId}")
    suspend fun deleteComment(@Path("commentId") commentId: Int): MessageResponse

    // ═══════════════════════════════════════════
    // 聊天
    // ═══════════════════════════════════════════

    @GET("chat/conversations")
    suspend fun getConversations(): List<Conversation>

    @GET("chat/messages/{partnerId}")
    suspend fun getMessages(@Path("partnerId") partnerId: Int): List<Message>

    @PATCH("chat/read/{partnerId}")
    suspend fun markMessagesRead(@Path("partnerId") partnerId: Int): MessageResponse

    @GET("chat/unread-count")
    suspend fun getUnreadCountRaw(): ResponseBody

    @PATCH("chat/read-all")
    suspend fun markAllMessagesRead(): MessageResponse

    // ═══════════════════════════════════════════
    // 好友
    // ═══════════════════════════════════════════

    @POST("friends/request")
    suspend fun sendFriendRequest(@Body request: SendFriendRequest): MessageResponse

    @PATCH("friends/{friendshipId}/accept")
    suspend fun acceptFriendRequest(@Path("friendshipId") friendshipId: Int): MessageResponse

    @PATCH("friends/{friendshipId}/reject")
    suspend fun rejectFriendRequest(@Path("friendshipId") friendshipId: Int): MessageResponse

    @GET("friends")
    suspend fun getFriends(): List<Friend>

    @GET("friends/pending")
    suspend fun getPendingRequests(): List<Friend>

    @GET("friends/status/{userId}")
    suspend fun getFriendshipStatus(@Path("userId") userId: Int): FriendshipStatus

    // ═══════════════════════════════════════════
    // 艺人
    // ═══════════════════════════════════════════

    @GET("artists")
    suspend fun getArtists(): List<Artist>

    @GET("artists/search")
    suspend fun searchArtists(@Query("q") q: String): List<Artist>

    @GET("artists/{slug}")
    suspend fun getArtistBySlug(@Path("slug") slug: String): Artist

    // ═══════════════════════════════════════════
    // 卖家
    // ═══════════════════════════════════════════

    @GET("sellers/{id}")
    suspend fun getSellerById(@Path("id") id: Int): SellerDetail

    // ═══════════════════════════════════════════
    // 上传
    // ═══════════════════════════════════════════

    @Multipart
    @POST("upload/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): UploadResponse

    @Multipart
    @POST("upload/chat-image")
    suspend fun uploadChatImage(@Part file: MultipartBody.Part): UploadResponse
}

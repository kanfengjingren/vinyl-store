package com.vinylstore.app.data.model

import com.google.gson.annotations.SerializedName

// ═══════════════════════════════════════════
// 分页
// ═══════════════════════════════════════════

data class PaginationMeta(
    val total: Int,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)

// ═══════════════════════════════════════════
// 专辑 / 曲目 / 分类
// ═══════════════════════════════════════════

data class Album(
    val id: Int,
    val artist: String,
    val title: String,
    val year: Int?,
    val label: String?,
    val country: String?,
    val price: Int,
    val badge: String?,
    val description: String?,
    val coverUrl: String?,
    val gradient: String?,
    val color: String?,
    val slug: String,
    val stock: Int,
    val status: String,
    val sellerId: Int?,
    @SerializedName("artistId") val artistInfoId: Int?,
    val tracks: List<Track>?,
    val categories: List<Category>?,
    val artistInfo: Artist?,
    val seller: SellerBrief?
)

data class Track(
    val id: Int,
    val albumId: Int,
    val title: String,
    val duration: String?,
    val position: Int,
    val isSection: Boolean = false,
    val audioUrl: String?
)

data class Category(
    val id: Int,
    val name: String,
    val slug: String
)

data class AlbumListResponse(
    val data: List<Album>,
    val pagination: PaginationMeta?
)

data class HotAlbum(
    val id: Int,
    val artist: String,
    val title: String,
    val price: Int,
    val coverUrl: String?,
    val gradient: String?,
    val slug: String,
    @SerializedName("hotSales") val salesCount: Int?,
    val rank: Int?
)

data class HotAlbumsResponse(val data: List<HotAlbum>)
data class RecommendationsResponse(val data: List<Album>)

data class AlbumSuggestion(
    val slug: String,
    val title: String,
    val artist: String,
    val coverUrl: String?
)

// ═══════════════════════════════════════════
// 用户 / 认证
// ═══════════════════════════════════════════

data class User(
    val id: Int,
    val email: String,
    val name: String?,
    val role: String,
    val balance: Int = 0,
    @SerializedName("defaultAddress") val address: String?,
    val avatar: String?,
    val showPurchases: Boolean?,
    val showFavorites: Boolean?
)

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String? = null,
    val role: String? = null,
    val storeName: String? = null
)
data class LoginResponse(val user: User, val token: String)

data class ForgotPasswordRequest(val email: String)
data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val newPassword: String
)
data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)

data class UpdateProfileRequest(
    val name: String? = null,
    val defaultAddress: String? = null
)
data class UpdatePrivacyRequest(
    val showPurchases: Boolean? = null,
    val showFavorites: Boolean? = null
)
data class UpdateAvatarRequest(val avatar: String?)

data class PublicUserProfile(
    val id: Int,
    val email: String?,
    val name: String?,
    val role: String,
    val avatar: String?,
    val seller: PublicSellerInfo?,
    val showPurchases: Boolean?,
    val showFavorites: Boolean?,
    val createdAt: String?
) {
    /** 从嵌套 seller 对象解析店铺名 */
    val storeName: String? get() = seller?.storeName
    val sellerApproved: Boolean? get() = seller?.status == "APPROVED"
}

data class PublicSellerInfo(
    val storeName: String?,
    val description: String?,
    val status: String?
)

/** 公开已购/收藏响应包装（服务端返回 { data, visible }） */
data class PublicDataResponse<T>(
    val data: List<T>?,
    val visible: Boolean?
)

data class UserSearchResult(
    val id: Int,
    val name: String?,
    val email: String? = null,
    val avatar: String?
)

// ═══════════════════════════════════════════
// 购物车
// ═══════════════════════════════════════════

data class CartItem(
    val id: Int,
    val albumId: Int,
    val quantity: Int,
    val album: Album?
)

data class CartResponse(
    val items: List<CartItem>,
    val total: Int
)

data class AddToCartRequest(val albumId: Int, val quantity: Int = 1)
data class UpdateCartItemRequest(val quantity: Int)

// ═══════════════════════════════════════════
// 订单
// ═══════════════════════════════════════════

data class Order(
    val id: Int,
    val userId: Int,
    val status: String,
    val shippingAddress: String?,
    @SerializedName("totalAmount") val totalAmount: Int = 0,
    val expiresAt: String?,
    val createdAt: String,
    val items: List<OrderItem>?,
    val user: User?
)

data class OrderItem(
    val id: Int,
    val orderId: Int,
    val albumId: Int?,
    val album: Album?,
    val quantity: Int,
    @SerializedName("unitPrice") val unitPrice: Int = 0,
    val status: String? = null,
    val refunded: Boolean?,
    val refundedAt: String?
)

data class CheckoutRequest(val shippingAddress: String)

// ═══════════════════════════════════════════
// 评论
// ═══════════════════════════════════════════

data class Comment(
    val id: Int,
    val albumId: Int,
    val userId: Int,
    val user: CommentUser?,
    val content: String,
    val createdAt: String,
    val replies: List<CommentReply>?,
    val replyCount: Int?,
    @SerializedName("_count") val count: CommentCount?
) {
    /** 服务端返回 _count.replies，优先取该值 */
    val totalReplyCount: Int
        get() = replyCount ?: count?.replies ?: (replies?.size ?: 0)
}

data class CommentCount(val replies: Int?)

data class CommentReply(
    val id: Int,
    val commentId: Int,
    val userId: Int,
    val user: CommentUser?,
    val content: String,
    val createdAt: String
)

data class CommentUser(
    val id: Int,
    val name: String?,
    val email: String,
    val avatar: String?
)

data class CreateCommentRequest(
    val content: String,
    val parentId: Int? = null
)

data class CommentListResponse(
    val data: List<Comment>,
    val pagination: PaginationMeta?
)

// ═══════════════════════════════════════════
// 聊天
// ═══════════════════════════════════════════

data class Conversation(
    @SerializedName("partner") val partner: ConversationPartner?,
    @SerializedName("lastMsg") val lastMessage: Message?,
    @SerializedName("unreadCount") val unreadCount: Int
) {
    val partnerId: Int get() = partner?.id ?: 0
}

data class ConversationPartner(
    val id: Int,
    val name: String?,
    val email: String? = null,
    val avatar: String?,
    val storeName: String? = null
)

data class Message(
    val id: Int? = null,
    val senderId: Int? = null,
    val receiverId: Int? = null,
    val content: String?,
    val type: String? = null,
    val imageUrl: String? = null,
    val createdAt: String?
)

data class UnreadCountResponse(val count: Int)

// ═══════════════════════════════════════════
// 好友
// ═══════════════════════════════════════════

data class Friend(
    @SerializedName("friend") val user: FriendUser?,
    @SerializedName("friendshipId") val friendshipId: Int?,
    @SerializedName("createdAt") val createdAt: String?
) {
    // Keep for backwards compat — not sent by server
    val id: Int get() = user?.id ?: 0
    val status: String get() = "accepted"
}

data class FriendUser(
    val id: Int,
    val name: String?,
    val email: String? = null,
    val avatar: String?
)

data class FriendshipStatus(
    val status: String,  // "none" | "pending" | "accepted" | "rejected"
    val friendshipId: Int?,
    val isSender: Boolean? = null  // true = current user sent the pending request
)

data class SendFriendRequest(val receiverId: Int)

// ═══════════════════════════════════════════
// 艺人 / 乐队
// ═══════════════════════════════════════════

data class Artist(
    val id: Int?,
    val name: String?,
    val slug: String?,
    val country: String?,
    val foundedYear: Int?,
    val description: String?,
    val photoUrl: String?,
    val albums: List<Album>?,
    val albumCount: Int?
)

// ═══════════════════════════════════════════
// 卖家
// ═══════════════════════════════════════════

data class SellerBrief(
    val id: Int,
    val userId: Int?,
    val storeName: String?
)

data class SellerDetail(
    val id: Int,
    val userId: Int,
    val storeName: String,
    val contactEmail: String? = null,
    val contactPhone: String?,
    val description: String?,
    val approved: Boolean?,
    val user: User?,
    val albums: List<Album>?
)

// ═══════════════════════════════════════════
// 收藏 / 评分 / 播放记录
// ═══════════════════════════════════════════

data class ToggleFavoriteRequest(val albumId: Int)
data class ToggleFavoriteResponse(val favorited: Boolean)
data class FavoriteItem(
    val id: Int,
    val album: Album
)

data class RatingResponse(
    @SerializedName("avgScore") val average: Float?,
    val count: Int,
    @SerializedName("userRating") val userScore: Int?
)
data class RateAlbumRequest(val score: Int)  // 1-5

data class PlayHistoryItem(
    val id: Int,
    val trackId: Int?,
    val albumId: Int?,
    val track: Track?,
    val album: Album?,
    val playedAt: String?
)
data class RecordPlayRequest(val trackId: Int?, val albumId: Int?)

// ═══════════════════════════════════════════
// 钱包
// ═══════════════════════════════════════════

data class RechargeRequest(val amount: Int)
data class RechargeResponse(val balance: Int)

// ═══════════════════════════════════════════
// 上传
// ═══════════════════════════════════════════

data class UploadResponse(val url: String?)

// ═══════════════════════════════════════════
// 颜色 / 国家（简单列表）
// ═══════════════════════════════════════════

data class ColorOption(val label: String, val name: String, val hex: String)

// ═══════════════════════════════════════════
// 通用消息响应
// ═══════════════════════════════════════════

data class MessageResponse(val message: String)

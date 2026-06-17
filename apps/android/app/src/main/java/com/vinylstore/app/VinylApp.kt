package com.vinylstore.app

import android.app.Application
import android.util.Log
import com.vinylstore.app.data.repository.*
import com.vinylstore.app.data.chat.ChatSocketManager
import com.vinylstore.app.local.TokenStorage
import com.vinylstore.app.player.AudioPlayerManager

class VinylApp : Application() {

    lateinit var tokenStorage: TokenStorage
        private set

    lateinit var albumRepository: AlbumRepository
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var cartRepository: CartRepository
        private set

    lateinit var orderRepository: OrderRepository
        private set

    lateinit var userRepository: UserRepository
        private set

    lateinit var commentRepository: CommentRepository
        private set

    lateinit var chatRepository: ChatRepository
        private set

    lateinit var friendRepository: FriendRepository
        private set

    lateinit var artistRepository: ArtistRepository
        private set

    lateinit var sellerRepository: SellerRepository
        private set

    lateinit var playHistoryRepository: PlayHistoryRepository
        private set

    /** 懒加载：延迟到首次使用时才创建 ExoPlayer，避免 Application.onCreate 时阻塞/崩溃 */
    val playerManager: AudioPlayerManager by lazy { AudioPlayerManager(this) }

    /** 懒加载：首次进入消息页面时创建 Socket.IO 连接 */
    val chatSocketManager: ChatSocketManager by lazy { ChatSocketManager(tokenStorage) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            tokenStorage = TokenStorage(this)
            albumRepository = AlbumRepository(tokenStorage)
            authRepository = AuthRepository(tokenStorage)
            cartRepository = CartRepository(tokenStorage)
            orderRepository = OrderRepository(tokenStorage)
            userRepository = UserRepository(tokenStorage)
            commentRepository = CommentRepository(tokenStorage)
            chatRepository = ChatRepository(tokenStorage)
            friendRepository = FriendRepository(tokenStorage)
            artistRepository = ArtistRepository(tokenStorage)
            sellerRepository = SellerRepository(tokenStorage)
            playHistoryRepository = PlayHistoryRepository(tokenStorage)
            // playerManager 延迟到首次播放时创建，不在这里初始化
        } catch (e: Exception) {
            Log.e("VinylApp", "onCreate init failed", e)
            throw e
        }
    }

    companion object {
        lateinit var instance: VinylApp
            private set
    }
}

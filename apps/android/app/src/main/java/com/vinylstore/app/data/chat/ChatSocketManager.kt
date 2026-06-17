package com.vinylstore.app.data.chat

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.vinylstore.app.BuildConfig
import com.vinylstore.app.local.TokenStorage
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.net.URI

enum class SocketState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

class ChatSocketManager(private val tokenStorage: TokenStorage) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val _connectionState = MutableStateFlow(SocketState.DISCONNECTED)
    val connectionState: StateFlow<SocketState> = _connectionState.asStateFlow()

    private val _newMessage = MutableStateFlow<JSONObject?>(null)
    val newMessage: StateFlow<JSONObject?> = _newMessage.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private var socket: Socket? = null
    private var activeScreenCount = 0

    fun onScreenEntered() {
        activeScreenCount++
        if (activeScreenCount == 1) {
            connect()
        }
    }

    fun onScreenLeft() {
        activeScreenCount = (activeScreenCount - 1).coerceAtLeast(0)
        if (activeScreenCount == 0) {
            disconnect()
        }
    }

    private fun connect() {
        if (socket?.connected() == true) return
        val token = tokenStorage.getTokenSync() ?: return

        try {
            val baseUrl = BuildConfig.API_BASE_URL
                .replace("/api", "")
                .trimEnd('/')

            val opts = IO.Options().apply {
                auth = mapOf("token" to token)
                transports = arrayOf("websocket", "polling")
                timeout = 15000
            }

            socket = IO.socket(URI.create("$baseUrl/chat"), opts).also { s ->
                s.on(Socket.EVENT_CONNECT) {
                    runOnMain {
                        _connectionState.value = SocketState.CONNECTED
                        Log.d("ChatSocket", "Connected to /chat")
                    }
                }
                s.on(Socket.EVENT_CONNECT_ERROR) { args ->
                    val err = args.getOrNull(0)
                    runOnMain {
                        _connectionState.value = SocketState.ERROR
                        Log.e("ChatSocket", "Connect error: $err")
                    }
                }
                s.on(Socket.EVENT_DISCONNECT) {
                    runOnMain {
                        _connectionState.value = SocketState.DISCONNECTED
                        Log.d("ChatSocket", "Disconnected")
                    }
                }
                s.on("newMessage") { args ->
                    val msg = args.getOrNull(0)
                    if (msg is JSONObject) {
                        runOnMain {
                            _newMessage.value = msg
                        }
                    }
                }
                // Also handle the message ack as newMessage might come via ack too
                s.on("unreadCount") { args ->
                    val count = (args.getOrNull(0) as? Number)?.toInt() ?: 0
                    runOnMain {
                        _unreadCount.value = count
                    }
                }
            }

            _connectionState.value = SocketState.CONNECTING
            socket!!.connect()
        } catch (e: Exception) {
            _connectionState.value = SocketState.ERROR
            Log.e("ChatSocket", "Failed to create socket", e)
        }
    }

    private fun disconnect() {
        Log.d("ChatSocket", "Disconnecting socket")
        socket?.disconnect()
        socket?.off()
        socket = null
        runOnMain {
            _connectionState.value = SocketState.DISCONNECTED
        }
    }

    /**
     * Send a message via the socket.
     * @param receiverId target user ID
     * @param content text content (null for image-only messages)
     * @param imageUrl uploaded image URL (null for text-only messages)
     * @param ackCallback delivers the server-confirmed message JSON
     */
    fun sendMessage(
        receiverId: Int,
        content: String? = null,
        imageUrl: String? = null,
        ackCallback: ((JSONObject) -> Unit)? = null
    ) {
        val payload = JSONObject().apply {
            put("receiverId", receiverId)
            if (!content.isNullOrBlank()) put("content", content)
            if (!imageUrl.isNullOrBlank()) put("imageUrl", imageUrl)
        }
        socket?.emit("sendMessage", payload, io.socket.client.Ack { response ->
            if (response != null && response.isNotEmpty()) {
                val ack = response[0] as? JSONObject
                if (ack != null) {
                    runOnMain { ackCallback?.invoke(ack) }
                }
            }
        })
    }

    fun requestUnreadCount() {
        socket?.emit("unreadCount")
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }
}

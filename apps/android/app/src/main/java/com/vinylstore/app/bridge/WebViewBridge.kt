package com.vinylstore.app.bridge

import android.util.Log
import android.webkit.JavascriptInterface
import com.google.gson.Gson
import com.vinylstore.app.local.TokenStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * WebView ↔ Native 通信桥。
 *
 * 前端调用方式（需要前端配合修改）：
 *   window.AndroidBridge?.setToken("jwt...")
 *   window.AndroidBridge?.navigateTo("album", "{slug}")
 *   window.AndroidBridge?.openNativeAlbum("some-slug")
 */
class WebViewBridge(
    private val tokenStorage: TokenStorage,
    private val onNavigateNative: (path: String, params: Map<String, String>) -> Unit = { _, _ -> },
    var onTitleChanged: (String) -> Unit = {},
    var onPlaybackChanged: (trackTitle: String, artistName: String, coverUrl: String?, gradient: String?, isPlaying: Boolean, currentSeconds: Float, duration: Float) -> Unit = { _, _, _, _, _, _, _ -> },
    var onPlaybackCleared: () -> Unit = {},
    private val gson: Gson = Gson()
) {

    companion object {
        private const val TAG = "WebViewBridge"
    }

    /**
     * 前端通知原生层保存 JWT token。
     * 用户从 WebView 的登录页登录后，前端通过此方法将 token 同步到 Native。
     */
    @JavascriptInterface
    fun setToken(token: String) {
        Log.d(TAG, "setToken: length=${token.length}")
        CoroutineScope(Dispatchers.IO).launch {
            tokenStorage.saveToken(token)
        }
    }

    /**
     * 前端通知原生层清除 token（退出登录）。
     */
    @JavascriptInterface
    fun setUser(userJson: String) {
        Log.d(TAG, "setUser: length=${userJson.length}")
        CoroutineScope(Dispatchers.IO).launch {
            tokenStorage.saveUserJson(userJson)
        }
    }

    @JavascriptInterface
    fun clearToken() {
        Log.d(TAG, "clearToken")
        CoroutineScope(Dispatchers.IO).launch {
            tokenStorage.clear()
        }
    }

    /**
     * 前端请求用原生方式打开专辑详情页。
     * @param albumSlug 专辑 slug
     */
    @JavascriptInterface
    fun openNativeAlbum(albumSlug: String) {
        Log.d(TAG, "openNativeAlbum: $albumSlug")
        onNavigateNative("album_detail", mapOf("slug" to albumSlug))
    }

    /**
     * 前端请求跳转到原生首页（Tab 0）
     */
    @JavascriptInterface
    fun goToHome() {
        Log.d(TAG, "goToHome")
        onNavigateNative("go_home", emptyMap())
    }

    /**
     * 前端通知原生层：正在播放的曲目信息（用于更新 MiniPlayerBar）
     * @param json { trackTitle, artistName, coverUrl?, gradient? }
     */
    @JavascriptInterface
    fun setPlaybackState(json: String) {
        Log.d(TAG, "setPlaybackState: $json")
        try {
            val map = gson.fromJson(json, Map::class.java) as? Map<*, *> ?: return
            val title = (map["trackTitle"] as? String) ?: ""
            val artist = (map["artistName"] as? String) ?: ""
            if (title.isEmpty()) {
                onPlaybackCleared()
            } else {
                val playing = (map["isPlaying"] as? Boolean) ?: true
                val sec = (map["currentSeconds"] as? Number)?.toFloat() ?: 0f
                val dur = (map["duration"] as? Number)?.toFloat() ?: 0f
                onPlaybackChanged(title, artist, map["coverUrl"] as? String, map["gradient"] as? String, playing, sec, dur)
            }
        } catch (_: Exception) {
            onPlaybackCleared()
        }
    }

    /**
     * 前端通知原生层：清除播放状态
     */
    @JavascriptInterface
    fun clearPlaybackState() {
        Log.d(TAG, "clearPlaybackState")
        onPlaybackCleared()
    }

    /**
     * 通用导航（保留扩展性）。
     */
    @JavascriptInterface
    fun navigateTo(path: String, jsonParams: String) {
        Log.d(TAG, "navigateTo: $path $jsonParams")
        val params: Map<String, String> = try {
            gson.fromJson(jsonParams, Map::class.java) as? Map<String, String>
                ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
        onNavigateNative(path, params)
    }
}

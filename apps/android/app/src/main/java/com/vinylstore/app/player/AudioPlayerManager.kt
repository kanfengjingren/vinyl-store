package com.vinylstore.app.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.vinylstore.app.data.model.Album
import com.vinylstore.app.data.model.Track
import com.vinylstore.app.data.model.resolveCoverUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerState(
    val track: Track? = null,
    val album: Album? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playlist: List<Track> = emptyList(),
    val currentIndex: Int = -1
)

@OptIn(UnstableApi::class)
class AudioPlayerManager(context: Context) {

    val exoPlayer: ExoPlayer = try {
        ExoPlayer.Builder(context).build()
    } catch (e: Exception) {
        Log.e("AudioPlayerManager", "Failed to create ExoPlayer", e)
        throw e
    }

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var playlist: List<Track> = emptyList()
    private var currentIdx: Int = -1

    private val handler = Handler(Looper.getMainLooper())
    private val positionUpdater = object : Runnable {
        override fun run() {
            try {
                val pos = exoPlayer.currentPosition
                val dur = exoPlayer.duration
                _state.value = _state.value.copy(
                    currentPosition = if (pos >= 0) pos else _state.value.currentPosition,
                    duration = if (dur > 0) dur else _state.value.duration
                )
            } catch (_: Exception) { }
            handler.postDelayed(this, 250)
        }
    }

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
                if (isPlaying) {
                    handler.post(positionUpdater)
                } else {
                    handler.removeCallbacks(positionUpdater)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        val dur = exoPlayer.duration
                        if (dur > 0) {
                            _state.value = _state.value.copy(duration = dur)
                        }
                    }
                    Player.STATE_ENDED -> {
                        handler.removeCallbacks(positionUpdater)
                        playNext()
                    }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e("AudioPlayerManager", "Playback error: ${error.message}", error)
                handler.removeCallbacks(positionUpdater)
            }
        })
    }

    fun play(track: Track, album: Album?, trackList: List<Track>?) {
        val url = resolveCoverUrl(track.audioUrl) ?: track.audioUrl ?: return

        val mediaItem = MediaItem.fromUri(url)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()

        // 开始位置轮询
        handler.removeCallbacks(positionUpdater)
        handler.post(positionUpdater)

        // Setup playlist
        if (trackList != null && trackList.isNotEmpty()) {
            playlist = trackList.filter { it.audioUrl != null && !it.isSection }
            currentIdx = playlist.indexOfFirst { it.id == track.id }
        } else {
            playlist = emptyList()
            currentIdx = -1
        }

        _state.value = _state.value.copy(
            track = track,
            album = album,
            isPlaying = true,
            currentPosition = 0L,
            duration = 0L,
            playlist = playlist,
            currentIndex = currentIdx
        )
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
    }

    fun playNext() {
        if (playlist.isEmpty() || currentIdx < 0) {
            stop()
            return
        }
        val next = (currentIdx + 1) % playlist.size
        val track = playlist[next]
        currentIdx = next
        play(track, _state.value.album, playlist)
    }

    fun playPrev() {
        if (playlist.isEmpty() || currentIdx < 0) return
        // If past 3 seconds, restart current; otherwise go to previous
        if (exoPlayer.currentPosition > 3000) {
            exoPlayer.seekTo(0)
            return
        }
        val prev = (currentIdx - 1 + playlist.size) % playlist.size
        val track = playlist[prev]
        currentIdx = prev
        play(track, _state.value.album, playlist)
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
    }

    fun stop() {
        handler.removeCallbacks(positionUpdater)
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        playlist = emptyList()
        currentIdx = -1
        _state.value = PlayerState()
    }

    fun release() {
        handler.removeCallbacks(positionUpdater)
        stop()
        exoPlayer.release()
    }
}

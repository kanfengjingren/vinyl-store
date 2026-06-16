import { createApp, watch } from 'vue';
import { createPinia, setActivePinia } from 'pinia';
import router from './router';
import App from './App.vue';
import 'animate.css';
import './style.css';
import { useCartStore } from './stores/cart';
import { player, usePlayer } from './stores/player';

// 刷新前记住滚动位置
window.addEventListener('beforeunload', () => {
  sessionStorage.setItem('__scrollY', window.scrollY);
});

const pinia = createPinia();
const app = createApp(App);
app.use(pinia);
app.use(router);



app.mount('#app');

/* ── Android Native Bridge ────────────────────────────────
 * 原生层通过共享 WebView 注入 JS 调此函数。
 */
window.addToCartFromNative = function (albumJson) {
  try {
    const album = JSON.parse(albumJson);
    setActivePinia(pinia);
    const cart = useCartStore();
    cart.add(album);
    console.log('[NativeBridge] addToCart:', album.title);
  } catch (e) {
    console.error('[NativeBridge] addToCart error:', e);
  }
};

window.playTrackFromNative = function (trackJson, artistName, albumJson, trackListJson) {
  try {
    const track = JSON.parse(trackJson);
    const albumInfo = albumJson ? JSON.parse(albumJson) : null;
    const trackList = trackListJson ? JSON.parse(trackListJson) : null;
    const { play, stop } = usePlayer();
    play(track, artistName || '', albumInfo, trackList);
    syncPlaybackToNative();
    console.log('[NativeBridge] playTrack:', track.title);
  } catch (e) {
    console.error('[NativeBridge] playTrack error:', e);
  }
};

/** 原生层调此函数打开全屏播放器 */
window.openFullPlayer = function () {
  try {
    player.showFullPlayer = true;
  } catch (e) {
    console.error('[NativeBridge] openFullPlayer error:', e);
  }
};

/** 原生层调此函数停止播放 */
window.stopPlayback = function () {
  try {
    // 先直接暂停 DOM 中的 audio 元素，再清理 store 状态
    const audio = player.audioEl || document.querySelector('audio');
    if (audio) {
      audio.pause();
      audio.removeAttribute('src');
    }
    setActivePinia(pinia);
    const { stop } = usePlayer();
    stop();
  } catch (e) {
    console.error('[NativeBridge] stopPlayback error:', e);
  }
};

/** 原生层调此函数切换播放/暂停 */
window.togglePlayback = function () {
  try {
    if (!player.track) return;
    // 尝试从 store 或 DOM 获取 audio 元素（AudioPlayer.vue 可能尚未挂载）
    let audio = player.audioEl;
    if (!audio) {
      audio = document.querySelector('audio');
      if (audio) player.audioEl = audio;
    }
    if (!audio) return;
    if (player.playing) {
      audio.pause();
    } else {
      audio.play().catch(() => {});
    }
  } catch (e) {
    console.error('[NativeBridge] togglePlayback error:', e);
  }
};

/** 原生层调此函数拖动进度 */
window.seekTo = function (seconds) {
  try {
    const audio = player.audioEl || document.querySelector('audio');
    if (audio) audio.currentTime = seconds;
  } catch (e) {}
};

/** 将当前播放状态同步到原生 MiniPlayerBar */
function syncPlaybackToNative() {
  try {
    if (typeof window.AndroidBridge === 'undefined') return;
    if (!player.track) return;
    const state = JSON.stringify({
      trackTitle: player.track.title || '',
      artistName: player.track.artist || player.album?.artist || '',
      coverUrl: player.album?.coverUrl || '',
      gradient: player.album?.gradient || '',
      isPlaying: player.playing || false,
      currentSeconds: player.currentSeconds || 0,
      duration: player.duration || 0
    });
    window.AndroidBridge.setPlaybackState(state);
  } catch (e) {
    console.error('[NativeBridge] syncPlaybackToNative error:', e);
  }
}

/** 节流版同步进度（最多每秒一次），供 AudioPlayer.vue 在 timeupdate 中调用 */
let _progressSyncTimer = null;
window.syncProgressToNative = function () {
  if (typeof window.AndroidBridge === 'undefined') return;
  if (!player.track) return;
  if (_progressSyncTimer) return;
  _progressSyncTimer = setTimeout(() => {
    _progressSyncTimer = null;
    syncPlaybackToNative();
  }, 1000);
};

/** 清除原生 MiniPlayerBar */
function clearPlaybackOnNative() {
  try {
    if (typeof window.AndroidBridge === 'undefined') return;
    window.AndroidBridge.clearPlaybackState();
  } catch (e) {}
}

// 监听播放状态变化，自动同步到原生层
watch(
  () => player.track,
  (track) => {
    if (track) {
      syncPlaybackToNative();
    } else {
      clearPlaybackOnNative();
    }
  }
);

// 监听播放/暂停状态变化，同步 isPlaying
watch(
  () => player.playing,
  () => {
    if (player.track) {
      syncPlaybackToNative();
    }
  }
);

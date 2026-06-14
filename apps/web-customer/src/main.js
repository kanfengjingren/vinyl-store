import { createApp } from 'vue';
import { createPinia, setActivePinia } from 'pinia';
import router from './router';
import App from './App.vue';
import 'animate.css';
import './style.css';
import { useCartStore } from './stores/cart';
import { usePlayer } from './stores/player';

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

window.playTrackFromNative = function (trackJson, artistName) {
  try {
    const track = JSON.parse(trackJson);
    const { play } = usePlayer();
    play(track, artistName || '');
    console.log('[NativeBridge] playTrack:', track.title);
  } catch (e) {
    console.error('[NativeBridge] playTrack error:', e);
  }
};

<template>
  <div v-if="loading" class="flex items-center justify-center min-h-screen text-white/50 text-lg">加载中...</div>
  <div v-else-if="album" class="relative min-h-screen">
    <!-- 全屏背景 -->
    <div class="fixed inset-0 z-0">
      <div
        v-if="album.coverUrl"
        class="absolute inset-0 bg-cover bg-center bg-fixed"
        :style="{ backgroundImage: `url(${coverSrc(album.coverUrl)})` }"
      ></div>
      <div
        v-else
        class="absolute inset-0"
        :style="{ background: album.gradient || '#1a1a2e' }"
      ></div>
      <div class="absolute inset-0 bg-black/60 backdrop-blur-[1px]"></div>
    </div>

    <!-- 内容 -->
    <div class="relative z-10 max-w-[1200px] mx-auto px-6 py-12">
      <div class="flex gap-12 max-lg:flex-col max-lg:gap-8">
        <!-- Cover card -->
        <div class="shrink-0 w-[380px] max-lg:w-full max-lg:max-w-[400px] max-lg:mx-auto">
          <div class="aspect-square rounded-3xl overflow-hidden shadow-[0_8px_40px_rgba(0,0,0,.5)]" :style="{ background: album.gradient }">
            <img v-if="album.coverUrl" :src="coverSrc(album.coverUrl)" class="w-full h-full object-cover" />
            <div v-else class="w-full h-full flex items-center justify-center text-[100px] font-bold tracking-[-0.04em] text-white/20">
              {{ album.title.slice(0, 2).toUpperCase() }}
            </div>
          </div>
        </div>

        <!-- Info -->
        <div class="flex-1 min-w-0 text-white">
          <span v-if="album.badge" class="inline-block text-xs font-medium tracking-[.04em] bg-white/10 backdrop-blur-md text-white px-2.5 py-1 rounded-[20px] mb-3">
            {{ album.badge }}
          </span>
          <p class="text-[19px] text-white/70 mb-1">
            <router-link
              v-if="album.artistInfo?.slug"
              :to="`/artists/${album.artistInfo.slug}`"
              class="no-underline text-white/70 hover:text-[rgb(196,147,51)] transition-colors"
            >{{ album.artist }}</router-link>
            <span v-else>{{ album.artist }}</span>
          </p>
          <h1 class="text-[clamp(28px,4vw,44px)] font-semibold tracking-[-0.02em] mb-2">{{ album.title }}</h1>
          <p class="text-[15px] text-white/50 mb-2">
            {{ album.year }} &middot; {{ album.country }} &middot; {{ album.label }}
          </p>
          <p v-if="album.seller?.id" class="text-[14px] text-white/40 mb-2">
            卖家:
            <router-link
              :to="`/seller/${album.seller.id}`"
              class="no-underline text-white/40 hover:text-[rgb(196,147,51)] transition-colors"
            >{{ album.seller.storeName }}</router-link>
          </p>
          <div v-if="album.categories?.length" class="flex gap-1.5 flex-wrap mb-5">
            <span v-for="cat in album.categories" :key="cat.id"
              class="text-xs font-medium bg-white/10 text-white/80 px-3 py-1 rounded-full">{{ cat.name }}</span>
          </div>
          <p class="text-[17px] text-white/70 leading-relaxed mb-6">{{ album.description }}</p>
          <p class="text-[32px] font-semibold tracking-[-0.02em] mb-6">&yen;{{ album.price }}</p>
          <p class="text-[13px] text-white/40 mb-4">库存: {{ album.stock }} 张</p>
          <div class="flex items-center gap-4">
            <button @click="handleBuy" :disabled="album.stock <= 0"
              class="inline-flex items-center gap-2 px-8 py-3.5 rounded-full text-[15px] font-semibold border-none cursor-pointer transition-all"
              :class="album.stock <= 0
                ? 'bg-white/10 text-white/30 cursor-not-allowed'
                : 'bg-[rgb(196,147,51)] text-white hover:bg-[rgb(176,127,31)] hover:scale-105'">
              {{ album.stock <= 0 ? '已售罄' : '加入购物车' }}
            </button>
            <button
              v-if="auth.isLoggedIn"
              @click="toggleFav"
              class="text-[28px] leading-none transition-all hover:scale-125 select-none"
              :class="favorited ? 'text-red-400' : 'text-white/40 hover:text-red-300'"
              :title="favorited ? '取消收藏' : '收藏'"
            >{{ favorited ? '♥' : '♡' }}</button>
          </div>

          <!-- Tracks -->
          <div class="mt-10">
            <h3 class="text-[17px] font-semibold mb-4">曲目列表</h3>
            <ul class="space-y-1.5">
              <li v-for="track in album.tracks" :key="track.id"
                :class="['flex items-center gap-3 py-1.5 border-b border-white/10', track.isSection ? 'font-semibold text-sm text-white/80' : 'text-[15px] text-white/70']">
                <button
                  v-if="track.audioUrl"
                  @click="onPlay(track)"
                  class="w-6 h-6 rounded-full bg-[rgb(196,147,51)]/20 hover:bg-[rgb(196,147,51)] text-[rgb(196,147,51)] hover:text-white flex items-center justify-center transition-colors shrink-0"
                  :class="{ 'bg-[rgb(196,147,51)] text-white': player.track?.id === track.id }"
                >
                  <span class="text-[10px]" v-if="player.track?.id === track.id">⏸</span>
                  <span class="text-[10px] ml-px" v-else>▶</span>
                </button>
                <span v-else class="w-6 shrink-0" />
                <span class="text-white/30 w-6 text-right text-[13px] shrink-0">{{ track.isSection ? '' : track.position }}</span>
                <span class="flex-1">{{ track.title }}</span>
                <span v-if="track.duration" class="text-white/40 text-[13px]">{{ track.duration }}</span>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <!-- Back -->
      <div class="mt-12 pt-8 border-t border-white/10">
        <router-link to="/" class="text-[15px] font-medium text-[rgb(196,147,51)] no-underline hover:underline">&larr; 返回全部收藏</router-link>
      </div>

      <!-- 评论区 -->
      <div class="mt-12 pt-8 border-t border-white/10" v-if="album.id">
        <CommentSection :album-id="album.id" :highlight-comment-id="highlightCommentId" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAlbumStore } from '../stores/albums';
import { useCartStore } from '../stores/cart';
import { useAuthStore } from '../stores/auth';
import { useModalStore, toggleFavorite, fetchFavorites } from '@vinyl-store/shared';
import { player, usePlayer } from '../stores/player';
import CommentSection from '../components/comment/CommentSection.vue';

const route = useRoute();
const router = useRouter();
const albumStore = useAlbumStore();
const cart = useCartStore();
const auth = useAuthStore();
const modal = useModalStore();
const { play } = usePlayer();
const album = ref(null);
const loading = ref(false);
const favorited = ref(false);
const highlightCommentId = computed(() => {
  const id = parseInt(route.query.commentId);
  return isNaN(id) ? null : id;
});

watch(() => route.params.slug, load, { immediate: true });

async function load() {
  loading.value = true;
  try {
    album.value = await albumStore.loadAlbum(route.params.slug);
    // 检查收藏状态
    if (auth.isLoggedIn && album.value) {
      try {
        const favs = await fetchFavorites();
        favorited.value = favs.some((f) => f.album?.id === album.value.id);
      } catch {}
    }
  } finally {
    loading.value = false;
  }
}

async function toggleFav() {
  if (!album.value) return;
  try {
    const res = await toggleFavorite(album.value.id);
    favorited.value = res.favorited;
  } catch {}
}

function coverSrc(url) {
  if (!url) return '';
  return url.startsWith('http') ? url : `/${url}`;
}

function onPlay(track) {
  if (player.track?.id === track.id) {
    // 同一首 → 打开全屏播放器
    player.showFullPlayer = true
    return
  }
  play(track, album.value?.artist, album.value)
}

async function handleBuy() {
  if (!auth.isLoggedIn) {
    const ok = await modal.open({
      message: '您还未登录，请先登录',
      confirmText: '去登录',
      cancelText: '取消',
    })
    if (ok) router.push('/login')
    return
  }
  cart.add(album.value)
  cart.open()
}
</script>

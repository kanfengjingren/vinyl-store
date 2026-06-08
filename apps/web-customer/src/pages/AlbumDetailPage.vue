<template>
  <div v-if="loading" class="max-w-[1200px] mx-auto px-6 py-20 text-center text-apple-secondary">加载中...</div>
  <div v-else-if="album" class="max-w-[1200px] mx-auto px-6 py-12">
    <div class="flex gap-12 max-lg:flex-col max-lg:gap-8">
      <!-- Cover -->
      <div class="shrink-0 w-[380px] max-lg:w-full max-lg:max-w-[400px] max-lg:mx-auto">
        <div class="aspect-square rounded-3xl overflow-hidden shadow-[0_8px_40px_rgba(0,0,0,.15)]" :style="{ background: album.gradient }">
          <img v-if="album.coverUrl" :src="coverSrc(album.coverUrl)" class="w-full h-full object-cover" />
          <div v-else class="w-full h-full flex items-center justify-center text-[100px] font-bold tracking-[-0.04em] text-white/25">
            {{ album.title.slice(0, 2).toUpperCase() }}
          </div>
        </div>
      </div>

      <!-- Info -->
      <div class="flex-1 min-w-0">
        <span v-if="album.badge" class="inline-block text-xs font-medium tracking-[.04em] bg-black/60 text-white px-2.5 py-1 rounded-[20px] backdrop-blur-md mb-3">
          {{ album.badge }}
        </span>
        <p class="text-[19px] text-apple-secondary mb-1">{{ album.artist }}</p>
        <h1 class="text-[clamp(28px,4vw,44px)] font-semibold tracking-[-0.02em] mb-2">{{ album.title }}</h1>
        <p class="text-[15px] text-apple-tertiary mb-2">
          {{ album.year }} &middot; {{ album.country }} &middot; {{ album.label }}
        </p>
        <div v-if="album.categories?.length" class="flex gap-1.5 flex-wrap mb-5">
          <span v-for="cat in album.categories" :key="cat.id"
            class="text-xs font-medium bg-apple-bg text-apple-secondary px-3 py-1 rounded-full">{{ cat.name }}</span>
        </div>
        <p class="text-[17px] text-apple-secondary leading-relaxed mb-6">{{ album.description }}</p>
        <p class="text-[32px] font-semibold tracking-[-0.02em] mb-6">&yen;{{ album.price }}</p>
        <p class="text-[13px] text-apple-tertiary mb-4">库存: {{ album.stock }} 张</p>
        <button @click="handleBuy" :disabled="album.stock <= 0" class="inline-flex items-center gap-2 px-8 py-3.5 rounded-full text-[15px] font-semibold border-none cursor-pointer transition-all"
          :class="album.stock <= 0 ? 'bg-apple-border text-apple-tertiary cursor-not-allowed' : 'bg-apple-accent text-white hover:bg-apple-accent-hover hover:scale-105'">
          {{ album.stock <= 0 ? '已售罄' : '加入购物车' }}
        </button>

        <!-- Tracks -->
        <div class="mt-10">
          <h3 class="text-[17px] font-semibold mb-4">曲目列表</h3>
          <ul class="space-y-1.5">
            <li v-for="track in album.tracks" :key="track.id" :class="['flex items-center gap-3 py-1.5 border-b border-apple-border/40', track.isSection ? 'font-semibold text-sm' : 'text-[15px]']">
              <span class="text-apple-tertiary w-6 text-right text-[13px] shrink-0">{{ track.isSection ? '' : track.position }}</span>
              <span class="flex-1">{{ track.title }}</span>
              <span v-if="track.duration" class="text-apple-tertiary text-[13px]">{{ track.duration }}</span>
            </li>
          </ul>
        </div>
      </div>
    </div>

    <!-- Back -->
    <div class="mt-12 pt-8 border-t border-apple-border">
      <router-link to="/" class="text-[15px] font-medium text-apple-accent no-underline hover:underline">&larr; 返回全部收藏</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAlbumStore } from '../stores/albums';
import { useCartStore } from '../stores/cart';
import { useAuthStore } from '../stores/auth';
import { useModalStore } from '@vinyl-store/shared';

const route = useRoute();
const router = useRouter();
const albumStore = useAlbumStore();
const cart = useCartStore();
const auth = useAuthStore();
const modal = useModalStore();
const album = ref(null);
const loading = ref(false);

watch(() => route.params.slug, load, { immediate: true });

async function load() {
  loading.value = true;
  try {
    album.value = await albumStore.loadAlbum(route.params.slug);
    console.log(album.value);
    
  } finally {
    loading.value = false;
  }
}

function coverSrc(url) {
  if (!url) return '';
  return url.startsWith('http') ? url : `/${url}`;
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

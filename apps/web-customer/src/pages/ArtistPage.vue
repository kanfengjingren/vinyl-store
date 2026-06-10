<template>
  <div v-if="loading" class="max-w-[1200px] mx-auto px-6 py-20 text-center text-apple-secondary">加载中...</div>
  <div v-else-if="artist" class="max-w-[1200px] mx-auto px-6 py-12">
    <!-- Artist 信息 -->
    <div class="flex gap-8 mb-12 max-md:flex-col max-md:items-center max-md:text-center">
      <div class="shrink-0 w-[180px] h-[180px] rounded-full overflow-hidden bg-apple-bg shadow-[0_4px_20px_rgba(0,0,0,.08)]">
        <img v-if="artist.photo" :src="photoSrc(artist.photo)" class="w-full h-full object-cover" />
        <div v-else class="w-full h-full flex items-center justify-center text-[60px] font-bold text-apple-tertiary">
          {{ artist.name.slice(0, 2).toUpperCase() }}
        </div>
      </div>
      <div class="flex-1 min-w-0 flex flex-col justify-center">
        <h1 class="text-[clamp(24px,4vw,40px)] font-semibold tracking-[-0.02em] mb-2">{{ artist.name }}</h1>
        <p v-if="artist.foundedYear || artist.country" class="text-[15px] text-apple-secondary mb-2">
          <span v-if="artist.foundedYear">{{ artist.foundedYear }}</span>
          <span v-if="artist.foundedYear && artist.country"> &middot; </span>
          <span v-if="artist.country">{{ artist.country }}</span>
        </p>
        <p v-if="artist.description" class="text-[15px] text-apple-secondary leading-relaxed max-w-[600px]">{{ artist.description }}</p>
        <p class="text-[13px] text-apple-tertiary mt-3">{{ artist._count?.albums ?? artist.albums?.length ?? 0 }} 张专辑</p>
      </div>
    </div>

    <!-- 专辑列表 -->
    <div v-if="artist.albums?.length">
      <h2 class="text-xl font-semibold tracking-[-0.01em] mb-6">旗下专辑</h2>
      <AlbumGrid :albums="artist.albums" />
    </div>
    <div v-else class="text-center text-apple-secondary py-16">
      暂无专辑
    </div>

    <!-- Back -->
    <div class="mt-12 pt-8 border-t border-apple-border">
      <router-link to="/" class="text-[15px] font-medium text-apple-accent no-underline hover:underline">&larr; 返回首页</router-link>
    </div>
  </div>
  <div v-else class="max-w-[1200px] mx-auto px-6 py-20 text-center text-apple-secondary">
    乐队未找到
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { fetchArtistBySlug } from '@vinyl-store/shared';
import AlbumGrid from '../components/album/AlbumGrid.vue';

const route = useRoute();
const artist = ref(null);
const loading = ref(false);

function photoSrc(url) {
  if (!url) return '';
  return url.startsWith('http') || url.startsWith('/') ? url : `/${url}`;
}

watch(() => route.params.slug, load, { immediate: true });

async function load() {
  loading.value = true;
  try {
    artist.value = await fetchArtistBySlug(route.params.slug);
  } catch {
    artist.value = null;
  } finally {
    loading.value = false;
  }
}
</script>

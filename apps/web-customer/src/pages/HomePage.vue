<template>
  <!-- Hero -->
  <section class="max-w-[1200px] mx-auto pt-20 pb-[60px] px-6 flex flex-col items-center text-center bg-[rgb(205,189,157)]">
    <h1 class="text-[clamp(44px,7vw,80px)] font-semibold tracking-[-0.03em] leading-[1.08] hero-text mb-4">幻觉贸易</h1>
    <span class="inline-flex items-center gap-2 bg-apple-accent text-white text-[15px] font-medium px-7 py-3 rounded-full hover:bg-apple-accent-hover hover:scale-105 transition-all cursor-pointer" @click="scrollToCatalog">探索收藏</span>
  </section>

  <div class="w-full hidden">
    <div class="w-full h-[15px] bg-red-500"></div>
    <div class="w-full h-[15px] bg-orange-500"></div>
    <div class="w-full h-[15px] bg-yellow-500"></div>
    <div class="w-full h-[15px] bg-green-500"></div>
    <div class="w-full h-[15px] bg-blue-500"></div>
    <div class="w-full h-[15px] bg-purple-500"></div>
  </div>

  <!-- Featured -->
  <section v-if="featured" class="max-w-[1200px] mx-auto px-6 pb-20 flex items-center gap-[60px] max-md:flex-col max-md:gap-8 max-md:items-stretch">
    <div class="featured-art shrink-0 w-[320px] aspect-square rounded-3xl overflow-hidden relative cursor-pointer shadow-[0_8px_40px_rgba(0,0,0,.12)] hover:scale-105 transition-all max-md:w-full max-md:max-w-[340px] max-md:mx-auto"
      @click="$router.push(`/albums/${featured.slug}`)">
      <img :src="featured.coverUrl" :alt="featured.title" class="absolute inset-0 w-full h-full object-cover">
    </div>
    <div class="flex-1 min-w-0">
      <span class="text-xs font-medium tracking-[.06em] uppercase text-[#e74c3c] mb-2 block">今日推荐</span>
      <p class="text-[19px] text-apple-secondary mb-1">{{ featured.artist }}</p>
      <h2 class="text-[clamp(28px,4vw,40px)] font-semibold tracking-[-0.02em] mb-2 cursor-pointer hover:text-apple-accent inline-block"
        @click="$router.push(`/albums/${featured.slug}`)">{{ featured.title }}</h2>
      <p class="text-[15px] text-apple-tertiary mb-5">{{ featuredInfo }}</p>
      <p class="text-[15px] text-apple-secondary leading-relaxed max-w-[480px] mb-6 max-md:max-w-full line-clamp-3">
        {{ featured.description }}
      </p>
      <p class="text-[28px] font-semibold tracking-[-0.02em] mb-5">&yen; {{ featured.price }}</p>
      <button @click="addFeatured" class="inline-flex items-center gap-2 bg-apple-text text-white text-[15px] font-medium px-7 py-3 rounded-full border-none cursor-pointer hover:bg-[#3a3a3c] hover:scale-105 transition-all">
        加入购物车
      </button>
    </div>
  </section>

  <!-- Banner -->
  <div class="max-w-[1200px] mx-auto px-6 mb-20">
    <div class="bg-apple-text text-[#f5f5f7] rounded-[32px] py-[60px] px-12 text-center max-sm:py-10 max-sm:px-6">
      <blockquote class="text-[clamp(22px,3.5vw,30px)] font-medium tracking-[-0.01em] leading-relaxed max-w-[700px] mx-auto mb-4">
        "前卫摇滚不只是音乐 — 它是一场将古典、爵士与摇滚编织在一起的先锋运动，每一次针尖划过沟槽，都在挑战音乐的边界。"
      </blockquote>
      <cite class="text-[15px] not-italic text-apple-tertiary">&mdash; 幻觉贸易</cite>
    </div>
  </div>

  <!-- Catalog -->
  <div ref="catalogRef" class="section-header max-w-[1200px] mx-auto px-6 pb-8 flex items-end justify-between flex-wrap gap-3 max-sm:flex-col max-sm:items-start">
    <h2 class="text-[clamp(28px,4vw,36px)] font-semibold tracking-[-0.02em]">全部收藏</h2>
    <span class="text-[15px] font-medium text-apple-accent no-underline hover:underline cursor-pointer">查看全部 &rarr;</span>
  </div>

  <div class="max-w-[1200px] mx-auto px-6 pb-10">
    <CategoryFilter :categories="categoryStore.list" :active="albumStore.filters.category" @select="albumStore.setFilter('category', $event)" />
  </div>

  <div class="max-w-[1200px] mx-auto px-6 pb-20">
    <AlbumGrid :albums="albumStore.list" :loading="albumStore.loading" />
    <!-- Pagination -->
    <div v-if="albumStore.pagination.totalPages > 1" class="flex justify-center gap-2 mt-10">
      <button v-for="p in albumStore.pagination.totalPages" :key="p" @click="albumStore.setPage(p)"
        :class="['w-10 h-10 rounded-full text-sm font-medium border transition-colors', p === albumStore.pagination.page ? 'bg-apple-text text-white border-apple-text' : 'bg-white text-apple-text border-apple-border hover:bg-apple-text hover:text-white']">
        {{ p }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, computed } from 'vue';
import { fetchAlbums } from '@vinyl-store/shared';
import { useAlbumStore } from '../stores/albums';
import { useCategoryStore } from '../stores/categories';
import { useCartStore } from '../stores/cart';
import AlbumGrid from '../components/album/AlbumGrid.vue';
import CategoryFilter from '../components/album/CategoryFilter.vue';

const albumStore = useAlbumStore();
const categoryStore = useCategoryStore();
const cart = useCartStore();
const catalogRef = ref(null);
const featured = ref(null);

const featuredInfo = computed(() => {
  if (!featured.value) return '';
  const parts = [];
  if (featured.value.year) parts.push(String(featured.value.year));
  if (featured.value.country) parts.push(featured.value.country);
  if (featured.value.label) parts.push(featured.value.label);
  return parts.join(' · ');
});

onMounted(async () => {
  albumStore.loadAlbums();
  categoryStore.load();
  // 随机选一张专辑作为推荐
  try {
    const data = await fetchAlbums({ limit: 50 });
    const albums = data.data || [];
    if (albums.length) {
      featured.value = albums[Math.floor(Math.random() * albums.length)];
    }
  } catch { /* 推荐加载失败不影响主列表 */ }
});

function scrollToCatalog() {
  catalogRef.value?.scrollIntoView({ behavior: 'smooth' });
}

function coverSrc(url) {
  if (!url) return '';
  return url.startsWith('http') ? url : `/${url}`;
}

function addFeatured() {
  if (!featured.value) return;
  cart.add({
    id: featured.value.id,
    slug: featured.value.slug,
    artist: featured.value.artist,
    title: featured.value.title,
    price: featured.value.price,
    badge: featured.value.badge,
    coverUrl: featured.value.coverUrl ? coverSrc(featured.value.coverUrl) : '',
    gradient: featured.value.gradient || '',
  });
  cart.open();
}
</script>

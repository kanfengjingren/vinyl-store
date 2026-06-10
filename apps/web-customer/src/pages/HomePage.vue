<template>
  <div class="home-page relative min-h-screen">
    <!-- 全屏固定背景 -->
    <div class="fixed inset-0 z-0">
      <div
        v-if="pageBg?.coverUrl"
        class="absolute inset-0 bg-cover bg-center bg-fixed transition-all duration-1000"
        :style="{ backgroundImage: `url(${coverSrc(pageBg.coverUrl)})` }"
      ></div>
      <div
        v-else-if="pageBg?.gradient"
        class="absolute inset-0"
        :style="{ background: pageBg.gradient }"
      ></div>
      <div v-else class="absolute inset-0 bg-[#1a1a2e]"></div>
      <!-- 暗色遮罩 -->
      <div class="absolute inset-0 bg-black/55 backdrop-blur-[1px]"></div>
    </div>

    <!-- 页面内容 -->
    <div class="relative z-10">
      <!-- Hero -->
      <section class="w-full flex flex-col items-center justify-center text-center py-24 px-6" style="min-height: clamp(460px, 60vh, 600px)">
        <!-- 未登录：入口页 -->
        <template v-if="!auth.isLoggedIn">
          <div class="mb-2 text-[13px] tracking-[.15em] uppercase text-white/40">Vinyl Archive</div>
          <h1 class="text-[clamp(48px,8vw,88px)] font-semibold tracking-[-0.03em] leading-[1.06] text-white mb-4 drop-shadow-lg">幻觉贸易</h1>
          <p class="text-white/50 text-[16px] max-w-[520px] leading-relaxed mb-10">
            探索前卫摇滚、爵士、古典与独立音乐的收藏世界。<br/>每一张黑胶，都是一段值得被听见的历史。
          </p>
          <div class="flex gap-4 flex-wrap justify-center">
            <router-link to="/login"
              class="inline-flex items-center gap-2 bg-white text-black text-[15px] font-semibold px-8 py-3.5 rounded-full hover:bg-white/90 hover:scale-105 transition-all no-underline shadow-lg">
              用户登录
            </router-link>
            <a href="/seller/login"
              class="inline-flex items-center gap-2 bg-white/10 backdrop-blur-md text-white text-[15px] font-medium px-8 py-3.5 rounded-full border border-white/20 hover:bg-white/20 hover:scale-105 transition-all no-underline">
              商家登录
            </a>
          </div>
        </template>

        <!-- 已登录：正常首页 -->
        <template v-else>
          <h1 class="text-[clamp(44px,7vw,80px)] font-semibold tracking-[-0.03em] leading-[1.08] text-white mb-3 drop-shadow-lg">幻觉贸易</h1>
          <p v-if="pageBg" class="text-white/60 text-[15px] mb-2 tracking-wide">
            {{ pageBg.artist }} — {{ pageBg.title }}
          </p>
          <SearchBar class="mb-5" />
          <span class="inline-flex items-center gap-2 bg-white/10 backdrop-blur-md text-white text-[15px] font-medium px-7 py-3 rounded-full border border-white/15 hover:bg-white/20 hover:scale-105 transition-all cursor-pointer"
            @click="scrollToCatalog">探索收藏</span>
        </template>
      </section>

      <!-- Featured -->
      <section v-if="featured" class="max-w-[1200px] mx-auto px-6 pb-16 flex items-center gap-[60px] max-md:flex-col max-md:gap-8 max-md:items-stretch">
        <div class="shrink-0 w-[320px] aspect-square rounded-3xl overflow-hidden relative cursor-pointer shadow-[0_8px_40px_rgba(0,0,0,.4)] hover:scale-105 transition-all max-md:w-full max-md:max-w-[340px] max-md:mx-auto"
          @click="$router.push(`/albums/${featured.slug}`)">
          <img :src="coverSrc(featured.coverUrl)" :alt="featured.title" class="absolute inset-0 w-full h-full object-cover">
        </div>
        <div class="flex-1 min-w-0 text-white">
          <span class="text-xs font-medium tracking-[.06em] uppercase text-[rgb(196,147,51)] mb-2 block">今日推荐</span>
          <p class="text-[19px] text-white/70 mb-1">{{ featured.artist }}</p>
          <h2 class="text-[clamp(28px,4vw,40px)] font-semibold tracking-[-0.02em] mb-2 cursor-pointer hover:text-[rgb(196,147,51)] inline-block transition-colors"
            @click="$router.push(`/albums/${featured.slug}`)">{{ featured.title }}</h2>
          <p class="text-[15px] text-white/50 mb-5">{{ featuredInfo }}</p>
          <p class="text-[15px] text-white/70 leading-relaxed max-w-[480px] mb-6 max-md:max-w-full line-clamp-3">
            {{ featured.description }}
          </p>
          <p class="text-[28px] font-semibold tracking-[-0.02em] mb-5">&yen; {{ featured.price }}</p>
          <button v-if="auth.isLoggedIn" @click="addFeatured"
            class="inline-flex items-center gap-2 bg-[rgb(196,147,51)] text-white text-[15px] font-medium px-7 py-3 rounded-full border-none cursor-pointer hover:bg-[rgb(176,127,31)] hover:scale-105 transition-all">
            加入购物车
          </button>
          <router-link v-else to="/login"
            class="inline-flex items-center gap-2 bg-white/10 backdrop-blur-md text-white text-[15px] font-medium px-7 py-3 rounded-full border border-white/20 hover:bg-white/20 hover:scale-105 transition-all no-underline">
            登录后购买
          </router-link>
        </div>
      </section>

      <!-- Banner -->
      <!-- <div class="max-w-[1200px] mx-auto px-6 mb-16">
        <div class="bg-white/5 backdrop-blur-sm border border-white/10 text-white/90 rounded-[32px] py-[60px] px-12 text-center max-sm:py-10 max-sm:px-6">
          <blockquote class="text-[clamp(22px,3.5vw,30px)] font-medium tracking-[-0.01em] leading-relaxed max-w-[700px] mx-auto mb-4">
            "前卫摇滚不只是音乐 — 它是一场将古典、爵士与摇滚编织在一起的先锋运动，每一次针尖划过沟槽，都在挑战音乐的边界。"
          </blockquote>
          <cite class="text-[15px] not-italic text-white/40">&mdash; 幻觉贸易</cite>
        </div>
      </div> -->

      <!-- Catalog -->
      <div ref="catalogRef" class="section-header max-w-[1200px] mx-auto px-6 pb-8 flex items-end justify-between flex-wrap gap-3 max-sm:flex-col max-sm:items-start">
        <h2 class="text-[clamp(28px,4vw,36px)] font-semibold tracking-[-0.02em] text-white">全部收藏</h2>
        <span class="text-[15px] font-medium text-[rgb(196,147,51)] no-underline hover:underline cursor-pointer">查看全部 &rarr;</span>
      </div>

      <div class="max-w-[1200px] mx-auto px-6 pb-6">
        <CategoryFilter :categories="categoryStore.list" :active="albumStore.filters.category" @select="albumStore.setFilter('category', $event)" />
      </div>

      <!-- 颜色筛选 -->
      <div class="max-w-[1200px] mx-auto px-6 pb-6">
        <div class="flex items-center gap-2 flex-wrap">
          <span class="text-xs text-white/50 mr-1">颜色</span>
          <button
            v-for="c in colors"
            :key="c.label"
            @click="albumStore.setFilter('color', albumStore.filters.color === c.label ? '' : c.label)"
            :class="[
              'w-7 h-7 rounded-full border-2 transition-all cursor-pointer',
              albumStore.filters.color === c.label
                ? 'border-white scale-110 shadow-[0_0_0_2px_rgba(255,255,255,.3)]'
                : 'border-white/20 hover:scale-105 hover:shadow-md'
            ]"
            :style="{ background: c.hex }"
            :title="c.name"
          ></button>
        </div>
      </div>

      <!-- 排序 + 国家筛选 -->
      <div class="max-w-[1200px] mx-auto px-6 pb-8 flex items-center gap-4 flex-wrap">
        <div class="flex items-center gap-1.5">
          <span class="text-xs text-white/50 mr-1">排序</span>
          <button v-for="s in sorts" :key="s.key + s.order"
            @click="albumStore.setFilter('sort', s.key); albumStore.setFilter('order', s.order)"
            :class="['text-xs px-3 py-1.5 rounded-full border transition-all',
              albumStore.filters.sort === s.key && albumStore.filters.order === s.order
                ? 'bg-[rgb(196,147,51)] text-white border-[rgb(196,147,51)]'
                : 'bg-white/5 text-white/60 border-white/15 hover:border-white/30']">
            {{ s.label }}
          </button>
        </div>
        <div class="flex items-center gap-1.5">
          <span class="text-xs text-white/50 mr-1">国家</span>
          <select
            :value="albumStore.filters.country"
            @change="albumStore.setFilter('country', ($event.target).value)"
            class="text-xs px-3 py-1.5 rounded-full border border-white/15 bg-black/30 text-white/70 outline-none focus:border-[rgb(196,147,51)] cursor-pointer"
          >
            <option value="">全部</option>
            <option v-for="c in countries" :key="c" :value="c">{{ c }}</option>
          </select>
        </div>
      </div>

      <div class="max-w-[1200px] mx-auto px-6 pb-20">
        <AlbumGrid :albums="albumStore.list" :loading="albumStore.loading" />
        <div v-if="albumStore.pagination.totalPages > 1" class="flex justify-center gap-2 mt-10">
          <button v-for="p in albumStore.pagination.totalPages" :key="p" @click="albumStore.setPage(p)"
            :class="['w-10 h-10 rounded-full text-sm font-medium border transition-colors',
              p === albumStore.pagination.page
                ? 'bg-white text-black border-white'
                : 'bg-white/5 text-white/70 border-white/15 hover:bg-white/10']">
            {{ p }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, computed } from 'vue';
import { fetchAlbums, fetchCountries, fetchColors } from '@vinyl-store/shared';
import { useAlbumStore } from '../stores/albums';
import { useCategoryStore } from '../stores/categories';
import { useCartStore } from '../stores/cart';
import { useAuthStore } from '../stores/auth';
import AlbumGrid from '../components/album/AlbumGrid.vue';
import CategoryFilter from '../components/album/CategoryFilter.vue';
import SearchBar from '../components/search/SearchBar.vue';

const albumStore = useAlbumStore();
const categoryStore = useCategoryStore();
const cart = useCartStore();
const auth = useAuthStore();
const catalogRef = ref(null);
const featured = ref(null);
const pageBg = ref(null);
const countries = ref([]);
const colors = ref([]);

const sorts = [
  { key: 'createdAt', label: '最新', order: 'desc' },
  { key: 'year', label: '年份', order: 'desc' },
  { key: 'price', label: '价格↑', order: 'asc' },
  { key: 'price', label: '价格↓', order: 'desc' },
];

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
  fetchCountries().then(c => countries.value = c).catch(() => {});
  fetchColors().then(c => colors.value = c).catch(() => {});
  // 随机选一张有封面的作为全页背景，再选一张当今日推荐
  try {
    const data = await fetchAlbums({ limit: 50 });
    const albums = (data.data || []).filter(a => a.coverUrl || a.gradient);
    if (albums.length >= 2) {
      const i = Math.floor(Math.random() * albums.length);
      pageBg.value = albums[i];
      let j = Math.floor(Math.random() * (albums.length - 1));
      if (j >= i) j++;
      featured.value = albums[j];
    } else if (albums.length === 1) {
      pageBg.value = albums[0];
      featured.value = albums[0];
    }
  } catch {}
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

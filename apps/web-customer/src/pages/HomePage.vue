<template>
  <div class="home-page bg-white min-h-screen">
    <!-- Hero -->
    <section class="w-full flex flex-col items-center justify-center text-center py-24 px-6" style="min-height: clamp(460px, 60vh, 600px)">
      <template v-if="!auth.isLoggedIn">
        <div class="mb-2 text-[13px] tracking-[.15em] uppercase text-gray-400">Vinyl Archive</div>
        <h1 class="text-[clamp(48px,8vw,88px)] font-semibold tracking-[-0.03em] leading-[1.06] text-black mb-4">幻觉贸易</h1>
        <p class="text-gray-500 text-[16px] max-w-[520px] leading-relaxed mb-10">
          探索前卫摇滚、爵士、古典与独立音乐的收藏世界。<br/>每一张黑胶，都是一段值得被听见的历史。
        </p>
        <div class="flex gap-4 flex-wrap justify-center">
          <router-link to="/login"
            class="inline-flex items-center gap-2 bg-black text-white text-[15px] font-semibold px-8 py-3.5 hover:bg-black/80 hover:scale-105 transition-all no-underline shadow-lg">
            用户登录
          </router-link>
          <a href="/seller/login"
            class="inline-flex items-center gap-2 bg-white text-black text-[15px] font-medium px-8 py-3.5 border border-gray-300 hover:border-black hover:scale-105 transition-all no-underline">
            商家登录
          </a>
        </div>
      </template>

      <template v-else>
        <h1 class="text-[clamp(44px,7vw,80px)] font-semibold tracking-[-0.03em] leading-[1.08] text-black mb-3">幻觉贸易</h1>
        <p v-if="featured" class="text-gray-500 text-[15px] mb-2 tracking-wide">
          {{ featured.artist }} — {{ featured.title }}
        </p>
        <SearchBar class="mb-5" />
        <span class="inline-flex items-center gap-2 bg-black text-white text-[15px] font-medium px-7 py-3 hover:bg-black/80 hover:scale-105 transition-all cursor-pointer"
          @click="scrollToRows">探索收藏</span>
      </template>
    </section>

    <!-- Featured -->
    <section v-if="featured" class="max-w-[1200px] mx-auto px-6 pb-16 flex items-center gap-[60px] max-md:flex-col max-md:gap-8 max-md:items-stretch">
      <div class="shrink-0 w-[320px] aspect-square overflow-hidden relative cursor-pointer shadow-[0_8px_40px_rgba(0,0,0,.1)] hover:scale-105 transition-all max-md:w-full max-md:max-w-[340px] max-md:mx-auto"
        @click="$router.push(`/albums/${featured.slug}`)">
        <img :src="coverSrc(featured.coverUrl)" :alt="featured.title" class="absolute inset-0 w-full h-full object-cover">
      </div>
      <div class="flex-1 min-w-0 text-black">
        <span class="text-xs font-medium tracking-[.06em] uppercase text-[rgb(196,147,51)] mb-2 block">今日推荐</span>
        <p class="text-[19px] text-gray-500 mb-1">{{ featured.artist }}</p>
        <h2 class="text-[clamp(28px,4vw,40px)] font-semibold tracking-[-0.02em] mb-2 cursor-pointer hover:text-[rgb(196,147,51)] inline-block transition-colors"
          @click="$router.push(`/albums/${featured.slug}`)">{{ featured.title }}</h2>
        <p class="text-[15px] text-gray-400 mb-5">{{ featuredInfo }}</p>
        <p class="text-[15px] text-gray-600 leading-relaxed max-w-[480px] mb-6 max-md:max-w-full line-clamp-3">
          {{ featured.description }}
        </p>
        <p class="text-[28px] font-semibold tracking-[-0.02em] mb-5">&yen; {{ featured.price }}</p>
        <button v-if="auth.isLoggedIn" @click="addFeatured"
          class="inline-flex items-center gap-2 bg-[rgb(196,147,51)] text-white text-[15px] font-medium px-7 py-3 border-none cursor-pointer hover:bg-[rgb(176,127,31)] hover:scale-105 transition-all">
          加入购物车
        </button>
        <router-link v-else to="/login"
          class="inline-flex items-center gap-2 bg-black text-white text-[15px] font-medium px-7 py-3 hover:bg-black/80 hover:scale-105 transition-all no-underline">
          登录后购买
        </router-link>
      </div>
    </section>

    <!-- 颜色筛选 -->
    <div ref="rowsRef" class="max-w-[1200px] mx-auto px-6 pb-8">
      <div class="flex items-center gap-2 flex-wrap">
        <span class="text-xs text-gray-400 mr-1">颜色</span>
        <button
          v-for="c in colors"
          :key="c.label"
          @click="albumStore.setFilter('color', albumStore.filters.color === c.label ? '' : c.label)"
          :class="[
            'w-6 h-10 border-2 transition-all cursor-pointer',
            albumStore.filters.color === c.label
              ? 'border-black scale-110 shadow-[0_0_0_2px_rgba(0,0,0,.1)]'
              : 'border-gray-200 hover:scale-105 hover:shadow-md'
          ]"
          :style="{ background: c.hex }"
          :title="c.name"
        ></button>
      </div>
    </div>

    <!-- 分类横排滚动行 -->
    <section v-for="row in categoryRows" :key="row.slug" class="pb-14">
      <div class="max-w-[1200px] mx-auto px-6 pb-4 flex items-end justify-between">
        <h3 class="text-xl font-semibold tracking-[-0.01em] text-black">{{ row.name }}</h3>
        <router-link :to="`/catalog?category=${row.slug}`"
          class="text-[13px] font-medium text-gray-400 hover:text-black transition-colors no-underline">查看全部 &rarr;</router-link>
      </div>
      <div class="max-w-[1200px] mx-auto px-6">
        <div class="flex gap-4 overflow-x-auto pb-2 scrollbar-hide cursor-grab active:cursor-grabbing"
          @mousedown="onDragStart"
          @mousemove="onDragMove"
          @mouseup="onDragEnd"
          @mouseleave="onDragEnd"
          @click.capture="onRowClick"
        >
          <div
            v-for="album in row.albums"
            :key="album.id"
            class="shrink-0 w-[220px] select-none"
          >
            <AlbumCard :album="album" />
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
defineOptions({ name: 'HomePage' });
import { onMounted, ref, computed } from 'vue';
import { fetchAlbums, fetchColors } from '@vinyl-store/shared';
import { useAlbumStore } from '../stores/albums';
import { useCategoryStore } from '../stores/categories';
import { useCartStore } from '../stores/cart';
import { useAuthStore } from '../stores/auth';
import AlbumCard from '../components/album/AlbumCard.vue';
import SearchBar from '../components/search/SearchBar.vue';

const albumStore = useAlbumStore();
const categoryStore = useCategoryStore();
const cart = useCartStore();
const auth = useAuthStore();
const rowsRef = ref(null);
const featured = ref(null);
const colors = ref([]);
const categoryRows = ref([]);

// 鼠标拖拽横向滚动
const dragState = ref({ el: null, startX: 0, scrollLeft: 0, dragging: false, moved: false });

function onDragStart(e) {
  const el = e.currentTarget;
  dragState.value = { el, startX: e.pageX, scrollLeft: el.scrollLeft, dragging: true, moved: false };
  el.classList.add('cursor-grabbing');
}
function onDragMove(e) {
  if (!dragState.value.dragging) return;
  const dx = e.pageX - dragState.value.startX;
  if (Math.abs(dx) > 3) {
    dragState.value.moved = true;
  }
  e.preventDefault();
  const { el, startX, scrollLeft } = dragState.value;
  el.scrollLeft = scrollLeft - dx;
}
function onDragEnd() {
  if (dragState.value.el) {
    dragState.value.el.classList.remove('cursor-grabbing');
  }
}
function onRowClick(e) {
  // 拖拽过就拦截点击，不让它冒泡到 AlbumCard
  if (dragState.value.moved) {
    e.stopPropagation();
  }
  dragState.value = { el: null, startX: 0, scrollLeft: 0, dragging: false, moved: false };
}

const featuredInfo = computed(() => {
  if (!featured.value) return '';
  const parts = [];
  if (featured.value.year) parts.push(String(featured.value.year));
  if (featured.value.country) parts.push(featured.value.country);
  if (featured.value.label) parts.push(featured.value.label);
  return parts.join(' · ');
});

onMounted(async () => {
  categoryStore.load();
  fetchColors().then(c => colors.value = c).catch(() => {});

  // 今日推荐
  try {
    const data = await fetchAlbums({ limit: 50 });
    const albums = (data.data || []).filter(a => a.coverUrl || a.gradient);
    if (albums.length > 0) {
      featured.value = albums[Math.floor(Math.random() * albums.length)];
    }
  } catch {}

  // 每个分类拉取专辑
  try {
    await categoryStore.load();
    const cats = categoryStore.list;
    const rows = [];
    for (const cat of cats) {
      try {
        const res = await fetchAlbums({ category: cat.slug, limit: 10, sort: 'createdAt', order: 'desc' });
        if (res.data && res.data.length > 0) {
          rows.push({ slug: cat.slug, name: cat.name, albums: res.data });
        }
      } catch {}
    }
    categoryRows.value = rows;
  } catch {}
});

function scrollToRows() {
  rowsRef.value?.scrollIntoView({ behavior: 'smooth' });
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

<style scoped>
.scrollbar-hide::-webkit-scrollbar {
  display: none;
}
.scrollbar-hide {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>

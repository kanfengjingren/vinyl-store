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
          <a href="http://139.224.29.234:3002/login"
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
        <router-link to="/catalog?category=symphonic"
          class="inline-flex items-center gap-2 bg-black text-white text-[15px] font-medium px-7 py-3 hover:bg-black/80 hover:scale-105 transition-all no-underline">探索收藏</router-link>
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

    <!-- 本月热销 -->
    <section v-if="hotAlbums.length > 0" class="pb-14">
      <div class="max-w-[1200px] mx-auto px-6 pb-4 flex items-end justify-between">
        <h3 class="text-xl font-semibold tracking-[-0.01em] text-black">本月热销</h3>
        <router-link to="/catalog?sort=sales&order=desc"
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
            v-for="(album, idx) in hotAlbums"
            :key="album.id"
            class="shrink-0 w-[220px] select-none relative pt-8"
          >
            <!-- 排名角标 -->
            <span
              class="absolute top-0 left-2 z-10 flex items-center justify-center font-bold text-white select-none"
              :class="idx < 3 ? 'w-8 h-8 text-[14px]' : 'w-7 h-7 text-[11px]'"
              :style="rankStyle(idx)"
            >{{ idx + 1 }}</span>
            <AlbumCard :album="album" />
            <!-- 销量 -->
            <p class="text-center text-[12px] text-gray-400 mt-2 tracking-wide">
              近30天售出 <span class="text-[rgb(196,147,51)] font-semibold">{{ album.hotSales ?? 0 }}</span> 件
            </p>
          </div>
        </div>
      </div>
    </section>

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
import { fetchAlbums, fetchHotAlbums } from '@vinyl-store/shared';
import { useCategoryStore } from '../stores/categories';
import { useCartStore } from '../stores/cart';
import { useAuthStore } from '../stores/auth';
import AlbumCard from '../components/album/AlbumCard.vue';
import SearchBar from '../components/search/SearchBar.vue';

const categoryStore = useCategoryStore();
const cart = useCartStore();
const auth = useAuthStore();
const featured = ref(null);
const hotAlbums = ref([]);
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
  // 今日推荐
  try {
    const data = await fetchAlbums({ limit: 50 });
    const albums = (data.data || []).filter(a => a.coverUrl || a.gradient);
    if (albums.length > 0) {
      featured.value = albums[Math.floor(Math.random() * albums.length)];
    }
  } catch {}

  // 本月热销
  try {
    const hot = await fetchHotAlbums(12);
    hotAlbums.value = hot.data || [];
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

function coverSrc(url) {
  if (!url) return '';
  return url.startsWith('http') ? url : `/${url}`;
}

const RANK_STYLES = [
  // 1st — 鎏金
  {
    background: 'linear-gradient(135deg, #fbbf24 0%, #f59e0b 40%, #d97706 100%)',
    boxShadow: '0 2px 12px rgba(245,158,11,0.5), inset 0 1px 0 rgba(255,255,255,0.3)',
    border: '1.5px solid rgba(251,191,36,0.6)',
    textShadow: '0 1px 2px rgba(180,83,9,0.5)',
    borderRadius: '0',
  },
  // 2nd — 亮银
  {
    background: 'linear-gradient(135deg, #cbd5e1 0%, #94a3b8 40%, #64748b 100%)',
    boxShadow: '0 2px 12px rgba(148,163,184,0.5), inset 0 1px 0 rgba(255,255,255,0.35)',
    border: '1.5px solid rgba(203,213,225,0.6)',
    textShadow: '0 1px 2px rgba(71,85,105,0.5)',
    borderRadius: '0',
  },
  // 3rd — 古铜
  {
    background: 'linear-gradient(135deg, #d4a373 0%, #b45309 40%, #78350f 100%)',
    boxShadow: '0 2px 12px rgba(180,83,9,0.45), inset 0 1px 0 rgba(255,255,255,0.2)',
    border: '1.5px solid rgba(212,163,115,0.5)',
    textShadow: '0 1px 2px rgba(120,53,15,0.5)',
    borderRadius: '0',
  },
];
function rankStyle(idx) {
  return RANK_STYLES[idx] || {
    background: 'linear-gradient(135deg, #4b5563, #1f2937)',
    boxShadow: '0 1px 6px rgba(0,0,0,0.25)',
    border: '1px solid rgba(255,255,255,0.08)',
    textShadow: '0 1px 1px rgba(0,0,0,0.4)',
    borderRadius: '0',
  };
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

<template>
  <div class="catalog-page bg-white min-h-screen">
    <!-- 页面标题 -->
    <section class="w-full pt-24 pb-10 text-center">
      <h1 class="text-[clamp(36px,5vw,52px)] font-semibold tracking-[-0.03em] text-black mb-2">全部收藏</h1>
      <p class="text-gray-400 text-[15px]">探索所有黑胶唱片</p>
    </section>

    <div class="max-w-[1200px] mx-auto px-6 pb-6">
      <CategoryFilter :categories="categoryStore.list" :active="albumStore.filters.category" @select="albumStore.setFilter('category', $event)" />
    </div>

    <!-- 颜色筛选 -->
    <div class="max-w-[1200px] mx-auto px-6 pb-6">
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

    <!-- 排序 + 国家筛选 -->
    <div class="max-w-[1200px] mx-auto px-6 pb-8 flex items-center gap-4 flex-wrap">
      <div class="flex items-center gap-1.5">
        <span class="text-xs text-gray-400 mr-1">排序</span>
        <button v-for="s in sorts" :key="s.key + s.order"
          @click="albumStore.setFilter('sort', s.key); albumStore.setFilter('order', s.order)"
          :class="['text-xs px-3 py-1.5 border transition-all',
            albumStore.filters.sort === s.key && albumStore.filters.order === s.order
              ? 'bg-[rgb(196,147,51)] text-white border-[rgb(196,147,51)]'
              : 'bg-white text-gray-500 border-gray-200 hover:border-gray-400']">
          {{ s.label }}
        </button>
      </div>
      <div class="flex items-center gap-1.5">
        <span class="text-xs text-gray-400 mr-1">国家</span>
        <select
          :value="albumStore.filters.country"
          @change="albumStore.setFilter('country', ($event.target).value)"
          class="text-xs px-3 py-1.5 border border-gray-200 bg-white text-gray-500 outline-none focus:border-[rgb(196,147,51)] cursor-pointer"
        >
          <option value="">全部</option>
          <option v-for="c in countries" :key="c" :value="c">{{ c }}</option>
        </select>
      </div>
    </div>

    <!-- 专辑网格 -->
    <div class="max-w-[1200px] mx-auto px-6 pb-20">
      <AlbumGrid :albums="albumStore.list" :loading="albumStore.loading" />
      <div v-if="albumStore.pagination.totalPages > 1" class="flex justify-center gap-2 mt-10">
        <button v-for="p in albumStore.pagination.totalPages" :key="p" @click="albumStore.setPage(p)"
          :class="['w-10 h-10 text-sm font-medium border transition-colors',
            p === albumStore.pagination.page
              ? 'bg-black text-white border-black'
              : 'bg-white text-gray-500 border-gray-200 hover:bg-gray-50']">
          {{ p }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { fetchCountries, fetchColors } from '@vinyl-store/shared';
import { useAlbumStore } from '../stores/albums';
import { useCategoryStore } from '../stores/categories';
import { useRoute } from 'vue-router';
import AlbumGrid from '../components/album/AlbumGrid.vue';
import CategoryFilter from '../components/album/CategoryFilter.vue';

const albumStore = useAlbumStore();
const categoryStore = useCategoryStore();
const route = useRoute();
const countries = ref([]);
const colors = ref([]);

const sorts = [
  { key: 'createdAt', label: '最新', order: 'desc' },
  { key: 'year', label: '年份', order: 'desc' },
  { key: 'price', label: '价格↑', order: 'asc' },
  { key: 'price', label: '价格↓', order: 'desc' },
];

onMounted(async () => {
  categoryStore.load();
  fetchCountries().then(c => countries.value = c).catch(() => {});
  fetchColors().then(c => colors.value = c).catch(() => {});

  // 如果 URL 带了 category 参数，预选分类
  if (route.query.category) {
    albumStore.filters.category = route.query.category;
  }

  albumStore.loadAlbums();
});
</script>

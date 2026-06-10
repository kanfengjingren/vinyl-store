<template>
  <div class="max-w-[1200px] mx-auto px-6 py-12">
    <!-- 搜索框 -->
    <div class="mb-10">
      <SearchBar />
    </div>

    <!-- 结果标题 -->
    <div v-if="q" class="mb-8">
      <h1 class="text-[clamp(22px,3vw,28px)] font-semibold tracking-[-0.02em]">
        搜索「<span class="text-[rgb(196,147,51)]">{{ q }}</span>」的结果
      </h1>
      <p v-if="!albumStore.loading" class="text-sm text-black/40 mt-2">
        共 {{ albumStore.pagination.total }} 张专辑
      </p>
    </div>

    <AlbumGrid :albums="albumStore.list" :loading="albumStore.loading" />

    <!-- 分页 -->
    <div v-if="albumStore.pagination.totalPages > 1" class="flex justify-center gap-2 mt-10">
      <button
        v-for="p in albumStore.pagination.totalPages"
        :key="p"
        @click="albumStore.setPage(p)"
        :class="[
          'w-10 h-10 rounded-full text-sm font-medium border transition-colors',
          p === albumStore.pagination.page
            ? 'bg-apple-text text-white border-apple-text'
            : 'bg-white text-apple-text border-apple-border hover:bg-apple-text hover:text-white',
        ]"
      >
        {{ p }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAlbumStore } from '../stores/albums'
import AlbumGrid from '../components/album/AlbumGrid.vue'
import SearchBar from '../components/search/SearchBar.vue'

const route = useRoute()
const albumStore = useAlbumStore()

const q = route.query.q || ''

onMounted(() => {
  if (q) {
    albumStore.setFilter('search', q)
  }
})

watch(() => route.query.q, (newQ) => {
  if (newQ) {
    albumStore.setFilter('search', newQ)
  }
})
</script>

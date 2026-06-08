<template>
  <div>
    <h2 class="text-xl font-semibold tracking-[-0.02em] mb-6">专辑管理</h2>

    <div v-if="store.loading" class="text-center py-16 text-black/40">加载中...</div>

    <div v-else-if="!store.list.length" class="text-center py-16 text-black/40">
      <p class="text-[15px]">暂无专辑，去上架一张吧</p>
      <RouterLink to="/create"
        class="inline-block mt-4 text-sm text-[rgb(196,147,51)] hover:underline">上架专辑</RouterLink>
    </div>

    <div v-else class="space-y-3">
      <AlbumManageItem
        v-for="album in store.list"
        :key="album.id"
        :album="album"
        @refresh="store.loadAlbums"
      />
    </div>

    <div v-if="store.pagination.totalPages > 1" class="flex justify-center gap-2 mt-10">
      <button v-for="p in store.pagination.totalPages" :key="p" @click="store.setPage(p)"
        :class="['w-10 h-10 rounded-full text-sm font-medium border transition-colors',
          p === store.pagination.page
            ? 'bg-apple-text text-white border-apple-text'
            : 'bg-white text-apple-text border-apple-border hover:bg-apple-text hover:text-white']">
        {{ p }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue';
import { useSellerAlbumStore } from '../stores/albums';
import AlbumManageItem from '../components/album/AlbumManageItem.vue';

const store = useSellerAlbumStore();

onMounted(() => store.loadAlbums());
</script>

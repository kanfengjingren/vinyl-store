<template>
  <div v-if="loading" class="text-center py-20 text-apple-secondary">加载中...</div>
  <div v-else-if="!albums.length" class="text-center py-20 text-apple-secondary">暂无专辑</div>
  <div v-else class="grid grid-cols-[repeat(auto-fill,minmax(260px,1fr))] gap-6 max-lg:grid-cols-[repeat(auto-fill,minmax(220px,1fr))] max-lg:gap-4 max-sm:grid-cols-2 max-sm:gap-3 max-[400px]:grid-cols-1">
    <AlbumCard v-for="album in albums" :key="album.id" :album="album" />
  </div>
</template>

<script setup>
import AlbumCard from './AlbumCard.vue';
defineProps({ albums: Array, loading: Boolean });
</script>

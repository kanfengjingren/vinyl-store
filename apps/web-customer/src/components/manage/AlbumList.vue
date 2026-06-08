<template>
    <div>
        <AlbumItem :album="item" v-for="item in album.list" :key="item.id" @refresh="album.loadAlbums"/>
    </div>
    <div v-if="album.pagination.totalPages > 1" class="flex justify-center gap-2 mt-10">
      <button v-for="p in album.pagination.totalPages" :key="p" @click="album.setPage(p)"
        :class="['w-10 h-10 rounded-full text-sm font-medium border transition-colors', p === album.pagination.page ? 'bg-apple-text text-white border-apple-text' : 'bg-white text-apple-text border-apple-border hover:bg-apple-text hover:text-white']">
        {{ p }}
      </button>
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import AlbumItem from '../album/AlbumItem.vue';
import { useAlbumStore } from '../../stores/albums.js';
const album = useAlbumStore()
onMounted(async ()=>{
    // album.setPage(2)
    await album.loadAlbums()
    console.log(album.list);
    
})

</script>

<style scoped>
</style>
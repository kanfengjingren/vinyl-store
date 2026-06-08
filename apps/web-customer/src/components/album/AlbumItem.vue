<template>
  <div class="border border-[rgb(196,147,51)]/20 rounded-xl bg-white flex items-center gap-5 px-5 py-4 hover:shadow-md transition-shadow">
    <!-- 封面 -->
    <img
      :src="coverSrc(album.coverUrl)"
      :alt="album.title"
      class="w-[72px] h-[72px] rounded-lg object-cover shrink-0 bg-black/5 shadow-sm cursor-pointer transition-transform duration-300 hover:scale-105"
    />

    <!-- 信息区 -->
    <div class="flex-1 min-w-0">
      <p class="text-[15px] font-semibold text-black truncate">{{ album.title }}</p>
      <p class="text-sm text-black/40 mt-0.5">
        <span class="text-black/60 font-medium mr-1">{{ album.stock }}</span>张
        <span v-if="album.artist" class="text-black/30 mx-1.5">|</span>
        <span v-if="album.artist" class="text-black/40">{{ album.artist }}</span>
      </p>
    </div>

    <!-- 操作区 -->
    <div class="flex items-center gap-3 shrink-0">
      <!-- 库存更新 -->
      <div class="flex items-center gap-1.5">
        <input
          v-model.number="stockAmount"
          type="number"
          min="0"
          placeholder="数量"
          class="w-18 h-9 px-2.5 border border-black/15 rounded-lg text-sm text-black outline-none focus:border-[rgb(196,147,51)] focus:ring-1 focus:ring-[rgb(196,147,51)]/20 transition-all placeholder:text-black/20 [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
        />
        <button
          :disabled="stockAmount <= 0"
          class="h-9 px-4 rounded-lg border border-[rgb(196,147,51)]/50 bg-[rgb(196,147,51)] text-white text-sm font-medium hover:bg-[rgb(176,127,31)] transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
          @click="onAddStock"
        >
          更新库存
        </button>
      </div>

      <!-- 删除 -->
      <button
        class="h-9 px-4 rounded-lg border border-red-200 bg-red-50 text-red-500 text-sm font-medium hover:bg-red-100 transition-colors"
        @click="delAlbum"
      >
        删除
      </button>
    </div>
  </div>
</template>

<script setup lang="js">
import { ref } from 'vue'
import { updateAlbum, deleteAlbum } from '@vinyl-store/shared'

const props = defineProps({ album: Object })
const emit = defineEmits(['refresh'])

const stockAmount = ref(0)

function coverSrc(url) {
  if (!url) return ''
  return url.startsWith('http') ? url : `/${url}`
}

function onAddStock() {
  if (stockAmount.value <= 0) return
  updateAlbum(props.album.id, { stock: stockAmount.value }).then(() => {
    stockAmount.value = 0
    emit('refresh')
  })
}

function delAlbum() {
  if (!confirm('确定要删除此专辑吗？相关数据将被清除。')) return
  deleteAlbum(props.album.id).then(() => {
    emit('refresh')
  })
}
</script>

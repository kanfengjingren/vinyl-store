<template>
  <div class="bg-white min-h-screen">
    <!-- 页面标题 -->
    <section class="w-full pt-24 pb-10 text-center">
      <h1 class="text-[clamp(36px,5vw,52px)] font-semibold tracking-[-0.03em] text-black mb-2">新品上架</h1>
      <p class="text-gray-400 text-[15px]">近七天最新上架的唱片</p>
    </section>

    <!-- 日期导航 -->
    <div class="max-w-[1200px] mx-auto px-6 pb-8">
      <div class="flex gap-2 justify-center flex-wrap">
        <button
          v-for="d in dates" :key="d.value"
          @click="selectedDate = d.value"
          :class="[
            'px-5 py-2.5 text-sm font-medium transition-all border',
            selectedDate === d.value
              ? 'bg-black text-white border-black'
              : 'bg-white text-gray-500 border-gray-200 hover:border-gray-400'
          ]"
        >
          <span class="block">{{ d.weekday }}</span>
          <span class="block text-[11px] opacity-70">{{ d.display }}</span>
        </button>
      </div>
    </div>

    <!-- 专辑网格 -->
    <div class="max-w-[1200px] mx-auto px-6 pb-20">
      <p v-if="!loading && albums.length === 0" class="text-center text-gray-400 py-20">
        {{ selectedDate }} 暂无新上架专辑
      </p>
      <div v-else class="grid grid-cols-4 gap-6 max-lg:grid-cols-3 max-md:grid-cols-2 max-sm:grid-cols-1">
        <AlbumCard v-for="album in albums" :key="album.id" :album="album" />
      </div>
      <div v-if="loading" class="text-center text-gray-400 py-20">加载中...</div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { fetchAlbums } from '@vinyl-store/shared'
import AlbumCard from '../components/album/AlbumCard.vue'

const albums = ref([])
const loading = ref(false)
const selectedDate = ref('')

// 生成近7天日期列表
const dates = ref([])
function buildDates() {
  const list = []
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const now = new Date()
  for (let i = 6; i >= 0; i--) {
    const d = new Date(now)
    d.setDate(d.getDate() - i)
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    const value = `${y}-${m}-${day}`
    list.push({
      value,
      weekday: weekdays[d.getDay()],
      display: `${m}/${day}`,
    })
  }
  dates.value = list
  selectedDate.value = list[list.length - 1].value // 默认选今天
}

async function loadAlbums() {
  if (!selectedDate.value) return
  loading.value = true
  try {
    const data = await fetchAlbums({
      date: selectedDate.value,
      sort: 'createdAt',
      order: 'desc',
      limit: 50,
    })
    albums.value = data.data || []
  } catch {
    albums.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  buildDates()
  loadAlbums()
})

watch(selectedDate, () => {
  loadAlbums()
})
</script>

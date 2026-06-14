<template>
  <Transition name="overlay">
    <div v-if="player.showFullPlayer" class="fixed inset-0 z-[200] flex flex-col">
      <!-- 背景：专辑颜色 -->
      <div class="absolute inset-0 transition-colors duration-700" :style="{ background: bgGradient }" @click="close"></div>

      <!-- 内容 -->
      <div class="relative z-10 flex flex-col h-full max-w-[1100px] mx-auto w-full px-6 py-3">

        <!-- 顶部栏 -->
        <div class="flex items-center justify-between shrink-0 mb-1">
          <button @click="close" class="w-9 h-9 rounded-full bg-white/10 hover:bg-white/20 text-white flex items-center justify-center transition-colors">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7"/>
            </svg>
          </button>
          <p class="text-white/60 text-sm font-medium tracking-wider">正在播放</p>
          <div class="w-9" />
        </div>

        <!-- 中间主体：唱片 + 信息区 -->
        <div class="flex-1 flex items-start pt-6 gap-[40px] min-h-0 max-lg:flex-col max-lg:items-center max-lg:pt-4 max-lg:gap-5">

          <!-- 左侧：旋转唱片 -->
          <div class="shrink-0 flex items-center justify-center  max-lg:max-w-[200px] mt-20 ">
            <div class="relative w-[min(400px,38vw)] h-[min(400px,38vw)] max-lg:w-[min(200px,55vw)] max-lg:h-[min(200px,55vw)]">
              <div class="absolute inset-0 rounded-full" :style="{ background: discGradient }"></div>
              <div class="absolute inset-[12%] rounded-full border border-white/5"></div>
              <div class="absolute inset-[22%] rounded-full border border-white/[0.03]"></div>
              <div class="absolute inset-[32%] rounded-full border border-white/5"></div>
              <div
                class="absolute inset-[15%] rounded-full overflow-hidden shadow-2xl"
                :class="{ 'animate-spin-slow': player.playing, 'pause-animation': !player.playing }"
              >
                <img
                  v-if="player.album?.coverUrl"
                  :src="coverSrc(player.album.coverUrl)"
                  class="w-full h-full object-cover"
                />
                <div v-else class="w-full h-full flex items-center justify-center bg-white/10 text-white/30 text-4xl">♫</div>
              </div>
              <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[14%] h-[14%] rounded-full bg-[#1a1a2e] border-[3px] border-white/20 z-10"></div>
            </div>
          </div>

          <!-- 右侧：信息 + Tabs -->
          <div class="flex-1 min-w-0 text-white flex flex-col max-lg:text-center max-lg:w-full">

            <!-- 第一行：歌曲名 -->
            <h1 class="text-[clamp(20px,3vw,28px)] font-bold tracking-[-0.02em] leading-tight mb-3 truncate">
              {{ player.track?.title || '未知歌曲' }}
            </h1>

            <!-- 第二行：专辑 / 歌手 / 风格 → 一行 -->
            <div class="flex items-center gap-4 text-[14px] text-white/50 flex-wrap max-lg:justify-center">
              <span>专辑：<span class="text-white/70">{{ player.album?.title || '未知专辑' }}</span></span>
              <span class="text-white/20">|</span>
              <span>歌手：<span class="text-white/70">{{ player.track?.artist || player.album?.artist || '未知歌手' }}</span></span>
              <span v-if="player.album?.categories?.length" class="text-white/20">|</span>
              <span v-if="player.album?.categories?.length" class="text-white/50">
                {{ player.album.categories.map(c => c.name || c.slug).join(' / ') }}
              </span>
            </div>

            <!-- Tabs 区域 -->
            <div class="mt-6 flex-1 flex flex-col min-h-0">
              <!-- Tab 导航 -->
              <div class="flex gap-0 border-b border-white/10">
                <button
                  v-for="tab in tabs" :key="tab.key"
                  @click="activeTab = tab.key"
                  class="px-5 py-2 text-[13px] font-medium transition-colors relative"
                  :class="activeTab === tab.key ? 'text-white' : 'text-white/40 hover:text-white/70'"
                >
                  {{ tab.label }}
                  <div
                    v-if="activeTab === tab.key"
                    class="absolute bottom-0 left-1/2 -translate-x-1/2 w-7 h-[3px] rounded-full bg-[rgb(196,147,51)]"
                  ></div>
                </button>
              </div>

              <!-- Tab 内容 -->
              <div class="flex-1 overflow-y-auto pt-4 custom-scrollbar">
                <!-- 百科 -->
                <div v-if="activeTab === 'wiki'">
                  <div v-if="player.album?.description" class="text-white/70 text-[14px] leading-relaxed whitespace-pre-line">
                    {{ player.album.description }}
                  </div>
                  <div v-else class="text-white/30 text-[14px] italic">暂无专辑介绍</div>
                </div>

                <!-- 相似曲风推荐 -->
                <div v-if="activeTab === 'similar'">
                  <div v-if="similarLoading" class="text-white/40 text-[13px]">加载中...</div>
                  <div v-else-if="similarAlbums.length === 0" class="text-white/30 text-[13px] italic">暂无推荐</div>
                  <div v-else class="grid grid-cols-4 gap-3 max-lg:grid-cols-3 max-sm:grid-cols-2">
                    <div
                      v-for="album in similarAlbums" :key="album.id"
                      class="group cursor-pointer"
                      @click="goToAlbum(album.slug)"
                    >
                      <div class="aspect-square rounded-lg overflow-hidden mb-1.5 shadow-md"
                        :style="{ background: album.gradient || '#1a1a2e' }">
                        <img
                          v-if="album.coverUrl"
                          :src="coverSrc(album.coverUrl)"
                          class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                        />
                        <div v-else class="w-full h-full flex items-center justify-center text-white/20 text-xl">♫</div>
                      </div>
                      <p class="text-[12px] text-white/70 truncate group-hover:text-white transition-colors">{{ album.title }}</p>
                      <p class="text-[11px] text-white/40 truncate">{{ album.artist }}</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部播放控制栏 -->
        <div class="shrink-0 mt-3 pb-2">
          <!-- 进度条 -->
          <div class="flex items-center gap-3 mb-2">
            <span class="text-xs text-white/40 w-10 text-right">{{ formatTime(player.currentSeconds) }}</span>
            <div class="flex-1 relative h-1.5 group cursor-pointer" @click="seekBar">
              <div class="absolute inset-0 rounded-full bg-white/15"></div>
              <div
                class="absolute inset-y-0 left-0 rounded-full bg-[rgb(196,147,51)] transition-[width] duration-150"
                :style="{ width: progressPercent + '%' }"
              ></div>
              <div
                class="absolute top-1/2 -translate-y-1/2 w-3.5 h-3.5 rounded-full bg-white opacity-0 group-hover:opacity-100 transition-opacity shadow-lg"
                :style="{ left: progressPercent + '%', marginLeft: '-7px' }"
              ></div>
            </div>
            <span class="text-xs text-white/40 w-10">{{ formatTime(player.duration) }}</span>
          </div>

          <!-- 播放控制按钮 -->
          <div class="flex items-center justify-center gap-6">
            <button class="w-9 h-9 rounded-full bg-white/10 hover:bg-white/20 text-white/50 flex items-center justify-center transition-colors cursor-not-allowed" disabled>
              <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M11 19l-7-7 7-7m8 14l-7-7 7-7"/>
              </svg>
            </button>

            <button @click="togglePlay" class="w-12 h-12 rounded-full bg-[rgb(196,147,51)] hover:bg-[rgb(176,127,31)] text-white flex items-center justify-center transition-all hover:scale-105 shadow-lg shadow-[rgb(196,147,51)]/30">
              <span v-if="!player.playing" class="text-xl ml-0.5">▶</span>
              <span v-else class="text-xl">⏸</span>
            </button>

            <button class="w-9 h-9 rounded-full bg-white/10 hover:bg-white/20 text-white/50 flex items-center justify-center transition-colors cursor-not-allowed" disabled>
              <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M13 5l7 7-7 7M5 5l7 7-7 7"/>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { player } from '../../stores/player'
import { fetchAlbums } from '@vinyl-store/shared'

// 打开播放器时禁用页面滚动
watch(() => player.showFullPlayer, (val) => {
  document.body.style.overflow = val ? 'hidden' : ''
})
onBeforeUnmount(() => {
  document.body.style.overflow = ''
})

const router = useRouter()

const activeTab = ref('wiki')
const tabs = [
  { key: 'wiki', label: '百科' },
  { key: 'similar', label: '相似曲风推荐' },
]

const similarAlbums = ref([])
const similarLoading = ref(false)

// 监听专辑变化和 tab 切换 → 加载推荐
watch([() => player.album, () => activeTab.value], async ([album, tab]) => {
  if (tab === 'similar' && album?.categories?.length) {
    await loadSimilar(album)
  }
}, { immediate: true })

async function loadSimilar(album) {
  similarLoading.value = true
  try {
    const categorySlug = album.categories[0]?.slug
    if (!categorySlug) {
      similarAlbums.value = []
      return
    }
    const data = await fetchAlbums({ category: categorySlug, limit: 4 })
    // 排除当前专辑
    similarAlbums.value = (data.data || []).filter(a => a.title !== album.title).slice(0, 4)
  } catch {
    similarAlbums.value = []
  } finally {
    similarLoading.value = false
  }
}

const progressPercent = computed(() => {
  if (!player.duration || player.duration === 0) return 0
  return Math.min((player.currentSeconds / player.duration) * 100, 100)
})

const bgGradient = computed(() => {
  if (player.album?.gradient) {
    return player.album.gradient
  }
  return 'linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%)'
})

const discGradient = computed(() => {
  return 'conic-gradient(from 0deg, #1a1a2e, #2a2a4e, #1a1a2e, #2a2a4e, #1a1a2e)'
})

function close() {
  player.showFullPlayer = false
}

function togglePlay() {
  if (!player.audioEl) return
  if (player.playing) {
    player.audioEl.pause()
  } else {
    player.audioEl.play().catch(() => {})
  }
}

function seekBar(e) {
  if (!player.audioEl || !player.duration) return
  const rect = e.currentTarget.getBoundingClientRect()
  const ratio = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width))
  player.audioEl.currentTime = ratio * player.duration
}

function goToAlbum(slug) {
  if (!slug) return
  close()
  router.push(`/albums/${slug}`)
}

function coverSrc(url) {
  if (!url) return ''
  return url.startsWith('http') ? url : `/${url}`
}

function formatTime(sec) {
  if (!sec || !isFinite(sec)) return '0:00'
  const m = Math.floor(sec / 60)
  const s = Math.floor(sec % 60)
  return `${m}:${String(s).padStart(2, '0')}`
}
</script>

<style scoped>
.overlay-enter-active {
  transition: opacity 0.35s ease;
}
.overlay-leave-active {
  transition: opacity 0.3s ease;
}
.overlay-enter-from,
.overlay-leave-to {
  opacity: 0;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.animate-spin-slow {
  animation: spin 20s linear infinite;
}

.pause-animation {
  animation-play-state: paused;
}

.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(255,255,255,0.15);
  border-radius: 2px;
}
</style>

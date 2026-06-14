<template>
  <Transition name="player">
    <div v-if="player.track" class="fixed bottom-0 left-0 right-0 z-[150] bg-white/95 backdrop-blur-xl border-t border-black/10 shadow-[0_-4px_20px_rgba(0,0,0,.06)]">
      <div class="max-w-[1000px] mx-auto px-6 h-16 flex items-center gap-4">
        <div class="flex-1 min-w-0 flex items-center gap-3">
          <button @click="toggle" class="w-9 h-9 rounded-full bg-[rgb(196,147,51)] text-white flex items-center justify-center hover:bg-[rgb(176,127,31)] transition-colors shrink-0">
            <span v-if="!playing" class="text-sm ml-0.5">▶</span>
            <span v-else class="text-sm">⏸</span>
          </button>
          <div class="min-w-0">
            <p class="text-sm font-medium text-black truncate">{{ player.track.title }}</p>
            <p class="text-xs text-black/40 truncate">{{ player.track.artist || '' }}</p>
          </div>
        </div>

        <div class="hidden sm:flex items-center gap-2 w-[200px] shrink-0">
          <span class="text-xs text-black/30 w-10 text-right">{{ currentTime }}</span>
          <input
            type="range" min="0" :max="duration || 0" :value="currentSeconds"
            @input="seek"
            class="flex-1 h-1 accent-[rgb(196,147,51)] cursor-pointer"
          />
          <span class="text-xs text-black/30 w-10">{{ durationTime }}</span>
        </div>

        <button @click="stop" class="shrink-0 text-black/30 hover:text-black/60 transition-colors text-lg">&times;</button>
      </div>

      <audio
        ref="audioEl"
        :src="player.src"
        @timeupdate="onTimeUpdate"
        @loadedmetadata="onLoaded"
        @ended="onEnded"
        @play="playing = true"
        @pause="playing = false"
      />
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { player } from '../../stores/player'
import { recordPlay } from '@vinyl-store/shared'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const audioEl = ref(null)
const playing = ref(false)
const currentSeconds = ref(0)
const duration = ref(0)

// 核心修复：src 变化时主动加载并播放
watch(() => player.src, (newSrc) => {
  if (!newSrc || !audioEl.value) return
  playing.value = false
  currentSeconds.value = 0
  duration.value = 0
  // 等 Vue 把 src 绑定到 DOM 后再 load + play
  setTimeout(() => {
    audioEl.value?.load()
    audioEl.value?.play().catch(() => {})
  }, 50)

  // 记录播放历史
  if (auth.isLoggedIn && player.track) {
    recordPlay(player.track.id, player.track.albumId).catch(() => {})
  }
})

function toggle() {
  if (!audioEl.value) return
  if (playing.value) audioEl.value.pause()
  else audioEl.value.play().catch(() => {})
}

function stop() {
  audioEl.value?.pause()
  player.track = null
  player.src = ''
  playing.value = false
  currentSeconds.value = 0
}

function seek(e) {
  const val = +e.target.value
  if (audioEl.value) audioEl.value.currentTime = val
}

function onTimeUpdate() {
  if (audioEl.value) currentSeconds.value = audioEl.value.currentTime
}

function onLoaded() {
  if (audioEl.value) duration.value = audioEl.value.duration
}

function onEnded() {
  playing.value = false
  currentSeconds.value = 0
}

const currentTime = computed(() => formatTime(currentSeconds.value))
const durationTime = computed(() => formatTime(duration.value))

function formatTime(sec) {
  if (!sec || !isFinite(sec)) return '0:00'
  const m = Math.floor(sec / 60)
  const s = Math.floor(sec % 60)
  return `${m}:${String(s).padStart(2, '0')}`
}
</script>

<style scoped>
.player-enter-active, .player-leave-active { transition: transform 0.3s ease, opacity 0.3s ease; }
.player-enter-from, .player-leave-to { transform: translateY(100%); opacity: 0; }
</style>

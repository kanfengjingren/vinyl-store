<template>
  <div ref="root" class="relative w-full max-w-[560px] mx-auto">
    <div class="relative flex items-center">
      <svg
        class="absolute left-4 w-4 h-4 text-black/30 pointer-events-none"
        viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
        stroke-linecap="round" stroke-linejoin="round"
      >
        <circle cx="11" cy="11" r="8" />
        <path d="m21 21-4.35-4.35" />
      </svg>
      <input
        ref="input"
        v-model="query"
        type="text"
        placeholder="搜索专辑、艺人..."
        class="w-full h-11 pl-11 pr-10 rounded-full border border-black/15 dark:border-neutral-700 bg-white/70 dark:bg-neutral-800/70 backdrop-blur-md text-[15px] text-black dark:text-white placeholder:text-black/30 dark:placeholder:text-neutral-500 outline-none transition-all focus:border-[rgb(196,147,51)]/50 focus:bg-white dark:focus:bg-neutral-800 focus:shadow-[0_4px_20px_rgba(0,0,0,.06)]"
        @keydown.enter="onEnter"
        @keydown.arrow-down.prevent="onArrowDown"
        @keydown.arrow-up.prevent="onArrowUp"
        @focus="onFocus"
      />
      <button
        v-if="query"
        class="absolute right-3 w-5 h-5 flex items-center justify-center rounded-full text-black/25 hover:text-black/50 transition-colors"
        @click="clear"
      >
        &times;
      </button>
    </div>

    <!-- 联想下拉 -->
    <Transition name="dropdown">
      <div
        v-if="visible"
        class="absolute top-full mt-2 left-0 right-0 bg-white dark:bg-neutral-800 rounded-2xl shadow-[0_12px_40px_rgba(0,0,0,.1)] dark:shadow-[0_12px_40px_rgba(0,0,0,.4)] border border-black/10 dark:border-neutral-700 overflow-hidden z-50"
      >
        <div v-if="loading" class="px-4 py-6 text-center text-sm text-black/30">搜索中...</div>
        <div v-else-if="!suggestions.length" class="px-4 py-6 text-center text-sm text-black/30">无匹配结果</div>
        <ul v-else class="py-1">
          <li
            v-for="(item, idx) in suggestions"
            :key="item.id"
            :class="[
              'flex items-center gap-3 px-4 py-2.5 cursor-pointer transition-colors',
              idx === activeIndex ? 'bg-[rgb(196,147,51)]/10' : 'hover:bg-black/[0.03]',
            ]"
            @mousedown.prevent="goAlbum(item.slug)"
            @mouseenter="activeIndex = idx"
          >
            <div
              class="w-8 h-8 rounded-md shrink-0 overflow-hidden"
              :style="{ background: item.gradient || '#e5e5e5' }"
            >
              <img
                v-if="item.coverUrl"
                :src="coverSrc(item.coverUrl)"
                class="w-full h-full object-cover"
              />
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-sm font-medium text-black/80 dark:text-white/80 truncate">{{ item.title }}</p>
              <p class="text-xs text-black/40 dark:text-white/40 truncate">{{ item.artist }}</p>
            </div>
          </li>
        </ul>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { fetchSuggestions } from '@vinyl-store/shared'

const router = useRouter()
const root = ref(null)
const input = ref(null)
const query = ref('')
const suggestions = ref([])
const activeIndex = ref(-1)
const loading = ref(false)
const visible = ref(false)

let timer = null

watch(query, () => {
  activeIndex.value = -1
  clearTimeout(timer)
  if (!query.value.trim()) {
    suggestions.value = []
    visible.value = false
    return
  }
  timer = setTimeout(async () => {
    loading.value = true
    visible.value = true
    try {
      suggestions.value = await fetchSuggestions(query.value.trim())
    } catch {
      suggestions.value = []
    } finally {
      loading.value = false
    }
  }, 300)
})

function onFocus() {
  if (query.value.trim() && suggestions.value.length > 0) {
    visible.value = true
  }
}

function goAlbum(slug) {
  visible.value = false
  query.value = ''
  router.push(`/albums/${slug}`)
}

function onEnter() {
  if (activeIndex.value >= 0 && suggestions.value[activeIndex.value]) {
    goAlbum(suggestions.value[activeIndex.value].slug)
    return
  }
  const q = query.value.trim()
  if (!q) return
  visible.value = false
  router.push(`/search?q=${encodeURIComponent(q)}`)
}

function onArrowDown() {
  visible.value = true
  activeIndex.value = Math.min(activeIndex.value + 1, suggestions.value.length - 1)
}

function onArrowUp() {
  activeIndex.value = Math.max(activeIndex.value - 1, -1)
}

function clear() {
  query.value = ''
  suggestions.value = []
  visible.value = false
  input.value?.focus()
}

function onClickOutside(e) {
  if (root.value && !root.value.contains(e.target)) {
    visible.value = false
  }
}

onMounted(() => document.addEventListener('click', onClickOutside))
onUnmounted(() => document.removeEventListener('click', onClickOutside))

function coverSrc(url) {
  if (!url) return ''
  return url.startsWith('http') ? url : `/${url}`
}
</script>

<style scoped>
.dropdown-enter-active,
.dropdown-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>

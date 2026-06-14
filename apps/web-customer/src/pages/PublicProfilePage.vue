<template>
  <div class="bg-white min-h-screen">
    <div v-if="loading" class="flex items-center justify-center min-h-[60vh] text-gray-400 text-sm">加载中...</div>
    <div v-else-if="!user" class="flex items-center justify-center min-h-[60vh] text-gray-400 text-sm">用户不存在</div>
    <div v-else class="max-w-[480px] mx-auto px-6 pt-20 pb-20 text-center">
      <!-- 头像 -->
      <div class="w-28 h-28 rounded-full overflow-hidden bg-gray-100 mx-auto mb-6 shadow-lg">
        <img v-if="user.avatar" :src="coverSrc(user.avatar)" class="w-full h-full object-cover" />
        <span v-else class="w-full h-full flex items-center justify-center text-4xl font-bold text-gray-300">{{ (user.name || '?').slice(0, 1).toUpperCase() }}</span>
      </div>

      <!-- 用户名 -->
      <h1 class="text-2xl font-semibold tracking-[-0.02em] text-black mb-3">{{ user.name || '匿名用户' }}</h1>

      <!-- 加入时间 -->
      <p class="text-sm text-gray-400">
        <span class="inline-block mr-1">🕐</span>
        {{ formatDate(user.createdAt) }} 加入
      </p>

      <!-- 返回 -->
      <router-link to="/" class="inline-block mt-10 text-sm text-[rgb(196,147,51)] hover:underline">&larr; 返回首页</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { fetchPublicProfile } from '@vinyl-store/shared'

const route = useRoute()
const user = ref(null)
const loading = ref(false)

watch(() => route.params.id, load, { immediate: true })

async function load() {
  loading.value = true
  try {
    user.value = await fetchPublicProfile(route.params.id)
  } catch {
    user.value = null
  } finally {
    loading.value = false
  }
}

function coverSrc(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return url.startsWith('/') ? url : `/${url}`
}

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long' })
}
</script>

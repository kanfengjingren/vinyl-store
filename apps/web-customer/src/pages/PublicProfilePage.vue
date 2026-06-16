<template>
  <div class="bg-white min-h-screen">
    <div v-if="loading" class="flex items-center justify-center min-h-[60vh] text-gray-400 text-sm">加载中...</div>
    <div v-else-if="!user" class="flex items-center justify-center min-h-[60vh] text-gray-400 text-sm">用户不存在</div>
    <div v-else class="max-w-[900px] mx-auto px-6 pt-16 pb-20">
      <!-- 头像 + 基本信息 -->
      <div class="text-center mb-10">
        <div class="w-28 h-28 rounded-full overflow-hidden bg-gray-100 mx-auto mb-6 shadow-lg">
          <img v-if="user.avatar" :src="coverSrc(user.avatar)" class="w-full h-full object-cover" />
          <span v-else class="w-full h-full flex items-center justify-center text-4xl font-bold text-gray-300">{{ (user.name || '?').slice(0, 1).toUpperCase() }}</span>
        </div>
        <h1 class="text-2xl font-semibold tracking-[-0.02em] text-black mb-3">
          {{ user.seller?.storeName || user.name || '匿名用户' }}
        </h1>
        <p v-if="isSeller && user.seller?.description" class="text-sm text-gray-500 max-w-[400px] mx-auto mb-2">{{ user.seller.description }}</p>
        <p class="text-sm text-gray-400">
          <span class="inline-block mr-1">🕐</span>
          {{ formatDate(user.createdAt) }} 加入
        </p>

        <!-- 好友按钮（仅普通用户，且不是自己） -->
        <div v-if="auth.isLoggedIn && !isSelf && !isSeller" class="mt-4">
          <button
            v-if="friendStatus === 'none'"
            @click="handleAddFriend"
            :disabled="friendLoading"
            class="inline-flex items-center gap-1.5 px-5 py-2 bg-[rgb(196,147,51)] text-white text-sm font-medium hover:bg-[rgb(176,127,31)] disabled:opacity-50 transition-colors"
          >{{ friendLoading ? '处理中...' : '➕ 添加好友' }}</button>
          <span v-else-if="friendStatus === 'pending_sent'" class="inline-block px-5 py-2 bg-gray-100 text-gray-400 text-sm font-medium">已发送申请</span>
          <span v-else-if="friendStatus === 'pending_received'" class="inline-block px-5 py-2 bg-gray-100 text-gray-400 text-sm font-medium">对方已向你发送申请</span>
          <span v-else-if="friendStatus === 'accepted'" class="inline-block px-5 py-2 bg-gray-100 text-gray-400 text-sm font-medium">✅ 已是好友</span>
        </div>
      </div>

      <!-- ─── 商家：发售专辑 ─── -->
      <template v-if="isSeller">
        <div class="border-t border-gray-100 pt-8">
          <h3 class="text-lg font-semibold text-black mb-6 text-center">发售专辑</h3>
          <div v-if="sellerAlbumsLoading" class="text-center py-20 text-gray-400 text-sm">加载中...</div>
          <div v-else-if="sellerAlbums.length === 0" class="text-center py-20 text-gray-400">
            <p class="text-5xl mb-4">💿</p>
            <p class="text-sm">暂无在售专辑</p>
          </div>
          <div v-else class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            <div
              v-for="album in sellerAlbums"
              :key="album.id"
              class="border border-gray-100 hover:shadow-lg hover:-translate-y-1 transition-all cursor-pointer"
              @click="$router.push(`/albums/${album.slug}`)"
            >
              <div class="aspect-square overflow-hidden" :style="{ background: album.gradient || '#f3f4f6' }">
                <img v-if="album.coverUrl" :src="coverSrc(album.coverUrl)" class="w-full h-full object-cover" />
              </div>
              <div class="p-3">
                <p class="text-[11px] text-gray-400 truncate">{{ album.artist }}</p>
                <p class="text-sm font-medium truncate">{{ album.title }}</p>
                <p class="text-sm font-semibold mt-1">&yen;{{ album.price }}</p>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- ─── 普通用户：已购 / 收藏 ─── -->
      <template v-else>
        <!-- Tab bar -->
        <div class="sticky top-0 z-20 bg-white/95 backdrop-blur-md border-b border-gray-100 mb-8">
          <div class="flex gap-0 justify-center">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              @click="activeTab = tab.key"
              class="py-4 px-6 text-[14px] font-medium border-b-2 transition-colors"
              :class="activeTab === tab.key
                ? 'text-black border-black'
                : 'text-gray-400 border-transparent hover:text-gray-600'"
            >{{ tab.label }}</button>
          </div>
        </div>

        <!-- Tab: 已购专辑 -->
        <div v-if="activeTab === 'purchases'">
          <div v-if="purchasesLoading" class="text-center py-20 text-gray-400 text-sm">加载中...</div>
          <template v-else-if="purchasesData">
            <div v-if="!purchasesData.visible" class="text-center py-20 text-gray-400">
              <p class="text-5xl mb-4">🔒</p>
              <p class="text-sm">该用户隐藏了已购专辑</p>
            </div>
            <div v-else-if="purchasesData.data.length === 0" class="text-center py-20 text-gray-400">
              <p class="text-5xl mb-4">💿</p>
              <p class="text-sm">该用户还没有购买过专辑</p>
            </div>
            <div v-else class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
              <div
                v-for="album in purchasesData.data"
                :key="album.id"
                class="border border-gray-100 hover:shadow-lg hover:-translate-y-1 transition-all cursor-pointer"
                @click="$router.push(`/albums/${album.slug}`)"
              >
                <div class="aspect-square overflow-hidden" :style="{ background: album.gradient || '#f3f4f6' }">
                  <img v-if="album.coverUrl" :src="coverSrc(album.coverUrl)" class="w-full h-full object-cover" />
                </div>
                <div class="p-3">
                  <p class="text-[11px] text-gray-400 truncate">{{ album.artist }}</p>
                  <p class="text-sm font-medium truncate">{{ album.title }}</p>
                  <p class="text-xs text-gray-400 mt-1">{{ album.tracks?.length || 0 }} 首曲目</p>
                </div>
              </div>
            </div>
          </template>
        </div>

        <!-- Tab: 收藏专辑 -->
        <div v-if="activeTab === 'favorites'">
          <div v-if="favsLoading" class="text-center py-20 text-gray-400 text-sm">加载中...</div>
          <template v-else-if="favsData">
            <div v-if="!favsData.visible" class="text-center py-20 text-gray-400">
              <p class="text-5xl mb-4">🔒</p>
              <p class="text-sm">该用户隐藏了收藏专辑</p>
            </div>
            <div v-else-if="favsData.data.length === 0" class="text-center py-20 text-gray-400">
              <p class="text-5xl mb-4">♡</p>
              <p class="text-sm">该用户还没有收藏专辑</p>
            </div>
            <div v-else class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
              <div
                v-for="fav in favsData.data"
                :key="fav.id"
                class="border border-gray-100 hover:shadow-lg hover:-translate-y-1 transition-all cursor-pointer"
                @click="$router.push(`/albums/${fav.album.slug}`)"
              >
                <div class="aspect-square overflow-hidden" :style="{ background: fav.album.gradient || '#f3f4f6' }">
                  <img v-if="fav.album.coverUrl" :src="coverSrc(fav.album.coverUrl)" class="w-full h-full object-cover" />
                </div>
                <div class="p-3">
                  <p class="text-[11px] text-gray-400 truncate">{{ fav.album.artist }}</p>
                  <p class="text-sm font-medium truncate">{{ fav.album.title }}</p>
                  <p class="text-sm font-semibold mt-1">&yen;{{ fav.album.price }}</p>
                </div>
              </div>
            </div>
          </template>
        </div>
      </template>

      <!-- 返回 -->
      <div class="text-center mt-12">
        <router-link to="/" class="text-sm text-[rgb(196,147,51)] hover:underline">&larr; 返回首页</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { fetchPublicProfile, fetchPublicPurchases, fetchPublicFavorites, fetchPublicSellerAlbums, fetchFriendshipStatus, sendFriendRequest } from '@vinyl-store/shared'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const auth = useAuthStore()
const user = ref(null)
const loading = ref(false)

const isSeller = computed(() => user.value?.role === 'SELLER' && user.value?.seller?.status === 'APPROVED')
const isSelf = computed(() => auth.isLoggedIn && auth.user?.id === user.value?.id)

// ── 好友状态 ──
const friendStatus = ref('none') // none | pending_sent | pending_received | accepted | self
const friendLoading = ref(false)

async function checkFriendStatus() {
  if (!auth.isLoggedIn || !user.value || isSelf.value || isSeller.value) return
  try {
    const res = await fetchFriendshipStatus(user.value.id)
    if (res.status === 'none') friendStatus.value = 'none'
    else if (res.status === 'pending') friendStatus.value = res.isSender ? 'pending_sent' : 'pending_received'
    else if (res.status === 'accepted') friendStatus.value = 'accepted'
  } catch {}
}

async function handleAddFriend() {
  if (!user.value) return
  friendLoading.value = true
  try {
    await sendFriendRequest(user.value.id)
    friendStatus.value = 'pending_sent'
  } catch (e) {
    alert(e.response?.data?.message || '发送失败')
  } finally {
    friendLoading.value = false
  }
}

// ── 商家 Tab ──
const sellerAlbums = ref([])
const sellerAlbumsLoading = ref(false)

// ── 普通用户 Tab ──
const tabs = [
  { key: 'purchases', label: '已购专辑' },
  { key: 'favorites', label: '收藏专辑' },
]
const activeTab = ref('purchases')
const purchasesData = ref(null)
const purchasesLoading = ref(false)
const favsData = ref(null)
const favsLoading = ref(false)

watch(() => route.params.id, load, { immediate: true })

async function load() {
  loading.value = true
  purchasesData.value = null
  favsData.value = null
  sellerAlbums.value = []
  try {
    user.value = await fetchPublicProfile(route.params.id)
  } catch {
    user.value = null
  } finally {
    loading.value = false
  }
}

// 加载商家专辑 + 检查好友状态
watch(user, (u) => {
  if (!u) return
  if (isSeller.value) {
    sellerAlbumsLoading.value = true
    fetchPublicSellerAlbums(u.id).then(res => {
      sellerAlbums.value = res.data || []
    }).catch(() => {}).finally(() => {
      sellerAlbumsLoading.value = false
    })
  } else {
    checkFriendStatus()
  }
})

// 按需加载普通用户的 Tab 数据（immediate: true 确保默认 Tab 首次渲染就加载）
watch(activeTab, async (tab) => {
  if (tab === 'purchases' && !purchasesData.value) {
    purchasesLoading.value = true
    try {
      purchasesData.value = await fetchPublicPurchases(route.params.id)
    } catch {} finally {
      purchasesLoading.value = false
    }
  }
  if (tab === 'favorites' && !favsData.value) {
    favsLoading.value = true
    try {
      favsData.value = await fetchPublicFavorites(route.params.id)
    } catch {} finally {
      favsLoading.value = false
    }
  }
}, { immediate: true })

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

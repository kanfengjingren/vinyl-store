<template>
  <div class="bg-white min-h-screen">
    <!-- Tab bar -->
    <div class="sticky top-0 z-20 bg-white/95 backdrop-blur-md border-b border-gray-100">
      <div class="max-w-[800px] mx-auto px-6 flex gap-0">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          @click="activeTab = tab.key"
          class="py-4 px-5 text-[14px] font-medium border-b-2 transition-colors"
          :class="activeTab === tab.key
            ? 'text-black border-black'
            : 'text-gray-400 border-transparent hover:text-gray-600'"
        >{{ tab.label }}</button>
      </div>
    </div>

    <div class="max-w-[800px] mx-auto px-6 pb-20">
      <!-- Tab: 个人信息 -->
      <div v-if="activeTab === 'profile'">
        <div v-if="profileLoading" class="text-center py-20 text-gray-400 text-sm">加载中...</div>
        <template v-else>
          <div class="mb-6 pt-10">
            <label class="block text-sm text-gray-500 mb-1.5">账户余额</label>
            <p class="text-[15px] bg-gray-50 px-4 py-3">&yen;{{ profile.balance ?? 0 }}</p>
          </div>
          <div class="mb-6 p-5 border border-dashed border-[rgb(196,147,51)]/30 bg-[rgb(196,147,51)]/[0.02]">
            <label class="block text-sm font-medium text-black mb-2">充值（测试）</label>
            <div class="flex gap-2">
              <input v-model.number="rechargeAmount" type="number" min="1" placeholder="输入金额"
                class="flex-1 px-3 py-2 border border-gray-200 text-sm outline-none focus:border-[rgb(196,147,51)] transition-colors" />
              <button :disabled="recharging || !rechargeAmount"
                class="px-5 py-2 bg-[rgb(196,147,51)] text-white text-sm font-medium hover:bg-[rgb(176,127,31)] disabled:opacity-50 transition-colors shrink-0"
                @click="handleRecharge">{{ recharging ? '充值中...' : '充值' }}</button>
            </div>
            <p v-if="rechargeMsg" :class="['text-xs mt-2', rechargeOk ? 'text-green-600' : 'text-red-500']">{{ rechargeMsg }}</p>
          </div>
          <div class="mb-6">
            <label class="block text-sm text-gray-500 mb-1.5">邮箱</label>
            <p class="text-[15px] bg-gray-50 px-4 py-3">{{ profile.email }}</p>
          </div>
          <div class="mb-6">
            <label class="block text-sm text-gray-500 mb-1.5">用户名</label>
            <p class="text-[15px] bg-gray-50 px-4 py-3">{{ profile.name || '未设置' }}</p>
          </div>
          <div class="mb-8 p-5 border border-black/5">
            <button class="text-sm font-medium text-gray-500 hover:text-black transition-colors" @click="showPwd = !showPwd">
              {{ showPwd ? '取消修改密码' : '修改密码 →' }}
            </button>
            <div v-if="showPwd" class="mt-4 space-y-3">
              <input v-model="oldPassword" type="password" placeholder="原密码"
                class="w-full px-3 py-2 border border-gray-200 text-sm outline-none focus:border-[rgb(196,147,51)] transition-colors" />
              <input v-model="newPassword" type="password" placeholder="新密码"
                class="w-full px-3 py-2 border border-gray-200 text-sm outline-none focus:border-[rgb(196,147,51)] transition-colors" />
              <button :disabled="pwdSaving || !oldPassword || !newPassword"
                class="px-5 py-2 bg-black text-white text-sm font-medium hover:bg-black/80 disabled:opacity-50 transition-colors"
                @click="handleChangePassword">{{ pwdSaving ? '修改中...' : '确认修改' }}</button>
              <p v-if="pwdMsg" :class="['text-xs', pwdOk ? 'text-green-600' : 'text-red-500']">{{ pwdMsg }}</p>
            </div>
          </div>
          <div class="mb-8">
            <label class="block text-sm text-gray-500 mb-1.5">默认收货地址 <span class="text-red-400 ml-0.5">*</span></label>
            <CitySelect v-model="address" v-model:detail="addressDetail" />
            <p v-if="addressError" class="text-red-400 text-sm mt-1.5">{{ addressError }}</p>
          </div>
          <button :disabled="saving"
            class="w-full py-3.5 bg-black text-white text-[15px] font-semibold border-none cursor-pointer hover:bg-black/80 transition-all disabled:opacity-50"
            @click="save">{{ saving ? '保存中...' : '保存' }}</button>
          <p v-if="saved" class="text-green-600 text-sm text-center mt-3">保存成功</p>
        </template>
      </div>

      <!-- Tab: 已购专辑 -->
      <div v-if="activeTab === 'purchases'" class="pt-10">
        <div v-if="purchasesLoading" class="text-center py-20 text-gray-400 text-sm">加载中...</div>
        <template v-else-if="purchases.length === 0">
          <div class="text-center py-20 text-gray-400">
            <p class="text-5xl mb-4">💿</p>
            <p class="text-sm">还没有购买过专辑</p>
            <router-link to="/catalog" class="text-[rgb(196,147,51)] text-sm hover:underline mt-2 inline-block">去逛逛 →</router-link>
          </div>
        </template>
        <template v-else>
          <div class="space-y-6">
            <div v-for="album in purchases" :key="album.id"
              class="flex gap-5 p-4 border border-gray-100 hover:border-gray-200 transition-colors cursor-pointer"
              @click="$router.push(`/albums/${album.slug}`)">
              <div class="shrink-0 w-[100px] h-[100px] overflow-hidden"
                :style="{ background: album.gradient || '#f3f4f6' }">
                <img v-if="album.coverUrl" :src="coverSrc(album.coverUrl)" class="w-full h-full object-cover" />
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-[13px] text-gray-400">{{ album.artist }}</p>
                <h3 class="text-base font-semibold truncate">{{ album.title }}</h3>
                <p class="text-xs text-gray-400 mt-1">{{ album.tracks?.length || 0 }} 首曲目</p>
                <div class="mt-3 space-y-1">
                  <button
                    v-for="track in (album.tracks || []).filter(t => t.audioUrl)"
                    :key="track.id"
                    @click.stop="playTrack(track, album.artist)"
                    class="flex items-center gap-2 text-[13px] text-gray-500 hover:text-[rgb(196,147,51)] transition-colors"
                  >
                    <span class="text-[10px]">▶</span>
                    <span>{{ track.title }}</span>
                    <span v-if="track.duration" class="text-gray-300 ml-auto">{{ track.duration }}</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>

      <!-- Tab: 我的收藏 -->
      <div v-if="activeTab === 'favorites'" class="pt-10">
        <div v-if="favsLoading" class="text-center py-20 text-gray-400 text-sm">加载中...</div>
        <template v-else-if="favorites.length === 0">
          <div class="text-center py-20 text-gray-400">
            <p class="text-5xl mb-4">♡</p>
            <p class="text-sm">还没有收藏专辑</p>
            <router-link to="/catalog" class="text-[rgb(196,147,51)] text-sm hover:underline mt-2 inline-block">去发现 →</router-link>
          </div>
        </template>
        <template v-else>
          <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            <div v-for="fav in favorites" :key="fav.id"
              class="border border-gray-100 hover:shadow-lg hover:-translate-y-1 transition-all cursor-pointer"
              @click="$router.push(`/albums/${fav.album.slug}`)">
              <div class="aspect-square overflow-hidden" :style="{ background: fav.album.gradient || '#f3f4f6' }">
                <img v-if="fav.album.coverUrl" :src="coverSrc(fav.album.coverUrl)" class="w-full h-full object-cover" />
              </div>
              <div class="p-3">
                <p class="text-[11px] text-gray-400 truncate">{{ fav.album.artist }}</p>
                <p class="text-sm font-medium truncate">{{ fav.album.title }}</p>
                <p class="text-sm font-semibold mt-1">&yen;{{ fav.album.price }}</p>
                <button
                  v-if="isPurchased(fav.album.id)"
                  class="mt-2 w-full py-1.5 text-xs bg-gray-100 text-gray-400 cursor-default">已购买</button>
                <button
                  v-else
                  @click.stop="buyFavorite(fav.album)"
                  class="mt-2 w-full py-1.5 text-xs bg-[rgb(196,147,51)] text-white hover:bg-[rgb(176,127,31)] transition-colors">购买后收听</button>
              </div>
            </div>
          </div>
        </template>
      </div>

      <!-- Tab: 播放历史 -->
      <div v-if="activeTab === 'history'" class="pt-10">
        <div v-if="historyLoading" class="text-center py-20 text-gray-400 text-sm">加载中...</div>
        <template v-else-if="playHistory.length === 0">
          <div class="text-center py-20 text-gray-400">
            <p class="text-5xl mb-4">🎧</p>
            <p class="text-sm">还没有播放记录</p>
          </div>
        </template>
        <template v-else>
          <div class="space-y-1">
            <div v-for="(h, idx) in playHistory" :key="h.id"
              class="flex items-center gap-4 px-4 py-3 hover:bg-gray-50 transition-colors cursor-pointer"
              @click="$router.push(`/albums/${h.album.slug}`)">
              <span class="text-xs text-gray-300 w-6 text-right shrink-0">{{ playHistory.length - idx }}</span>
              <div class="w-10 h-10 shrink-0 overflow-hidden" :style="{ background: h.album.gradient || '#f3f4f6' }">
                <img v-if="h.album.coverUrl" :src="coverSrc(h.album.coverUrl)" class="w-full h-full object-cover" />
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-sm font-medium truncate">{{ h.track.title }}</p>
                <p class="text-xs text-gray-400 truncate">{{ h.album.artist }} — {{ h.album.title }}</p>
              </div>
              <span class="text-xs text-gray-300 shrink-0">{{ timeAgo(h.playedAt) }}</span>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { fetchProfile, updateProfile, recharge, changePassword, fetchPurchases, fetchFavorites, fetchPlayHistory } from '@vinyl-store/shared'
import { useCartStore } from '../stores/cart'
import { usePlayer } from '../stores/player'
import CitySelect from '@vinyl-store/shared/ui/CitySelect'

const router = useRouter()
const cart = useCartStore()
const { play } = usePlayer()

// Tabs
const tabs = [
  { key: 'profile', label: '个人信息' },
  { key: 'purchases', label: '已购专辑' },
  { key: 'favorites', label: '我的收藏' },
  { key: 'history', label: '播放历史' },
]
const activeTab = ref('profile')

// Profile
const profile = ref({})
const address = ref('')
const addressDetail = ref('')
const profileLoading = ref(true)
const saving = ref(false)
const saved = ref(false)
const addressError = ref('')
const rechargeAmount = ref(null)
const recharging = ref(false)
const rechargeMsg = ref('')
const rechargeOk = ref(false)
const showPwd = ref(false)
const oldPassword = ref('')
const newPassword = ref('')
const pwdSaving = ref(false)
const pwdMsg = ref('')
const pwdOk = ref(false)

// Purchases
const purchases = ref([])
const purchasesLoading = ref(false)

// Favorites
const favorites = ref([])
const favsLoading = ref(false)

// Play history
const playHistory = ref([])
const historyLoading = ref(false)

// Purchased album IDs (for checking in favorites)
const purchasedIds = ref(new Set())

function coverSrc(url) {
  if (!url) return ''
  return url.startsWith('http') ? url : `/${url}`
}

function timeAgo(dateStr) {
  const diff = Date.now() - new Date(dateStr).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return '刚刚'
  if (mins < 60) return `${mins}分钟前`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days}天前`
  return `${Math.floor(days / 30)}月前`
}

function isPurchased(albumId) {
  return purchasedIds.value.has(albumId)
}

function playTrack(track, artist) {
  if (!track.audioUrl) return
  play(track, artist)
}

async function buyFavorite(album) {
  cart.add(album)
  cart.open()
}

// Profile actions
async function handleRecharge() {
  if (!rechargeAmount.value || rechargeAmount.value <= 0) return
  recharging.value = true
  rechargeMsg.value = ''
  try {
    const result = await recharge(rechargeAmount.value)
    profile.value.balance = result.balance
    rechargeAmount.value = null
    rechargeOk.value = true
    rechargeMsg.value = `充值成功！当前余额 ¥${result.balance}`
  } catch (e) {
    rechargeOk.value = false
    rechargeMsg.value = e.response?.data?.message || '充值失败'
  } finally { recharging.value = false }
}

async function handleChangePassword() {
  if (!oldPassword.value || !newPassword.value) return
  pwdSaving.value = true
  pwdMsg.value = ''
  try {
    await changePassword({ oldPassword: oldPassword.value, newPassword: newPassword.value })
    pwdOk.value = true
    pwdMsg.value = '密码修改成功'
    oldPassword.value = ''
    newPassword.value = ''
  } catch (e) {
    pwdOk.value = false
    pwdMsg.value = e.response?.data?.message || '修改失败'
  } finally { pwdSaving.value = false }
}

async function save() {
  if (!address.value.trim()) {
    addressError.value = '收货地址不能为空'
    return
  }
  addressError.value = ''
  saving.value = true
  saved.value = false
  try {
    const data = await updateProfile({ defaultAddress: address.value.trim() })
    profile.value = data
    saved.value = true
  } finally { saving.value = false }
}

// Load data based on active tab
async function loadTabData() {
  if (activeTab.value === 'purchases' && purchases.value.length === 0) {
    purchasesLoading.value = true
    try { purchases.value = await fetchPurchases() } catch {} finally { purchasesLoading.value = false }
  }
  if (activeTab.value === 'favorites' && favorites.value.length === 0) {
    favsLoading.value = true
    try {
      favorites.value = await fetchFavorites()
    } catch {} finally { favsLoading.value = false }
  }
  if (activeTab.value === 'history' && playHistory.value.length === 0) {
    historyLoading.value = true
    try { playHistory.value = await fetchPlayHistory(30) } catch {} finally { historyLoading.value = false }
  }
}

onMounted(async () => {
  // Load profile
  try {
    const data = await fetchProfile()
    profile.value = data
    address.value = data.defaultAddress || ''
  } finally { profileLoading.value = false }

  // Pre-load purchases to build purchasedIds set
  try {
    purchases.value = await fetchPurchases()
    purchasedIds.value = new Set(purchases.value.map(a => a.id))
  } catch {}
})

// Watch tab changes
watch(activeTab, loadTabData)
</script>

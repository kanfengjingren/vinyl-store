<template>
  <div class="max-w-[720px] mx-auto px-6 py-12">
    <!-- 返回 -->
    <router-link to="/orders" class="inline-flex items-center gap-1 text-sm text-black/40 hover:text-[rgb(196,147,51)] transition-colors mb-8">
      &larr; 返回订单列表
    </router-link>

    <div v-if="loading" class="text-center py-20 text-black/40">加载中...</div>

    <template v-else-if="order">
      <!-- 头部 -->
      <div class="mb-8">
        <div class="flex items-center justify-between mb-3">
          <h1 class="text-2xl font-semibold tracking-[-0.02em]">订单 #{{ order.id }}</h1>
          <span :class="['text-xs font-medium px-3 py-1 rounded-full border', statusStyle]">
            {{ statusLabel }}
          </span>
        </div>
        <p v-if="order.shippingAddress" class="text-sm text-black/40 mb-1">{{ order.shippingAddress }}</p>
        <div class="flex items-center gap-4">
          <time class="text-sm text-black/50">{{ formatDate(order.createdAt) }}</time>
          <span v-if="countdown" class="text-xs text-red-400">{{ countdown }}</span>
        </div>
      </div>

      <!-- 分割线 -->
      <hr class="border-[rgb(196,147,51)]/20 mb-6">

      <!-- 专辑列表 -->
      <ul class="divide-y divide-black/5">
        <li
          v-for="item in order.items"
          :key="item.id"
          :class="[
            'flex items-center gap-4 py-4',
            (item.status === 'REFUNDED' || item.album?.status === 'DELISTED') ? 'opacity-60' : ''
          ]"
        >
          <!-- 已退款：占位图 + 已退款标记 -->
          <div
            v-if="item.status === 'REFUNDED'"
            class="relative w-16 h-16 rounded-lg shrink-0 overflow-hidden"
            :style="{ background: item.album?.gradient || 'rgb(196,147,51)' }"
          >
            <img
              v-if="item.album?.coverUrl"
              :src="coverSrc(item.album.coverUrl)"
              class="w-full h-full object-cover grayscale"
            />
            <div class="absolute inset-0 rounded-lg bg-black/30 flex items-center justify-center">
              <span class="text-xs text-white font-medium">已退款</span>
            </div>
          </div>

          <!-- 已下架 (软删除)：显示占位图 + 遮罩，不可点击 -->
          <div
            v-else-if="item.album?.status === 'DELISTED'"
            class="relative w-16 h-16 rounded-lg shrink-0 overflow-hidden"
            :style="{ background: item.album?.gradient || 'rgb(196,147,51)' }"
          >
            <img
              v-if="item.album?.coverUrl"
              :src="coverSrc(item.album.coverUrl)"
              class="w-full h-full object-cover grayscale"
            />
            <div class="absolute inset-0 rounded-lg bg-black/30 flex items-center justify-center">
              <span class="text-xs text-white font-medium">已下架</span>
            </div>
          </div>

          <!-- 正常专辑：可点击跳转 -->
          <router-link
            v-else-if="item.album?.slug"
            :to="`/albums/${item.album.slug}`"
            class="w-16 h-16 rounded-lg shrink-0 overflow-hidden cursor-pointer hover:scale-105 transition-transform"
            :style="{ background: item.album?.gradient || 'rgb(196,147,51)' }"
          >
            <img
              v-if="item.album?.coverUrl"
              :src="coverSrc(item.album.coverUrl)"
              class="w-full h-full object-cover"
            />
          </router-link>

          <!-- 已删除 (album=null)：硬删兜底 -->
          <div
            v-else
            class="w-16 h-16 rounded-lg shrink-0 overflow-hidden bg-[rgb(196,147,51)]/10 flex items-center justify-center"
          >
            <span class="text-xs text-black/30">已下架</span>
          </div>

          <div class="flex-1 min-w-0">
            <p
              :class="[
                'text-sm font-medium truncate',
                item.status === 'REFUNDED' || item.album?.status === 'DELISTED' ? 'text-black/30 line-through' : 'text-black'
              ]"
            >
              {{ item.status === 'REFUNDED' ? '已退款' : (item.album?.status === 'DELISTED' ? '专辑已下架' : (item.album?.title ?? '专辑已下架')) }}
            </p>
            <p
              v-if="item.status !== 'REFUNDED' && item.album?.status !== 'DELISTED'"
              class="text-xs text-black/40 mt-0.5"
            >
              {{ item.album?.artist }}
            </p>
            <p
              v-if="item.status === 'REFUNDED'"
              class="text-xs text-green-600 font-medium mt-0.5"
            >
              已退款 ¥{{ item.unitPrice * item.quantity }}
            </p>
          </div>

          <span class="text-sm text-black/50">&times;{{ item.quantity }}</span>
          <span class="text-sm font-medium text-black w-20 text-right">&yen;{{ item.unitPrice }}</span>
        </li>
      </ul>

      <!-- 分割线 -->
      <hr class="border-[rgb(196,147,51)]/20 mt-6 mb-5">

      <!-- 汇总 -->
      <div class="flex justify-end items-baseline gap-4">
        <span class="text-sm text-black/50">共 {{ order.items.length }} 件</span>
        <span class="text-sm text-black/50">总价</span>
        <span class="text-xl font-semibold tracking-[-0.02em]">&yen;{{ order.totalAmount }}</span>
      </div>

      <!-- 操作按钮 -->
      <div v-if="order.status === 'PENDING'" class="flex gap-3 mt-8 pt-6 border-t border-black/5">
        <button
          class="text-sm px-5 py-2 rounded-lg border border-black/15 text-black/50 hover:text-red-600 hover:border-red-300 transition-colors"
          @click="onCancel"
        >
          取消订单
        </button>
        <button
          class="text-sm px-5 py-2 rounded-lg border border-[rgb(196,147,51)] bg-[rgb(196,147,51)] text-white hover:bg-[rgb(176,127,31)] transition-colors"
          @click="onPay"
        >
          继续付款
        </button>
      </div>
    </template>

    <div v-else class="text-center py-20">
      <p class="text-[17px] text-black/40 mb-4">订单不存在</p>
      <router-link to="/orders" class="text-[rgb(196,147,51)] hover:underline text-[15px]">返回订单列表</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchOrderById, cancelOrder, payOrder } from '@vinyl-store/shared'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const loading = ref(true)
const countdown = ref('')
let timer = null

function tick() {
  if (!order.value || order.value.status !== 'PENDING' || !order.value.expiresAt) {
    countdown.value = ''
    return
  }
  const left = new Date(order.value.expiresAt).getTime() - Date.now()
  if (left <= 0) {
    countdown.value = '订单已超时'
    clearInterval(timer)
    return
  }
  const m = Math.floor(left / 60000)
  const s = Math.floor((left % 60000) / 1000)
  countdown.value = `${m}分${s}秒后自动关闭`
}

const statusMap = {
  PENDING:  { label: '待付款', style: 'text-[rgb(196,147,51)] border-[rgb(196,147,51)]/30 bg-[rgb(196,147,51)]/5' },
  PAID:     { label: '待收货', style: 'text-blue-600 border-blue-200 bg-blue-50' },
  SHIPPED:  { label: '待收货', style: 'text-blue-600 border-blue-200 bg-blue-50' },
  DELIVERED:{ label: '已完成', style: 'text-green-700 border-green-200 bg-green-50' },
  CANCELLED:{ label: '已取消', style: 'text-black/40 border-black/10 bg-black/5' },
}

const statusLabel = computed(() => statusMap[order.value?.status]?.label ?? '')
const statusStyle = computed(() => statusMap[order.value?.status]?.style ?? '')

function formatDate(d) {
  return new Date(d).toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit',
  })
}

function coverSrc(url) {
  if (!url) return ''
  return url.startsWith('http') ? url : `/${url}`
}

async function onCancel() {
  if (!confirm('确定要取消此订单吗？')) return
  await cancelOrder(order.value.id)
  await load()
}

async function onPay() {
  if (!confirm(`确认从余额支付 ¥${order.value.totalAmount}？`)) return
  try {
    await payOrder(order.value.id)
    await load()
  } catch (e) {
    alert(e.response?.data?.message || '付款失败')
  }
}

async function load() {
  loading.value = true
  try {
    order.value = await fetchOrderById(route.params.id)
    tick()
    clearInterval(timer)
    timer = setInterval(tick, 1000)
  } finally {
    loading.value = false
  }
}

onMounted(() => load())
onUnmounted(() => clearInterval(timer))
</script>

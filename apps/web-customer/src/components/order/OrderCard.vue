<template>
  <div class="border border-black/5 bg-white">
    <!-- 头部 -->
    <div class="flex items-center justify-between px-6 py-4 border-b border-black/5">
      <div>
        <time class="text-sm text-gray-500">{{ formatDate(order.createdAt) }}</time>
        <span v-if="countdown" class="text-xs text-red-400 ml-3">{{ countdown }}</span>
      </div>
      <span :class="['text-xs px-3 py-1', statusStyle]">
        {{ statusLabel }}
      </span>
    </div>

    <!-- 专辑列表 -->
    <ul class="divide-y divide-black/5">
      <li
        v-for="item in order.items"
        :key="item.id"
        :class="[
          'flex items-center gap-4 px-6 py-4',
          (item.status === 'REFUNDED' || item.album?.status === 'DELISTED') ? 'opacity-40' : ''
        ]"
      >
        <div class="w-12 h-12 shrink-0 overflow-hidden" :style="{ background: item.album?.gradient || '#eee' }">
          <img v-if="item.album?.coverUrl" :src="coverSrc(item.album.coverUrl)" class="w-full h-full object-cover" />
        </div>
        <div class="flex-1 min-w-0">
          <p class="text-sm text-black truncate">{{ item.album?.title ?? '专辑已下架' }}</p>
          <p class="text-xs text-gray-400">{{ item.album?.artist }}</p>
        </div>
        <span class="text-sm text-gray-400">&times;{{ item.quantity }}</span>
        <span class="text-sm text-black w-20 text-right">&yen;{{ item.unitPrice * item.quantity }}</span>
      </li>
    </ul>

    <!-- 底部 -->
    <div class="flex items-center justify-between px-6 py-4 border-t border-black/5 bg-gray-50/50">
      <div class="flex items-center gap-2">
        <template v-if="order.status === 'PENDING'">
          <button class="text-xs px-4 py-2 border border-gray-200 text-gray-500 hover:text-red-500 hover:border-red-200 transition-colors" @click="emit('cancel')">取消订单</button>
          <button class="text-xs px-4 py-2 border border-gray-200 text-gray-500 hover:text-black hover:border-gray-400 transition-colors" @click="emit('detail')">订单详情</button>
          <button class="text-xs px-4 py-2 bg-black text-white hover:bg-black/80 transition-colors" @click="emit('pay')">继续付款</button>
        </template>

        <template v-if="order.status === 'PAID' || order.status === 'SHIPPED'">
          <button class="text-xs px-4 py-2 border border-gray-200 text-gray-500 hover:text-black hover:border-gray-400 transition-colors" @click="emit('detail')">订单详情</button>
        </template>

        <button v-if="order.status === 'DELIVERED'" class="text-xs px-4 py-2 border border-[rgb(196,147,51)]/30 text-[rgb(196,147,51)] hover:bg-[rgb(196,147,51)]/5 transition-colors" @click="emit('buyAgain')">再次购买</button>
      </div>

      <div class="flex items-center gap-3 text-sm">
        <span class="text-gray-400">共 {{ order.items.length }} 件</span>
        <span class="font-semibold text-black">&yen;{{ order.totalAmount }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'

const props = defineProps({ order: Object })
const emit = defineEmits(['cancel', 'pay', 'detail', 'buyAgain'])

const statusMap = {
  PENDING:  { label: '待付款', style: 'bg-yellow-50 text-yellow-700' },
  PAID:     { label: '待收货', style: 'bg-blue-50 text-blue-600' },
  SHIPPED:  { label: '待收货', style: 'bg-blue-50 text-blue-600' },
  DELIVERED:{ label: '已完成', style: 'bg-green-50 text-green-600' },
  CANCELLED:{ label: '已取消', style: 'bg-gray-100 text-gray-400' },
}

const statusLabel = computed(() => statusMap[props.order.status]?.label ?? props.order.status)
const statusStyle = computed(() => statusMap[props.order.status]?.style ?? '')

const countdown = ref('')
let timer = null

function tick() {
  if (props.order.status !== 'PENDING' || !props.order.expiresAt) {
    countdown.value = ''
    return
  }
  const left = new Date(props.order.expiresAt).getTime() - Date.now()
  if (left <= 0) {
    countdown.value = '订单已超时'
    clearInterval(timer)
    return
  }
  const m = Math.floor(left / 60000)
  const s = Math.floor((left % 60000) / 1000)
  countdown.value = `${m}分${s}秒后自动关闭`
}

onMounted(() => {
  tick()
  timer = setInterval(tick, 1000)
})

onUnmounted(() => clearInterval(timer))

function formatDate(d) {
  return new Date(d).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function coverSrc(url) {
  if (!url) return ''
  return url.startsWith('http') ? url : `/${url}`
}
</script>

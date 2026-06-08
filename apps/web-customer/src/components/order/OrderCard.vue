<template>
  <div class="border border-[rgb(196,147,51)]/30 rounded-lg overflow-hidden bg-white">
    <!-- 头部：时间 + 状态 -->
    <div class="flex items-center justify-between px-5 py-3 border-b border-black/10">
      <div>
        <time class="text-sm text-black/60">{{ formatDate(order.createdAt) }}</time>
        <span v-if="countdown" class="text-xs text-red-400 ml-3">{{ countdown }}</span>
      </div>
      <span :class="['text-xs font-medium px-3 py-1 rounded-full border', statusStyle]">
        {{ statusLabel }}
      </span>
    </div>

    <!-- 收货地址 -->
    <div v-if="order.shippingAddress" class="px-5 py-2.5 border-b border-black/5 bg-black/[0.01]">
      <span class="text-xs text-black/40 mr-2">收货地址</span>
      <span class="text-xs text-black/60">{{ order.shippingAddress }}</span>
    </div>

    <!-- 专辑列表 -->
    <ul class="divide-y divide-black/5">
      <li
        v-for="item in order.items"
        :key="item.id"
        :class="[
          'flex items-center gap-3 px-5 py-3',
          (item.status === 'REFUNDED' || item.album?.status === 'DELISTED') ? 'bg-black/[0.02] opacity-60' : ''
        ]"
      >
        <div class="relative w-10 h-10 rounded shrink-0 overflow-hidden" :style="{ background: item.album?.gradient }">
          <img
            v-if="item.album?.coverUrl"
            :src="coverSrc(item.album.coverUrl)"
            class="w-full h-full object-cover"
          />
          <!-- 已下架遮罩 -->
          <div
            v-if="item.album?.status === 'DELISTED'"
            class="absolute inset-0 rounded bg-black/30 flex items-center justify-center"
          >
            <span class="text-[8px] text-white font-medium">下架</span>
          </div>
        </div>
        <span
          :class="[
            'flex-1 text-sm truncate',
            item.status === 'REFUNDED' || item.album?.status === 'DELISTED' ? 'text-black/30 line-through' : 'text-black/80'
          ]"
        >
          {{ item.status === 'REFUNDED' ? '已退款' : (item.album?.status === 'DELISTED' ? '专辑已下架' : (item.album?.title ?? '专辑已下架')) }}
        </span>
        <span
          v-if="item.status === 'REFUNDED'"
          class="text-[11px] text-green-600 font-medium shrink-0"
        >已退款 ¥{{ item.unitPrice * item.quantity }}</span>
        <span class="text-sm text-black/60">&times;{{ item.quantity }}</span>
        <span class="text-sm font-medium text-black w-16 text-right">&yen;{{ item.unitPrice }}</span>
      </li>
    </ul>

    <!-- 底部：操作按钮 + 汇总 -->
    <div class="flex items-center justify-between px-5 py-3 border-t border-[rgb(196,147,51)]/30 bg-[rgb(196,147,51)]/5 gap-4">
      <!-- 操作按钮 -->
      <div class="flex items-center gap-2 flex-wrap">
        <!-- 待付款 -->
        <template v-if="order.status === 'PENDING'">
          <button
            class="text-xs px-3 py-1.5 rounded border border-black/15 text-black/50 hover:text-red-600 hover:border-red-300 transition-colors"
            @click="emit('cancel')"
          >
            取消订单
          </button>
          <button
            class="text-xs px-3 py-1.5 rounded border border-black/15 text-black/50 hover:text-black hover:border-black/40 transition-colors"
            @click="emit('detail')"
          >
            订单详情
          </button>
          <button
            class="text-xs px-3 py-1.5 rounded border border-[rgb(196,147,51)] bg-[rgb(196,147,51)] text-white hover:bg-[rgb(176,127,31)] transition-colors"
            @click="emit('pay')"
          >
            继续付款
          </button>
        </template>

        <!-- 待收货 -->
        <template v-if="order.status === 'PAID' || order.status === 'SHIPPED'">
          <button
            class="text-xs px-3 py-1.5 rounded border border-black/15 text-black/50 hover:text-black hover:border-black/40 transition-colors"
            @click="emit('detail')"
          >
            订单详情
          </button>
        </template>

        <!-- 已完成 -->
        <button
          v-if="order.status === 'DELIVERED'"
          class="text-xs px-3 py-1.5 rounded border border-[rgb(196,147,51)]/50 text-[rgb(196,147,51)] hover:bg-[rgb(196,147,51)]/10 transition-colors"
          @click="emit('buyAgain')"
        >
          再次购买
        </button>
      </div>

      <!-- 汇总 -->
      <div class="flex items-center gap-3 shrink-0">
        <span class="text-sm text-black/60">共 {{ order.items.length }} 件</span>
        <span class="text-base font-semibold tracking-[-0.02em]">
          <span class="text-sm font-normal text-black/60 mr-1">总价</span>
          &yen;{{ order.totalAmount }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'

const props = defineProps({ order: Object })
const emit = defineEmits(['cancel', 'pay', 'detail', 'buyAgain'])

const statusMap = {
  PENDING:  { label: '待付款', style: 'text-[rgb(196,147,51)] border-[rgb(196,147,51)]/30 bg-[rgb(196,147,51)]/5' },
  PAID:     { label: '待收货', style: 'text-blue-600 border-blue-200 bg-blue-50' },
  SHIPPED:  { label: '待收货', style: 'text-blue-600 border-blue-200 bg-blue-50' },
  DELIVERED:{ label: '已完成', style: 'text-green-700 border-green-200 bg-green-50' },
  CANCELLED:{ label: '已取消', style: 'text-black/40 border-black/10 bg-black/5' },
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

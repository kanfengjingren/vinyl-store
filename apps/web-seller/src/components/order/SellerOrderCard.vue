<template>
  <div class="border border-black/10 rounded-xl bg-white p-5 hover:shadow-sm transition-shadow">
    <div class="flex items-start justify-between mb-3">
      <div>
        <p class="text-sm font-semibold text-black">订单 #{{ order.id }}</p>
        <p class="text-xs text-black/40 mt-0.5">
          {{ order.user?.name || order.user?.email || '--' }}
          <span class="mx-1">·</span>
          ¥{{ order.totalAmount }}
        </p>
      </div>
      <div :class="[
        'px-3 py-1 rounded-full text-xs font-medium',
        order.status === 'PAID' && !hasAnyShipped ? 'bg-yellow-50 text-yellow-700' :
          order.status === 'PAID' ? 'bg-blue-50 text-blue-700' :
            'bg-green-50 text-green-700'
      ]">
        {{ order.status === 'PAID' && !hasAnyShipped ? '待发货' : order.status === 'PAID' ? '部分发货' : '已完成' }}
      </div>
    </div>

    <div class="space-y-1.5">
      <div v-for="item in order.items" :key="item.id"
        :class="[
          'flex items-center gap-3 text-sm rounded-lg',
          (item.status === 'REFUNDED' || item.album?.status === 'DELISTED') ? 'bg-black/[0.03] opacity-60' : ''
        ]"
      >
        <div class="relative shrink-0">
          <img
            v-if="item.album?.coverUrl"
            :src="coverSrc(item.album.coverUrl)"
            class="w-8 h-8 rounded object-cover bg-black/5"
          />
          <div v-else class="w-8 h-8 rounded bg-black/5 flex items-center justify-center">
            <span class="text-[10px] text-black/30">无图</span>
          </div>
          <!-- 已下架遮罩 -->
          <div
            v-if="item.album?.status === 'DELISTED'"
            class="absolute inset-0 rounded bg-black/20 flex items-center justify-center"
          >
            <span class="text-[8px] text-white font-medium">已下架</span>
          </div>
        </div>

        <span :class="[
          'flex-1 truncate',
          item.status === 'REFUNDED' || item.album?.status === 'DELISTED' ? 'text-black/30 line-through' : 'text-black/70'
        ]">
          {{ item.album?.title || '已删除专辑' }}
        </span>

        <!-- SHIPPED 标签 -->
        <span
          v-if="item.status === 'SHIPPED'"
          class="text-[11px] text-blue-600 font-medium shrink-0"
        >
          已发货
        </span>

        <!-- REFUNDED 标签 -->
        <span
          v-if="item.status === 'REFUNDED'"
          class="text-[11px] text-green-600 font-medium shrink-0"
        >
          已退款 ¥{{ item.unitPrice * item.quantity }}
        </span>

        <!-- 已下架未退款 → 确认退款按钮 -->
        <button
          v-else-if="item.album?.status === 'DELISTED' && order.status === 'PAID'"
          class="text-[11px] px-2 py-1 rounded border border-red-300 bg-red-50 text-red-500 hover:bg-red-100 transition-colors shrink-0"
          @click="onRefund(item)"
        >
          确认退款 ¥{{ item.unitPrice * item.quantity }}
        </button>

        <!-- 已下架但订单非 PAID（已发货等）→ 仅提示 -->
        <span
          v-else-if="item.album?.status === 'DELISTED'"
          class="text-[11px] text-red-400 font-medium shrink-0"
        >
          需退款 ¥{{ item.unitPrice * item.quantity }}
        </span>

        <span class="text-black/40">x{{ item.quantity }}</span>
        <span :class="item.status === 'REFUNDED' || item.album?.status === 'DELISTED' ? 'text-black/30' : 'text-black/60'">
          ¥{{ item.unitPrice * item.quantity }}
        </span>
      </div>
    </div>

    <div v-if="order.shippingAddress" class="text-xs text-black/30 mt-3 pt-3 border-t border-black/5">
      收货地址：{{ order.shippingAddress }}
    </div>

    <div v-if="order.status === 'PAID'" class="mt-4 pt-3 border-t border-black/5 flex justify-end items-center gap-3">
      <span v-if="!hasActiveItem && hasAnyShipped" class="text-xs text-blue-600 font-medium">该订单你的商品已全部发货，等待其他卖家发货</span>
      <span v-else-if="!hasActiveItem" class="text-xs text-black/40">该订单商品已全部退款</span>
      <span v-else-if="hasDelistedUnrefunded" class="text-xs text-red-500 font-medium">请先对已下架专辑退款后再发货</span>
      <button
        v-if="hasActiveItem && !hasDelistedUnrefunded"
        @click="$emit('ship', order)"
        class="px-5 py-2 bg-[rgb(196,147,51)] text-white text-sm font-medium rounded-full hover:bg-[rgb(176,127,31)] transition-colors"
      >
        确认发货
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { refundOrderItem, useModalStore } from '@vinyl-store/shared'
import { useSellerAuthStore } from '../../stores/auth'

const props = defineProps({ order: Object })
const emit = defineEmits(['ship', 'refresh'])
const modal = useModalStore()
const auth = useSellerAuthStore()

const hasActiveItem = computed(() =>
  props.order.items.some(item => item.status === 'ACTIVE')
)

const hasAnyShipped = computed(() =>
  props.order.items.some(item => item.status === 'SHIPPED')
)

const hasDelistedUnrefunded = computed(() =>
  props.order.items.some(
    item => item.status === 'ACTIVE' && item.album?.status === 'DELISTED'
  )
)

async function onRefund(item) {
  const ok = await modal.open({
    message: `确认对「${item.album?.title}」退款 ¥${item.unitPrice * item.quantity}？`,
    confirmText: '确认退款',
    cancelText: '取消',
  })
  if (!ok) return
  try {
    await refundOrderItem(props.order.id, item.id)
    await auth.checkAuth() // 刷新卖家余额（买家退款回到买家，卖家余额不变；但后续可能涉及退卖家款）
    emit('refresh')
  } catch (e) {
    console.error('退款失败', e)
  }
}

function coverSrc(url) {
  if (!url) return '';
  return url.startsWith('http') ? url : `/${url}`;
}
</script>

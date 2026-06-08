<template>
  <div class="border border-[rgb(196,147,51)]/20 rounded-xl bg-white overflow-hidden">
    <!-- 头部 -->
    <div class="flex items-center justify-between px-5 py-3 border-b border-black/10">
      <div>
        <span class="text-sm font-medium text-black">{{ order.user?.name || order.user?.email }}</span>
        <span class="text-xs text-black/40 ml-3">{{ formatDate(order.createdAt) }}</span>
      </div>
      <span :class="['text-xs font-medium px-3 py-1 rounded-full border', order.status === 'PAID' ? 'text-blue-600 border-blue-200 bg-blue-50' : 'text-green-700 border-green-200 bg-green-50']">
        {{ order.status === 'PAID' ? '待发货' : '已发货' }}
      </span>
    </div>

    <!-- 收货地址 -->
    <div v-if="order.shippingAddress" class="px-5 py-2.5 border-b border-black/5 bg-black/[0.01]">
      <span class="text-xs text-black/40 mr-2">收货地址</span>
      <span class="text-xs text-black/60">{{ order.shippingAddress }}</span>
    </div>

    <!-- 专辑列表 -->
    <ul class="divide-y divide-black/5">
      <li v-for="item in order.items" :key="item.id" class="flex items-center gap-3 px-5 py-2.5">
        <div
          class="w-8 h-8 rounded shrink-0 overflow-hidden"
          :style="{ background: item.album?.gradient }"
        >
          <img
            v-if="item.album?.coverUrl"
            :src="coverSrc(item.album.coverUrl)"
            class="w-full h-full object-cover"
          />
        </div>
        <span class="flex-1 text-sm text-black/80 truncate">{{ item.album?.title ?? '已下架' }}</span>
        <span class="text-sm text-black/50">&times;{{ item.quantity }}</span>
        <span class="text-sm font-medium text-black w-16 text-right">&yen;{{ item.unitPrice }}</span>
      </li>
    </ul>

    <!-- 底部 -->
    <div class="flex items-center justify-between px-5 py-3 border-t border-[rgb(196,147,51)]/20 bg-[rgb(196,147,51)]/3">
      <span class="text-sm text-black/50">共 {{ order.items.length }} 件</span>
      <div class="flex items-center gap-4">
        <span class="text-base font-semibold">&yen;{{ order.totalAmount }}</span>
        <button
          v-if="order.status === 'PAID'"
          class="text-sm px-5 py-2 rounded-lg bg-[rgb(196,147,51)] text-white hover:bg-[rgb(176,127,31)] transition-colors"
          @click="emit('ship')"
        >
          发货
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({ order: Object })
const emit = defineEmits(['ship'])

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
</script>

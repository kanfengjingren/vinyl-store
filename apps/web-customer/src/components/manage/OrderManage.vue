<template>
  <div>
    <h2 class="text-xl font-semibold tracking-[-0.02em] mb-6">订单管理</h2>

    <!-- Tabs -->
    <div class="flex gap-1 mb-6 border-b border-black/10">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        @click="activeTab = tab.key"
        :class="[
          'px-5 py-2.5 text-sm font-medium transition-colors relative -mb-[1px]',
          activeTab === tab.key
            ? 'text-black border-b-2 border-[rgb(196,147,51)]'
            : 'text-black/50 hover:text-black/80'
        ]"
      >
        {{ tab.label }}
      </button>
    </div>

    <div v-if="loading" class="text-center py-16 text-black/40">加载中...</div>

    <div v-else-if="!filteredOrders.length" class="text-center py-16 text-black/40">
      <p class="text-[15px]">{{ activeTab === 'pending' ? '暂无待处理订单' : '暂无已完成订单' }}</p>
    </div>

    <div v-else class="space-y-4">
      <AdminOrderCard
        v-for="order in filteredOrders"
        :key="order.id"
        :order="order"
        @ship="onShip(order)"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import AdminOrderCard from '../admin/AdminOrderCard.vue'
import { fetchSellerOrders, shipOrder } from '@vinyl-store/shared'

const orders = ref([])
const loading = ref(false)
const activeTab = ref('pending')

const tabs = [
  { key: 'pending', label: '待处理' },
  { key: 'completed', label: '已处理' },
]

const filteredOrders = computed(() => {
  const status = activeTab.value === 'pending' ? 'PAID' : 'DELIVERED'
  return orders.value.filter(o => o.status === status)
})

async function load() {
  loading.value = true
  try {
    orders.value = await fetchSellerOrders()
  } finally {
    loading.value = false
  }
}

async function onShip(order) {
  if (!confirm(`确认对订单 #${order.id} 发货？发货后状态将变为已完成。`)) return
  await shipOrder(order.id)
  await load()
}

onMounted(() => load())
</script>

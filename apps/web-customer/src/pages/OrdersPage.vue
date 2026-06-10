<template>
  <div class="max-w-[1000px] mx-auto px-8 py-12">
    <h1 class="text-[clamp(28px,4vw,36px)] font-semibold tracking-[-0.02em] mb-8">我的订单</h1>

    <!-- Tabs -->
    <div class="flex gap-1 mb-8 border-b border-black/10">
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

    <!-- Content -->
    <div v-if="loading" class="text-center py-20 text-black/40">加载中...</div>
    <div v-else-if="!filteredOrders.length" class="text-center py-20">
      <p class="text-[17px] text-black/40 mb-4">暂无订单</p>
      <router-link to="/" class="text-[rgb(196,147,51)] hover:underline text-[15px]">去选购 &rarr;</router-link>
    </div>

    <div v-else class="space-y-4">
      <OrderCard
        v-for="order in filteredOrders"
        :key="order.id"
        :order="order"
        @cancel="onCancel(order)"
        @pay="onPay(order)"
        @detail="onDetail(order)"
        @buyAgain="onBuyAgain(order)"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import OrderCard from '../components/order/OrderCard.vue'
import { fetchOrders, cancelOrder, payOrder } from '@vinyl-store/shared'

const router = useRouter()
const orders = ref([])
const loading = ref(false)
const activeTab = ref('all')

const tabs = [
  { key: 'all', label: '全部' },
  { key: 'PENDING', label: '待付款' },
  { key: 'receiving', label: '待收货' },
  { key: 'DELIVERED', label: '已完成' },
]

const filteredOrders = computed(() => {
  if (activeTab.value === 'all') return orders.value
  if (activeTab.value === 'receiving') {
    return orders.value.filter(o => o.status === 'PAID' || o.status === 'SHIPPED')
  }
  return orders.value.filter(o => o.status === activeTab.value)
})

async function load() {
  loading.value = true
  try {
    orders.value = await fetchOrders()
  } finally {
    loading.value = false
  }
}

async function onCancel(order) {
  if (!confirm('确定要取消此订单吗？')) return
  await cancelOrder(order.id)
  await load()
}

async function onPay(order) {
  if (!confirm(`确认从余额支付 ¥${order.totalAmount}？`)) return
  try {
    await payOrder(order.id)
    await load()
  } catch (e) {
    alert(e.response?.data?.message || '付款失败')
  }
  
}

function onDetail(order) {
  router.push(`/orders/${order.id}`)
}

function onBuyAgain(order) {

  const first = order.items[0]
  if (first?.album?.slug) {
    router.push(`/albums/${first.album.slug}`)
  }
}

onMounted(() => load())
</script>

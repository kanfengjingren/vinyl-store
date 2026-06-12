<template>
  <div class="bg-white">
    <div v-if="!cart.items.length && !submitted" class="text-center py-20 text-black/40">
      <p class="text-[17px] mb-4">购物车为空</p>
      <router-link to="/" class="text-[rgb(196,147,51)] hover:underline text-[15px]">去选购 &rarr;</router-link>
    </div>

    <template v-else-if="!submitted">
      <div class="flex max-md:flex-col" style="height: calc(100vh - 64px)">
        <!-- 左侧：收货地址 -->
        <div class="flex-1 flex justify-center py-10 px-10 max-md:p-6 border-r border-black/5 max-md:border-r-0 max-md:border-b">
          <div class="w-full max-w-[440px] flex flex-col">
            <div>
              <h2 class="text-lg font-semibold tracking-[-0.01em] mb-6">收货地址</h2>
              <CitySelect v-model="shippingAddress" v-model:detail="addressDetail" />
              <p class="text-xs text-black/30 mt-4">修改仅对本次订单生效，不会更新默认地址</p>
            </div>
            <router-link to="/"
              class="mt-auto mb-0 text-sm font-medium px-6 py-2.5 bg-white text-black border border-gray-300 hover:border-black transition-all no-underline self-start">
              取消订单
            </router-link>
          </div>
        </div>

        <!-- 右侧：订单摘要 + 确认 -->
        <div class="w-[380px] max-md:w-full flex flex-col overflow-hidden">
          <h2 class="text-lg font-semibold tracking-[-0.01em] px-10 pt-10 pb-4 shrink-0">订单内容</h2>

          <!-- 专辑条目（可滚动） -->
          <div class="flex-1 overflow-y-auto px-10">
            <div v-for="item in cart.items" :key="item.id" class="flex items-center gap-4 py-3 border-b border-black/5 last:border-0">
              <div class="w-12 h-12 bg-black/5 shrink-0 overflow-hidden">
                <img v-if="item.album.coverUrl" :src="item.album.coverUrl" class="w-full h-full object-cover" />
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-sm font-medium truncate">{{ item.album.artist }}</p>
                <p class="text-xs text-black/50 truncate">{{ item.album.title }}</p>
              </div>
              <span class="text-xs text-black/40">&times;{{ item.quantity }}</span>
              <span class="text-sm font-medium">&yen;{{ item.album.price * item.quantity }}</span>
            </div>
          </div>

          <!-- 总计 + 余额（固定在底部） -->
          <div class="shrink-0 px-10 pb-10 pt-4 border-t border-black/5 space-y-2">
            <div class="flex justify-between text-sm">
              <span class="text-black/50">小计</span>
              <span>&yen;{{ cart.total }}</span>
            </div>
            <div class="flex justify-between text-sm">
              <span class="text-black/50">账户余额</span>
              <span :class="balance < cart.total ? 'text-red-500' : 'text-green-600'">&yen;{{ balance }}</span>
            </div>
            <div class="flex justify-between text-lg font-semibold pt-2 border-t border-black/5">
              <span>应付</span>
              <span>&yen;{{ cart.total }}</span>
            </div>

            <p v-if="balance < cart.total" class="text-xs text-red-400 mt-1">余额不足，请先充值</p>
            <p v-if="error" class="text-xs text-red-400 mt-1">{{ error }}</p>

            <button @click="confirmCheckout" :disabled="loading || balance < cart.total"
              class="w-full mt-4 py-3.5 bg-black text-white text-[15px] font-semibold border-none cursor-pointer hover:bg-black/80 transition-all disabled:opacity-40 disabled:cursor-not-allowed">
              {{ loading ? '提交中...' : '确认下单' }}
            </button>
          </div>
        </div>
      </div>
    </template>

    <!-- Success -->
    <div v-else class="text-center py-20">
      <div class="w-16 h-16 bg-green-500 text-white flex items-center justify-center text-2xl mx-auto mb-6">&#10003;</div>
      <h2 class="text-[24px] font-semibold mb-2">下单成功！</h2>
      <p class="text-black/50 mb-6">订单已创建，我们会在发货时通知你。</p>
      <div class="flex gap-3 justify-center">
        <router-link to="/orders" class="px-6 py-2.5 bg-black text-white text-[15px] no-underline hover:bg-black/80 transition-all">查看订单</router-link>
        <router-link to="/" class="px-6 py-2.5 bg-black/5 text-black/80 text-[15px] no-underline hover:bg-black/10 transition-all">继续购物</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useCartStore } from '../stores/cart'
import { fetchProfile, checkout as checkoutApi } from '@vinyl-store/shared'
import CitySelect from '@vinyl-store/shared/ui/CitySelect'

const cart = useCartStore()
const loading = ref(false)
const error = ref('')
const submitted = ref(false)
const shippingAddress = ref('')
const addressDetail = ref('')
const balance = ref(0)

onMounted(async () => {
  try {
    const profile = await fetchProfile()
    shippingAddress.value = profile.defaultAddress || ''
    balance.value = profile.balance ?? 0
  } catch {
    shippingAddress.value = ''
  }
})

async function confirmCheckout() {
  error.value = ''
  loading.value = true
  try {
    await checkoutApi(shippingAddress.value.trim() || undefined)
    submitted.value = true
    await cart.refresh()
  } catch (e) {
    error.value = e.response?.data?.message || '下单失败'
  } finally {
    loading.value = false
  }
}
</script>

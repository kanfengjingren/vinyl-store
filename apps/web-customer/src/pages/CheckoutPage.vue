<template>
  <div class="max-w-[700px] mx-auto px-6 py-12">
    <h1 class="text-[clamp(28px,4vw,36px)] font-semibold tracking-[-0.02em] mb-8">确认订单</h1>

    <div v-if="!cart.items.length && !submitted" class="text-center py-20 text-black/40">
      <p class="text-[17px] mb-4">购物车为空</p>
      <router-link to="/" class="text-[rgb(196,147,51)] hover:underline text-[15px]">去选购 &rarr;</router-link>
    </div>

    <template v-else-if="!submitted">
      <!-- 订单摘要 -->
      <div class="bg-white rounded-[18px] p-6 mb-6 border border-black/5">
        <h3 class="text-[15px] font-semibold mb-4">订单摘要</h3>
        <div v-for="item in cart.items" :key="item.id" class="flex items-center gap-3 py-2 border-b border-black/5 last:border-0">
          <span class="flex-1 truncate text-sm">{{ item.album.artist }} — {{ item.album.title }}</span>
          <span class="text-sm text-black/50">&times;{{ item.quantity }}</span>
          <span class="font-medium text-sm">&yen;{{ item.album.price * item.quantity }}</span>
        </div>
      </div>

      <div class="bg-white rounded-[18px] p-6 mb-6 border border-black/5">
        <div class="flex justify-between items-baseline">
          <span class="text-black/50">总计</span>
          <span class="text-2xl font-semibold">&yen;{{ cart.total }}</span>
        </div>
        <div class="flex justify-between items-baseline mt-2 pt-2 border-t border-black/5">
          <span class="text-black/50 text-sm">账户余额</span>
          <span :class="['text-sm font-medium', balance < cart.total ? 'text-red-500' : 'text-green-600']">&yen;{{ balance }}</span>
        </div>
        <p v-if="balance < cart.total" class="text-xs text-red-400 mt-1">余额不足，请先充值</p>
      </div>

      <p v-if="error" class="text-red-400 text-sm mb-4">{{ error }}</p>

      <button @click="showAddressModal = true" :disabled="loading || balance < cart.total" class="w-full py-3.5 bg-[rgb(196,147,51)] text-white text-[15px] font-semibold border-none rounded-full cursor-pointer hover:bg-[rgb(176,127,31)] transition-all disabled:opacity-50">
        {{ loading ? '提交中...' : '确认下单' }}
      </button>

      <!-- 地址确认弹窗 -->
      <Teleport to="body">
        <div v-if="showAddressModal" class="fixed inset-0 z-[300] flex items-center justify-center" @click.self="showAddressModal = false">
          <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" />
          <div class="relative bg-white rounded-2xl shadow-2xl px-8 py-7 w-[420px] max-w-[90vw] border border-[rgb(196,147,51)]/20">
            <h3 class="text-[17px] font-semibold mb-4">确认收货地址</h3>
            <textarea
              v-model="shippingAddress"
              rows="3"
              placeholder="请填写收货地址"
              class="w-full px-4 py-3 rounded-xl border border-black/15 text-[15px] text-black/80 outline-none focus:border-[rgb(196,147,51)] focus:ring-2 focus:ring-[rgb(196,147,51)]/15 transition-all resize-none placeholder:text-black/25 mb-5"
            />
            <p class="text-xs text-black/40 mb-5">修改仅对本次订单生效，不会更新默认地址</p>
            <div class="flex justify-end gap-3">
              <button class="text-sm px-5 py-2 rounded-lg border border-black/15 text-black/50 hover:text-black hover:border-black/30 transition-colors" @click="showAddressModal = false">
                取消
              </button>
              <button class="text-sm px-5 py-2 rounded-lg bg-[rgb(196,147,51)] text-white hover:bg-[rgb(176,127,31)] transition-colors" @click="confirmCheckout">
                确认
              </button>
            </div>
          </div>
        </div>
      </Teleport>
    </template>

    <!-- Success -->
    <div v-else class="text-center py-16">
      <div class="w-16 h-16 rounded-full bg-green-500 text-white flex items-center justify-center text-2xl mx-auto mb-6">&#10003;</div>
      <h2 class="text-[24px] font-semibold mb-2">下单成功！</h2>
      <p class="text-black/50 mb-6">订单已创建，我们会在发货时通知你。</p>
      <div class="flex gap-3 justify-center">
        <router-link to="/orders" class="px-6 py-2.5 bg-[rgb(196,147,51)] text-white text-[15px] no-underline rounded-full hover:bg-[rgb(176,127,31)] transition-all">查看订单</router-link>
        <router-link to="/" class="px-6 py-2.5 bg-black/5 text-black/80 text-[15px] no-underline rounded-full hover:bg-black/10 transition-all">继续购物</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useCartStore } from '../stores/cart'
import { useAuthStore } from '../stores/auth'
import { fetchProfile, checkout as checkoutApi } from '@vinyl-store/shared'

const cart = useCartStore()
const auth = useAuthStore()
const loading = ref(false)
const error = ref('')
const submitted = ref(false)
const showAddressModal = ref(false)
const shippingAddress = ref('')
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
  showAddressModal.value = false
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

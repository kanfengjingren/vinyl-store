<template>
  <div class="max-w-[900px] mx-auto px-6 py-12">
    <h1 class="text-[clamp(28px,4vw,40px)] font-semibold tracking-[-0.02em] mb-8">购物车</h1>

    <div v-if="!cart.items.length" class="text-center py-20 text-apple-secondary">
      <p class="text-[17px] mb-4">购物车为空</p>
      <router-link to="/" class="text-apple-accent hover:underline text-[15px]">去选购 &rarr;</router-link>
    </div>

    <template v-else>
      <div class="space-y-3">
        <div v-for="item in cart.items" :key="item.id" class="bg-white rounded-[18px] p-4">
          <div class="flex gap-3">
            <!-- 封面 -->
            <router-link :to="`/albums/${item.album.slug}`" class="shrink-0 w-[88px] h-[88px] rounded-xl overflow-hidden bg-[#e0e0e2]">
              <img v-if="item.album.coverUrl" :src="coverSrc(item.album.coverUrl)" class="w-full h-full object-cover" />
              <div v-else class="w-full h-full" :style="{ background: item.album.gradient || '#e0e0e2' }"></div>
            </router-link>

            <!-- 信息 -->
            <div class="flex-1 min-w-0 flex flex-col justify-between">
              <div>
                <p class="font-semibold text-[15px] leading-snug line-clamp-1">{{ item.album.title }}</p>
                <p class="text-[13px] text-apple-secondary mt-0.5">{{ item.album.artist }}</p>
              </div>
              <p class="text-[15px] font-semibold text-apple-accent mt-1">&yen;{{ item.album.price }}</p>
            </div>
          </div>

          <!-- 底部操作栏 -->
          <div class="flex items-center justify-between mt-3 pt-3 border-t border-apple-border">
            <button @click="cart.remove(item.id)" class="text-[20px] text-apple-tertiary hover:text-red-500 transition-colors leading-none px-1">&times;</button>
            <div class="flex items-center gap-1.5">
              <button @click="handleQty(item, item.quantity - 1)" class="w-[30px] h-[30px] rounded-full border border-apple-border flex items-center justify-center text-[16px] hover:bg-black/5 transition-colors">-</button>
              <span class="w-9 text-center text-[15px] font-medium">{{ item.quantity }}</span>
              <button @click="handleQty(item, item.quantity + 1)" class="w-[30px] h-[30px] rounded-full border border-apple-border flex items-center justify-center text-[16px] hover:bg-black/5 transition-colors">+</button>
            </div>
            <span class="text-[17px] font-semibold">&yen;{{ item.album.price * item.quantity }}</span>
          </div>
        </div>
      </div>

      <div class="mt-8 bg-white rounded-[18px] p-6 flex items-center justify-between">
        <div>
          <span class="text-sm text-apple-secondary">共 {{ cart.itemCount }} 件商品</span>
        </div>
        <div class="text-right">
          <span class="text-2xl font-semibold">&yen;{{ cart.total }}</span>
        </div>
      </div>

      <div class="mt-4 text-right">
        <button v-if="auth.isLoggedIn" @click="goCheckout" class="px-10 py-3.5 bg-apple-accent text-white text-[15px] font-semibold border-none rounded-full cursor-pointer hover:bg-apple-accent-hover transition-all">
          去结算
        </button>
        <router-link v-else to="/login" class="inline-block px-10 py-3.5 bg-apple-accent text-white text-[15px] font-semibold no-underline rounded-full hover:bg-apple-accent-hover transition-all">
          登录后结算
        </router-link>
      </div>
    </template>
  </div>
</template>

<script setup>
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useCartStore } from '../stores/cart';
import { useAuthStore } from '../stores/auth';

const cart = useCartStore();
const auth = useAuthStore();
const router = useRouter();

onMounted(() => {
  cart.load();
});

function coverSrc(url) {
  if (!url) return '';
  return url.startsWith('http') ? url : `/${url}`;
}

function handleQty(item, qty) {
  if (qty <= 0) cart.remove(item.id);
  else cart.update(item.id, qty);
}

function goCheckout() {
  router.push('/checkout');
}
</script>

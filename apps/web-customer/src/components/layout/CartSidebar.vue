<template>
  <Teleport to="body">
    <Transition name="overlay-fade">
      <div v-if="cart.isOpen" class="fixed inset-0 z-[150] bg-black/40" @click="cart.close()" />
    </Transition>
    <Transition name="cart-sidebar">
      <aside v-if="cart.isOpen" class="fixed top-0 right-0 z-[160] w-[420px] h-screen bg-white shadow-[-4px_0_40px_rgba(0,0,0,.15)] flex flex-col max-md:w-screen">
        <div class="flex items-center justify-between px-6 py-4 border-b border-apple-border shrink-0">
          <div>
            <h3 class="text-[19px] font-semibold tracking-[-0.01em]">购物车</h3>
            <span class="text-sm text-apple-secondary font-normal">{{ cart.itemCount }} 件商品</span>
          </div>
          <button @click="cart.close()" class="w-8 h-8 rounded-full border-none bg-black/5 text-lg cursor-pointer text-apple-secondary flex items-center justify-center hover:bg-black/10 transition-colors">&times;</button>
        </div>

        <div v-if="!cart.items.length" class="flex-1 flex flex-col items-center justify-center text-apple-tertiary gap-2">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="w-12 h-12 opacity-40">
            <circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/>
            <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/>
          </svg>
          <p class="text-[15px]">购物车为空</p>
        </div>

        <div v-else class="flex-1 overflow-y-auto py-2">
          <div v-for="item in cart.items" :key="item.id"
            class="flex gap-3 px-6 py-3 border-b border-apple-border/50">
            <router-link :to="`/albums/${item.album.slug}`" @click="cart.close()" class="shrink-0 w-[60px] h-[60px] rounded-lg overflow-hidden bg-[#e0e0e2]">
              <img v-if="item.album.coverUrl" :src="coverSrc(item.album.coverUrl)" class="w-full h-full object-cover" />
              <div v-else class="w-full h-full" :style="{ background: item.album.gradient }"></div>
            </router-link>
            <div class="flex-1 min-w-0">
              <p class="text-[13px] font-medium truncate">{{ item.album.title }}</p>
              <p class="text-[11px] text-apple-secondary">{{ item.album.artist }}</p>
              <div class="flex items-center justify-between mt-1.5">
                <div class="flex items-center gap-1">
                  <button @click="cart.update(item.id, item.quantity - 1)" class="w-5 h-5 rounded-full border border-apple-border flex items-center justify-center text-xs text-apple-secondary hover:bg-black/5">-</button>
                  <span class="text-sm w-7 text-center">{{ item.quantity }}</span>
                  <button @click="cart.update(item.id, item.quantity + 1)" class="w-5 h-5 rounded-full border border-apple-border flex items-center justify-center text-xs text-apple-secondary hover:bg-black/5">+</button>
                </div>
                <span class="text-sm font-semibold">&yen;{{ item.album.price * item.quantity }}</span>
              </div>
            </div>
            <button @click="cart.remove(item.id)" class="self-start text-apple-tertiary hover:text-red-500 transition-colors">&times;</button>
          </div>
        </div>

        <div v-if="cart.items.length" class="shrink-0 px-6 pt-4 pb-7 border-t border-apple-border">
          <div class="flex justify-between items-baseline mb-4">
            <span class="text-sm text-apple-secondary">合计</span>
            <span class="text-2xl font-semibold tracking-[-0.02em]">&yen;{{ cart.total }}</span>
          </div>
          <button @click="goCheckout" class="w-full py-3.5 bg-apple-accent text-white text-[15px] font-semibold tracking-[.01em] border-none rounded-full cursor-pointer hover:bg-apple-accent-hover hover:scale-105 transition-all">
            结算
          </button>
        </div>
      </aside>
    </Transition>
  </Teleport>
</template>

<script setup>
import { watch } from 'vue';
import { useRouter } from 'vue-router';
import { useCartStore } from '../../stores/cart';

const cart = useCartStore();
const router = useRouter();

watch(() => cart.isOpen, (val) => {
  document.body.style.overflow = val ? 'hidden' : '';
});

function coverSrc(url) {
  if (!url) return '';
  return url.startsWith('http') ? url : `/${url}`;
}

function goCheckout() {
  cart.close();
  router.push('/checkout');
}
</script>

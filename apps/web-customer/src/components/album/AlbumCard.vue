<template>
  <div class="card bg-white overflow-hidden shadow-[0_1px_3px_rgba(0,0,0,.08)] hover:-translate-y-1.5 hover:shadow-[0_8px_40px_rgba(0,0,0,.12)] transition-all cursor-pointer"
    @click="$router.push(`/albums/${album.slug}`)">
    <div class="card-art aspect-square relative overflow-hidden" :style="{ background: album.gradient }">
      <img v-if="album.coverUrl" :src="coverSrc(album.coverUrl)" class="card-img-inner absolute inset-0 w-full h-full object-cover transition-transform duration-500 ease-out" />
      <div v-else class="w-full h-full flex items-center justify-center text-[60px] font-bold tracking-[-0.04em] text-white/25">
        {{ album.title.slice(0, 2).toUpperCase() }}
      </div>
      <span v-if="album.badge" class="absolute top-3 left-3 z-[2] text-[11px] font-semibold tracking-[.04em] bg-black/60 text-white px-2.5 py-1 rounded-[20px] backdrop-blur-md">
        {{ album.badge }}
      </span>
    </div>
    <div class="p-4 pb-5 px-[18px]">
      <p class="text-xs font-medium uppercase tracking-[.04em] text-apple-secondary mb-1">
      <router-link
        v-if="album.artistInfo?.slug"
        :to="`/artists/${album.artistInfo.slug}`"
        @click.stop
        class="no-underline text-apple-secondary hover:text-apple-accent transition-colors"
      >{{ album.artist }}</router-link>
      <span v-else>{{ album.artist }}</span>
      <span v-if="album.seller?.id" class="text-[11px] text-black/30 ml-1">
        &middot;
        <router-link
          :to="`/seller/${album.seller.id}`"
          @click.stop
          class="no-underline text-black/30 hover:text-[rgb(196,147,51)] transition-colors"
        >{{ album.seller.storeName }}</router-link>
      </span>
    </p>
      <h3 class="text-base font-semibold tracking-[-0.01em] mb-1 truncate">{{ album.title }}</h3>
      <p class="text-[13px] text-apple-tertiary mb-3">{{ album.year }} &middot; {{ album.country }}</p>
      <div class="flex items-center justify-between">
        <span class="text-lg font-semibold tracking-[-0.02em]">&yen;{{ album.price }}</span>
        <button
          v-if="purchased"
          disabled
          class="text-xs font-semibold text-gray-300 px-3.5 py-1.5 rounded-full bg-gray-50 border-none cursor-default">
          已购买
        </button>
        <button v-else @click.stop="handleBuy" class="text-xs font-semibold text-apple-accent no-underline px-3.5 py-1.5 rounded-full bg-apple-accent/10 hover:bg-apple-accent/20 transition-colors border-none cursor-pointer">
          购买
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cart'
import { useAuthStore } from '../../stores/auth'
import { useModalStore } from '@vinyl-store/shared'
import { usePurchased } from '../../composables/usePurchased'

const props = defineProps({ album: Object })
const cart = useCartStore()
const auth = useAuthStore()
const modal = useModalStore()
const router = useRouter()
const { isPurchased, load } = usePurchased()

const purchased = computed(() => isPurchased(props.album.id))

onMounted(() => { load() })

function coverSrc(url) {
  if (!url) return ''
  return url.startsWith('http') ? url : `/${url}`
}

async function handleBuy() {
  if (!auth.isLoggedIn) {
    const ok = await modal.open({
      message: '您还未登录，请先登录',
      confirmText: '去登录',
      cancelText: '取消',
    })
    if (ok) router.push('/login')
    return
  }
  cart.add(props.album)
  cart.open()
}
</script>

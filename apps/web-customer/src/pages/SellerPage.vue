<template>
  <div class="bg-white min-h-screen">
    <div v-if="loading" class="flex items-center justify-center min-h-[60vh] text-black/30 text-lg">加载中...</div>

    <div v-else-if="seller" class="max-w-[1200px] mx-auto px-6 py-12">
      <!-- 卖家信息头部 -->
      <div class="flex gap-10 max-md:flex-col max-md:gap-6 mb-14">
        <!-- 头像占位 -->
        <div class="shrink-0 w-[180px] h-[180px] bg-black/5 flex items-center justify-center text-5xl font-semibold text-black/20 tracking-[-0.03em]">
          {{ seller.storeName.slice(0, 2).toUpperCase() }}
        </div>

        <div class="flex-1 min-w-0 text-black">
          <h1 class="text-[clamp(28px,4vw,44px)] font-semibold tracking-[-0.02em] mb-2">{{ seller.storeName }}</h1>
          <p v-if="seller.contactEmail || seller.contactPhone" class="text-[15px] text-black/40 mb-2">
            {{ seller.contactEmail }}<span v-if="seller.contactEmail && seller.contactPhone"> &middot; </span>{{ seller.contactPhone }}
          </p>
          <p v-if="seller.description" class="text-[15px] text-black/60 leading-relaxed max-w-[560px] mb-5">
            {{ seller.description }}
          </p>
          <p class="text-sm text-black/30 mb-5">{{ seller.albumCount ?? seller.albums?.length ?? 0 }} 张专辑</p>

          <!-- 联系卖家按钮 -->
          <button
            v-if="auth.isLoggedIn"
            @click="chatOpen = true"
            class="inline-flex items-center gap-2 bg-black text-white text-[15px] font-medium px-7 py-3 hover:bg-black/80 hover:scale-105 transition-all cursor-pointer"
          >
            💬 联系卖家
          </button>
          <router-link
            v-else
            to="/login"
            class="inline-flex items-center gap-2 bg-black text-white text-[15px] font-medium px-7 py-3 hover:bg-black/80 hover:scale-105 transition-all no-underline"
          >
            登录后联系卖家
          </router-link>
        </div>
      </div>

      <!-- 专辑列表 -->
      <section>
        <h2 class="text-xl font-semibold tracking-[-0.01em] text-black mb-6">在售专辑</h2>
        <AlbumGrid v-if="seller.albums?.length" :albums="seller.albums" />
        <p v-else class="text-black/30 text-lg py-12 text-center">暂无在售专辑</p>
      </section>

      <!-- 聊天组件（Phase 3 接入） -->
      <ChatWidget v-if="chatOpen" v-model="chatOpen" :sellerId="seller.userId" :sellerName="seller.storeName" />
    </div>

    <div v-else class="flex items-center justify-center min-h-[60vh] text-black/30 text-lg">卖家不存在或未通过审核</div>
  </div>
</template>

<script setup>
defineOptions({ name: 'SellerPage' });
import { onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { fetchSellerById } from '@vinyl-store/shared';
import { useAuthStore } from '../stores/auth';
import AlbumGrid from '../components/album/AlbumGrid.vue';
import ChatWidget from '../components/chat/ChatWidget.vue';

const route = useRoute();
const auth = useAuthStore();
const seller = ref(null);
const loading = ref(true);
const chatOpen = ref(false);

async function load() {
  const id = Number(route.params.id);
  if (!id) return;
  loading.value = true;
  try {
    seller.value = await fetchSellerById(id);
  } catch {
    seller.value = null;
  } finally {
    loading.value = false;
  }
}

watch(() => route.params.id, load, { immediate: true });
</script>

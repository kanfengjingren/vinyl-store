<template>
  <div class="flex h-screen">
    <!-- Sidebar -->
    <aside class="w-[220px] shrink-0 border-r border-black/5 bg-white/50">
      <router-link to="/profile" class="px-6 py-5 border-b border-black/5 flex items-center gap-3 hover:bg-black/[0.02] transition-colors no-underline">
        <div class="w-9 h-9 rounded-full overflow-hidden bg-gray-100 shrink-0">
          <img v-if="auth.user?.avatar" :src="coverSrc(auth.user.avatar)" class="w-full h-full object-cover" />
          <span v-else class="w-full h-full flex items-center justify-center text-xs font-bold text-gray-400">{{ (auth.storeName || '?').slice(0, 1).toUpperCase() }}</span>
        </div>
        <div class="min-w-0">
          <div class="text-sm font-semibold tracking-[-0.02em] truncate">{{ auth.storeName || '商家后台' }}</div>
          <div class="text-[11px] text-gray-400 truncate">{{ auth.user?.name || auth.user?.email }}</div>
        </div>
      </router-link>
      <nav class="flex flex-col py-4">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          :class="[
            'px-6 py-3 text-sm transition-colors border-l-[3px]',
            $route.path === item.to
              ? 'text-black font-medium border-[rgb(196,147,51)] bg-[rgb(196,147,51)]/5'
              : 'text-black/50 border-transparent hover:text-black hover:bg-black/[0.03]'
          ]"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="px-6 mt-auto pt-4 border-t border-black/5">
        <div class="text-sm font-medium text-black/70 mb-1">余额 &yen;{{ auth.seller?.balance ?? 0 }}</div>
        <div class="text-xs text-black/40 mb-2">{{ auth.user?.email }}</div>
        <button @click="handleLogout"
          class="text-sm text-black/50 hover:text-black transition-colors">退出登录</button>
      </div>
    </aside>

    <!-- Main -->
    <main class="flex-1 overflow-auto p-8">
      <RouterView v-slot="{ Component }">
        <KeepAlive include="CreateAlbumPage">
          <component :is="Component" />
        </KeepAlive>
      </RouterView>
    </main>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useSellerAuthStore } from '../stores/auth';

const auth = useSellerAuthStore();
const router = useRouter();

const navItems = [
  { to: '/profile', label: '主页设置' },
  { to: '/albums', label: '专辑管理' },
  { to: '/create', label: '上架专辑' },
  { to: '/orders', label: '订单管理' },
  { to: '/stats', label: '数据统计' },
  { to: '/chat', label: '消息' },
];

function coverSrc(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return url.startsWith('/') ? url : `/${url}`
}

function handleLogout() {
  auth.logout();
  router.push('/login');
}

onMounted(() => {
  document.documentElement.style.overflow = 'hidden';
});

onUnmounted(() => {
  document.documentElement.style.overflow = '';
});
</script>

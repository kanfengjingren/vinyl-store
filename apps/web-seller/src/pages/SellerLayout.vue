<template>
  <div class="flex h-screen">
    <!-- Sidebar -->
    <aside class="w-[220px] shrink-0 border-r border-black/5 bg-white/50">
      <div class="px-6 py-5 text-lg font-semibold tracking-[-0.02em] border-b border-black/5">
        {{ auth.storeName || '商家后台' }}
      </div>
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
  { to: '/albums', label: '专辑管理' },
  { to: '/create', label: '上架专辑' },
  { to: '/orders', label: '订单管理' },
];

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

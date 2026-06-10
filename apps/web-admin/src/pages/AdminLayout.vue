<template>
  <div class="flex h-screen">
    <!-- Sidebar -->
    <aside class="w-[220px] shrink-0 border-r border-black/5 bg-white/50 flex flex-col">
      <div class="px-6 py-5 text-lg font-semibold tracking-[-0.02em] border-b border-black/5">
        管理后台
      </div>
      <nav class="flex flex-col py-4 flex-1">
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

      <div class="px-6 py-4 border-t border-black/5">
        <div class="text-xs text-black/40 mb-2">{{ auth.user?.email }}</div>
        <button @click="handleLogout"
          class="text-sm text-black/50 hover:text-black transition-colors">退出登录</button>
      </div>
    </aside>

    <!-- Main -->
    <main class="flex-1 overflow-auto p-8">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAdminAuthStore } from '../stores/auth';

const auth = useAdminAuthStore();
const router = useRouter();

const navItems = [
  { to: '/sellers', label: '卖家审核' },
  { to: '/artists', label: '乐队管理' },
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

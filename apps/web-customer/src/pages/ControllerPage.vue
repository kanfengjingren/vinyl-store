<template>
  <div class="flex h-[calc(100vh-52px)]">
    <!-- 侧边栏 -->
    <aside class="w-[200px] shrink-0 border-r border-black/5 bg-white/50 overflow-hidden">
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
    </aside>

    <!-- 内容区 -->
    <main class="flex-1 overflow-auto p-8">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'

const navItems = [
  { to: '/admin/album-list', label: '专辑列表' },
  { to: '/admin/create', label: '上传专辑' },
  { to: '/admin/orders', label: '订单管理' },
]

onMounted(() => {
  document.documentElement.style.overflow = 'hidden'
})

onUnmounted(() => {
  document.documentElement.style.overflow = ''
})
</script>

<template>
  <nav class="sticky top-0 z-[100] nav-blur border-b border-black/10 ">
    <div class="max-w-[1200px] mx-auto flex items-center justify-between px-6 h-[52px]">
      <div class="flex items-center gap-10">
        <router-link to="/" class="text-xl font-semibold tracking-[-0.02em] text-apple-text no-underline">
          幻觉贸易
        </router-link>
        <ul v-if="!auth.isAdmin" class="hidden md:flex gap-7 list-none">
          <li><router-link to="/catalog?category=symphonic"
              class="text-[13px] text-apple-secondary no-underline hover:text-apple-text transition-colors">全部唱片</router-link>
          </li>
          <li><a href="#"
              class="text-[13px] text-apple-secondary no-underline hover:text-apple-text transition-colors">新品上架</a>
          </li>
          <li><a href="#"
              class="text-[13px] text-apple-secondary no-underline hover:text-apple-text transition-colors">关于我们</a>
          </li>
        </ul>
      </div>
      <div class="flex items-center gap-3">
        <template v-if="auth.isLoggedIn">
          <router-link v-if="!auth.isAdmin" to="/orders"
            class="text-[13px] text-apple-secondary no-underline hover:text-apple-text transition-colors">订单</router-link>
          <router-link v-if="!auth.isAdmin" to="/messages"
            class="text-[13px] text-apple-secondary no-underline hover:text-apple-text transition-colors relative">
            消息
            <span v-if="unreadCount > 0"
              class="absolute -top-1.5 -right-3 min-w-[16px] h-[16px] px-[4px] bg-red-500 text-white text-[10px] font-semibold leading-[16px] text-center rounded-[8px]">
              {{ unreadCount > 99 ? '99+' : unreadCount }}
            </span>
          </router-link>
          <router-link v-if="!auth.isAdmin" to="/profile" class="flex items-center gap-2 text-[13px] text-apple-secondary no-underline hover:text-apple-text transition-colors">
            <span class="w-6 h-6 rounded-full overflow-hidden bg-gray-100 shrink-0">
              <img v-if="auth.user?.avatar" :src="coverSrc(auth.user.avatar)" class="w-full h-full object-cover" />
              <span v-else class="w-full h-full flex items-center justify-center text-[10px] font-bold text-gray-400">{{ (auth.user?.name || auth.user?.email || '?').slice(0, 1).toUpperCase() }}</span>
            </span>
            {{ auth.user?.name || auth.user?.email }}
          </router-link>
          <button @click="handleLogout"
            class="text-[13px] text-apple-secondary hover:text-apple-text transition-colors">退出</button>
          <router-link v-if="auth.isAdmin" to="/admin/album-list"
            class="text-[13px] text-apple-accent no-underline hover:underline">管理</router-link>
        </template>
        <template v-else>
          <router-link to="/login"
            class="text-[13px] text-apple-secondary no-underline hover:text-apple-text transition-colors">登录</router-link>
        </template>
        <button v-if="auth.isLoggedIn && !auth.isAdmin" @click="cart.toggle()"
          class="relative flex items-center gap-1.5 bg-transparent border-none cursor-pointer text-[13px] font-medium text-apple-text px-3 py-1.5 rounded-full hover:bg-black/5 transition-colors">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"
            stroke-linejoin="round" class="w-[18px] h-[18px]">
            <circle cx="9" cy="21" r="1" />
            <circle cx="20" cy="21" r="1" />
            <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
          </svg>
          <span v-if="cart.itemCount"
            class="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] px-[5px] bg-apple-accent text-white text-[11px] font-semibold leading-[18px] text-center rounded-[9px]">
            {{ cart.itemCount }}
          </span>
        </button>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue';
import { useRouter } from 'vue-router';
import { io } from 'socket.io-client';
import { useAuthStore } from '../../stores/auth';
import { useCartStore } from '../../stores/cart';

const auth = useAuthStore();
const cart = useCartStore();
const router = useRouter();
const unreadCount = ref(0);
let socket = null;

function connectSocket() {
  if (!auth.isLoggedIn || auth.isAdmin) { unreadCount.value = 0; return; }
  const token = localStorage.getItem('token');
  if (!token) return;

  const isDev = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
  const serverUrl = isDev ? 'http://localhost:3000' : '';

  socket = io(serverUrl + '/chat', {
    auth: { token },
    transports: ['websocket', 'polling'],
  });

  socket.on('unreadCount', (count) => {
    unreadCount.value = typeof count === 'number' ? count : 0;
  });

  socket.on('connect', () => {
    // 连接后立即获取一次未读数
    socket.emit('unreadCount', {}, (count) => {
      unreadCount.value = typeof count === 'number' ? count : 0;
    });
  });
}

function disconnectSocket() {
  if (socket) { socket.disconnect(); socket = null; }
}

watch(() => auth.isLoggedIn, (val) => {
  if (val) connectSocket(); else { disconnectSocket(); unreadCount.value = 0; }
});

onMounted(() => { if (auth.isLoggedIn) connectSocket(); });
onBeforeUnmount(() => disconnectSocket());

function coverSrc(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return url.startsWith('/') ? url : `/${url}`
}

function handleLogout() {
  disconnectSocket();
  auth.logout();
  cart.clear();
  router.push('/');
}
</script>

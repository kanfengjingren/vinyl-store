<template>
  <div class="max-w-[420px] mx-auto px-6 py-20">
    <h1 class="text-[32px] font-semibold tracking-[-0.02em] text-center mb-8">登录</h1>
    <form @submit.prevent="handleSubmit" class="space-y-4">
      <div>
        <label class="block text-sm text-apple-secondary mb-1">邮箱</label>
        <input v-model="form.email" type="email" required class="w-full px-4 py-3 rounded-xl border border-apple-border text-[15px] outline-none focus:border-apple-accent focus:ring-2 focus:ring-apple-accent/20 transition-all" />
      </div>
      <div>
        <label class="block text-sm text-apple-secondary mb-1">密码</label>
        <input v-model="form.password" type="password" required class="w-full px-4 py-3 rounded-xl border border-apple-border text-[15px] outline-none focus:border-apple-accent focus:ring-2 focus:ring-apple-accent/20 transition-all" />
      </div>
      <p v-if="error" class="text-red-500 text-sm">{{ error }}</p>
      <button type="submit" :disabled="loading" class="w-full py-3.5 bg-apple-accent text-white text-[15px] font-semibold border-none rounded-full cursor-pointer hover:bg-apple-accent-hover transition-all disabled:opacity-50">
        {{ loading ? '登录中...' : '登录' }}
      </button>
    </form>
    <p class="text-center text-sm text-apple-secondary mt-6">
      还没有账号？<router-link to="/register" class="text-apple-accent no-underline hover:underline">注册</router-link>
    </p>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { useCartStore } from '../stores/cart';

const auth = useAuthStore();
const cart = useCartStore();
const router = useRouter();
const route = useRoute();

const form = reactive({ email: '', password: '' });
const error = ref('');
const loading = ref(false);

async function handleSubmit() {
  error.value = '';
  loading.value = true;
  try {
    const data = await auth.login({ email: form.email, password: form.password });
    if (data.user.role === 'ADMIN') {
      router.push('/admin/album-list');
      return;
    }
    await cart.refresh();
    const redirect = route.query.redirect || '/';
    router.push(redirect);
  } catch (e) {
    error.value = e.response?.data?.message || '登录失败';
  } finally {
    loading.value = false;
  }
}
</script>

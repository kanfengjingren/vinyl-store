<template>
  <div class="min-h-screen flex items-center justify-center bg-apple-bg">
    <div class="w-full max-w-sm">
      <h1 class="text-2xl font-semibold tracking-[-0.02em] text-center mb-8">商家登录</h1>

      <form @submit.prevent="handleLogin" class="space-y-4">
        <div>
          <label class="block text-sm font-medium mb-1">邮箱</label>
          <input v-model="email" type="email" required
            class="w-full px-3 py-2.5 border border-black/15 rounded-lg text-sm outline-none focus:border-[rgb(196,147,51)] focus:ring-1 focus:ring-[rgb(196,147,51)]/20 transition-all" />
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">密码</label>
          <input v-model="password" type="password" required
            class="w-full px-3 py-2.5 border border-black/15 rounded-lg text-sm outline-none focus:border-[rgb(196,147,51)] focus:ring-1 focus:ring-[rgb(196,147,51)]/20 transition-all" />
        </div>
        <div v-if="error" class="text-red-500 text-sm">{{ error }}</div>
        <button type="submit" :disabled="submitting"
          class="w-full py-2.5 bg-[rgb(196,147,51)] text-white font-medium rounded-full hover:bg-[rgb(176,127,31)] disabled:opacity-50 transition-colors">
          {{ submitting ? '登录中...' : '登录' }}
        </button>
      </form>

      <p class="text-center text-sm text-apple-secondary mt-6">
        还没有卖家账号？<RouterLink to="/register" class="text-[rgb(196,147,51)] hover:underline">申请入驻</RouterLink>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useSellerAuthStore } from '../stores/auth';

const router = useRouter();
const auth = useSellerAuthStore();

const email = ref('');
const password = ref('');
const error = ref('');
const submitting = ref(false);

async function handleLogin() {
  error.value = '';
  submitting.value = true;
  try {
    await auth.login({ email: email.value, password: password.value });
    await router.push('/albums');
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '登录失败';
  } finally {
    submitting.value = false;
  }
}
</script>

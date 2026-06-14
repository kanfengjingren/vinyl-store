<template>
  <div class="max-w-[420px] mx-auto px-6 py-20">
    <!-- 登录模式 -->
    <template v-if="mode === 'login'">
      <h1 class="text-[32px] font-semibold tracking-[-0.02em] text-center mb-8">登录</h1>
      <form @submit.prevent="handleLogin" class="space-y-4">
        <div>
          <label class="block text-sm text-apple-secondary mb-1">邮箱</label>
          <input v-model="form.email" type="email" required class="w-full px-4 py-3 rounded-xl border border-apple-border text-[15px] outline-none focus:border-apple-accent focus:ring-2 focus:ring-apple-accent/20 transition-all" />
        </div>
        <div>
          <label class="block text-sm text-apple-secondary mb-1">密码</label>
          <input v-model="form.password" type="password" required class="w-full px-4 py-3 rounded-xl border border-apple-border text-[15px] outline-none focus:border-apple-accent focus:ring-2 focus:ring-apple-accent/20 transition-all" />
        </div>
        <p class="text-right">
          <button type="button" @click="switchToForgot" class="text-sm text-apple-accent hover:underline">忘记密码？</button>
        </p>
        <p v-if="error" class="text-red-500 text-sm">{{ error }}</p>
        <button type="submit" :disabled="loading" class="w-full py-3.5 bg-apple-accent text-white text-[15px] font-semibold border-none rounded-full cursor-pointer hover:bg-apple-accent-hover transition-all disabled:opacity-50">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>
      <p class="text-center text-sm text-apple-secondary mt-6">
        还没有账号？<router-link to="/register" class="text-apple-accent no-underline hover:underline">注册</router-link>
      </p>
    </template>

    <!-- 忘记密码模式 -->
    <template v-else>
      <h1 class="text-[32px] font-semibold tracking-[-0.02em] text-center mb-8">重置密码</h1>
      <form @submit.prevent="handleReset" class="space-y-4">
        <div>
          <label class="block text-sm text-apple-secondary mb-1">邮箱</label>
          <div class="flex gap-2">
            <input v-model="forgot.email" type="email" required placeholder="注册时使用的邮箱"
              class="flex-1 px-4 py-3 rounded-xl border border-apple-border text-[15px] outline-none focus:border-apple-accent focus:ring-2 focus:ring-apple-accent/20 transition-all" />
            <button type="button" @click="sendCode" :disabled="countdown > 0 || sending"
              class="shrink-0 px-4 py-3 rounded-xl text-sm font-medium transition-all"
              :class="countdown > 0 ? 'bg-black/5 text-black/30 cursor-not-allowed' : 'bg-[rgb(196,147,51)] text-white hover:bg-[rgb(176,127,31)]'">
              {{ countdown > 0 ? `${countdown}s` : sending ? '发送中...' : '发送验证码' }}
            </button>
          </div>
        </div>
        <div>
          <label class="block text-sm text-apple-secondary mb-1">验证码</label>
          <input v-model="forgot.code" type="text" required maxlength="6" placeholder="6位数字验证码"
            class="w-full px-4 py-3 rounded-xl border border-apple-border text-[15px] outline-none focus:border-apple-accent focus:ring-2 focus:ring-apple-accent/20 transition-all text-center tracking-[8px] text-lg" />
        </div>
        <div>
          <label class="block text-sm text-apple-secondary mb-1">新密码</label>
          <input v-model="forgot.newPassword" type="password" required minlength="6" placeholder="至少6位"
            class="w-full px-4 py-3 rounded-xl border border-apple-border text-[15px] outline-none focus:border-apple-accent focus:ring-2 focus:ring-apple-accent/20 transition-all" />
        </div>
        <div>
          <label class="block text-sm text-apple-secondary mb-1">确认新密码</label>
          <input v-model="forgot.confirmPassword" type="password" required minlength="6" placeholder="再次输入新密码"
            class="w-full px-4 py-3 rounded-xl border border-apple-border text-[15px] outline-none focus:border-apple-accent focus:ring-2 focus:ring-apple-accent/20 transition-all" />
        </div>
        <p v-if="forgotMsg" :class="['text-sm', forgotOk ? 'text-green-600' : 'text-red-500']">{{ forgotMsg }}</p>
        <button type="submit" :disabled="resetting" class="w-full py-3.5 bg-apple-accent text-white text-[15px] font-semibold border-none rounded-full cursor-pointer hover:bg-apple-accent-hover transition-all disabled:opacity-50">
          {{ resetting ? '重置中...' : '重置密码' }}
        </button>
      </form>
      <p class="text-center text-sm text-apple-secondary mt-6">
        <button type="button" @click="switchToLogin" class="text-apple-accent hover:underline">&larr; 返回登录</button>
      </p>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { useCartStore } from '../stores/cart';
import { forgotPassword, resetPassword } from '@vinyl-store/shared';

const auth = useAuthStore();
const cart = useCartStore();
const router = useRouter();
const route = useRoute();

const mode = ref('login');
const form = reactive({ email: '', password: '' });
const error = ref('');
const loading = ref(false);

// 忘记密码
const forgot = reactive({ email: '', code: '', newPassword: '', confirmPassword: '' });
const countdown = ref(0);
const sending = ref(false);
const resetting = ref(false);
const forgotMsg = ref('');
const forgotOk = ref(false);
let timer = null;

function switchToForgot() {
  mode.value = 'forgot';
  forgot.email = form.email; // 带上已输入的邮箱
  error.value = '';
  forgotMsg.value = '';
}

function switchToLogin() {
  mode.value = 'login';
  forgotMsg.value = '';
  clearInterval(timer);
  countdown.value = 0;
}

async function sendCode() {
  if (!forgot.email) return;
  sending.value = true;
  forgotMsg.value = '';
  try {
    await forgotPassword(forgot.email);
    forgotMsg.value = '验证码已发送，请查看邮箱';
    forgotOk.value = true;
    countdown.value = 90;
    clearInterval(timer);
    timer = setInterval(() => {
      countdown.value--;
      if (countdown.value <= 0) clearInterval(timer);
    }, 1000);
  } catch (e) {
    forgotMsg.value = e.response?.data?.message || '发送失败';
    forgotOk.value = false;
  } finally {
    sending.value = false;
  }
}

async function handleReset() {
  forgotMsg.value = '';
  if (forgot.newPassword !== forgot.confirmPassword) {
    forgotMsg.value = '两次输入的密码不一致';
    forgotOk.value = false;
    return;
  }
  if (forgot.newPassword.length < 6) {
    forgotMsg.value = '密码至少6位';
    forgotOk.value = false;
    return;
  }
  resetting.value = true;
  try {
    await resetPassword(forgot.email, forgot.code, forgot.newPassword);
    forgotMsg.value = '密码重置成功，请登录';
    forgotOk.value = true;
    setTimeout(() => switchToLogin(), 1500);
  } catch (e) {
    forgotMsg.value = e.response?.data?.message || '重置失败';
    forgotOk.value = false;
  } finally {
    resetting.value = false;
  }
}

async function handleLogin() {
  error.value = '';
  loading.value = true;
  try {
    const data = await auth.login({ email: form.email, password: form.password, portal: 'customer' });
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

<template>
  <div class="min-h-screen flex items-center justify-center bg-apple-bg">
    <div class="w-full max-w-sm">
      <h1 class="text-2xl font-semibold tracking-[-0.02em] text-center mb-8">卖家入驻</h1>

      <form @submit.prevent="handleRegister" class="space-y-4">
        <div>
          <label class="block text-sm font-medium mb-1">厂牌名称 *</label>
          <input v-model="form.storeName" type="text" required
            class="w-full px-3 py-2.5 border border-black/15 rounded-lg text-sm outline-none focus:border-[rgb(196,147,51)] focus:ring-1 focus:ring-[rgb(196,147,51)]/20 transition-all"
            placeholder="如：摩登天空" />
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">联系人</label>
          <input v-model="form.name" type="text"
            class="w-full px-3 py-2.5 border border-black/15 rounded-lg text-sm outline-none focus:border-[rgb(196,147,51)] focus:ring-1 focus:ring-[rgb(196,147,51)]/20 transition-all"
            placeholder="你的称呼" />
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">邮箱 *</label>
          <input v-model="form.email" type="email" required
            class="w-full px-3 py-2.5 border border-black/15 rounded-lg text-sm outline-none focus:border-[rgb(196,147,51)] focus:ring-1 focus:ring-[rgb(196,147,51)]/20 transition-all" />
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">密码 *</label>
          <input v-model="form.password" type="password" required
            class="w-full px-3 py-2.5 border border-black/15 rounded-lg text-sm outline-none focus:border-[rgb(196,147,51)] focus:ring-1 focus:ring-[rgb(196,147,51)]/20 transition-all" />
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">联系电话</label>
          <input v-model="form.contactPhone" type="tel"
            class="w-full px-3 py-2.5 border border-black/15 rounded-lg text-sm outline-none focus:border-[rgb(196,147,51)] focus:ring-1 focus:ring-[rgb(196,147,51)]/20 transition-all"
            placeholder="便于管理员联系" />
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">厂牌简介</label>
          <textarea v-model="form.description" rows="3"
            class="w-full px-3 py-2 border border-black/15 rounded-lg text-sm outline-none focus:border-[rgb(196,147,51)] focus:ring-1 focus:ring-[rgb(196,147,51)]/20 transition-all resize-none"
            placeholder="简单介绍一下你的厂牌..."></textarea>
        </div>

        <div v-if="error" class="text-red-500 text-sm">{{ error }}</div>
        <div v-if="success" class="text-green-600 text-sm text-center">{{ success }}</div>

        <button type="submit" :disabled="submitting"
          class="w-full py-2.5 bg-[rgb(196,147,51)] text-white font-medium rounded-full hover:bg-[rgb(176,127,31)] disabled:opacity-50 transition-colors">
          {{ submitting ? '提交中...' : '提交入驻申请' }}
        </button>
      </form>

      <p class="text-center text-sm text-apple-secondary mt-6">
        已有卖家账号？<RouterLink to="/login" class="text-[rgb(196,147,51)] hover:underline">去登录</RouterLink>
      </p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { register } from '@vinyl-store/shared';

const router = useRouter();

const form = reactive({
  storeName: '',
  name: '',
  email: '',
  password: '',
  contactPhone: '',
  description: '',
});

const error = ref('');
const success = ref('');
const submitting = ref(false);

async function handleRegister() {
  error.value = '';
  success.value = '';

  if (!form.storeName.trim()) {
    error.value = '请填写厂牌名称';
    return;
  }

  submitting.value = true;
  try {
    await register({
      email: form.email,
      password: form.password,
      name: form.name || undefined,
      role: 'SELLER',
      storeName: form.storeName,
      contactEmail: form.email,
      contactPhone: form.contactPhone || undefined,
      description: form.description || undefined,
    });
    success.value = '入驻申请已提交，请等待管理员审核通过。';
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '入驻失败';
  } finally {
    submitting.value = false;
  }
}
</script>

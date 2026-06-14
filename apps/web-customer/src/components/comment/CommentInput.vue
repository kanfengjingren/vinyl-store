<template>
  <div class="flex gap-3">
    <!-- 头像 -->
    <div class="shrink-0 w-10 h-10 rounded-full bg-white/10 flex items-center justify-center text-white/50 text-sm font-medium overflow-hidden">
      <img v-if="auth.user?.avatar" :src="coverSrc(auth.user.avatar)" class="w-full h-full object-cover" />
      <span v-else-if="!auth.user?.name">?</span>
      <span v-else>{{ auth.user.name.charAt(0) }}</span>
    </div>

    <!-- 输入区 -->
    <div class="flex-1 min-w-0" v-if="auth.isLoggedIn">
      <textarea
        v-model="text"
        :placeholder="placeholder"
        rows="3"
        maxlength="500"
        class="w-full bg-white/5 border border-white/10 rounded-lg px-4 py-3 text-sm text-white placeholder-white/30
               focus:outline-none focus:border-[rgb(196,147,51)]/50 focus:bg-white/[0.07]
               resize-none transition-colors"
      ></textarea>
      <div class="flex items-center justify-between mt-2">
        <span class="text-white/25 text-xs">{{ text.length }}/500</span>
        <button
          @click="submit"
          :disabled="!text.trim() || submitting"
          class="px-5 py-1.5 rounded-full text-sm font-medium transition-all
                 bg-[rgb(196,147,51)] text-white hover:bg-[rgb(176,127,31)]
                 disabled:opacity-30 disabled:cursor-not-allowed"
        >
          {{ submitting ? '发送中...' : '发送' }}
        </button>
      </div>
    </div>

    <!-- 未登录 -->
    <div v-else class="flex-1 flex items-center gap-3 bg-white/5 border border-white/10 rounded-lg px-4 py-3">
      <span class="text-white/30 text-sm">请先登录后发表评论</span>
      <router-link to="/login" class="text-[rgb(196,147,51)] text-sm hover:underline">去登录</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useAuthStore } from '../../stores/auth';
import { ToastNotification } from '@vinyl-store/shared/ui';

const props = defineProps({
  placeholder: { type: String, default: '发表你的评论...' },
  albumId: { type: Number, required: true },
  parentId: { type: Number, default: null },
});

const emit = defineEmits(['submitted']);

const auth = useAuthStore();
const text = ref('');

function coverSrc(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return url.startsWith('/') ? url : `/${url}`
}
const submitting = ref(false);

async function submit() {
  if (!text.value.trim() || submitting.value) return;
  submitting.value = true;
  try {
    const { createComment } = await import('@vinyl-store/shared');
    await createComment(props.albumId, {
      content: text.value.trim(),
      parentId: props.parentId,
    });
    text.value = '';
    emit('submitted');
  } catch (e) {
    const toast = ToastNotification.show?.('评论失败，请重试');
  } finally {
    submitting.value = false;
  }
}
</script>

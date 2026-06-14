<template>
  <div class="flex gap-2.5 py-2.5">
    <!-- 头像（小号） -->
    <div class="shrink-0 w-7 h-7 rounded-full bg-white/10 flex items-center justify-center text-white/50 text-[11px] font-medium">
      {{ reply.user?.name?.charAt(0) || '?' }}
    </div>

    <div class="flex-1 min-w-0">
      <!-- 用户名 + 时间 -->
      <div class="flex items-center gap-2 mb-1">
        <span class="text-[13px] font-medium text-[rgb(196,147,51)]">{{ reply.user?.name || '匿名' }}</span>
        <span class="text-white/25 text-[11px]">{{ formatTime(reply.createdAt) }}</span>
      </div>

      <!-- 内容 -->
      <p class="text-sm text-white/80 leading-relaxed whitespace-pre-wrap break-words">
        <span v-if="reply.replyToUserName" class="text-[rgb(196,147,51)]">回复 @{{ reply.replyToUserName }}</span>
        <span v-if="reply.replyToUserName" class="text-white/30 mx-1">：</span>
        {{ reply.content }}
      </p>

      <!-- 操作栏 -->
      <div class="flex items-center gap-4 mt-1.5">
        <button
          @click="showReplyInput = !showReplyInput"
          class="text-white/25 text-[11px] hover:text-[rgb(196,147,51)] transition-colors"
        >
          回复
        </button>
        <button
          v-if="auth.user?.id === reply.userId"
          @click="handleDelete"
          class="text-white/20 text-[11px] hover:text-red-400 transition-colors"
        >
          删除
        </button>
      </div>

      <!-- 内联回复框 -->
      <div v-if="showReplyInput" class="mt-2">
        <CommentInput
          :album-id="albumId"
          :parent-id="reply.id"
          :placeholder="`回复 @${reply.user?.name || '匿名'}...`"
          @submitted="showReplyInput = false; $emit('replied')"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useAuthStore } from '../../stores/auth';
import { deleteComment } from '@vinyl-store/shared';
import CommentInput from './CommentInput.vue';

const props = defineProps({
  reply: { type: Object, required: true },
  albumId: { type: Number, required: true },
});

const emit = defineEmits(['deleted', 'replied']);

const auth = useAuthStore();
const showReplyInput = ref(false);

function formatTime(dateStr) {
  if (!dateStr) return '';
  const now = Date.now();
  const then = new Date(dateStr).getTime();
  const diff = Math.floor((now - then) / 1000);
  if (diff < 60) return '刚刚';
  if (diff < 3600) return Math.floor(diff / 60) + '分钟前';
  if (diff < 86400) return Math.floor(diff / 3600) + '小时前';
  if (diff < 2592000) return Math.floor(diff / 86400) + '天前';
  return new Date(dateStr).toLocaleDateString('zh-CN');
}

async function handleDelete() {
  try {
    await deleteComment(props.reply.id);
    emit('deleted');
  } catch {}
}
</script>

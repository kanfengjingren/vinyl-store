<template>
  <div :id="'comment-' + comment.id" class="py-4 border-b border-white/[0.06]">
    <!-- 主评论 -->
    <div class="flex gap-3">
      <!-- 头像 -->
      <div class="shrink-0 w-10 h-10 rounded-full bg-white/10 flex items-center justify-center text-white/50 text-sm font-medium overflow-hidden">
        {{ comment.user?.name?.charAt(0) || '?' }}
      </div>

      <div class="flex-1 min-w-0">
        <!-- 用户名 + 时间 -->
        <div class="flex items-center gap-2 mb-1.5">
          <span class="text-sm font-medium text-white">{{ comment.user?.name || '匿名' }}</span>
          <span class="text-white/25 text-xs">{{ formatTime(comment.createdAt) }}</span>
        </div>

        <!-- 内容 -->
        <p class="text-sm text-white/85 leading-relaxed whitespace-pre-wrap break-words mb-2">{{ comment.content }}</p>

        <!-- 操作栏 -->
        <div class="flex items-center gap-5">
          <button
            @click="showReplyInput = !showReplyInput"
            class="text-white/30 text-xs hover:text-[rgb(196,147,51)] transition-colors"
          >
            💬 回复
          </button>
          <button
            v-if="auth.user?.id === comment.userId"
            @click="handleDelete"
            class="text-white/20 text-xs hover:text-red-400 transition-colors"
          >
            🗑 删除
          </button>
        </div>

        <!-- 子回复区域 -->
        <div v-if="comment.replies?.length || comment._count?.replies" class="mt-2 ml-1 pl-5 border-l border-white/[0.08]">
          <!-- 已加载的子回复（分批显示） -->
          <ReplyItem
            v-for="reply in visibleReplies"
            :key="reply.id"
            :reply="reply"
            :album-id="albumId"
            @deleted="handleReplyDeleted(reply.id)"
            @replied="onReplied"
          />

          <!-- 展开更多（首次从3条展开到8条） -->
          <button
            v-if="!allReplies && remainingCount > 0"
            @click="expandReplies"
            class="flex items-center gap-1 text-[rgb(196,147,51)] text-xs hover:underline mt-1"
          >
            展开更多 {{ remainingCount }} 条回复 ▼
          </button>

          <!-- 加载更多（已展开但超过8条） -->
          <button
            v-if="allReplies && hasMoreReplies"
            @click="loadMoreReplies"
            class="flex items-center gap-1 text-[rgb(196,147,51)] text-xs hover:underline mt-1"
          >
            查看更多回复（{{ replyPage }} / {{ totalReplyPages }}）▼
          </button>

          <!-- 收起 -->
          <button
            v-if="allReplies && allReplies.length > 8"
            @click="collapseReplies"
            class="flex items-center gap-1 text-white/30 text-xs hover:text-white/60 transition-colors mt-1"
          >
            收起 ▲
          </button>
        </div>

        <!-- 内联回复框 -->
        <div v-if="showReplyInput" class="mt-3">
          <CommentInput
            :album-id="albumId"
            :parent-id="comment.id"
            :placeholder="`回复 @${comment.user?.name || '匿名'}...`"
            @submitted="showReplyInput = false; $emit('replied')"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useAuthStore } from '../../stores/auth';
import { deleteComment, fetchReplies } from '@vinyl-store/shared';
import CommentInput from './CommentInput.vue';
import ReplyItem from './ReplyItem.vue';

const REPLY_PAGE_SIZE = 8;

const props = defineProps({
  comment: { type: Object, required: true },
  albumId: { type: Number, required: true },
});

const emit = defineEmits(['deleted', 'replied']);

const auth = useAuthStore();
const showReplyInput = ref(false);
const allReplies = ref(null);   // null=未展开, []或[...]=已展开全部
const replyPage = ref(1);       // 当前显示到第几页（每页8条）

// 未展开时的剩余数量
const remainingCount = computed(() => {
  if (allReplies.value) return 0;
  const total = props.comment._count?.replies || 0;
  const shown = props.comment.replies?.length || 0;
  return total - shown;
});

// 所有已加载的子回复
const loadedReplies = computed(() => {
  return allReplies.value || props.comment.replies || [];
});

// 可见的子回复（分页截取）
const visibleReplies = computed(() => {
  return loadedReplies.value.slice(0, replyPage.value * REPLY_PAGE_SIZE);
});

// 是否还有更多可显示
const hasMoreReplies = computed(() => {
  return visibleReplies.value.length < loadedReplies.value.length;
});

// 总页数
const totalReplyPages = computed(() => {
  return Math.ceil(loadedReplies.value.length / REPLY_PAGE_SIZE);
});

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
    await deleteComment(props.comment.id);
    emit('deleted');
  } catch {}
}

function handleReplyDeleted(replyId) {
  if (allReplies.value) {
    allReplies.value = allReplies.value.filter(r => r.id !== replyId);
  } else {
    props.comment.replies = props.comment.replies.filter(r => r.id !== replyId);
  }
  if (props.comment._count) props.comment._count.replies--;
}

// 展开：首次获取全部子回复，显示前8条
async function expandReplies() {
  try {
    const data = await fetchReplies(props.comment.id);
    allReplies.value = data;
    replyPage.value = 1; // 显示第1页（前8条）
  } catch {}
}

// 加载更多：下一页
function loadMoreReplies() {
  replyPage.value++;
}

// 收起：回到只显示前3条（API 默认状态）
function collapseReplies() {
  allReplies.value = null;
  replyPage.value = 1;
}

function onReplied() {
  // 重新获取全部回复
  expandReplies();
}
</script>

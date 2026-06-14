<template>
  <div class="comment-section">
    <!-- 标题 -->
    <h3 class="text-lg font-semibold text-white mb-5 flex items-center gap-2">
      💬 评论
      <span v-if="total > 0" class="text-white/40 text-base font-normal">({{ total }})</span>
    </h3>

    <!-- 发表框 -->
    <div class="mb-6">
      <CommentInput :album-id="albumId" @submitted="onSubmitted" />
    </div>

    <!-- 评论列表 -->
    <div v-if="loading" class="text-center text-white/30 py-10 text-sm">加载中...</div>

    <div v-else-if="comments.length === 0" class="text-center text-white/20 py-10 text-sm">
      还没有评论，来发表第一条评论吧
    </div>

    <div v-else>
      <CommentItem
        v-for="comment in comments"
        :key="comment.id"
        :comment="comment"
        :album-id="albumId"
        @deleted="loadComments"
        @replied="loadComments"
      />

      <!-- 分页器 -->
      <div v-if="pagination.totalPages > 1" class="flex items-center justify-center gap-1.5 mt-8 pb-4">
        <button
          @click="goPage(pagination.page - 1)"
          :disabled="pagination.page <= 1"
          class="px-3 py-1.5 rounded text-xs text-white/50 hover:text-white hover:bg-white/10
                 disabled:opacity-20 disabled:cursor-not-allowed transition-colors"
        >
          上一页
        </button>

        <template v-for="p in pages" :key="p">
          <span v-if="p === '...'" class="px-1 text-white/20 text-xs">...</span>
          <button
            v-else
            @click="goPage(p)"
            :class="[
              'w-8 h-8 rounded text-xs font-medium transition-all',
              p === pagination.page
                ? 'bg-[rgb(196,147,51)] text-white'
                : 'text-white/50 hover:text-white hover:bg-white/10'
            ]"
          >
            {{ p }}
          </button>
        </template>

        <button
          @click="goPage(pagination.page + 1)"
          :disabled="pagination.page >= pagination.totalPages"
          class="px-3 py-1.5 rounded text-xs text-white/50 hover:text-white hover:bg-white/10
                 disabled:opacity-20 disabled:cursor-not-allowed transition-colors"
        >
          下一页
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue';
import { fetchComments, fetchReplies } from '@vinyl-store/shared';
import CommentInput from './CommentInput.vue';
import CommentItem from './CommentItem.vue';

const props = defineProps({
  albumId: { type: Number, required: true },
  highlightCommentId: { type: Number, default: null },
});

const comments = ref([]);
const pagination = ref({ page: 1, limit: 10, total: 0, totalPages: 0 });
const loading = ref(false);

const total = computed(() => pagination.value.total);

// 生成页码数组（带省略号）
const pages = computed(() => {
  const pg = pagination.value;
  if (pg.totalPages <= 7) {
    return Array.from({ length: pg.totalPages }, (_, i) => i + 1);
  }
  const pages = [];
  pages.push(1);
  if (pg.page > 3) pages.push('...');
  const start = Math.max(2, pg.page - 1);
  const end = Math.min(pg.totalPages - 1, pg.page + 1);
  for (let i = start; i <= end; i++) pages.push(i);
  if (pg.page < pg.totalPages - 2) pages.push('...');
  pages.push(pg.totalPages);
  return pages;
});

async function loadComments(page = 1) {
  loading.value = true;
  try {
    const data = await fetchComments(props.albumId, { page, limit: 10 });
    comments.value = data.data;
    pagination.value = data.pagination;
  } catch {} finally {
    loading.value = false;
  }
}

function goPage(page) {
  if (page < 1 || page > pagination.value.totalPages) return;
  loadComments(page);
}

function onSubmitted() {
  loadComments(1); // 新评论后回到第一页
}

async function scrollToHighlight() {
  if (!props.highlightCommentId) return;
  // 先看当前页有没有
  if (comments.value.find(c => Number(c.id) === Number(props.highlightCommentId))) {
    jumpToComment();
    return;
  }
  // 逐页扫描，最多 20 页
  for (let p = 1; p <= Math.min(pagination.value.totalPages || 20, 20); p++) {
    try {
      const data = await fetchComments(props.albumId, { page: p, limit: 10 });
      if (data.data.some(c => Number(c.id) === Number(props.highlightCommentId))) {
        comments.value = data.data;
        pagination.value = data.pagination;
        await nextTick();
        jumpToComment();
        return;
      }
    } catch { break; }
  }
}

async function jumpToComment() {
  // 预加载该评论的全部回复
  try {
    const replies = await fetchReplies(props.highlightCommentId);
    const target = comments.value.find(c => Number(c.id) === Number(props.highlightCommentId));
    if (target && replies) {
      target.replies = replies;
      target._count = { replies: replies.length };
    }
  } catch {}

  setTimeout(() => {
    const el = document.getElementById('comment-' + props.highlightCommentId);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      el.style.transition = 'background 0.3s';
      el.style.background = 'rgba(196,147,51,0.15)';
      setTimeout(() => { el.style.background = ''; }, 2000);
    }
  }, 400);
}

onMounted(async () => {
  await loadComments();
  if (props.highlightCommentId) scrollToHighlight();
});
</script>

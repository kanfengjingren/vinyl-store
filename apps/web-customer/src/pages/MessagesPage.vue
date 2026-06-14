<template>
  <!-- 图片大图预览 -->
  <Teleport to="body">
    <div v-if="previewImage" class="fixed inset-0 z-[250] bg-black/80 flex items-center justify-center cursor-pointer" @click="previewImage = null">
      <img :src="previewImage" class="max-w-[90vw] max-h-[90vh] object-contain" />
    </div>
  </Teleport>

  <div class="fixed top-[52px] left-0 right-0 bottom-0 flex z-[1]">
    <!-- 左侧会话列表 -->
    <div class="w-[300px] shrink-0 border-r border-black/5 bg-white/50 flex flex-col">
      <div class="px-5 py-4 text-sm font-semibold text-black border-b border-black/5">消息</div>
      <div class="flex-1 overflow-y-auto">
        <div v-if="conversations.length === 0" class="px-5 py-8 text-center text-black/20 text-sm">
          暂无消息
        </div>
        <div
          v-for="c in conversations"
          :key="c.partner.id"
          @click="select(c.partner)"
          :class="[
            'px-5 py-4 border-b border-black/[0.03] cursor-pointer hover:bg-black/[0.02] transition-colors flex items-center gap-3',
            activeId === c.partner.id ? 'bg-[rgb(196,147,51)]/5 border-l-[3px] border-l-[rgb(196,147,51)]' : '',
          ]"
        >
          <!-- 头像 -->
          <router-link :to="`/user/${c.partner.id}`" @click.stop class="shrink-0 w-10 h-10 rounded-full bg-gray-100 flex items-center justify-center text-gray-400 text-sm font-medium overflow-hidden hover:ring-2 hover:ring-[rgb(196,147,51)]/50 transition-all cursor-pointer">
            <img v-if="c.partner.avatar" :src="coverSrc(c.partner.avatar)" class="w-full h-full object-cover" />
            <span v-else>{{ (c.partner.name || c.partner.email || '?').slice(0, 1).toUpperCase() }}</span>
          </router-link>
          <div class="flex-1 min-w-0">
            <div class="flex items-center justify-between mb-1">
              <span class="text-sm font-medium text-black">{{ c.partner.storeName || c.partner.name || c.partner.email }}</span>
              <span v-if="c.unreadCount > 0" class="text-[11px] bg-[rgb(196,147,51)] text-white px-1.5 py-0.5 rounded-full font-medium">{{ c.unreadCount }}</span>
            </div>
            <p class="text-xs text-black/30 truncate">{{ formatConversationPreview(c.lastMsg) }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧聊天区 -->
    <div class="flex-1 flex flex-col bg-white">
      <template v-if="active">
        <!-- Header -->
        <div class="px-5 py-4 border-b border-black/5 shrink-0">
          <p class="font-semibold text-black text-[15px]">{{ active.storeName || active.name || active.email }}</p>
        </div>

        <!-- 消息列表 -->
        <div ref="msgList" class="flex-1 overflow-y-auto px-5 py-4 space-y-3 flex flex-col">
          <div v-if="loading" class="text-center text-black/20 text-sm py-8">加载中...</div>
          <div
            v-for="msg in messages"
            :key="msg.id"
            :class="[
              'flex max-w-[75%] text-[14px] leading-relaxed',
              msg.senderId === myUserId ? 'ml-auto' : 'mr-auto',
            ]"
          >
            <!-- 评论通知卡片 -->
            <div
              v-if="isCommentNotification(msg)"
              @click="goToAlbum(msg)"
              class="bg-[rgb(196,147,51)]/5 border border-[rgb(196,147,51)]/20 rounded-xl px-4 py-3 cursor-pointer hover:bg-[rgb(196,147,51)]/10 transition-colors max-w-[320px]"
            >
              <p class="text-xs text-[rgb(196,147,51)] mb-1">💬 评论回复</p>
              <p class="text-sm text-black/70 leading-relaxed">{{ getCommentNotifyText(msg) }}</p>
              <p class="text-xs text-black/30 mt-1.5">点击查看 →</p>
            </div>

            <!-- 普通消息 -->
            <div
              v-else
              :class="msg.senderId === myUserId
                ? 'bg-[rgb(196,147,51)] text-white rounded-bl-2xl'
                : 'bg-gray-100 text-black rounded-br-2xl'"
              class="px-1 py-1 rounded-2xl rounded-tl-md overflow-hidden"
            >
              <img v-if="msg.imageUrl" :src="msg.imageUrl" class="max-w-[240px] max-h-[320px] object-cover rounded-xl cursor-pointer" @click="previewImage = msg.imageUrl" />
              <p v-if="msg.content" class="px-3 py-1.5">{{ msg.content }}</p>
            </div>
          </div>
        </div>

        <!-- 连接状态 -->
        <div v-if="connectionError" class="px-4 py-2 bg-red-50 text-red-500 text-xs text-center">{{ connectionError }}</div>
        <div v-else-if="!connected" class="px-4 py-2 bg-yellow-50 text-yellow-600 text-xs text-center">正在连接...</div>

        <!-- 输入区 -->
        <div v-if="uploading" class="px-4 py-1 text-xs text-black/40 text-center">📷 上传中...</div>
        <div class="px-4 py-3 border-t border-black/5 shrink-0 flex gap-2">
          <input ref="fileInput" type="file" accept="image/*" class="hidden" @change="onFilePicked" />
          <button @click="fileInput.click()" :disabled="!connected" class="w-10 h-10 flex items-center justify-center text-lg hover:bg-gray-100 transition-colors cursor-pointer disabled:opacity-30 shrink-0" title="发送图片">🖼</button>
          <input
            v-model="text"
            @keydown.enter="send"
            :disabled="!connected"
            :placeholder="connected ? '输入消息...' : '连接中...'"
            class="flex-1 px-4 py-2.5 bg-gray-50 border border-black/5 text-sm outline-none focus:border-[rgb(196,147,51)] disabled:opacity-40"
          />
          <button
            @click="send"
            :disabled="!text.trim() || !connected"
            class="px-5 py-2.5 bg-black text-white text-sm font-medium hover:bg-black/80 disabled:opacity-30 transition-colors cursor-pointer"
          >发送</button>
        </div>
      </template>
      <div v-else class="flex-1 flex items-center justify-center text-black/20 text-sm">
        选择一个对话开始聊天
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { io } from 'socket.io-client';
import { fetchConversations, fetchMessages, markMessagesRead, uploadChatImage } from '@vinyl-store/shared';

const router = useRouter();

const msgList = ref(null);
const conversations = ref([]);
const messages = ref([]);
const active = ref(null);
const activeId = ref(null);
const text = ref('');
const loading = ref(false);
const socket = ref(null);
const connected = ref(false);
const connectionError = ref('');
const uploading = ref(false);
const previewImage = ref(null);
const fileInput = ref(null);

function coverSrc(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return url.startsWith('/') ? url : `/${url}`
}

const myUserId = ref(null);
try {
  const u = JSON.parse(localStorage.getItem('user') || '{}');
  myUserId.value = u.id;
} catch {}

async function loadConversations() {
  try {
    conversations.value = await fetchConversations();
  } catch (e) {
    console.error('加载会话列表失败:', e);
  }
}

async function select(partner) {
  active.value = partner;
  activeId.value = partner.id;
  messages.value = [];
  loading.value = true;

  try {
    const history = await fetchMessages(partner.id);
    messages.value = history || [];
    await markMessagesRead(partner.id);
    loadConversations();
    scrollBottom();
  } catch {
    messages.value = [];
  } finally {
    loading.value = false;
  }
}

function send() {
  const content = text.value.trim();
  if (!content || !socket.value?.connected || !active.value) return;
  emitMessage({ content });
  text.value = '';
}

async function onFilePicked(e) {
  const file = e.target.files?.[0];
  if (!file) return;
  uploading.value = true;
  try {
    const url = await uploadChatImage(file);
    emitMessage({ imageUrl: url });
  } catch (err) {
    console.error('[Messages] 图片上传失败:', err);
    connectionError.value = '图片上传失败，请重试';
    setTimeout(() => { connectionError.value = ''; }, 3000);
  } finally {
    uploading.value = false;
    if (fileInput.value) fileInput.value.value = '';
  }
}

function emitMessage({ content, imageUrl }) {
  if (!socket.value?.connected || !active.value) return;
  const tempId = -Date.now();
  messages.value.push({
    id: tempId,
    senderId: myUserId.value,
    receiverId: active.value.id,
    content: content || '',
    imageUrl: imageUrl || null,
    createdAt: new Date().toISOString(),
    sender: { id: myUserId.value, name: '我' },
  });
  scrollBottom();

  socket.value.emit('sendMessage', { receiverId: active.value.id, content: content || '', imageUrl }, (response) => {
    if (response?.id) {
      const idx = messages.value.findIndex((m) => m.id === tempId);
      if (idx !== -1) messages.value[idx] = response;
    }
  });
}

function scrollBottom() {
  nextTick(() => {
    const el = msgList.value;
    if (el) el.scrollTop = el.scrollHeight;
  });
}

onMounted(() => {
  const token = localStorage.getItem('token');
  if (!token) return;

  const isDev = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
  const serverUrl = isDev ? 'http://localhost:3000' : '';

  socket.value = io(serverUrl + '/chat', {
    auth: { token },
    transports: ['websocket', 'polling'],
  });

  socket.value.on('connect', () => {
    connected.value = true;
    connectionError.value = '';
  });

  socket.value.on('connect_error', (err) => {
    connected.value = false;
    connectionError.value = '连接失败: ' + err.message;
  });

  socket.value.on('disconnect', () => {
    connected.value = false;
  });

  socket.value.on('newMessage', (msg) => {
    // 更新当前聊天
    if (
      active.value &&
      ((msg.senderId === myUserId.value && msg.receiverId === active.value.id) ||
       (msg.senderId === active.value.id && msg.receiverId === myUserId.value))
    ) {
      // 去重
      if (msg.id > 0 && messages.value.some((m) => m.id === msg.id)) {
        return;
      }
      const dup = messages.value.findIndex((m) =>
        m.id < 0 && m.senderId === msg.senderId && m.content === msg.content,
      );
      if (dup !== -1) {
        messages.value[dup] = msg;
      } else {
        messages.value.push(msg);
      }
      scrollBottom();
      if (msg.senderId === active.value.id) {
        markMessagesRead(active.value.id);
      }
    }
    // 刷新会话列表
    loadConversations();
  });

  loadConversations();
});

// 会话列表预览文案
function formatConversationPreview(lastMsg) {
  if (!lastMsg) return '暂无消息';
  if (lastMsg.imageUrl) return '[图片]';
  if (isCommentNotification(lastMsg)) return '💬 ' + getCommentNotifyText(lastMsg);
  return lastMsg.content || '';
}

// 判断是否为评论通知消息
function isCommentNotification(msg) {
  try {
    const data = JSON.parse(msg.content || '{}');
    return data.type === 'comment_reply';
  } catch { return false; }
}

// 获取评论通知的可读文本
function getCommentNotifyText(msg) {
  try {
    const data = JSON.parse(msg.content || '{}');
    return `在《${data.albumTitle}》中回复了你`;
  } catch { return msg.content; }
}

// 点击跳转到专辑详情页
function goToAlbum(msg) {
  try {
    const data = JSON.parse(msg.content || '{}');
    if (data.albumSlug) {
      const q = data.commentId ? `?commentId=${data.commentId}` : '';
      router.push(`/albums/${data.albumSlug}${q}`);
    }
  } catch {}
}

onBeforeUnmount(() => {
  socket.value?.disconnect();
});
</script>

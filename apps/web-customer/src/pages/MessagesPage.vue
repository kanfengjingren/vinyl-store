<template>
  <!-- 图片大图预览 -->
  <Teleport to="body">
    <div v-if="previewImage" class="fixed inset-0 z-[250] bg-black/80 flex items-center justify-center cursor-pointer" @click="previewImage = null">
      <img :src="previewImage" class="max-w-[90vw] max-h-[90vh] object-contain" />
    </div>
  </Teleport>

  <div class="fixed top-[52px] left-0 right-0 flex z-[1]" :class="player.track ? 'bottom-16' : 'bottom-0'">
    <!-- 左侧会话列表 -->
    <div class="w-[300px] shrink-0 border-r border-black/5 bg-white/50 flex flex-col">
      <div class="px-5 py-4 text-sm font-semibold text-black border-b border-black/5">消息</div>
      <div class="flex-1 overflow-y-auto">
        <div class="px-5 py-3 text-xs font-medium text-gray-400 uppercase tracking-wider">消息</div>
        <div v-if="filteredConversations.length === 0 && !loading" class="px-5 py-8 text-center text-black/20 text-sm">
          暂无消息
        </div>
        <div
          v-for="c in filteredConversations"
          :key="c.partner.id"
          @click="select(c.partner)"
          :class="[
            'px-5 py-3 cursor-pointer hover:bg-black/[0.02] transition-colors flex items-center gap-3',
            activeId === c.partner.id ? 'bg-[rgb(196,147,51)]/5 border-l-[3px] border-l-[rgb(196,147,51)]' : '',
          ]"
        >
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

    <!-- 右侧聊天区 (移动端仅在选中会话时显示) -->
    <div :class="['flex-1 flex flex-col bg-white', !active ? 'hidden md:flex' : 'flex']">
      <template v-if="active">
        <!-- Header -->
        <div class="px-4 py-3 border-b border-black/5 shrink-0 flex items-center gap-3">
          <button @click="backToList" class="md:hidden text-[18px] leading-none text-black/50 hover:text-black transition-colors px-1">&larr;</button>
          <p class="font-semibold text-black text-[15px]">{{ active.storeName || active.name || active.email }}</p>
        </div>

        <!-- 消息列表 -->
        <div ref="msgList" class="flex-1 overflow-y-auto px-5 py-4 space-y-4 flex flex-col">
          <div v-if="loading" class="text-center text-black/20 text-sm py-8">加载中...</div>
          <div
            v-for="msg in messages"
            :key="msg.id"
            :class="[
              'flex items-end gap-2',
              msg.senderId === myUserId ? 'flex-row-reverse self-end' : 'self-start',
            ]"
          >
            <!-- 评论通知卡片（无头像） -->
            <div
              v-if="isCommentNotification(msg)"
              @click="goToAlbum(msg)"
              class="bg-[rgb(196,147,51)]/5 border border-[rgb(196,147,51)]/20 rounded-xl px-4 py-3 cursor-pointer hover:bg-[rgb(196,147,51)]/10 transition-colors max-w-[320px]"
            >
              <p class="text-xs text-[rgb(196,147,51)] mb-1">💬 评论回复</p>
              <p class="text-sm text-black/70 leading-relaxed">{{ getCommentNotifyText(msg) }}</p>
              <p class="text-xs text-black/30 mt-1.5">点击查看 →</p>
            </div>

            <!-- 好友请求卡片 -->
            <div
              v-else-if="isFriendRequest(msg)"
              class="bg-white border border-gray-200 rounded-xl px-4 py-3 max-w-[300px]"
            >
              <template v-if="friendReqData(msg).status === 'pending'">
                <p class="text-sm text-black/70 mb-1">
                  <template v-if="msg.senderId === myUserId">你已发送好友申请</template>
                  <template v-else><span class="font-medium">{{ msg.sender?.name || '用户' }}</span> 请求添加你为好友</template>
                </p>
                <div v-if="msg.senderId !== myUserId" class="flex gap-2 mt-3">
                  <button
                    @click="handleAcceptFriend(msg)"
                    :disabled="friendReqLoading === msg.id"
                    class="px-4 py-1.5 bg-[rgb(196,147,51)] text-white text-xs font-medium hover:bg-[rgb(176,127,31)] disabled:opacity-50 transition-colors"
                  >接受</button>
                  <button
                    @click="handleRejectFriend(msg)"
                    :disabled="friendReqLoading === msg.id"
                    class="px-4 py-1.5 bg-gray-100 text-gray-500 text-xs font-medium hover:bg-gray-200 disabled:opacity-50 transition-colors"
                  >拒绝</button>
                </div>
              </template>
              <template v-else-if="friendReqData(msg).status === 'accepted'">
                <p class="text-sm text-green-600">✅ 你们已成为好友，可以开始聊天了</p>
              </template>
              <template v-else-if="friendReqData(msg).status === 'rejected'">
                <p class="text-sm text-gray-400">已拒绝好友申请</p>
              </template>
            </div>

            <!-- 普通消息 + 头像 -->
            <template v-else>
              <!-- 头像 -->
              <router-link
                :to="`/user/${msg.senderId}`"
                class="shrink-0 w-7 h-7 rounded-full bg-gray-100 flex items-center justify-center text-gray-400 text-[11px] font-medium overflow-hidden hover:ring-2 hover:ring-[rgb(196,147,51)]/50 transition-all cursor-pointer"
                :class="msg.senderId === myUserId ? 'self-end' : 'self-end'"
              >
                <img v-if="getSenderAvatar(msg)" :src="getSenderAvatar(msg)" class="w-full h-full object-cover" />
                <span v-else>{{ getSenderName(msg).slice(0, 1).toUpperCase() }}</span>
              </router-link>

              <!-- 气泡 -->
              <div
                :class="msg.senderId === myUserId
                  ? 'bg-[rgb(196,147,51)] text-white rounded-bl-2xl'
                  : 'bg-gray-100 text-black rounded-br-2xl'"
                class="px-1 py-1 rounded-2xl rounded-tl-md overflow-hidden max-w-[320px]"
              >
                <img v-if="msg.imageUrl" :src="msg.imageUrl" class="max-w-[240px] max-h-[320px] object-cover rounded-xl cursor-pointer" @click="previewImage = msg.imageUrl" />
                <p v-if="msg.content" class="px-3 py-1.5">{{ msg.content }}</p>
              </div>
            </template>
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
import { ref, computed, nextTick, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { io } from 'socket.io-client';
import { fetchConversations, fetchMessages, markMessagesRead, uploadChatImage, acceptFriendRequest, rejectFriendRequest, fetchFriends, searchUsers, sendFriendRequest } from '@vinyl-store/shared';
import { player } from '../stores/player';

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

// ── 好友列表 ──
const friends = ref([]);

async function loadFriends() {
  try {
    friends.value = await fetchFriends();
  } catch {}
}

// ── 用户搜索 ──
const searchQ = ref('');
const searchResults = ref([]);
const searchLoading = ref(false);
const searchFocused = ref(false);
let searchTimer = null;

function onSearchInput() {
  clearTimeout(searchTimer);
  const q = searchQ.value.trim();
  if (!q) {
    searchResults.value = [];
    return;
  }
  searchLoading.value = true;
  searchTimer = setTimeout(async () => {
    try {
      searchResults.value = await searchUsers(q);
    } catch {} finally {
      searchLoading.value = false;
    }
  }, 300);
}

async function handleSearchAddFriend(user) {
  try {
    await sendFriendRequest(user.id);
    // 从搜索结果移除，避免重复发送
    searchResults.value = searchResults.value.filter((u) => u.id !== user.id);
  } catch (e) {
    alert(e.response?.data?.message || '发送失败');
  }
}

// 过滤掉已是好友的对话（好友已在上面单独显示）
const filteredConversations = computed(() => {
  const friendIds = new Set(friends.value.map((f) => f.friend.id));
  return conversations.value.filter((c) => !friendIds.has(c.partner.id));
});

function coverSrc(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return url.startsWith('/') ? url : `/${url}`
}

const myUserId = ref(null);
const myAvatar = ref('');
try {
  const u = JSON.parse(localStorage.getItem('user') || '{}');
  myUserId.value = u.id;
  myAvatar.value = u.avatar || '';
} catch {}

function getSenderAvatar(msg) {
  const url = msg.senderId === myUserId.value ? myAvatar.value : (msg.sender?.avatar || '')
  if (!url) return ''
  if (url.startsWith('http')) return url
  return url.startsWith('/') ? url : `/${url}`
}

function getSenderName(msg) {
  if (msg.senderId === myUserId.value) return '我'
  return msg.sender?.name || '?'
}

async function loadConversations() {
  try {
    conversations.value = await fetchConversations();
    updateNavBarUnread();
  } catch (e) {
    console.error('加载会话列表失败:', e);
  }
}

function backToList() {
  active.value = null;
  activeId.value = null;
  messages.value = [];
}

function updateNavBarUnread() {
  const total = conversations.value.reduce((sum, c) => sum + (c.unreadCount || 0), 0);
  window.dispatchEvent(new CustomEvent('vinyl:unread-changed', { detail: total }));
}

async function select(partner) {
  active.value = partner;
  activeId.value = partner.id;
  messages.value = [];
  loading.value = true;

  // 乐观更新：立即清除对应会话的未读红点
  const conv = conversations.value.find(c => c.partner.id === partner.id);
  if (conv && conv.unreadCount > 0) {
    conv.unreadCount = 0;
    updateNavBarUnread();
  }

  try {
    const history = await fetchMessages(partner.id);
    messages.value = history || [];
    await markMessagesRead(partner.id);
    await loadConversations();
    updateNavBarUnread();
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
    sender: { id: myUserId.value, name: '我', avatar: myAvatar.value },
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

  const hostname = window.location.hostname;
  const isDev = hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '10.0.2.2';
  const serverUrl = isDev ? `http://${hostname}:3000` : '';

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
  loadFriends();
});

// 会话列表预览文案
function formatConversationPreview(lastMsg) {
  if (!lastMsg) return '暂无消息';
  if (lastMsg.imageUrl) return '[图片]';
  if (isFriendRequest(lastMsg)) {
    const d = friendReqData(lastMsg);
    return d.status === 'accepted' ? '✅ 你们已成为好友' : '🤝 好友申请';
  }
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

// 判断是否为好友请求消息
function isFriendRequest(msg) {
  try {
    const data = JSON.parse(msg.content || '{}');
    return data.type === 'friend_request';
  } catch { return false; }
}

// 解析好友请求消息数据
function friendReqData(msg) {
  try { return JSON.parse(msg.content || '{}'); } catch { return {}; }
}

const friendReqLoading = ref(null);

async function handleAcceptFriend(msg) {
  const data = friendReqData(msg);
  if (!data.friendshipId) return;
  friendReqLoading.value = msg.id;
  try {
    await acceptFriendRequest(data.friendshipId);
    // 更新消息内容为已接受
    msg.content = JSON.stringify({ ...data, status: 'accepted' });
    loadConversations();
    loadFriends();
  } catch (e) {
    alert(e.response?.data?.message || '操作失败');
  } finally {
    friendReqLoading.value = null;
  }
}

async function handleRejectFriend(msg) {
  const data = friendReqData(msg);
  if (!data.friendshipId) return;
  friendReqLoading.value = msg.id;
  try {
    await rejectFriendRequest(data.friendshipId);
    // 更新消息状态
    msg.content = JSON.stringify({ ...data, status: 'rejected' });
    loadConversations();
  } catch (e) {
    alert(e.response?.data?.message || '操作失败');
  } finally {
    friendReqLoading.value = null;
  }
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

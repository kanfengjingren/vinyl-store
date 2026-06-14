<template>
  <Teleport to="body">
    <Transition name="chat-panel">
      <div v-if="modelValue" class="fixed inset-0 z-[200] flex justify-end">
        <div class="absolute inset-0 bg-black/40" @click="close"></div>
        <div class="relative w-[420px] max-w-[90vw] h-full bg-white shadow-[-4px_0_30px_rgba(0,0,0,.15)] flex flex-col">
          <!-- Header -->
          <div class="flex items-center justify-between px-5 py-4 border-b border-black/5 shrink-0">
            <div>
              <p class="font-semibold text-black text-[15px]">{{ sellerName }}</p>
              <p class="text-[11px] text-black/30">在线聊天</p>
            </div>
            <button @click="close" class="text-black/30 hover:text-black/60 text-2xl leading-none">&times;</button>
          </div>

          <!-- 消息列表 -->
          <div ref="msgList" class="flex-1 overflow-y-auto px-4 py-4 space-y-3 flex flex-col">
            <div v-if="loading" class="text-center text-black/20 text-sm py-8">加载中...</div>
            <div v-else-if="messages.length === 0" class="text-center text-black/20 text-sm py-8">发送第一条消息吧</div>
            <div
              v-for="msg in messages"
              :key="msg.id"
              :class="[
                'flex max-w-[75%] text-[14px] leading-relaxed',
                msg.senderId === myUserId ? 'ml-auto' : 'mr-auto',
              ]"
            >
              <div
                :class="msg.senderId === myUserId
                  ? 'bg-[rgb(196,147,51)] text-white rounded-bl-2xl'
                  : 'bg-gray-100 text-black rounded-br-2xl'"
                class="px-1 py-1 rounded-2xl rounded-tl-md overflow-hidden"
              >
                <img v-if="msg.imageUrl" :src="msg.imageUrl" class="max-w-[240px] max-h-[320px] object-cover rounded-xl" @click="previewImage = msg.imageUrl" />
                <p v-if="msg.content" class="px-3 py-1.5">{{ msg.content }}</p>
              </div>
            </div>
          </div>

          <!-- 连接状态 -->
          <div v-if="connectionError" class="px-4 py-2 bg-red-50 text-red-500 text-xs text-center">{{ connectionError }}</div>
          <div v-else-if="!connected && !loading" class="px-4 py-2 bg-yellow-50 text-yellow-600 text-xs text-center">正在连接...</div>

          <!-- 图片大图预览 -->
          <Teleport to="body">
            <div v-if="previewImage" class="fixed inset-0 z-[250] bg-black/80 flex items-center justify-center cursor-pointer" @click="previewImage = null">
              <img :src="previewImage" class="max-w-[90vw] max-h-[90vh] object-contain" />
            </div>
          </Teleport>

          <!-- 输入区 -->
          <div v-if="uploading" class="px-4 py-1.5 text-xs text-black/40 text-center">📷 上传中...</div>
          <div class="px-4 py-3 border-t border-black/5 shrink-0 flex gap-2">
            <input
              ref="fileInput"
              type="file"
              accept="image/*"
              class="hidden"
              @change="onFilePicked"
            />
            <button
              @click="fileInput.click()"
              :disabled="!connected"
              class="w-10 h-10 flex items-center justify-center text-lg hover:bg-gray-100 transition-colors cursor-pointer disabled:opacity-30 shrink-0"
              title="发送图片"
            >🖼</button>
            <input
              v-model="text"
              @keydown.enter="send"
              :disabled="!connected"
              :placeholder="connected ? '输入消息...' : '连接中...'"
              class="flex-1 px-4 py-2.5 bg-gray-50 border border-black/5 text-sm outline-none focus:border-[rgb(196,147,51)] disabled:opacity-40"
            />
            <button
              @click="send"
              :disabled="!text.trim()"
              class="px-5 py-2.5 bg-black text-white text-sm font-medium hover:bg-black/80 disabled:opacity-30 transition-colors cursor-pointer"
            >发送</button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue';
import { io } from 'socket.io-client';
import { fetchMessages, uploadChatImage } from '@vinyl-store/shared';

const props = defineProps({
  sellerId: { type: Number, required: true },
  sellerName: { type: String, default: '' },
  modelValue: Boolean,
});
const emit = defineEmits(['update:modelValue']);

const msgList = ref(null);
const messages = ref([]);
const text = ref('');
const loading = ref(true);
const socket = ref(null);
const connected = ref(false);
const connectionError = ref('');
const uploading = ref(false);
const previewImage = ref(null);
const fileInput = ref(null);

const myUserId = ref(null);
try {
  const u = JSON.parse(localStorage.getItem('user') || '{}');
  myUserId.value = u.id;
} catch {}

function close() {
  emit('update:modelValue', false);
}

async function connect() {
  const token = localStorage.getItem('token');
  console.log('[ChatWidget] connect() 被调用, token 存在:', !!token);
  if (!token) return;

  // 开发环境用绝对路径连 3000 端口，生产环境走同域名
  const isDev = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
  const serverUrl = isDev ? 'http://localhost:3000' : '';
  const fullUrl = serverUrl + '/chat';
  console.log('[ChatWidget] 连接地址:', fullUrl, '| 开发模式:', isDev);

  socket.value = io(fullUrl, {
    auth: { token },
    transports: ['websocket', 'polling'],
  });
  console.log('[ChatWidget] io() 已调用, socket 实例:', !!socket.value);

  socket.value.on('connect', () => {
    console.log('[ChatWidget] ✅ WebSocket 已连接! socket.id:', socket.value?.id);
    connected.value = true;
    connectionError.value = '';
  });

  socket.value.on('newMessage', (msg) => {
    console.log('[ChatWidget] 📩 收到 newMessage:', msg.id, msg.content?.slice(0, 20));
    // 只处理属于这个会话的消息
    if (
      (msg.senderId === myUserId.value && msg.receiverId === props.sellerId) ||
      (msg.senderId === props.sellerId && msg.receiverId === myUserId.value)
    ) {
      // 去重
      // 1) 已有相同真实 ID → 跳过
      if (msg.id > 0 && messages.value.some((m) => m.id === msg.id)) {
        return;
      }
      // 2) 替换临时乐观消息（相同发送者 + 相同内容）
      const dup = messages.value.findIndex((m) =>
        m.id < 0 && m.senderId === msg.senderId && m.content === msg.content,
      );
      if (dup !== -1) {
        messages.value[dup] = msg;
      } else {
        messages.value.push(msg);
      }
      scrollBottom();
    }
  });

  socket.value.on('connect_error', (err) => {
    console.error('[ChatWidget] ❌ connect_error:', err.message, err);
    connected.value = false;
    connectionError.value = '连接失败: ' + err.message;
  });

  socket.value.on('disconnect', (reason) => {
    console.log('[ChatWidget] 🔌 已断开, 原因:', reason);
    connected.value = false;
  });

  socket.value.io.on('reconnect_attempt', (attempt) => {
    console.log('[ChatWidget] 🔄 重连第', attempt, '次...');
  });

  socket.value.io.on('error', (err) => {
    console.error('[ChatWidget] ⚠️ 底层错误:', err);
  });

  // 加载历史消息
  try {
    loading.value = true;
    console.log('[ChatWidget] 加载历史消息, sellerId:', props.sellerId);
    const history = await fetchMessages(props.sellerId);
    messages.value = history || [];
    console.log('[ChatWidget] 历史消息:', history?.length, '条');
    scrollBottom();
  } catch (e) {
    console.error('[ChatWidget] 加载历史消息失败:', e);
    messages.value = [];
  } finally {
    loading.value = false;
  }

  console.log('[ChatWidget] connect() 完成, 当前状态: connected=', connected.value, 'loading=', loading.value);
}

function send() {
  const content = text.value.trim();
  if (!content || !socket.value?.connected) return;
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
    console.error('[ChatWidget] 图片上传失败:', err);
    connectionError.value = '图片上传失败，请重试';
    setTimeout(() => { connectionError.value = ''; }, 3000);
  } finally {
    uploading.value = false;
    // 清空 file input 以便重复选同一文件
    if (fileInput.value) fileInput.value.value = '';
  }
}

function emitMessage({ content, imageUrl }) {
  if (!socket.value?.connected) return;
  const tempId = -Date.now();
  messages.value.push({
    id: tempId,
    senderId: myUserId.value,
    receiverId: props.sellerId,
    content: content || '',
    imageUrl: imageUrl || null,
    createdAt: new Date().toISOString(),
    sender: { id: myUserId.value, name: '我' },
  });
  scrollBottom();

  socket.value.emit('sendMessage', { receiverId: props.sellerId, content: content || '', imageUrl }, (response) => {
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

// 组件挂载即连接（因为 v-if="chatOpen" 保证挂载时一定是打开状态）
onMounted(() => {
  console.log('[ChatWidget] 组件已挂载, modelValue:', props.modelValue);
  connect();
});

onBeforeUnmount(() => {
  console.log('[ChatWidget] 组件即将卸载, 断开连接');
  socket.value?.disconnect();
  socket.value = null;
});
</script>

<style scoped>
.chat-panel-enter-active,
.chat-panel-leave-active {
  transition: opacity 0.3s ease;
}
.chat-panel-enter-active > div:last-child,
.chat-panel-leave-active > div:last-child {
  transition: transform 0.35s cubic-bezier(0.22, 0.61, 0.36, 1);
}
.chat-panel-enter-from,
.chat-panel-leave-to {
  opacity: 0;
}
.chat-panel-enter-from > div:last-child {
  transform: translateX(100%);
}
.chat-panel-leave-to > div:last-child {
  transform: translateX(100%);
}
</style>

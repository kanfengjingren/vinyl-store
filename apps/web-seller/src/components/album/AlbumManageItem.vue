<template>
  <div
    :class="[
      'border rounded-xl flex items-center gap-5 px-5 py-4 transition-shadow',
      album.status === 'DELISTED'
        ? 'border-black/5 bg-black/[0.02] opacity-60'
        : 'border-[rgb(196,147,51)]/20 bg-white hover:shadow-md'
    ]"
  >
    <div class="relative shrink-0">
      <img
        :src="coverSrc(album.coverUrl)"
        :alt="album.title"
        class="w-[72px] h-[72px] rounded-lg object-cover bg-black/5 shadow-sm"
        :class="album.status === 'DELISTED' ? 'grayscale' : ''"
      />
      <div
        v-if="album.status === 'DELISTED'"
        class="absolute inset-0 rounded-lg bg-black/30 flex items-center justify-center"
      >
        <span class="text-white text-[11px] font-medium">已下架</span>
      </div>
    </div>

    <div class="flex-1 min-w-0">
      <div class="flex items-center gap-2">
        <p
          :class="[
            'text-[15px] font-semibold truncate',
            album.status === 'DELISTED' ? 'text-black/40 line-through' : 'text-black'
          ]"
        >
          {{ album.title }}
        </p>
        <span
          v-if="album.status === 'DELISTED'"
          class="text-[11px] font-medium px-2 py-0.5 rounded-full bg-red-100 text-red-500 shrink-0"
        >已下架</span>
      </div>
      <p class="text-sm text-black/40 mt-0.5">
        <span class="text-black/60 font-medium mr-1">¥{{ album.price }}</span>
        <span class="text-black/30 mx-1">|</span>
        <span>库存 {{ album.stock }}</span>
        <span v-if="album.artist" class="text-black/30 mx-1.5">|</span>
        <span v-if="album.artist" class="text-black/40">{{ album.artist }}</span>
      </p>
      <p v-if="album.label" class="text-xs text-black/30 mt-0.5">{{ album.label }}</p>
    </div>

    <!-- ACTIVE: 库存操作 + 删除 -->
    <div v-if="album.status !== 'DELISTED'" class="flex items-center gap-3 shrink-0">
      <div class="flex items-center gap-1.5">
        <input
          v-model.number="stockAmount"
          type="number"
          min="0"
          placeholder="数量"
          class="w-16 h-9 px-2.5 border border-black/15 rounded-lg text-sm outline-none focus:border-[rgb(196,147,51)] focus:ring-1 focus:ring-[rgb(196,147,51)]/20 transition-all [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
        />
        <button
          :disabled="stockAmount <= 0"
          class="h-9 px-4 rounded-lg border border-[rgb(196,147,51)]/50 bg-[rgb(196,147,51)] text-white text-sm font-medium hover:bg-[rgb(176,127,31)] transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
          @click="onUpdateStock"
        >
          更新库存
        </button>
      </div>
      <button
        class="h-9 px-4 rounded-lg border border-red-200 bg-red-50 text-red-500 text-sm font-medium hover:bg-red-100 transition-colors"
        @click="onDelete"
      >
        删除
      </button>
    </div>

    <!-- DELISTED: 无操作 -->
    <div v-else class="shrink-0 text-xs text-black/30">
      该专辑已下架，不可操作
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { updateAlbum, deleteAlbum, useModalStore } from '@vinyl-store/shared';

const modal = useModalStore();

const props = defineProps({ album: Object });
const emit = defineEmits(['refresh']);

const stockAmount = ref(0);

function coverSrc(url) {
  if (!url) return '';
  return url.startsWith('http') ? url : `/${url}`;
}

function onUpdateStock() {
  if (stockAmount.value <= 0) return;
  updateAlbum(props.album.id, { stock: stockAmount.value }).then(() => {
    stockAmount.value = 0;
    emit('refresh');
  });
}

async function onDelete() {
  const ok = await modal.open({ message: '确定要删除此专辑吗？', confirmText: '确定删除', cancelText: '取消' });
  if (!ok) return;
  deleteAlbum(props.album.id).then(() => emit('refresh'));
}
</script>

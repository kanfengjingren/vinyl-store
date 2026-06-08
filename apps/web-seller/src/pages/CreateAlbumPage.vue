<template>
  <div>
    <h2 class="text-xl font-semibold tracking-[-0.02em] mb-6">上架专辑</h2>

    <form @submit.prevent="handleSubmit" class="max-w-2xl space-y-5">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
        <div>
          <label class="block text-sm font-medium mb-1">专辑名称 *</label>
          <input v-model="form.title" type="text" required
            class="w-full px-3 py-2 border border-black/15 rounded-lg text-sm outline-none focus:border-[rgb(196,147,51)] focus:ring-1 focus:ring-[rgb(196,147,51)]/20 transition-all" />
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">艺术家 *</label>
          <input v-model="form.artist" type="text" required
            class="w-full px-3 py-2 border border-black/15 rounded-lg text-sm outline-none focus:border-[rgb(196,147,51)] focus:ring-1 focus:ring-[rgb(196,147,51)]/20 transition-all" />
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">价格 (¥) *</label>
          <input v-model.number="form.price" type="number" min="0" required
            class="w-full px-3 py-2 border border-black/15 rounded-lg text-sm outline-none focus:border-[rgb(196,147,51)] focus:ring-1 focus:ring-[rgb(196,147,51)]/20 transition-all" />
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">国家</label>
          <input v-model="form.country" type="text"
            class="w-full px-3 py-2 border border-black/15 rounded-lg text-sm outline-none focus:border-[rgb(196,147,51)] focus:ring-1 focus:ring-[rgb(196,147,51)]/20 transition-all" />
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">标签</label>
          <input v-model="form.badge" type="text"
            class="w-full px-3 py-2 border border-black/15 rounded-lg text-sm outline-none focus:border-[rgb(196,147,51)] focus:ring-1 focus:ring-[rgb(196,147,51)]/20 transition-all" />
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">URL 标识</label>
          <input v-model="form.slug" type="text" @input="onSlugInput"
            class="w-full px-3 py-2 border border-black/15 rounded-lg text-sm outline-none focus:border-[rgb(196,147,51)] focus:ring-1 focus:ring-[rgb(196,147,51)]/20 transition-all" />
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">发行年份</label>
          <input v-model.number="form.year" type="number"
            class="w-full px-3 py-2 border border-black/15 rounded-lg text-sm outline-none focus:border-[rgb(196,147,51)] focus:ring-1 focus:ring-[rgb(196,147,51)]/20 transition-all" />
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">库存</label>
          <input v-model.number="form.stock" type="number" min="0"
            class="w-full px-3 py-2 border border-black/15 rounded-lg text-sm outline-none focus:border-[rgb(196,147,51)] focus:ring-1 focus:ring-[rgb(196,147,51)]/20 transition-all" />
        </div>
      </div>

      <div>
        <label class="block text-sm font-medium mb-1">简介</label>
        <textarea v-model="form.description" rows="3"
          class="w-full px-3 py-2 border border-black/15 rounded-lg text-sm outline-none focus:border-[rgb(196,147,51)] focus:ring-1 focus:ring-[rgb(196,147,51)]/20 transition-all resize-none"
          placeholder="关于这张专辑的一些描述..."></textarea>
      </div>

      <div>
        <label class="block text-sm font-medium mb-1">封面</label>
        <div class="w-[100px] h-[100px] border-2 border-dashed border-black/15 rounded-lg flex items-center justify-center cursor-pointer hover:border-[rgb(196,147,51)]/50 transition-colors"
          @click="imgInput?.click()">
          <img v-if="previewUrl" :src="previewUrl" class="w-full h-full object-cover rounded-lg" />
          <span v-else class="text-xs text-black/30">点击上传</span>
        </div>
        <input ref="imgInput" type="file" class="hidden" accept="image/*" @change="onFileChange" />
      </div>

      <div v-if="errorMsg" class="text-red-500 text-sm">{{ errorMsg }}</div>
      <div v-if="successMsg" class="text-green-600 text-sm">{{ successMsg }}</div>

      <button type="submit" :disabled="submitting"
        class="w-full py-3 bg-[rgb(196,147,51)] text-white font-semibold rounded-full hover:bg-[rgb(176,127,31)] disabled:opacity-50 transition-colors">
        {{ submitting ? '提交中...' : '上架专辑' }}
      </button>
    </form>
  </div>
</template>

<script setup>
import { reactive, ref, watch, useTemplateRef } from 'vue';
defineOptions({ name: 'CreateAlbumPage' });
import { uploadCover } from '@vinyl-store/shared';
import { useSellerAlbumStore } from '../stores/albums';

const store = useSellerAlbumStore();
const imgInput = useTemplateRef('imgInput');
const previewUrl = ref('');

const initialState = {
  title: '', artist: '', price: 0, country: '', badge: '', slug: '',
  description: '', year: new Date().getFullYear(), stock: 0,
};

const form = reactive({ ...initialState });

// 根据 artist + title 自动生成 slug
let slugManuallyEdited = false;
watch([() => form.artist, () => form.title], ([artist, title]) => {
  if (slugManuallyEdited) return;
  form.slug = [artist, title]
    .filter(Boolean)
    .join('-')
    .toLowerCase()
    .replace(/[^a-z0-9一-鿿]+/g, '-')
    .replace(/^-|-$/g, '')
    .replace(/-+/g, '-');
});
function onSlugInput() {
  slugManuallyEdited = !!form.slug.trim();
}
const submitting = ref(false);
const errorMsg = ref('');
const successMsg = ref('');

function onFileChange(e) {
  const file = e.target.files[0];
  if (file) previewUrl.value = URL.createObjectURL(file);
}

async function doUploadCover() {
  const file = imgInput.value?.files?.[0];
  if (!file) return null;
  return uploadCover(file);
}

async function handleSubmit() {
  errorMsg.value = '';
  successMsg.value = '';

  if (!form.title.trim() || !form.artist.trim()) {
    errorMsg.value = '请填写专辑名称和艺术家';
    return;
  }

  submitting.value = true;
  try {
    form.coverUrl = (await doUploadCover()) || '';
    await store.addAlbum({ ...form });
    successMsg.value = '专辑上架成功！';
    Object.assign(form, initialState);
    previewUrl.value = '';
  } catch (err) {
    errorMsg.value = err.response?.data?.message || '上架失败';
  } finally {
    submitting.value = false;
  }
}
</script>

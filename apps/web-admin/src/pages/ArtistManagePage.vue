<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-xl font-semibold tracking-[-0.02em]">乐队/艺人管理</h2>
      <button @click="openCreate"
        class="px-4 py-2 rounded-full bg-[rgb(196,147,51)] text-white text-sm font-medium hover:bg-[rgb(176,127,31)] transition-colors">
        + 新建乐队
      </button>
    </div>

    <div v-if="loading" class="text-center py-16 text-black/40">加载中...</div>

    <div v-else-if="!artists.length" class="text-center py-16 text-black/40">
      <p class="text-[15px]">暂无乐队/艺人</p>
    </div>

    <div v-else class="space-y-3">
      <div
        v-for="artist in artists"
        :key="artist.id"
        class="border border-black/10 rounded-xl bg-white px-5 py-4 flex items-center justify-between hover:shadow-sm transition-shadow"
      >
        <div class="flex items-center gap-4 flex-1 min-w-0">
          <div class="w-12 h-12 rounded-full bg-black/5 overflow-hidden shrink-0">
            <img v-if="artist.photo" :src="artistPhotoSrc(artist.photo)" class="w-full h-full object-cover" />
            <div v-else class="w-full h-full flex items-center justify-center text-lg font-bold text-black/20">
              {{ artist.name.slice(0, 2).toUpperCase() }}
            </div>
          </div>
          <div class="flex-1 min-w-0">
            <p class="text-[15px] font-semibold text-black">{{ artist.name }}</p>
            <p class="text-sm text-black/40">
              slug: {{ artist.slug }}
              <span v-if="artist.foundedYear" class="text-black/25 mx-1.5">|</span>
              <span v-if="artist.foundedYear">{{ artist.foundedYear }}</span>
              <span v-if="artist.country" class="text-black/25 mx-1.5">|</span>
              <span v-if="artist.country">{{ artist.country }}</span>
              <span class="text-black/25 mx-1.5">|</span>
              {{ artist._count?.albums ?? 0 }} 张专辑
            </p>
            <p v-if="artist.description" class="text-xs text-black/30 mt-1 line-clamp-1">{{ artist.description }}</p>
          </div>
        </div>

        <div class="flex items-center gap-2 shrink-0 ml-4">
          <button @click="openEdit(artist)"
            class="px-4 py-2 rounded-lg border border-black/15 text-sm font-medium hover:bg-black/[0.03] transition-colors">
            编辑
          </button>
        </div>
      </div>
    </div>

    <!-- Modal: 创建/编辑 -->
    <div v-if="showModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40" @click.self="closeModal">
      <div class="bg-white rounded-2xl w-[480px] max-h-[90vh] overflow-y-auto shadow-xl p-6">
        <h3 class="text-lg font-semibold mb-5">{{ editing ? '编辑乐队' : '新建乐队' }}</h3>
        <form @submit.prevent="handleSave" class="space-y-4">
          <div>
            <label class="block text-sm font-medium mb-1">乐队名称 *</label>
            <input v-model="form.name" type="text" required
              class="w-full px-3 py-2 border border-black/15 rounded-lg text-sm outline-none focus:border-[rgb(196,147,51)] focus:ring-1 focus:ring-[rgb(196,147,51)]/20 transition-all" />
          </div>
          <div>
            <label class="block text-sm font-medium mb-2">头像</label>
            <div class="flex items-center gap-4">
              <div
                class="w-[80px] h-[80px] rounded-full border-2 border-dashed border-black/15 flex items-center justify-center cursor-pointer hover:border-[rgb(196,147,51)]/50 transition-colors relative overflow-hidden shrink-0"
                @click="photoInput?.click()"
              >
                <img v-if="photoPreview" :src="photoPreview" class="w-full h-full object-cover absolute inset-0" />
                <span v-if="!photoPreview" class="text-[11px] text-black/30 text-center leading-tight">点击<br/>上传</span>
              </div>
              <input ref="photoInput" type="file" class="hidden" accept="image/*" @change="onPhotoChange" />
              <p class="text-xs text-black/30">支持 JPG/PNG/WebP，≤5MB</p>
            </div>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium mb-1">成立年份</label>
              <input v-model.number="form.foundedYear" type="number" min="1900"
                class="w-full px-3 py-2 border border-black/15 rounded-lg text-sm outline-none focus:border-[rgb(196,147,51)] focus:ring-1 focus:ring-[rgb(196,147,51)]/20 transition-all" />
            </div>
            <div>
              <label class="block text-sm font-medium mb-1">国家</label>
              <input v-model="form.country" type="text"
                class="w-full px-3 py-2 border border-black/15 rounded-lg text-sm outline-none focus:border-[rgb(196,147,51)] focus:ring-1 focus:ring-[rgb(196,147,51)]/20 transition-all" />
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium mb-1">简介</label>
            <textarea v-model="form.description" rows="3"
              class="w-full px-3 py-2 border border-black/15 rounded-lg text-sm outline-none focus:border-[rgb(196,147,51)] focus:ring-1 focus:ring-[rgb(196,147,51)]/20 transition-all resize-none"
              placeholder="乐队简介..."></textarea>
          </div>
          <div v-if="errorMsg" class="text-red-500 text-sm">{{ errorMsg }}</div>
          <div class="flex justify-end gap-3 pt-2">
            <button type="button" @click="closeModal"
              class="px-5 py-2.5 rounded-full border border-black/15 text-sm font-medium hover:bg-black/[0.03] transition-colors">
              取消
            </button>
            <button type="submit" :disabled="saving"
              class="px-5 py-2.5 rounded-full bg-[rgb(196,147,51)] text-white text-sm font-medium hover:bg-[rgb(176,127,31)] disabled:opacity-50 transition-colors">
              {{ saving ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, useTemplateRef } from 'vue';
import { fetchArtists, createArtist, updateArtist, uploadArtistPhoto } from '@vinyl-store/shared';

function artistPhotoSrc(url) {
  if (!url) return '';
  return url.startsWith('http') || url.startsWith('/') ? url : `/${url}`;
}

const photoInput = useTemplateRef('photoInput');
const photoFile = ref(null);
const photoPreview = ref('');
const artists = ref([]);
const loading = ref(false);

onMounted(() => load());

async function load() {
  loading.value = true;
  try {
    artists.value = await fetchArtists();
  } finally {
    loading.value = false;
  }
}

function onPhotoChange(e) {
  const file = e.target.files[0];
  if (file) {
    photoFile.value = file;
    photoPreview.value = URL.createObjectURL(file);
  }
}

// Modal
const showModal = ref(false);
const editing = ref(null);
const saving = ref(false);
const errorMsg = ref('');

const emptyForm = { name: '', photo: '', foundedYear: null, country: '', description: '' };
const form = reactive({ ...emptyForm });

function openCreate() {
  editing.value = null;
  Object.assign(form, emptyForm);
  photoFile.value = null;
  photoPreview.value = '';
  errorMsg.value = '';
  showModal.value = true;
}

function openEdit(artist) {
  editing.value = artist;
  form.name = artist.name;
  form.photo = artist.photo || '';
  form.foundedYear = artist.foundedYear ?? null;
  form.country = artist.country || '';
  form.description = artist.description || '';
  photoFile.value = null;
  photoPreview.value = artistPhotoSrc(artist.photo);
  errorMsg.value = '';
  showModal.value = true;
}

function closeModal() {
  showModal.value = false;
  editing.value = null;
}

async function handleSave() {
  if (!form.name.trim()) {
    errorMsg.value = '请输入乐队名称';
    return;
  }
  saving.value = true;
  errorMsg.value = '';
  try {
    // 先上传头像（如有新文件）
    let photoUrl = form.photo; // 保留旧的（如果是编辑且未换图）
    if (photoFile.value) {
      photoUrl = await uploadArtistPhoto(photoFile.value);
    }
    const data = { ...form, photo: photoUrl || '', foundedYear: form.foundedYear || undefined };
    if (editing.value) {
      await updateArtist(editing.value.id, data);
    } else {
      await createArtist(data);
    }
    closeModal();
    await load();
  } catch (err) {
    errorMsg.value = err.response?.data?.message || err.message || '保存失败';
  } finally {
    saving.value = false;
  }
}
</script>

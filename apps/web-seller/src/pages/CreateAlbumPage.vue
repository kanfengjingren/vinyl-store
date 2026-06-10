<template>
  <div>
    <h2 class="text-xl font-semibold tracking-[-0.02em] mb-6">上架专辑</h2>

    <form @submit.prevent="handleSubmit" class="max-w-2xl space-y-5">
      <!-- 专辑基本信息 -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
        <div>
          <label class="block text-sm font-medium mb-1">专辑名称 *</label>
          <input v-model="form.title" type="text" required
            class="w-full px-3 py-2 border border-black/15 rounded-lg text-sm outline-none focus:border-[rgb(196,147,51)] focus:ring-1 focus:ring-[rgb(196,147,51)]/20 transition-all" />
        </div>
        <div class="relative">
          <label class="block text-sm font-medium mb-1">艺术家 *</label>
          <input
            v-model="artistQuery"
            type="text"
            required
            placeholder="输入乐队/艺人名称搜索..."
            autocomplete="off"
            @focus="onArtistFocus"
            @blur="onArtistBlur"
            @input="onArtistInput"
            class="w-full px-3 py-2 border border-black/15 rounded-lg text-sm outline-none focus:border-[rgb(196,147,51)] focus:ring-1 focus:ring-[rgb(196,147,51)]/20 transition-all"
          />
          <!-- 下拉联想 -->
          <ul
            v-if="showArtistDropdown && (artistSuggestions.length > 0 || artistQuery.trim())"
            class="absolute z-20 left-0 right-0 mt-1 bg-white border border-black/10 rounded-lg shadow-lg max-h-48 overflow-y-auto"
          >
            <li
              v-for="a in artistSuggestions"
              :key="a.id"
              @mousedown.prevent="selectArtist(a)"
              class="px-3 py-2 text-sm cursor-pointer hover:bg-[rgb(196,147,51)]/10 transition-colors flex items-center gap-2"
            >
              <img v-if="a.photo" :src="a.photo" class="w-6 h-6 rounded-full object-cover" />
              <span>{{ a.name }}</span>
            </li>
            <li
              v-if="artistQuery.trim() && !artistSuggestions.some(a => a.name.toLowerCase() === artistQuery.trim().toLowerCase())"
              @mousedown.prevent="createNewArtist"
              class="px-3 py-2 text-sm cursor-pointer hover:bg-[rgb(196,147,51)]/10 transition-colors border-t border-black/5 text-[rgb(196,147,51)]"
            >
              ✦ 新建「{{ artistQuery.trim() }}」
            </li>
          </ul>
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

      <!-- 封面上传 -->
      <div>
        <label class="block text-sm font-medium mb-1">封面</label>
        <div class="w-[100px] h-[100px] border-2 border-dashed border-black/15 rounded-lg flex items-center justify-center cursor-pointer hover:border-[rgb(196,147,51)]/50 transition-colors relative overflow-hidden"
          @click="imgInput?.click()">
          <img v-if="previewUrl" :src="previewUrl" class="w-full h-full object-cover rounded-lg absolute inset-0" />
          <span v-if="!previewUrl" class="text-xs text-black/30">点击上传</span>
        </div>
        <input ref="imgInput" type="file" class="hidden" accept="image/*" @change="onCoverChange" />
      </div>

      <!-- 风格选择 -->
      <div>
        <label class="block text-sm font-medium mb-2">风格标签（可选）</label>
        <div class="flex flex-wrap gap-2 mb-2">
          <button
            v-for="cat in allCategories"
            :key="cat.slug"
            type="button"
            @click="toggleCategory(cat.slug)"
            :class="[
              'text-xs px-3 py-1.5 rounded-full border transition-all',
              selectedCats.includes(cat.slug)
                ? 'bg-[rgb(196,147,51)] text-white border-[rgb(196,147,51)]'
                : 'bg-white text-black/60 border-black/15 hover:border-[rgb(196,147,51)]/50 hover:text-[rgb(196,147,51)]'
            ]"
          >
            {{ cat.name }}
          </button>
        </div>
        <div class="flex gap-2">
          <input
            v-model="newCatName"
            type="text"
            placeholder="输入新风格名称，回车添加"
            class="flex-1 px-3 py-2 border border-black/15 rounded-lg text-sm outline-none focus:border-[rgb(196,147,51)] focus:ring-1 focus:ring-[rgb(196,147,51)]/20 transition-all"
            @keydown.enter.prevent="addNewCategory"
          />
          <button type="button" @click="addNewCategory" :disabled="!newCatName.trim()"
            class="px-4 py-2 text-sm rounded-lg border border-[rgb(196,147,51)] text-[rgb(196,147,51)] hover:bg-[rgb(196,147,51)]/10 disabled:opacity-30 transition-colors shrink-0">
            新建
          </button>
        </div>
      </div>

      <!-- 曲目列表 -->
      <div>
        <div class="flex items-center justify-between mb-3">
          <label class="text-sm font-medium">曲目列表（可选）</label>
          <button type="button" @click="addTrack"
            class="text-xs px-3 py-1.5 rounded-full border border-[rgb(196,147,51)] text-[rgb(196,147,51)] hover:bg-[rgb(196,147,51)]/10 transition-colors">
            + 添加曲目
          </button>
        </div>

        <div v-if="tracks.length === 0" class="text-sm text-black/30 py-4 text-center border border-dashed border-black/10 rounded-lg">
          暂未添加曲目，点击上方按钮添加
        </div>

        <div v-else class="space-y-2">
          <div v-for="(track, idx) in tracks" :key="idx"
            class="flex items-center gap-3 p-3 border border-black/10 rounded-lg bg-black/[0.01]">
            <span class="text-xs text-black/30 w-5 shrink-0">{{ idx + 1 }}</span>
            <input v-model="track.title" type="text" placeholder="曲目名称"
              class="flex-1 min-w-0 px-3 py-2 border border-black/15 rounded-lg text-sm outline-none focus:border-[rgb(196,147,51)] focus:ring-1 focus:ring-[rgb(196,147,51)]/20 transition-all" />
            <input v-model="track.duration" type="text" placeholder="时长，如 3:45"
              class="w-28 shrink-0 px-3 py-2 border border-black/15 rounded-lg text-sm outline-none focus:border-[rgb(196,147,51)] focus:ring-1 focus:ring-[rgb(196,147,51)]/20 transition-all" />
            <label class="shrink-0 cursor-pointer text-xs text-black/40 hover:text-[rgb(196,147,51)] transition-colors px-2 py-2 border border-dashed border-black/15 rounded-lg hover:border-[rgb(196,147,51)]/50"
              :class="{ 'text-green-600 border-green-300': track.file }">
              {{ track.file ? '✓' : '📁 音频' }}
              <input type="file" class="hidden" accept="audio/*" @change="(e) => onTrackFileChange(idx, e)" />
            </label>
            <button type="button" @click="removeTrack(idx)"
              class="shrink-0 w-6 h-6 flex items-center justify-center rounded-full text-black/25 hover:text-red-500 hover:bg-red-50 transition-colors text-sm">&times;</button>
          </div>
        </div>
      </div>

      <!-- 状态消息 -->
      <div v-if="errorMsg" class="text-red-500 text-sm">{{ errorMsg }}</div>
      <div v-if="successMsg" class="text-green-600 text-sm">{{ successMsg }}</div>

      <!-- 提交 -->
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
import { createAlbum, createTracks, uploadCover, uploadAudio } from '@vinyl-store/shared';
import { fetchCategories, createCategory } from '@vinyl-store/shared';
import { searchArtists, createArtist } from '@vinyl-store/shared';
import { onMounted } from 'vue';

const imgInput = useTemplateRef('imgInput');
const previewUrl = ref('');
const coverFile = ref(null);

const initialState = {
  title: '', artist: '', artistId: null, price: 0, country: '', slug: '',
  description: '', year: new Date().getFullYear(), stock: 10,
};

const form = reactive({ ...initialState });

// --- Artist combobox ---
const artistQuery = ref('');
const artistSuggestions = ref([]);
const showArtistDropdown = ref(false);
let artistDebounceTimer = null;

function onArtistFocus() {
  showArtistDropdown.value = true;
  if (artistQuery.value.trim()) searchArtistsDebounced(artistQuery.value.trim());
}

function onArtistBlur() {
  // 延迟关闭，让 mousedown 先触发
  setTimeout(() => { showArtistDropdown.value = false; }, 150);
}

function onArtistInput() {
  showArtistDropdown.value = true;
  const q = artistQuery.value.trim();
  if (!q) { artistSuggestions.value = []; return; }
  clearTimeout(artistDebounceTimer);
  artistDebounceTimer = setTimeout(() => searchArtistsDebounced(q), 250);
}

async function searchArtistsDebounced(q) {
  try {
    artistSuggestions.value = await searchArtists(q);
  } catch { artistSuggestions.value = []; }
}

function selectArtist(artist) {
  form.artist = artist.name;
  form.artistId = artist.id;
  artistQuery.value = artist.name;
  artistSuggestions.value = [];
  showArtistDropdown.value = false;
}

async function createNewArtist() {
  const name = artistQuery.value.trim();
  if (!name) return;
  try {
    const artist = await createArtist({ name });
    selectArtist(artist);
  } catch (err) {
    // 创建失败时仍保留字符串作为 artist
    form.artist = name;
    form.artistId = null;
    showArtistDropdown.value = false;
  }
}

let slugManuallyEdited = false;
// slug 跟随 artistQuery（用户正在输入的）变化
watch([artistQuery, () => form.title], ([artist, title]) => {
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

// 风格选择
const allCategories = ref([]);
const selectedCats = ref([]);
const newCatName = ref('');

onMounted(async () => {
  try { allCategories.value = await fetchCategories(); } catch {}
});

function toggleCategory(slug) {
  const idx = selectedCats.value.indexOf(slug);
  if (idx >= 0) selectedCats.value.splice(idx, 1);
  else selectedCats.value.push(slug);
}

async function addNewCategory() {
  const name = newCatName.value.trim();
  if (!name) return;
  try {
    const cat = await createCategory(name);
    if (!allCategories.value.find(c => c.slug === cat.slug)) {
      allCategories.value.push(cat);
    }
    if (!selectedCats.value.includes(cat.slug)) {
      selectedCats.value.push(cat.slug);
    }
    newCatName.value = '';
  } catch {}
}

// 曲目列表
const tracks = ref([]);

function addTrack() {
  tracks.value.push({ title: '', duration: '', file: null });
}

function removeTrack(idx) {
  tracks.value.splice(idx, 1);
}

function onTrackFileChange(idx, e) {
  const file = e.target.files[0];
  if (!file) return;
  tracks.value[idx].file = file;
  tracks.value[idx]._filename = file.name;

  // 自动读取音频时长
  const audio = new Audio();
  audio.src = URL.createObjectURL(file);
  audio.addEventListener('loadedmetadata', () => {
    const sec = audio.duration;
    if (sec && isFinite(sec)) {
      const m = Math.floor(sec / 60);
      const s = Math.floor(sec % 60);
      tracks.value[idx].duration = `${m}:${String(s).padStart(2, '0')}`;
    }
    URL.revokeObjectURL(audio.src);
  });
}

function onCoverChange(e) {
  const file = e.target.files[0];
  if (file) {
    coverFile.value = file;
    previewUrl.value = URL.createObjectURL(file);
  }
}

const submitting = ref(false);
const errorMsg = ref('');
const successMsg = ref('');

async function handleSubmit() {
  errorMsg.value = '';
  successMsg.value = '';

  if (!form.title.trim() || !form.artist.trim()) {
    errorMsg.value = '请填写专辑名称和艺术家';
    return;
  }

  submitting.value = true;
  try {
    // 1. 上传封面
    let coverUrl = '';
    if (coverFile.value) {
      coverUrl = await uploadCover(coverFile.value);
    }

    // 2. 创建专辑（含风格）
    const album = await createAlbum({ ...form, coverUrl, categories: selectedCats.value });
    if (!album || !album.id) throw new Error('专辑创建失败');

    // 3. 处理曲目（筛选有标题的）
    const validTracks = tracks.value.filter(t => t.title.trim());
    if (validTracks.length > 0) {
      // 对每个有音频文件的曲目先上传音频
      const trackList = [];
      for (const t of validTracks) {
        let audioUrl = '';
        if (t.file) {
          try {
            audioUrl = await uploadAudio(t.file);
          } catch { /* 音频上传失败不阻塞 */ }
        }
        trackList.push({
          title: t.title.trim(),
          duration: t.duration.trim() || null,
          audioUrl: audioUrl || null,
        });
      }
      await createTracks(album.id, trackList);
    }

    successMsg.value = `「${form.title}」上架成功！`;
    // 重置表单
    Object.assign(form, initialState);
    form.year = new Date().getFullYear();
    form.stock = 10;
    artistQuery.value = '';
    artistSuggestions.value = [];
    tracks.value = [];
    selectedCats.value = [];
    previewUrl.value = '';
    coverFile.value = null;
    slugManuallyEdited = false;
  } catch (err) {
    errorMsg.value = err.response?.data?.message || err.message || '上架失败';
  } finally {
    submitting.value = false;
  }
}
</script>

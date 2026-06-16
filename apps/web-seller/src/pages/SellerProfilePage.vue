<template>
  <div class="max-w-[600px]">
    <h1 class="text-2xl font-semibold tracking-[-0.02em] mb-8">主页设置</h1>

    <!-- 头像 -->
    <div class="mb-8 flex items-center gap-5">
      <div class="relative group">
        <div class="w-20 h-20 rounded-full overflow-hidden bg-gray-100 flex items-center justify-center shadow-md">
          <img v-if="form.avatar" :src="coverSrc(form.avatar)" class="w-full h-full object-cover" />
          <span v-else class="text-2xl font-semibold text-gray-400">{{ (form.name || auth.user?.email || '?').slice(0, 1).toUpperCase() }}</span>
        </div>
        <label class="absolute inset-0 rounded-full bg-black/40 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer">
          <span class="text-white text-xs font-medium">{{ uploading ? '上传中...' : '更换' }}</span>
          <input type="file" accept="image/*" class="hidden" @change="onAvatarChange" :disabled="uploading" />
        </label>
      </div>
      <div>
        <p class="text-sm font-medium text-black">{{ form.storeName || '未设置厂牌名' }}</p>
        <p class="text-xs text-gray-400 mt-0.5">点击头像更换</p>
        <p v-if="avatarError" class="text-red-400 text-xs mt-1">{{ avatarError }}</p>
      </div>
    </div>

    <!-- 表单 -->
    <div class="space-y-5">
      <div>
        <label class="block text-sm text-gray-500 mb-1.5">昵称</label>
        <input v-model="form.name" class="w-full px-3 py-2 border border-gray-200 text-sm outline-none focus:border-[rgb(196,147,51)] transition-colors" />
      </div>
      <div>
        <label class="block text-sm text-gray-500 mb-1.5">厂牌名</label>
        <input v-model="form.storeName" class="w-full px-3 py-2 border border-gray-200 text-sm outline-none focus:border-[rgb(196,147,51)] transition-colors" />
      </div>
      <div>
        <label class="block text-sm text-gray-500 mb-1.5">地址</label>
        <textarea v-model="form.defaultAddress" rows="2" class="w-full px-3 py-2 border border-gray-200 text-sm outline-none focus:border-[rgb(196,147,51)] transition-colors resize-none"></textarea>
      </div>
      <div>
        <label class="block text-sm text-gray-500 mb-1.5">厂牌介绍</label>
        <textarea v-model="form.description" rows="3" class="w-full px-3 py-2 border border-gray-200 text-sm outline-none focus:border-[rgb(196,147,51)] transition-colors resize-none" placeholder="介绍一下你的厂牌..."></textarea>
      </div>
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="block text-sm text-gray-500 mb-1.5">联系邮箱</label>
          <input v-model="form.contactEmail" class="w-full px-3 py-2 border border-gray-200 text-sm outline-none focus:border-[rgb(196,147,51)] transition-colors" />
        </div>
        <div>
          <label class="block text-sm text-gray-500 mb-1.5">联系电话</label>
          <input v-model="form.contactPhone" class="w-full px-3 py-2 border border-gray-200 text-sm outline-none focus:border-[rgb(196,147,51)] transition-colors" />
        </div>
      </div>

      <div class="pt-3">
        <button
          @click="handleSave"
          :disabled="saving"
          class="px-8 py-2.5 bg-[rgb(196,147,51)] text-white text-sm font-medium hover:bg-[rgb(176,127,31)] disabled:opacity-50 transition-colors"
        >
          {{ saving ? '保存中...' : '保存' }}
        </button>
        <span v-if="saveMsg" class="ml-3 text-sm" :class="saveOk ? 'text-green-600' : 'text-red-500'">{{ saveMsg }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useSellerAuthStore } from '../stores/auth'
import { updateSellerProfile, uploadAvatar } from '@vinyl-store/shared'

const auth = useSellerAuthStore()

const form = ref({
  name: '',
  avatar: '',
  defaultAddress: '',
  storeName: '',
  description: '',
  contactEmail: '',
  contactPhone: '',
})

const uploading = ref(false)
const avatarError = ref('')
const saving = ref(false)
const saveMsg = ref('')
const saveOk = ref(false)

onMounted(() => {
  const u = auth.user
  const s = auth.seller
  if (u) {
    form.value.name = u.name || ''
    form.value.avatar = u.avatar || ''
    form.value.defaultAddress = u.defaultAddress || ''
  }
  if (s) {
    form.value.storeName = s.storeName || ''
    form.value.description = s.description || ''
    form.value.contactEmail = s.contactEmail || ''
    form.value.contactPhone = s.contactPhone || ''
  }
})

function coverSrc(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return url.startsWith('/') ? url : `/${url}`
}

async function onAvatarChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  avatarError.value = ''
  uploading.value = true
  try {
    const url = await uploadAvatar(file)
    form.value.avatar = url
    // 同步更新 auth store
    auth.updateProfile({ avatar: url })
  } catch {
    avatarError.value = '上传失败，请重试'
  } finally {
    uploading.value = false
  }
}

async function handleSave() {
  saving.value = true
  saveMsg.value = ''
  try {
    const result = await updateSellerProfile({
      name: form.value.name,
      avatar: form.value.avatar,
      defaultAddress: form.value.defaultAddress,
      storeName: form.value.storeName,
      description: form.value.description,
      contactEmail: form.value.contactEmail,
      contactPhone: form.value.contactPhone,
    })
    // 更新 auth store（替换整个对象确保响应式）
    auth.updateProfile({
      name: result.name,
      avatar: result.avatar,
      defaultAddress: result.defaultAddress,
    })
    saveOk.value = true
    saveMsg.value = '保存成功'
  } catch {
    saveOk.value = false
    saveMsg.value = '保存失败'
  } finally {
    saving.value = false
  }
}
</script>

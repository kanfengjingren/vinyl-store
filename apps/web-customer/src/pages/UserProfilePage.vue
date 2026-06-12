<template>
  <div class="bg-white min-h-screen">
    <!-- Header -->
    <!-- <section class="w-full flex flex-col items-center text-center pt-20 pb-12 px-6">
      <h1 class="text-[clamp(32px,5vw,48px)] font-semibold tracking-[-0.02em] text-black mb-2">个人信息</h1>
      <p class="text-gray-400 text-[15px]">{{ profile.email }}</p>
    </section> -->

    <div class="max-w-[560px] mx-auto px-6 pb-20">
      <div v-if="loading" class="text-center py-20 text-gray-400 text-sm">加载中...</div>

      <template v-else>
        <!-- 账户余额 -->
        <div class="mb-6 pt-10">
          <label class="block text-sm text-gray-500 mb-1.5">账户余额</label>
          <p class="text-[15px] bg-gray-50 px-4 py-3">&yen;{{ profile.balance ?? 0 }}</p>
        </div>

        <!-- 充值 -->
        <div class="mb-6 p-5 border border-dashed border-[rgb(196,147,51)]/30 bg-[rgb(196,147,51)]/[0.02]">
          <label class="block text-sm font-medium text-black mb-2">充值（测试）</label>
          <div class="flex gap-2">
            <input v-model.number="rechargeAmount" type="number" min="1" placeholder="输入金额"
              class="flex-1 px-3 py-2 border border-gray-200 text-sm outline-none focus:border-[rgb(196,147,51)] transition-colors" />
            <button :disabled="recharging || !rechargeAmount"
              class="px-5 py-2 bg-[rgb(196,147,51)] text-white text-sm font-medium hover:bg-[rgb(176,127,31)] disabled:opacity-50 transition-colors shrink-0"
              @click="handleRecharge">
              {{ recharging ? '充值中...' : '充值' }}
            </button>
          </div>
          <p v-if="rechargeMsg" :class="['text-xs mt-2', rechargeOk ? 'text-green-600' : 'text-red-500']">{{ rechargeMsg }}</p>
        </div>

        <!-- 邮箱 -->
        <div class="mb-6">
          <label class="block text-sm text-gray-500 mb-1.5">邮箱</label>
          <p class="text-[15px] bg-gray-50 px-4 py-3">{{ profile.email }}</p>
        </div>

        <!-- 用户名 -->
        <div class="mb-6">
          <label class="block text-sm text-gray-500 mb-1.5">用户名</label>
          <p class="text-[15px] bg-gray-50 px-4 py-3">{{ profile.name || '未设置' }}</p>
        </div>

        <!-- 修改密码 -->
        <div class="mb-8 p-5 border border-black/5">
          <button class="text-sm font-medium text-gray-500 hover:text-black transition-colors" @click="showPwd = !showPwd">
            {{ showPwd ? '取消修改密码' : '修改密码 →' }}
          </button>
          <div v-if="showPwd" class="mt-4 space-y-3">
            <input v-model="oldPassword" type="password" placeholder="原密码"
              class="w-full px-3 py-2 border border-gray-200 text-sm outline-none focus:border-[rgb(196,147,51)] transition-colors" />
            <input v-model="newPassword" type="password" placeholder="新密码"
              class="w-full px-3 py-2 border border-gray-200 text-sm outline-none focus:border-[rgb(196,147,51)] transition-colors" />
            <button :disabled="pwdSaving || !oldPassword || !newPassword"
              class="px-5 py-2 bg-black text-white text-sm font-medium hover:bg-black/80 disabled:opacity-50 transition-colors"
              @click="handleChangePassword">
              {{ pwdSaving ? '修改中...' : '确认修改' }}
            </button>
            <p v-if="pwdMsg" :class="['text-xs', pwdOk ? 'text-green-600' : 'text-red-500']">{{ pwdMsg }}</p>
          </div>
        </div>

        <!-- 默认收货地址 -->
        <div class="mb-8">
          <label class="block text-sm text-gray-500 mb-1.5">
            默认收货地址
            <span class="text-red-400 ml-0.5">*</span>
          </label>
          <CitySelect v-model="address" v-model:detail="addressDetail" />
          <p v-if="addressError" class="text-red-400 text-sm mt-1.5">{{ addressError }}</p>
        </div>

        <button
          :disabled="saving"
          class="w-full py-3.5 bg-black text-white text-[15px] font-semibold border-none cursor-pointer hover:bg-black/80 transition-all disabled:opacity-50"
          @click="save"
        >
          {{ saving ? '保存中...' : '保存' }}
        </button>
        <p v-if="saved" class="text-green-600 text-sm text-center mt-3">保存成功</p>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { fetchProfile, updateProfile, recharge, changePassword } from '@vinyl-store/shared'
import CitySelect from '@vinyl-store/shared/ui/CitySelect'

const profile = ref({})
const address = ref('')
const addressDetail = ref('')
const loading = ref(true)
const saving = ref(false)
const saved = ref(false)
const addressError = ref('')
const rechargeAmount = ref(null)
const recharging = ref(false)
const rechargeMsg = ref('')
const rechargeOk = ref(false)
const showPwd = ref(false)
const oldPassword = ref('')
const newPassword = ref('')
const pwdSaving = ref(false)
const pwdMsg = ref('')
const pwdOk = ref(false)

async function handleRecharge() {
  if (!rechargeAmount.value || rechargeAmount.value <= 0) return
  recharging.value = true
  rechargeMsg.value = ''
  try {
    const result = await recharge(rechargeAmount.value)
    profile.value.balance = result.balance
    rechargeAmount.value = null
    rechargeOk.value = true
    rechargeMsg.value = `充值成功！当前余额 ¥${result.balance}`
  } catch (e) {
    rechargeOk.value = false
    rechargeMsg.value = e.response?.data?.message || '充值失败'
  } finally {
    recharging.value = false
  }
}

async function handleChangePassword() {
  if (!oldPassword.value || !newPassword.value) return
  pwdSaving.value = true
  pwdMsg.value = ''
  try {
    await changePassword({ oldPassword: oldPassword.value, newPassword: newPassword.value })
    pwdOk.value = true
    pwdMsg.value = '密码修改成功'
    oldPassword.value = ''
    newPassword.value = ''
  } catch (e) {
    pwdOk.value = false
    pwdMsg.value = e.response?.data?.message || '修改失败'
  } finally {
    pwdSaving.value = false
  }
}

onMounted(async () => {
  try {
    const data = await fetchProfile()
    profile.value = data
    address.value = data.defaultAddress || ''
  } finally {
    loading.value = false
  }
})

async function save() {
  if (!address.value.trim()) {
    addressError.value = '收货地址不能为空'
    return
  }
  addressError.value = ''
  saving.value = true
  saved.value = false
  try {
    const data = await updateProfile({ defaultAddress: address.value.trim() })
    profile.value = data
    saved.value = true
  } finally {
    saving.value = false
  }
}
</script>

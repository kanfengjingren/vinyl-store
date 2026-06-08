import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useModalStore = defineStore('modal', () => {
  const visible = ref(false)
  const message = ref('')
  const confirmText = ref('确定')
  const cancelText = ref('取消')
  const showCancel = ref(true)
  let resolvePromise = null

  function open({ message: msg, confirmText: ct, cancelText: cl, showCancel: sc } = {}) {
    message.value = msg || ''
    confirmText.value = ct || '确定'
    cancelText.value = cl || '取消'
    showCancel.value = sc !== false
    visible.value = true
    return new Promise((resolve) => {
      resolvePromise = resolve
    })
  }

  function confirm() {
    visible.value = false
    if (resolvePromise) resolvePromise(true)
  }

  function cancel() {
    visible.value = false
    if (resolvePromise) resolvePromise(false)
  }

  return { visible, message, confirmText, cancelText, showCancel, open, confirm, cancel }
})

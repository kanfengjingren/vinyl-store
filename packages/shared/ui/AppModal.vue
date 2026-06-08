<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="modal.visible"
        class="fixed inset-0 z-[300] flex items-center justify-center"
        @click.self="modal.cancel"
        @keydown.escape="modal.cancel"
      >
        <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" />

        <div class="relative bg-white rounded-2xl shadow-2xl px-8 py-7 w-[380px] max-w-[90vw] border border-[rgb(196,147,51)]/20">
          <p class="text-[15px] text-black/80 leading-relaxed mb-7">{{ modal.message }}</p>

          <div class="flex justify-end gap-3">
            <button
              v-if="modal.showCancel"
              class="text-sm px-5 py-2 rounded-lg border border-black/15 text-black/50 hover:text-black hover:border-black/30 transition-colors"
              @click="modal.cancel"
            >
              {{ modal.cancelText }}
            </button>
            <button
              class="text-sm px-5 py-2 rounded-lg bg-[rgb(196,147,51)] text-white hover:bg-[rgb(176,127,31)] transition-colors"
              @click="modal.confirm"
            >
              {{ modal.confirmText }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { useModalStore } from '../stores/modal'
const modal = useModalStore()
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
.modal-enter-active .relative,
.modal-leave-active .relative {
  transition: transform 0.2s ease, opacity 0.2s ease;
}
.modal-enter-from .relative {
  transform: scale(0.95);
  opacity: 0;
}
.modal-leave-to .relative {
  transform: scale(0.95);
  opacity: 0;
}
</style>

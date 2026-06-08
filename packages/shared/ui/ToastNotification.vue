<template>
  <Teleport to="body">
    <Transition name="toast">
      <div v-if="msg" class="fixed bottom-8 left-1/2 bg-apple-text text-white text-sm font-medium px-6 py-3 rounded-full z-[200] shadow-[0_8px_40px_rgba(0,0,0,.12)]" style="transform:translateX(-50%)">
        {{ msg }}
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref } from 'vue';

const msg = ref('');
let timer = null;

function show(text, duration = 2000) {
  msg.value = text;
  clearTimeout(timer);
  timer = setTimeout(() => { msg.value = ''; }, duration);
}

defineExpose({ show });
</script>

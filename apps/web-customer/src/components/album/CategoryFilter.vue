<template>
  <div class="flex gap-2.5 flex-wrap">
    <button @click="$emit('select', '')"
      :class="['text-sm font-medium px-[18px] py-2 cursor-pointer whitespace-nowrap transition-all border', active === '' ? 'bg-apple-text text-white border-apple-text' : 'bg-white text-apple-text border-apple-border hover:bg-apple-text hover:text-white hover:border-apple-text']">
      全部
    </button>
    <button v-for="cat in categories" :key="cat.slug" @click="$emit('select', cat.slug)"
      :class="['text-sm font-medium px-[18px] py-2 cursor-pointer whitespace-nowrap transition-all border', active === cat.slug ? 'bg-apple-text text-white border-apple-text' : 'bg-white text-apple-text border-apple-border hover:bg-apple-text hover:text-white hover:border-apple-text']">
      {{ cat.name }}
    </button>
  </div>
</template>

<script setup>
defineProps({ categories: Array, active: String });
defineEmits(['select']);
</script>

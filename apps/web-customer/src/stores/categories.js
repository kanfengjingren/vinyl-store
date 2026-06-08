import { defineStore } from 'pinia';
import { ref } from 'vue';
import { fetchCategories } from '@vinyl-store/shared';

export const useCategoryStore = defineStore('categories', () => {
  const list = ref([]);
  const loading = ref(false);

  async function load() {
    if (list.value.length) return;
    loading.value = true;
    try {
      list.value = await fetchCategories();
    } finally {
      loading.value = false;
    }
  }

  return { list, loading, load };
});

import { defineStore } from 'pinia';
import { ref, reactive } from 'vue';
import { fetchMyAlbums, createAlbum } from '@vinyl-store/shared';

export const useSellerAlbumStore = defineStore('sellerAlbums', () => {
  const list = ref([]);
  const loading = ref(false);
  const pagination = reactive({ page: 1, limit: 12, total: 0, totalPages: 0 });

  async function loadAlbums() {
    loading.value = true;
    try {
      const data = await fetchMyAlbums({ page: pagination.page, limit: pagination.limit });
      list.value = data.data;
      Object.assign(pagination, data.pagination);
    } finally {
      loading.value = false;
    }
  }

  function setPage(page) {
    pagination.page = page;
    loadAlbums();
  }

  async function addAlbum(form) {
    await createAlbum(form);
    pagination.page = 1;
    await loadAlbums();
  }

  return { list, loading, pagination, loadAlbums, setPage, addAlbum };
});

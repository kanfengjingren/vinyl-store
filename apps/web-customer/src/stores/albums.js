import { defineStore } from 'pinia';
import { ref, reactive } from 'vue';
import { fetchAlbums, fetchAlbumBySlug } from '@vinyl-store/shared';

export const useAlbumStore = defineStore('albums', () => {
  const list = ref([]);
  const current = ref(null);
  const loading = ref(false);
  const pagination = reactive({ page: 1, limit: 12, total: 0, totalPages: 0 });
  const filters = reactive({ category: '', search: '', sort: '', order: 'asc' });

  async function loadAlbums() {
    loading.value = true;
    try {
      const params = {
        page: pagination.page,
        limit: pagination.limit,
        ...filters,
      };
      // Remove empty filters
      Object.keys(params).forEach((k) => {
        if (!params[k]) delete params[k];
      });

      const data = await fetchAlbums(params);
      list.value = data.data;
      Object.assign(pagination, data.pagination);
    } finally {
      loading.value = false;
    }
  }

  function setFilter(key, value) {
    filters[key] = value;
    pagination.page = 1;
    loadAlbums();
  }

  function setPage(page) {
    pagination.page = page;
    loadAlbums();
  }

  async function loadAlbum(slug) {
    current.value = null;
    const data = await fetchAlbumBySlug(slug);
    current.value = data;
    return data;
  }

  return { list, current, loading, pagination, filters, loadAlbums, setFilter, setPage, loadAlbum };
});

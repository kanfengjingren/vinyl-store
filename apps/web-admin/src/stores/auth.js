import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { login as loginApi, getMe } from '@vinyl-store/shared';

export const useAdminAuthStore = defineStore('adminAuth', () => {
  const user = ref(JSON.parse(localStorage.getItem('admin_user') || 'null'));
  const token = ref(localStorage.getItem('admin_token') || '');

  const isLoggedIn = computed(() => !!token.value);

  async function login(credentials) {
    const data = await loginApi(credentials);
    if (data.user.role !== 'ADMIN') {
      throw new Error('此账号不是管理员账号');
    }
    token.value = data.token;
    localStorage.setItem('token', data.token);
    localStorage.setItem('admin_token', data.token);

    const profile = await getMe();
    user.value = profile;
    localStorage.setItem('admin_user', JSON.stringify(profile));
    localStorage.setItem('user', JSON.stringify(profile));
    return data;
  }

  function logout() {
    token.value = '';
    user.value = null;
    localStorage.removeItem('admin_token');
    localStorage.removeItem('admin_user');
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  }

  async function checkAuth() {
    if (!token.value) return;
    try {
      const data = await getMe();
      if (data.role !== 'ADMIN') throw new Error('不是管理员');
      user.value = data;
      localStorage.setItem('admin_user', JSON.stringify(data));
    } catch {
      logout();
    }
  }

  return { user, token, isLoggedIn, login, logout, checkAuth };
});

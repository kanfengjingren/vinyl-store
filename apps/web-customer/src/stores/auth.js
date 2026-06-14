import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { login as loginApi, register as registerApi, getMe } from '@vinyl-store/shared';

export const useAuthStore = defineStore('auth', () => {
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'));
  const token = ref(localStorage.getItem('token') || '');

  const isLoggedIn = computed(() => !!token.value);
  const isAdmin = computed(() => user.value?.role === 'ADMIN');
  const isSeller = computed(() => user.value?.role === 'SELLER');

  async function login(credentials) {
    const data = await loginApi(credentials);
    token.value = data.token;
    user.value = data.user;
    localStorage.setItem('token', data.token);
    localStorage.setItem('user', JSON.stringify(data.user));
    return data;
  }

  async function register(form) {
    const data = await registerApi(form);
    token.value = data.token;
    user.value = data.user;
    localStorage.setItem('token', data.token);
    localStorage.setItem('user', JSON.stringify(data.user));
    return data;
  }

  function logout() {
    token.value = '';
    user.value = null;
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  }

  async function checkAuth() {
    if (!token.value) return;
    try {
      const data = await getMe();
      user.value = data;
      localStorage.setItem('user', JSON.stringify(data));
    } catch {
      logout();
    }
  }

  return { user, token, isLoggedIn, isAdmin, isSeller, login, register, logout, checkAuth };
});

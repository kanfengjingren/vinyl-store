import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { login as loginApi, getMe } from '@vinyl-store/shared';

export const useSellerAuthStore = defineStore('sellerAuth', () => {
  const user = ref(JSON.parse(localStorage.getItem('seller_user') || 'null'));
  const token = ref(localStorage.getItem('seller_token') || '');

  const isLoggedIn = computed(() => !!token.value);
  const seller = computed(() => user.value?.seller ?? null);
  const storeName = computed(() => seller.value?.storeName ?? '');
  const sellerStatus = computed(() => seller.value?.status ?? null);

  async function login(credentials) {
    const data = await loginApi(credentials);
    if (data.user.role !== 'SELLER') {
      throw new Error('此账号不是卖家账号');
    }
    token.value = data.token;
    // 先写 localStorage，否则 getMe() 的 axios interceptor 拿不到 token → 401
    localStorage.setItem('token', data.token);
    localStorage.setItem('seller_token', data.token);

    // 获取完整用户信息（含 seller 入驻状态）
    const profile = await getMe();
    user.value = profile;

    if (profile.seller?.status === 'PENDING') {
      logout();
      throw new Error('卖家入驻审核中，请等待管理员审核通过');
    }
    if (profile.seller?.status === 'REJECTED') {
      logout();
      throw new Error('卖家入驻申请未通过，请联系管理员');
    }

    localStorage.setItem('seller_user', JSON.stringify(profile));
    localStorage.setItem('user', JSON.stringify(profile));
    return data;
  }

  function logout() {
    token.value = '';
    user.value = null;
    localStorage.removeItem('seller_token');
    localStorage.removeItem('seller_user');
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  }

  async function checkAuth() {
    if (!token.value) return;
    try {
      const data = await getMe();
      if (data.role !== 'SELLER') throw new Error('不是卖家');
      if (data.seller?.status !== 'APPROVED') {
        logout();
        return;
      }
      user.value = data;
      localStorage.setItem('seller_user', JSON.stringify(data));
      localStorage.setItem('user', JSON.stringify(data));
    } catch {
      logout();
    }
  }

  return { user, token, isLoggedIn, seller, storeName, sellerStatus, login, logout, checkAuth };
});

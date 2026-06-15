import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { fetchCart, addToCart, updateCartItem, removeCartItem } from '@vinyl-store/shared';
import { useAuthStore } from './auth';

const isAndroidWebView = typeof navigator !== 'undefined' && /VinylStoreAndroid/.test(navigator.userAgent || '');

export const useCartStore = defineStore('cart', () => {
  const items = ref([]);
  const total = ref(0);
  const isOpen = ref(false);
  const loading = ref(false);

  const itemCount = computed(() =>
    items.value.reduce((sum, item) => sum + item.quantity, 0),
  );

  function open() { isOpen.value = true; }
  function close() { isOpen.value = false; }
  function toggle() { isOpen.value = !isOpen.value; }

  const localCart = () => {
    try { return JSON.parse(localStorage.getItem('localCart') || '[]'); } catch { return []; }
  };
  const saveLocal = (data) => localStorage.setItem('localCart', JSON.stringify(data));

  async function load() {
    const auth = useAuthStore();
    if (!auth.isLoggedIn) {
      items.value = localCart();
      calcTotal();
      return;
    }
    loading.value = true;
    try {
      const data = await fetchCart();
      items.value = data.items;
      total.value = data.total;
    } finally {
      loading.value = false;
    }
  }

  function calcTotal() {
    total.value = items.value.reduce(
      (sum, item) => sum + (item.album?.price || 0) * item.quantity,
      0,
    );
  }

  function goToCart() {
    if (isAndroidWebView && window.AndroidBridge?.navigateTo) {
      window.AndroidBridge.navigateTo('go_cart', '{}');
    }
  }

  async function add(album) {
    const auth = useAuthStore();
    if (!auth.isLoggedIn) {
      const local = localCart();
      const idx = local.findIndex((i) => i.album.id === album.id);
      if (idx >= 0) local[idx].quantity += 1;
      else local.push({ id: Date.now(), quantity: 1, album });
      saveLocal(local);
      items.value = local;
      calcTotal();
      goToCart();
      return;
    }
    const data = await addToCart(album.id, 1);
    items.value = data.items;
    total.value = data.total;
    goToCart();
  }

  async function update(itemId, quantity) {
    const auth = useAuthStore();
    if (!auth.isLoggedIn) {
      if (quantity <= 0) {
        items.value = items.value.filter((i) => i.id !== itemId);
      } else {
        const item = items.value.find((i) => i.id === itemId);
        if (item) item.quantity = quantity;
      }
      saveLocal(items.value.map((i) => ({ id: i.id, quantity: i.quantity, album: i.album })));
      calcTotal();
      return;
    }
    const data = await updateCartItem(itemId, quantity);
    items.value = data.items;
    total.value = data.total;
  }

  async function remove(itemId) {
    return update(itemId, 0);
  }

  async function refresh() {
    const auth = useAuthStore();
    if (!auth.isLoggedIn) {
      items.value = localCart();
      calcTotal();
      return;
    }
    const data = await fetchCart();
    items.value = data.items;
    total.value = data.total;
  }

  function clear() {
    items.value = [];
    total.value = 0;
    localStorage.removeItem('localCart');
  }

  return { items, total, isOpen, loading, itemCount, open, close, toggle, load, add, update, remove, refresh, clear };
});

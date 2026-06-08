import api from './client';

export function fetchCart() {
  return api.get('/cart').then((r) => r.data);
}

export function addToCart(albumId, quantity = 1) {
  return api.post('/cart/items', { albumId, quantity }).then((r) => r.data);
}

export function updateCartItem(id, quantity) {
  return api.patch(`/cart/items/${id}`, { quantity }).then((r) => r.data);
}

export function removeCartItem(id) {
  return api.delete(`/cart/items/${id}`).then((r) => r.data);
}

import api from './client';

export function checkout(address) {
  return api.post('/orders', { shippingAddress: address }).then((r) => r.data);
}

export function fetchOrders() {
  return api.get('/orders').then((r) => r.data);
}

export function fetchOrderById(id) {
  return api.get(`/orders/${id}`).then((r) => r.data);
}

export function cancelOrder(id) {
  return api.patch(`/orders/${id}/cancel`).then((r) => r.data);
}

export function payOrder(id) {
  return api.patch(`/orders/${id}/pay`).then((r) => r.data);
}

export function fetchSellerOrders() {
  return api.get('/orders/seller').then((r) => r.data);
}

export function shipOrder(id) {
  return api.patch(`/orders/${id}/ship`).then((r) => r.data);
}

export function refundOrderItem(orderId, itemId) {
  return api.patch(`/orders/${orderId}/items/${itemId}/refund`).then((r) => r.data);
}

import api from './client';

export function fetchSellers(status) {
  return api.get('/admin/sellers', { params: status ? { status } : {} }).then((r) => r.data);
}

export function approveSeller(id) {
  return api.patch(`/admin/sellers/${id}/approve`).then((r) => r.data);
}

export function rejectSeller(id) {
  return api.patch(`/admin/sellers/${id}/reject`).then((r) => r.data);
}

// ── 数据看板 ──
export function fetchDashboardStats() {
  return api.get('/admin/stats').then((r) => r.data);
}
export function fetchSalesTrend() {
  return api.get('/admin/stats/sales-trend').then((r) => r.data);
}
export function fetchCategorySales() {
  return api.get('/admin/stats/category-sales').then((r) => r.data);
}
export function fetchTopAlbums() {
  return api.get('/admin/stats/top-albums').then((r) => r.data);
}

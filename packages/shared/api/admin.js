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

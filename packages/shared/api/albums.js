import api from './client';

export function fetchAlbums(params = {}) {
  return api.get('/albums', { params }).then((r) => r.data);
}

/** 卖家查看自己的专辑（含已下架） */
export function fetchMyAlbums(params = {}) {
  return api.get('/albums/mine', { params }).then((r) => r.data);
}

export function fetchAlbumBySlug(slug) {
  return api.get(`/albums/${slug}`).then((r) => r.data);
}

export function createAlbum(data) {
  return api.post('/albums', data).then((r) => r.data);
}

export function updateAlbum(id, data) {
  return api.patch(`/albums/${id}`, data).then((r) => r.data);
}

export function deleteAlbum(id) {
  return api.delete(`/albums/${id}`).then((r) => r.data);
}

export function createTracks(albumId, tracks) {
  return api.post(`/albums/${albumId}/tracks`, { tracks }).then((r) => r.data);
}

export function fetchCountries() {
  return api.get('/albums/countries').then((r) => r.data);
}

export function fetchSuggestions(q) {
  return api.get('/albums/suggest', { params: { q } }).then((r) => r.data);
}

export function fetchColors() {
  return api.get('/albums/colors').then((r) => r.data);
}

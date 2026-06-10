import api from './client';

export function fetchArtists() {
  return api.get('/artists').then((r) => r.data);
}

export function searchArtists(q) {
  return api.get('/artists/search', { params: { q } }).then((r) => r.data);
}

export function fetchArtistBySlug(slug) {
  return api.get(`/artists/${slug}`).then((r) => r.data);
}

export function createArtist(data) {
  return api.post('/artists', data).then((r) => r.data);
}

export function updateArtist(id, data) {
  return api.patch(`/artists/${id}`, data).then((r) => r.data);
}

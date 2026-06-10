import api from './client';

export function fetchCategories() {
  return api.get('/categories').then((r) => r.data);
}

export function createCategory(name) {
  return api.post('/categories', { name }).then((r) => r.data);
}

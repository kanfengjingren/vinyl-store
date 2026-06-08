import api from './client';

export function fetchCategories() {
  return api.get('/categories').then((r) => r.data);
}

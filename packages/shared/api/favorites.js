import api from './client';

/** 切换收藏状态，返回 { favorited: boolean } */
export function toggleFavorite(albumId) {
  return api.post('/favorites', { albumId }).then((r) => r.data);
}

/** 获取用户收藏列表 */
export function fetchFavorites() {
  return api.get('/favorites').then((r) => r.data);
}

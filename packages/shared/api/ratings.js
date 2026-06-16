import api from './client';

/** 获取专辑评分统计（含可选当前用户评分） */
export function fetchAlbumRating(albumId) {
  return api.get(`/albums/${albumId}/rating`).then((r) => r.data);
}

/** 评分或修改评分（需登录），body: { score: 1-5 } */
export function rateAlbum(albumId, score) {
  return api.post(`/albums/${albumId}/rating`, { score }).then((r) => r.data);
}

import api from './client';

/** 记录一次播放 */
export function recordPlay(trackId, albumId) {
  return api.post('/play-history', { trackId, albumId }).then((r) => r.data);
}

/** 获取播放历史 */
export function fetchPlayHistory(limit = 20) {
  return api.get('/play-history', { params: { limit } }).then((r) => r.data);
}

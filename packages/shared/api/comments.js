import api from './client';

// 获取专辑评论（分页）
export function fetchComments(albumId, params = {}) {
  return api.get(`/albums/${albumId}/comments`, { params }).then(r => r.data);
}

// 获取某条评论的全部子回复
export function fetchReplies(commentId) {
  return api.get(`/comments/${commentId}/replies`).then(r => r.data);
}

// 发表评论
export function createComment(albumId, data) {
  return api.post(`/albums/${albumId}/comments`, data).then(r => r.data);
}

// 删除评论
export function deleteComment(commentId) {
  return api.delete(`/comments/${commentId}`).then(r => r.data);
}

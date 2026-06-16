import api from './client';

/** 发送好友申请 */
export function sendFriendRequest(receiverId) {
  return api.post('/friends/request', { receiverId }).then((r) => r.data);
}

/** 接受好友申请 */
export function acceptFriendRequest(friendshipId) {
  return api.patch(`/friends/${friendshipId}/accept`).then((r) => r.data);
}

/** 拒绝好友申请 */
export function rejectFriendRequest(friendshipId) {
  return api.patch(`/friends/${friendshipId}/reject`).then((r) => r.data);
}

/** 获取好友列表 */
export function fetchFriends() {
  return api.get('/friends').then((r) => r.data);
}

/** 获取待处理的好友申请 */
export function fetchPendingRequests() {
  return api.get('/friends/pending').then((r) => r.data);
}

/** 获取与某用户的好友状态 */
export function fetchFriendshipStatus(userId) {
  return api.get(`/friends/status/${userId}`).then((r) => r.data);
}

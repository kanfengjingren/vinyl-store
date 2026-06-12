import api from './client';

export function fetchConversations() {
  return api.get('/chat/conversations').then((r) => r.data);
}

export function fetchMessages(partnerId) {
  return api.get(`/chat/messages/${partnerId}`).then((r) => r.data);
}

export function markMessagesRead(partnerId) {
  return api.patch(`/chat/read/${partnerId}`).then((r) => r.data);
}

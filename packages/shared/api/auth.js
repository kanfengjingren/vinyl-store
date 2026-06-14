import api from './client';

/**
 * 用户登录
 * @param {{ email: string, password: string }} data
 * @returns {Promise<{ user: Object, token: string }>}
 */
export function login(data) {
  return api.post('/auth/login', data).then((r) => r.data);
}

/**
 * 用户注册
 * @param {{ email: string, password: string, name?: string, role?: string, storeName?: string }} data
 * @returns {Promise<{ user: Object, token: string }>}
 */
export function register(data) {
  return api.post('/auth/register', data).then((r) => r.data);
}

/**
 * 获取当前登录用户信息（含 seller 入驻状态）
 * @returns {Promise<Object>}
 */
export function getMe() {
  return api.get('/auth/me').then((r) => r.data);
}

/**
 * 获取用户公开资料
 * @returns {Promise<Object>}
 */
export function fetchProfile() {
  return api.get('/auth/profile').then((r) => r.data);
}

/**
 * 更新用户资料
 * @param {{ defaultAddress: string }} data
 * @returns {Promise<Object>}
 */
export function updateProfile(data) {
  return api.patch('/auth/profile', data).then((r) => r.data);
}

/**
 * 修改密码
 * @param {{ oldPassword: string, newPassword: string }} data
 * @returns {Promise<{ message: string }>}
 */
export function changePassword(data) {
  return api.patch('/auth/password', data).then((r) => r.data);
}

export function forgotPassword(email) {
  return api.post('/auth/forgot-password', { email }).then((r) => r.data);
}

export function resetPassword(email, code, newPassword) {
  return api.post('/auth/reset-password', { email, code, newPassword }).then((r) => r.data);
}

/** 获取用户已购买的专辑及曲目 */
export function fetchPurchases() {
  return api.get('/users/me/purchases').then((r) => r.data);
}

/** 更新用户头像 */
export function updateAvatar(avatar) {
  return api.patch('/users/me/avatar', { avatar }).then((r) => r.data);
}

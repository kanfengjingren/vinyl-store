import api from './client';

/**
 * 用户充值（测试口，后续对接支付宝/微信）
 * @param {number} amount
 * @returns {Promise<{ balance: number }>}
 */
export function recharge(amount) {
  return api.post('/users/recharge', { amount }).then((r) => r.data);
}

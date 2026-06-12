import api from './client';

export function fetchSellerById(id) {
  return api.get(`/sellers/${id}`).then((r) => r.data);
}

export function fetchSellerSalesTrend() {
  return api.get('/sellers/stats/sales-trend').then((r) => r.data);
}

export function fetchSellerCategoryDistribution() {
  return api.get('/sellers/stats/category-distribution').then((r) => r.data);
}

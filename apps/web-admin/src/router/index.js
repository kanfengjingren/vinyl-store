import { createRouter, createWebHistory } from 'vue-router';
import { useAdminAuthStore } from '../stores/auth';

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../pages/LoginPage.vue'),
  },
  {
    path: '/',
    component: () => import('../pages/AdminLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/sellers',
      },
      {
        path: 'sellers',
        name: 'seller-review',
        component: () => import('../pages/SellerReviewPage.vue'),
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory('/admin/'),
  routes,
});

router.beforeEach((to) => {
  const auth = useAdminAuthStore();
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return '/login';
  }
  if (to.path === '/login' && auth.isLoggedIn) {
    return '/sellers';
  }
});

export default router;

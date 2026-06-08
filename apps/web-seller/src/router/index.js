import { createRouter, createWebHistory } from 'vue-router';
import { useSellerAuthStore } from '../stores/auth';

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../pages/LoginPage.vue'),
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../pages/RegisterPage.vue'),
  },
  {
    path: '/',
    component: () => import('../pages/SellerLayout.vue'),
    meta: { requiresAuth: true, role: 'SELLER' },
    children: [
      {
        path: '',
        redirect: '/albums',
      },
      {
        path: 'albums',
        name: 'album-manage',
        component: () => import('../pages/AlbumManagePage.vue'),
      },
      {
        path: 'create',
        name: 'create-album',
        component: () => import('../pages/CreateAlbumPage.vue'),
      },
      {
        path: 'orders',
        name: 'order-manage',
        component: () => import('../pages/OrderManagePage.vue'),
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to) => {
  const auth = useSellerAuthStore();
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return '/login';
  }
  if (to.path === '/login' && auth.isLoggedIn) {
    return '/albums';
  }
});

export default router;

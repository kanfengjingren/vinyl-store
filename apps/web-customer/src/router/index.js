import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('../pages/HomePage.vue'),
  },
  {
    path: '/admin',
    name: 'manage',
    component: () => import('../pages/ControllerPage.vue'),
    children: [
      {
        path: '/admin/create',
        name: 'createAlbum',
        component: () => import('../components/manage/CreateAlbum.vue'),
      },
      {
        path: '/admin/album-list',
        name: 'album-list',
        component: () => import('../components/manage/AlbumList.vue'),
      },
      {
        path: '/admin/orders',
        name: 'order-manage',
        component: () => import('../components/manage/OrderManage.vue'),
      },
    ]
  },
  {
    path: '/albums/:slug',
    name: 'album-detail',
    component: () => import('../pages/AlbumDetailPage.vue'),
  },
  {
    path: '/cart',
    name: 'cart',
    component: () => import('../pages/CartPage.vue'),
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../pages/LoginPage.vue'),
    meta: { guestOnly: true },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../pages/RegisterPage.vue'),
    meta: { guestOnly: true },
  },
  {
    path: '/orders',
    name: 'orders',
    component: () => import('../pages/OrdersPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/orders/:id',
    name: 'order-detail',
    component: () => import('../pages/OrderDetailPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/checkout',
    name: 'checkout',
    component: () => import('../pages/CheckoutPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/search',
    name: 'search',
    component: () => import('../pages/SearchPage.vue'),
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('../pages/UserProfilePage.vue'),
    meta: { requiresAuth: true },
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition;
    }
    return { top: 0 };
  },
});

router.beforeEach((to) => {
  const auth = useAuthStore();

  if (auth.isAdmin && !to.path.startsWith('/admin') && to.path !== '/login' && to.path !== '/register') {
    return '/admin/album-list';
  }
});

export default router;

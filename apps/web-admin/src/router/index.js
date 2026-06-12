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
        redirect: '/dashboard',
      },
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('../pages/DashboardPage.vue'),
      },
      {
        path: 'sellers',
        name: 'seller-review',
        component: () => import('../pages/SellerReviewPage.vue'),
      },
      {
        path: 'artists',
        name: 'artist-manage',
        component: () => import('../pages/ArtistManagePage.vue'),
      },
      {
        path: ':pathMatch(.*)*',
        name: 'not-found',
        component: () => import('../pages/NotFoundPage.vue'),
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('../pages/NotFoundPage.vue'),
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

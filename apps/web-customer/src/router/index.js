import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('../pages/HomePage.vue'),
  },
  // {
  //   path: '/admin',
  //   name: 'manage',
  //   component: () => import('../pages/ControllerPage.vue'),
  //   children: [
  //     {
  //       path: '/admin/create',
  //       name: 'createAlbum',
  //       component: () => import('../components/manage/CreateAlbum.vue'),
  //     },
  //     {
  //       path: '/admin/album-list',
  //       name: 'album-list',
  //       component: () => import('../components/manage/AlbumList.vue'),
  //     },
  //     {pnpm 
  //       path: '/admin/orders',
  //       name: 'order-manage',
  //       component: () => import('../components/manage/OrderManage.vue'),
  //     },
  //   ]
  // },
  {
    path: '/albums/:slug',
    name: 'album-detail',
    component: () => import('../pages/AlbumDetailPage.vue'),
  },
  {
    path: '/artists/:slug',
    name: 'artist',
    component: () => import('../pages/ArtistPage.vue'),
  },
  {
    path: '/seller/:id',
    name: 'seller',
    component: () => import('../pages/SellerPage.vue'),
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
    path: '/catalog',
    name: 'catalog',
    component: () => import('../pages/CatalogPage.vue'),
  },
  {
    path: '/new-arrivals',
    name: 'new-arrivals',
    component: () => import('../pages/NewArrivalsPage.vue'),
  },
  {
    path: '/search',
    name: 'search',
    component: () => import('../pages/SearchPage.vue'),
  },
  {
    path: '/user/:id',
    name: 'user-profile',
    component: () => import('../pages/PublicProfilePage.vue'),
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('../pages/UserProfilePage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/messages',
    name: 'messages',
    component: () => import('../pages/MessagesPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/about',
    name: 'about',
    component: () => import('../pages/AboutPage.vue'),
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('../pages/NotFoundPage.vue'),
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition;
    }
    // 刷新页面后恢复滚动位置
    const y = sessionStorage.getItem('__scrollY');
    if (y && !from.name) {
      sessionStorage.removeItem('__scrollY');
      return { top: Number(y) };
    }
  },
});



//全局守卫
router.beforeEach((to) => {
  const auth = useAuthStore();

  // admin 用户只能访问 /admin 下的页面
  if (auth.isAdmin && !to.path.startsWith('/admin') && to.path !== '/login' && to.path !== '/register') {
    return '/admin/album-list';
  }

  // 需要登录的页面，未登录跳转登录页
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return '/login';
  }
});

export default router;

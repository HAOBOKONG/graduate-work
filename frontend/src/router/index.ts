import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../auth/token'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/admin', component: () => import('../views/admin/AdminLayout.vue'), meta: { requiresAuth: true, requiresAdmin: true }, children: [
      { path: '', component: () => import('../views/admin/AdminHomeView.vue') },
      { path: 'review', component: () => import('../views/admin/AdminReviewView.vue') },
      { path: 'products', component: () => import('../views/admin/AdminProductsView.vue') },
      { path: 'users', component: () => import('../views/admin/AdminUsersView.vue') },
    ] },
    { path: '/', component: () => import('../views/HomeView.vue') },
    { path: '/products/new', component: () => import('../views/ProductPublishView.vue'), meta: { requiresAuth: true } },
    { path: '/products/my', component: () => import('../views/MyProductsView.vue'), meta: { requiresAuth: true } },
    { path: '/favorites', component: () => import('../views/MyFavoritesView.vue'), meta: { requiresAuth: true } },
    { path: '/products/:id/edit', component: () => import('../views/ProductPublishView.vue'), meta: { requiresAuth: true } },
    { path: '/products/:id', component: () => import('../views/ProductDetailView.vue') },
    { path: '/orders/my', component: () => import('../views/MyOrdersView.vue'), meta: { requiresAuth: true } },
    { path: '/orders/:id', component: () => import('../views/OrderDetailView.vue'), meta: { requiresAuth: true } },
    { path: '/login', component: () => import('../views/LoginView.vue') },
    { path: '/register', component: () => import('../views/RegisterView.vue') },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach(async (to) => {
  if (to.meta.requiresAuth && !getToken()) return '/login'
  if (to.meta.requiresAdmin) {
    try {
      const { currentUser } = await import('../api/auth')
      if ((await currentUser()).role !== 'ADMIN') return '/'
    } catch { return '/login' }
  }
  if ((to.path === '/login' || to.path === '/register') && getToken()) return '/'
})

export default router

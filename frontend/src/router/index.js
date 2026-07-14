import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/', component: () => import('../views/portal/HomeView.vue') },
  { path: '/article/:id', component: () => import('../views/portal/ArticleDetail.vue') },
  { path: '/create', redirect: '/user' },
  { path: '/user', component: () => import('../views/portal/UserCenter.vue'), meta: { requiresAuth: true, requiresUser: true } },
  { path: '/login', component: () => import('../views/LoginView.vue'), meta: { loginType: 'USER' } },
  { path: '/admin/login', component: () => import('../views/LoginView.vue'), meta: { loginType: 'ADMIN' } },
  {
    path: '/admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', component: () => import('../views/admin/DashboardView.vue') },
      { path: 'settings', component: () => import('../views/admin/SiteSetting.vue') },
      { path: 'mail-settings', component: () => import('../views/admin/MailSetting.vue') },
      { path: 'articles', component: () => import('../views/admin/ArticleManage.vue') },
      { path: 'taxonomies', component: () => import('../views/admin/TaxonomyManage.vue') },
      { path: 'images', component: () => import('../views/admin/ImageManage.vue') },
      { path: 'comments', component: () => import('../views/admin/CommentManage.vue') },
      { path: 'users', component: () => import('../views/admin/UserManage.vue') },
      { path: 'logs', component: () => import('../views/admin/OperationLog.vue') }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.path === '/login' && auth.isUserLogin) {
    return '/user'
  }
  if (to.path === '/admin/login' && auth.isAdminLogin) {
    return '/admin/dashboard'
  }
  if (to.meta.requiresAdmin && !auth.isAdminLogin) {
    return { path: '/admin/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresUser && !auth.isUserLogin) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

export default router

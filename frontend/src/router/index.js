import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/', component: () => import('../views/portal/HomeView.vue') },
  { path: '/article/:id', component: () => import('../views/portal/ArticleDetail.vue') },
  { path: '/create', redirect: '/user' },
  { path: '/user', component: () => import('../views/portal/UserCenter.vue') },
  { path: '/login', component: () => import('../views/LoginView.vue') },
  {
    path: '/admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', component: () => import('../views/admin/DashboardView.vue') },
      { path: 'articles', component: () => import('../views/admin/ArticleManage.vue') },
      { path: 'taxonomies', component: () => import('../views/admin/TaxonomyManage.vue') },
      { path: 'images', component: () => import('../views/admin/ImageManage.vue') },
      { path: 'comments', component: () => import('../views/admin/CommentManage.vue') },
      { path: 'users', component: () => import('../views/admin/UserManage.vue') }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.path.startsWith('/admin') && !auth.isAdmin) {
    return '/login?redirect=/admin/dashboard'
  }
  if (to.path === '/user' && !auth.isLogin) {
    return '/login?redirect=/user'
  }
})

export default router

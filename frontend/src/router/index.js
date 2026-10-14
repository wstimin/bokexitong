import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useSiteStore } from '../stores/site'
import { installApi } from '../api/blog'

const routes = [
  { path: '/install', component: () => import('../views/InstallView.vue') },
  { path: '/', component: () => import('../views/portal/HomeView.vue') },
  { path: '/archive', component: () => import('../views/portal/ArchiveView.vue') },
  { path: '/about', component: () => import('../views/portal/AboutView.vue') },
  { path: '/article/:id', component: () => import('../views/portal/ArticleDetail.vue') },
  { path: '/create', redirect: '/user' },
  { path: '/user', component: () => import('../views/portal/UserCenter.vue'), meta: { requiresAuth: true, requiresUser: true } },
  { path: '/login', component: () => import('../views/LoginView.vue'), meta: { loginType: 'USER' } },
  {
    path: '/admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', component: () => import('../views/admin/DashboardView.vue') },
      { path: 'settings', component: () => import('../views/admin/SiteSetting.vue') },
      { path: 'mail-settings', component: () => import('../views/admin/MailSetting.vue') },
      { path: 'system-update', component: () => import('../views/admin/SystemUpdate.vue') },
      { path: 'articles', component: () => import('../views/admin/ArticleManage.vue') },
      { path: 'taxonomies', component: () => import('../views/admin/TaxonomyManage.vue') },
      { path: 'images', component: () => import('../views/admin/ImageManage.vue') },
      { path: 'comments', component: () => import('../views/admin/CommentManage.vue') },
      { path: 'users', component: () => import('../views/admin/UserManage.vue') },
      { path: 'logs', component: () => import('../views/admin/OperationLog.vue') }
    ]
  },
  { path: '/:pathMatch(.*)*', component: () => import('../views/LoginView.vue'), meta: { dynamicAdminLogin: true, loginType: 'ADMIN' } }
]

const router = createRouter({ history: createWebHistory(), routes })

let installState
let installStatePromise

const resolveInstallState = async () => {
  if (typeof installState === 'boolean') return installState
  if (!installStatePromise) {
    installStatePromise = installApi.status()
      .then((res) => {
        installState = Boolean(res.data?.installed)
        return installState
      })
      .finally(() => { installStatePromise = null })
  }
  return installStatePromise
}

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  const site = useSiteStore()

  // The install form must render even when the initial datasource is unavailable.
  // The install view performs its status check in the background.
  if (to.path === '/install') {
    return undefined
  }

  try {
    const installed = await resolveInstallState()
    if (!installed) {
      return '/install'
    }
  } catch (error) {
    console.error(error)
    return '/install'
  }

  // Login entry checks must always use the server value. This prevents an old
  // custom path cached in another tab or browser session from remaining usable.
  const siteLoaded = await site.loadSite(Boolean(to.meta.dynamicAdminLogin))
  if (to.meta.dynamicAdminLogin && siteLoaded === false) {
    return '/'
  }
  const adminLoginPath = site.adminLoginPath || '/admin/login'
  if (to.meta.dynamicAdminLogin) {
    if (to.path === adminLoginPath) {
      return auth.isAdminLogin ? '/admin/dashboard' : undefined
    }
    return '/'
  }
  if (to.path === '/login' && auth.isUserLogin) {
    return '/user'
  }
  if (to.path === adminLoginPath && auth.isAdminLogin) {
    return '/admin/dashboard'
  }
  if (to.meta.requiresAdmin && !auth.isAdminLogin) {
    return { path: adminLoginPath, query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresUser && !auth.isUserLogin) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

export default router

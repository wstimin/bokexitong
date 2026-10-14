import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useSiteStore } from '../stores/site'
import { installApi } from '../api/blog'
import { adminBasePathFromLogin, adminPagePath, isAdminPagePath } from '../utils/adminPath'

const adminAreaMeta = { requiresAuth: true, requiresAdmin: true, dynamicAdminArea: true }
const adminPageRoute = (page, component) => ({
  path: `/:adminBase(.*)/${page}`,
  component: () => import('../views/admin/AdminLayout.vue'),
  meta: { ...adminAreaMeta, adminPage: page },
  children: [
    { path: '', component, meta: { adminPage: page } }
  ]
})

const routes = [
  { path: '/install', component: () => import('../views/InstallView.vue') },
  { path: '/', component: () => import('../views/portal/HomeView.vue') },
  { path: '/archive', component: () => import('../views/portal/ArchiveView.vue') },
  { path: '/about', component: () => import('../views/portal/AboutView.vue') },
  { path: '/article/:id', component: () => import('../views/portal/ArticleDetail.vue') },
  { path: '/create', redirect: '/user' },
  { path: '/user', component: () => import('../views/portal/UserCenter.vue'), meta: { requiresAuth: true, requiresUser: true } },
  { path: '/login', component: () => import('../views/LoginView.vue'), meta: { loginType: 'USER' } },
  { path: '/:adminLoginBase(.*)/login', component: () => import('../views/LoginView.vue'), meta: { dynamicAdminLogin: true, loginType: 'ADMIN' } },
  adminPageRoute('dashboard', () => import('../views/admin/DashboardView.vue')),
  adminPageRoute('settings', () => import('../views/admin/SiteSetting.vue')),
  adminPageRoute('mail-settings', () => import('../views/admin/MailSetting.vue')),
  adminPageRoute('system-update', () => import('../views/admin/SystemUpdate.vue')),
  adminPageRoute('articles', () => import('../views/admin/ArticleManage.vue')),
  adminPageRoute('taxonomies', () => import('../views/admin/TaxonomyManage.vue')),
  adminPageRoute('images', () => import('../views/admin/ImageManage.vue')),
  adminPageRoute('comments', () => import('../views/admin/CommentManage.vue')),
  adminPageRoute('users', () => import('../views/admin/UserManage.vue')),
  adminPageRoute('logs', () => import('../views/admin/OperationLog.vue')),
  {
    path: '/:adminBase(.*)',
    redirect: (to) => `/${to.params.adminBase}/dashboard`,
    meta: adminAreaMeta
  }
]

const router = createRouter({ history: createWebHistory(), routes })

const INSTALL_CACHE_KEY = 'blog_installation_complete'
const cachedInstallState = typeof localStorage !== 'undefined'
  ? localStorage.getItem(INSTALL_CACHE_KEY)
  : null
let installState = cachedInstallState === 'true' ? true : null
let installStatePromise

const resolveInstallState = async () => {
  // Only a confirmed installed state can skip the server check. A missing or
  // cached false value must be revalidated so a stale cache cannot trap an
  // already-installed deployment on the installation page.
  if (installState === true) return true
  if (!installStatePromise) {
    installStatePromise = installApi.status()
      .then((res) => {
        installState = Boolean(res.data?.installed)
        if (typeof localStorage !== 'undefined') {
          localStorage.setItem(INSTALL_CACHE_KEY, String(installState))
        }
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
  const checksAdminRoute = Boolean(to.meta.dynamicAdminLogin || to.meta.dynamicAdminArea)
  const adminLoginPath = site.adminLoginPath || '/admin/login'
  const cachedAdminPathMatches = isAdminPagePath(to.path, adminLoginPath)
  let siteLoaded = true
  if (!checksAdminRoute) {
    if (!site.loaded) void site.loadSite()
  } else if (site.loaded && cachedAdminPathMatches) {
    void site.loadSite(true)
  } else {
    siteLoaded = await site.loadSite(true)
  }
  if (checksAdminRoute && siteLoaded === false) return '/'
  const adminBasePath = adminBasePathFromLogin(adminLoginPath)
  if (to.meta.dynamicAdminLogin) {
    if (to.path === adminLoginPath) {
      return auth.isAdminLogin ? adminPagePath(adminLoginPath, 'dashboard') : undefined
    }
    return '/'
  }
  if (to.path === '/login' && auth.isUserLogin) {
    return '/user'
  }
  if (to.meta.requiresAdmin) {
    const requestedBasePath = `/${String(to.params.adminBase || '').replace(/^\/+|\/+$/g, '')}`
    if (requestedBasePath !== adminBasePath) return '/'
    if (!auth.isAdminLogin) {
      return { path: adminLoginPath, query: { redirect: to.fullPath } }
    }
  }
  if (to.meta.requiresUser && !auth.isUserLogin) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

export default router

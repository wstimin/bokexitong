import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'
import App from './App.vue'
import router from './router'
import { setUnauthorizedHandler } from './api/http'
import { useAuthStore } from './stores/auth'
import { SITE_SETTINGS_SYNC_KEY, useSiteStore } from './stores/site'
import { isAdminPagePath } from './utils/adminPath'
import { activateThemeScope, initializeTheme } from './utils/theme'
import './assets/styles.css'
import './assets/theme.css'

const pinia = createPinia()
const app = createApp(App)
const cachedAdminLoginPath = localStorage.getItem('blog_admin_login_path') || '/admin/login'
const initialRoute = router.resolve(window.location.pathname)
const initialThemeScope = (
  initialRoute.meta.requiresAdmin
  || initialRoute.meta.loginType === 'ADMIN'
  || isAdminPagePath(window.location.pathname, cachedAdminLoginPath)
)
  ? 'admin'
  : 'portal'

initializeTheme(initialThemeScope)
router.afterEach((to) => {
  const scope = to.meta.requiresAdmin || to.meta.loginType === 'ADMIN' ? 'admin' : 'portal'
  activateThemeScope(scope)
})

app.use(pinia).use(router)

window.addEventListener('storage', (event) => {
  if (event.key === SITE_SETTINGS_SYNC_KEY && event.newValue) {
    useSiteStore(pinia).loadSite(true)
  }
})

setUnauthorizedHandler(() => {
  const auth = useAuthStore()
  const site = useSiteStore()
  const current = router.currentRoute.value
  const adminLoginPath = site.adminLoginPath || '/admin/login'
  const isAdminArea = current.meta.requiresAdmin || current.meta.loginType === 'ADMIN' || isAdminPagePath(current.path, adminLoginPath)
  auth.logout(isAdminArea ? 'admin' : 'user')
  const loginPath = isAdminArea ? adminLoginPath : '/login'
  if (current.path !== '/login' && current.path !== adminLoginPath) {
    router.push({ path: loginPath, query: { redirect: current.fullPath } })
  }
})

app.mount('#app')

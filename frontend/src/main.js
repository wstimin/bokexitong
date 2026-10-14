import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'
import App from './App.vue'
import router from './router'
import { setUnauthorizedHandler } from './api/http'
import { useAuthStore } from './stores/auth'
import { useSiteStore } from './stores/site'
import { isAdminPagePath } from './utils/adminPath'
import { initializeTheme } from './utils/theme'
import './assets/styles.css'
import './assets/theme.css'

const pinia = createPinia()
const app = createApp(App)

initializeTheme()

app.use(pinia).use(router)

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

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import 'highlight.js/styles/atom-one-light.css'
import App from './App.vue'
import router from './router'
import { setUnauthorizedHandler } from './api/http'
import { useAuthStore } from './stores/auth'
import { useSiteStore } from './stores/site'
import './assets/styles.css'

const pinia = createPinia()
const app = createApp(App)

app.use(pinia).use(router).use(ElementPlus)

setUnauthorizedHandler(() => {
  const auth = useAuthStore()
  const site = useSiteStore()
  const current = router.currentRoute.value
  const isAdminArea = current.meta.requiresAdmin || current.meta.loginType === 'ADMIN' || current.path.startsWith('/admin')
  auth.logout(isAdminArea ? 'admin' : 'user')
  const adminLoginPath = site.adminLoginPath || '/admin/login'
  const loginPath = isAdminArea ? adminLoginPath : '/login'
  if (current.path !== '/login' && current.path !== adminLoginPath) {
    router.push({ path: loginPath, query: { redirect: current.fullPath } })
  }
})

app.mount('#app')

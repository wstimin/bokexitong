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
import './assets/styles.css'

const pinia = createPinia()
const app = createApp(App)

app.use(pinia).use(router).use(ElementPlus)

setUnauthorizedHandler(() => {
  const auth = useAuthStore()
  const current = router.currentRoute.value
  auth.logout()
  if (current.path !== '/login') {
    router.push({ path: '/login', query: { redirect: current.fullPath } })
  }
})

app.mount('#app')

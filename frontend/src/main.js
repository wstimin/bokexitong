import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import 'highlight.js/styles/atom-one-light.css'
import App from './App.vue'
import router from './router'
import './assets/styles.css'

createApp(App).use(createPinia()).use(router).use(ElementPlus).mount('#app')

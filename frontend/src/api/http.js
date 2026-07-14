import axios from 'axios'
import { ElMessage } from 'element-plus'

let unauthorizedHandler = null
let unauthorizedNotified = false

export const setUnauthorizedHandler = (handler) => {
  unauthorizedHandler = handler
}

const http = axios.create({
  baseURL: '/api',
  timeout: 12000
})

http.interceptors.request.use((config) => {
  const currentPath = window.location.pathname || ''
  const adminToken = localStorage.getItem('blog_admin_token') || ''
  const userToken = localStorage.getItem('blog_user_token') || localStorage.getItem('blog_token') || ''
  const token = currentPath.startsWith('/admin') ? adminToken : userToken
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message || '请求失败'

    if (status === 401) {
      if (!unauthorizedNotified) {
        unauthorizedNotified = true
        ElMessage.warning(message || '登录已过期，请重新登录')
        window.setTimeout(() => {
          unauthorizedNotified = false
        }, 1500)
      }
      unauthorizedHandler?.()
      return Promise.reject(error)
    }

    if (status === 403) {
      ElMessage.warning(message || '没有访问权限')
      return Promise.reject(error)
    }

    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default http

import axios from 'axios'
import { ElMessage } from 'element-plus'

let unauthorizedHandler = null
let unauthorizedNotified = false

const isAdminPath = (path = '') => {
  if (!path) return false
  if (path.startsWith('/admin')) return true
  if (path === '/login' || path === '/') return false
  const adminLoginPath = localStorage.getItem('blog_admin_login_path') || '/admin/login'
  return path === adminLoginPath
}

export const setUnauthorizedHandler = (handler) => {
  unauthorizedHandler = handler
}

const http = axios.create({
  baseURL: '/api',
  timeout: 12000
})

const responseMessage = (data) => {
  if (!data) return ''
  if (typeof data === 'string') {
    const value = data.trim()
    return /<\s*!doctype|<\s*html/i.test(value) ? '' : value
  }
  return data.message || data.msg || data.error || ''
}

http.interceptors.request.use((config) => {
  const currentPath = window.location.pathname || ''
  const adminToken = localStorage.getItem('blog_admin_token') || ''
  const userToken = localStorage.getItem('blog_user_token') || localStorage.getItem('blog_token') || ''
  const token = isAdminPath(currentPath) ? adminToken : userToken
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const status = error.response?.status
    const silentError = error.config?.silentError === true
    const silentRestartError = error.config?.silentRestartError === true
      && (status === 502 || status === 503 || status === 504 || error.code === 'ECONNABORTED' || !error.response)
    const backendMessage = responseMessage(error.response?.data)
    const resolvedMessage = backendMessage
      || (status === 502 || status === 503 || status === 504
        ? '后端服务暂时不可用，请检查 1Panel 中的 Java 服务是否正在运行，以及反向代理端口是否为 18080'
        : error.code === 'ECONNABORTED'
          ? '请求超时，请检查 Java 服务状态和数据库连接'
          : !error.response
            ? '无法连接后端服务，请检查反向代理和 Java 服务'
            : '请求失败')
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
      if (!silentError) {
        ElMessage.warning(message || '没有访问权限')
      }
      return Promise.reject(error)
    }

    if (!silentError && !silentRestartError) {
      ElMessage.error(resolvedMessage)
    }
    return Promise.reject(error)
  }
)

export default http

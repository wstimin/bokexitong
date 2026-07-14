import { defineStore } from 'pinia'
import { authApi } from '../api/blog'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    userToken: localStorage.getItem('blog_user_token') || localStorage.getItem('blog_token') || '',
    user: JSON.parse(localStorage.getItem('blog_user_profile') || localStorage.getItem('blog_user') || 'null'),
    adminToken: localStorage.getItem('blog_admin_token') || '',
    admin: JSON.parse(localStorage.getItem('blog_admin_profile') || 'null'),
    adminPasswordChangeRequired: localStorage.getItem('blog_admin_password_change_required') === 'true'
  }),
  getters: {
    isLogin: (state) => Boolean(state.userToken || state.adminToken),
    isUserLogin: (state) => Boolean(state.userToken),
    isAdminLogin: (state) => Boolean(state.adminToken),
    isAdmin: (state) => Boolean(state.adminToken),
    currentUser: (state) => state.user,
    currentAdmin: (state) => state.admin,
    passwordChangeRequired: (state) => state.adminPasswordChangeRequired
  },
  actions: {
    async login(form) {
      const res = await authApi.login(form)
      const loginType = form.loginType === 'ADMIN' || res.data.user?.role === 'ADMIN' ? 'ADMIN' : 'USER'
      if (loginType === 'ADMIN') {
        this.adminToken = res.data.token
        this.admin = res.data.user
        this.adminPasswordChangeRequired = Boolean(res.data.passwordChangeRequired)
        localStorage.setItem('blog_admin_token', this.adminToken)
        localStorage.setItem('blog_admin_profile', JSON.stringify(this.admin))
        localStorage.setItem('blog_admin_password_change_required', String(this.adminPasswordChangeRequired))
      } else {
        this.userToken = res.data.token
        this.user = res.data.user
        localStorage.setItem('blog_user_token', this.userToken)
        localStorage.setItem('blog_user_profile', JSON.stringify(this.user))
        localStorage.removeItem('blog_token')
        localStorage.removeItem('blog_user')
        localStorage.removeItem('blog_password_change_required')
      }
    },
    setUser(user) {
      this.user = user
      localStorage.setItem('blog_user_profile', JSON.stringify(user))
    },
    logout(scope = 'all') {
      if (scope === 'all' || scope === 'user') {
        this.userToken = ''
        this.user = null
        localStorage.removeItem('blog_user_token')
        localStorage.removeItem('blog_user_profile')
        localStorage.removeItem('blog_token')
        localStorage.removeItem('blog_user')
        localStorage.removeItem('blog_password_change_required')
      }
      if (scope === 'all' || scope === 'admin') {
        this.adminToken = ''
        this.admin = null
        this.adminPasswordChangeRequired = false
        localStorage.removeItem('blog_admin_token')
        localStorage.removeItem('blog_admin_profile')
        localStorage.removeItem('blog_admin_password_change_required')
      }
    },
    clearPasswordRequired() {
      this.adminPasswordChangeRequired = false
      localStorage.setItem('blog_admin_password_change_required', 'false')
    }
  }
})

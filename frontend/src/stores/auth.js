import { defineStore } from 'pinia'
import { authApi } from '../api/blog'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('blog_token') || '',
    user: JSON.parse(localStorage.getItem('blog_user') || 'null'),
    passwordChangeRequired: localStorage.getItem('blog_password_change_required') === 'true'
  }),
  getters: {
    isLogin: (state) => Boolean(state.token),
    isAdmin: (state) => state.user?.role === 'ADMIN'
  },
  actions: {
    async login(form) {
      const res = await authApi.login(form)
      this.token = res.data.token
      this.user = res.data.user
      this.passwordChangeRequired = Boolean(res.data.passwordChangeRequired)
      localStorage.setItem('blog_token', this.token)
      localStorage.setItem('blog_user', JSON.stringify(this.user))
      localStorage.setItem('blog_password_change_required', String(this.passwordChangeRequired))
    },
    setUser(user) {
      this.user = user
      localStorage.setItem('blog_user', JSON.stringify(user))
    },
    logout() {
      this.token = ''
      this.user = null
      this.passwordChangeRequired = false
      localStorage.removeItem('blog_token')
      localStorage.removeItem('blog_user')
      localStorage.removeItem('blog_password_change_required')
    },
    clearPasswordRequired() {
      this.passwordChangeRequired = false
      localStorage.setItem('blog_password_change_required', 'false')
    }
  }
})

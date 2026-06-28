import { defineStore } from 'pinia'
import { authApi } from '../api/blog'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('blog_token') || '',
    user: JSON.parse(localStorage.getItem('blog_user') || 'null')
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
      localStorage.setItem('blog_token', this.token)
      localStorage.setItem('blog_user', JSON.stringify(this.user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('blog_token')
      localStorage.removeItem('blog_user')
    }
  }
})

<template>
  <header class="topbar">
    <div class="shell nav">
      <RouterLink class="brand" to="/"><span class="brand-mark"></span><span>星绘 Blog</span></RouterLink>
      <nav class="nav-links">
        <RouterLink class="nav-link" to="/">首页</RouterLink>
        <RouterLink v-if="auth.isLogin" class="nav-link" to="/user">用户中心</RouterLink>
        <RouterLink v-if="!auth.isLogin" class="nav-link" to="/login">登录</RouterLink>
        <button v-else class="btn-ghost" @click="logout">退出</button>
      </nav>
    </div>
  </header>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const logout = () => {
  auth.logout()
  router.push('/')
}
</script>

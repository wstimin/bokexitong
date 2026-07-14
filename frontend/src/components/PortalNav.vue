<template>
  <header class="topbar">
    <div class="shell nav">
      <RouterLink class="brand" to="/">
        <img v-if="site.logoSrc" class="brand-logo" :src="site.logoSrc" :alt="site.name" />
        <span v-else class="brand-mark"></span>
        <span>{{ site.name }}</span>
      </RouterLink>
      <nav class="nav-links">
        <RouterLink class="nav-link" to="/">首页</RouterLink>
        <RouterLink v-if="auth.isUserLogin" class="nav-link" to="/user">用户中心</RouterLink>
        <RouterLink v-if="auth.isAdminLogin" class="nav-link" to="/admin/dashboard">后台管理</RouterLink>
        <RouterLink v-if="!auth.isUserLogin" class="nav-link" to="/login">登录</RouterLink>
        <RouterLink v-if="!auth.isUserLogin" class="nav-link" to="/login?mode=register">注册</RouterLink>
        <button v-else class="btn-ghost" @click="logout">退出用户</button>
      </nav>
    </div>
  </header>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useSiteStore } from '../stores/site'

const auth = useAuthStore()
const site = useSiteStore()
const router = useRouter()

onMounted(() => site.loadSite())

const logout = () => {
  auth.logout('user')
  router.push('/')
}
</script>

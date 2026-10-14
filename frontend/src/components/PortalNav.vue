<template>
  <header class="topbar">
    <div class="shell nav">
      <RouterLink class="brand" to="/">
        <img v-if="site.logoSrc" class="brand-logo" :src="site.logoSrc" :alt="site.name" />
        <span v-else class="brand-mark">B</span>
        <span class="brand-copy"><strong>{{ site.name }}</strong><small>Journal</small></span>
      </RouterLink>

      <button class="mobile-menu-button" type="button" :aria-expanded="menuOpen" aria-label="打开导航菜单" @click="menuOpen = !menuOpen">
        <span></span><span></span><span></span>
      </button>

      <nav :class="['nav-links', { 'is-open': menuOpen }]" @click="menuOpen = false">
        <RouterLink class="nav-link" to="/">首页</RouterLink>
        <RouterLink class="nav-link" to="/archive?view=categories">分类</RouterLink>
        <RouterLink class="nav-link" to="/archive">归档</RouterLink>
        <RouterLink class="nav-link" to="/about">关于</RouterLink>
        <button class="nav-search" type="button" aria-label="搜索文章" @click.stop="searchOpen = true">
          <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></svg>
          <span>搜索</span>
        </button>
        <ThemeToggle />
        <RouterLink v-if="auth.isUserLogin" class="nav-link" to="/user">用户中心</RouterLink>
        <RouterLink v-if="!auth.isUserLogin" class="nav-link" to="/login">登录</RouterLink>
        <RouterLink v-if="!auth.isUserLogin" class="nav-signup" to="/login?mode=register">开始创作</RouterLink>
        <button v-else class="btn-ghost" type="button" @click="logout">退出</button>
      </nav>
    </div>
  </header>
  <SearchOverlay v-model="searchOpen" />
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import SearchOverlay from './SearchOverlay.vue'
import ThemeToggle from './ThemeToggle.vue'
import { useAuthStore } from '../stores/auth'
import { useSiteStore } from '../stores/site'

const auth = useAuthStore()
const site = useSiteStore()
const router = useRouter()
const menuOpen = ref(false)
const searchOpen = ref(false)

onMounted(() => site.loadSite())

const logout = () => {
  auth.logout('user')
  router.push('/')
}
</script>

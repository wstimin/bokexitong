<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <RouterLink class="brand" to="/"><span class="brand-mark"></span><span>星绘后台</span></RouterLink>
      <nav class="admin-menu">
        <RouterLink to="/admin/dashboard">仪表盘</RouterLink>
        <RouterLink to="/admin/articles">文章管理</RouterLink>
        <RouterLink to="/admin/taxonomies">分类标签</RouterLink>
        <RouterLink to="/admin/images">图片链接</RouterLink>
        <RouterLink to="/admin/comments">评论审核</RouterLink>
        <RouterLink to="/admin/users">用户权限</RouterLink>
      </nav>
    </aside>
    <main class="admin-main">
      <div class="admin-header">
        <h1>{{ title }}</h1>
        <RouterLink class="btn-ghost" to="/">返回前台</RouterLink>
      </div>
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const names = {
  '/admin/dashboard': '仪表盘',
  '/admin/articles': '文章管理',
  '/admin/taxonomies': '分类标签',
  '/admin/images': '图片链接管理',
  '/admin/comments': '评论审核',
  '/admin/users': '用户权限'
}
const title = computed(() => names[route.path] || '后台管理')
</script>

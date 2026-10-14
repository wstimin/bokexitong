<template>
  <footer class="portal-footer">
    <div class="shell portal-footer-inner">
      <div class="footer-brand">
        <RouterLink class="brand" to="/">
          <img v-if="site.logoSrc" class="brand-logo" :src="site.logoSrc" :alt="site.name" />
          <span v-else class="brand-mark">B</span>
          <span>{{ site.name }}</span>
        </RouterLink>
        <p>{{ site.footerText || '记录思想、技术与生活，让值得留下的内容被看见。' }}</p>
      </div>
      <div class="footer-links">
        <strong>探索</strong>
        <RouterLink to="/">首页</RouterLink>
        <RouterLink to="/archive">文章归档</RouterLink>
        <RouterLink to="/about">关于我们</RouterLink>
      </div>
      <div class="footer-links">
        <strong>连接</strong>
        <span v-if="safeContactHtml" class="portal-contact-html" v-html="safeContactHtml"></span>
        <RouterLink v-else to="/about#contact">联系方式</RouterLink>
        <a href="/rss.xml" rel="alternate">RSS Feed</a>
      </div>
      <div class="footer-bottom">
        <span>© {{ year }} {{ site.name }}</span>
        <span v-if="site.icpBeian">{{ site.icpBeian }}</span>
        <span class="version-badge">v{{ version }}</span>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { portalApi } from '../api/blog'
import { useSiteStore } from '../stores/site'
import { sanitizeContactHtml } from '../utils/sanitize'

const site = useSiteStore()
const version = ref('1.1.17')
const year = new Date().getFullYear()

const safeContactHtml = computed(() => sanitizeContactHtml(site.contactHtml).trim())
onMounted(async () => {
  site.loadSite()
  try {
    const res = await portalApi.version()
    version.value = res.data?.version || version.value
  } catch (error) {
    console.error(error)
  }
})
</script>

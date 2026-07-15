<template>
  <footer v-if="visible" class="portal-footer">
    <div class="shell portal-footer-inner">
      <div v-if="safeContactHtml" class="portal-contact-html" v-html="safeContactHtml"></div>
      <div class="portal-footer-meta">
        <span v-if="site.footerText">{{ site.footerText }}</span>
        <span v-if="site.icpBeian">{{ site.icpBeian }}</span>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useSiteStore } from '../stores/site'

const site = useSiteStore()

const sanitizeHtml = (html = '') => String(html)
  .replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, '')
  .replace(/\son\w+="[^"]*"/gi, '')
  .replace(/\son\w+='[^']*'/gi, '')
  .replace(/javascript:/gi, '')

const safeContactHtml = computed(() => sanitizeHtml(site.contactHtml).trim())
const visible = computed(() => Boolean(site.footerText || site.icpBeian || safeContactHtml.value))

onMounted(() => site.loadSite())
</script>

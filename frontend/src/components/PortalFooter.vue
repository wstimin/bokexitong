<template>
  <footer v-if="visible" class="portal-footer">
    <div class="shell portal-footer-inner">
      <span v-if="site.footerText">{{ site.footerText }}</span>
      <span v-if="site.icpBeian">{{ site.icpBeian }}</span>
    </div>
  </footer>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useSiteStore } from '../stores/site'

const site = useSiteStore()
const visible = computed(() => Boolean(site.footerText || site.icpBeian))

onMounted(() => site.loadSite())
</script>

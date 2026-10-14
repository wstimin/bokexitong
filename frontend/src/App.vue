<template>
  <router-view />
  <BackToTop />
  <WelcomeGate />
</template>

<script setup>
import BackToTop from './components/BackToTop.vue'
import WelcomeGate from './components/WelcomeGate.vue'
</script>

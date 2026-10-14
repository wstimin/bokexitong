<template>
  <router-view v-if="contentAllowed" />
  <BackToTop v-if="contentAllowed" />
  <WelcomeGate @access-change="handleAccessChange" />
</template>

<script setup>
import { ref } from 'vue'
import BackToTop from './components/BackToTop.vue'
import WelcomeGate from './components/WelcomeGate.vue'

const contentAllowed = ref(false)
const handleAccessChange = ({ allowContent }) => {
  contentAllowed.value = Boolean(allowContent)
}
</script>

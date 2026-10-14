<template>
  <Transition name="back-top">
    <button v-if="visible" class="back-to-top" type="button" aria-label="返回顶部" @click="goTop">
      <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m6 15 6-6 6 6"/></svg>
    </button>
  </Transition>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

const visible = ref(false)
const update = () => { visible.value = window.scrollY > 520 }
const goTop = () => window.scrollTo({ top: 0, behavior: 'smooth' })
onMounted(() => window.addEventListener('scroll', update, { passive: true }))
onBeforeUnmount(() => window.removeEventListener('scroll', update))
</script>

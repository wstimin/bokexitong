<template>
  <button
    class="theme-toggle"
    type="button"
    :aria-label="theme === 'dark' ? '切换到浅色模式' : '切换到深色模式'"
    :title="theme === 'dark' ? '浅色模式' : '深色模式'"
    @click="toggleTheme"
  >
    <span class="theme-toggle__track" :class="{ 'is-dark': theme === 'dark' }">
      <svg v-if="theme === 'dark'" viewBox="0 0 24 24" aria-hidden="true">
        <path d="M20.1 15.4A8.3 8.3 0 0 1 8.6 3.9 8.4 8.4 0 1 0 20.1 15.4Z" />
      </svg>
      <svg v-else viewBox="0 0 24 24" aria-hidden="true">
        <circle cx="12" cy="12" r="3.5" />
        <path d="M12 2v2.2M12 19.8V22M4.9 4.9l1.6 1.6M17.5 17.5l1.6 1.6M2 12h2.2M19.8 12H22M4.9 19.1l1.6-1.6M17.5 6.5l1.6-1.6" />
      </svg>
    </span>
  </button>
</template>

<script setup>
import { onMounted } from 'vue'
import { useTheme } from '../utils/theme'

const props = defineProps({
  scope: {
    type: String,
    default: 'portal'
  }
})

const { theme, toggleTheme, activateTheme } = useTheme(props.scope)

onMounted(activateTheme)
</script>

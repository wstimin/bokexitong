<template>
  <span class="upload-trigger-wrap" :class="{ disabled }">
    <label class="upload-trigger" :for="inputId" :aria-disabled="disabled ? 'true' : 'false'">
      <slot />
    </label>
  </span>
  <input
    :id="inputId"
    ref="inputRef"
    class="upload-native-input"
    type="file"
    :accept="accept"
    :multiple="multiple"
    :disabled="disabled"
    @change="handleChange"
  />
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  accept: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  multiple: { type: Boolean, default: false }
})

const emit = defineEmits(['select'])
const inputRef = ref(null)
const inputId = `file-upload-${Math.random().toString(36).slice(2, 10)}`

const handleChange = (event) => {
  const file = event?.target?.files?.[0]
  if (file) emit('select', file)
  if (event?.target) event.target.value = ''
}

const openPicker = () => {
  if (props.disabled) return
  const input = inputRef.value
  if (!input) return
  input.value = ''
  if (typeof input.showPicker === 'function') {
    try {
      input.showPicker()
      return
    } catch (error) {
      console.warn('file picker fallback', error)
    }
  }
  input.click()
}
</script>

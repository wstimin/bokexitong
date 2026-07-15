<template>
  <button class="upload-trigger" type="button" :disabled="disabled" @click="openPicker">
    <slot />
  </button>
  <input
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

const openPicker = () => {
  if (props.disabled) return
  const input = inputRef.value
  if (!input) return
  input.value = ''
  input.click()
}

const handleChange = (event) => {
  const file = event?.target?.files?.[0]
  if (file) emit('select', file)
  if (event?.target) event.target.value = ''
}
</script>

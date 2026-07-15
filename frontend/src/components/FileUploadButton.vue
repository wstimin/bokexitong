<template>
  <span class="upload-trigger-wrap">
    <button class="upload-trigger" type="button" :disabled="disabled" @click="openPicker">
      <slot />
    </button>
    <input ref="inputRef" class="upload-hidden-input" type="file" :accept="accept" @change="onChange" />
  </span>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  accept: { type: String, default: '' },
  disabled: { type: Boolean, default: false }
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

const onChange = (event) => {
  const file = event.target.files?.[0]
  if (file) emit('select', file)
  event.target.value = ''
}
</script>

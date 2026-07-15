<template>
  <label class="upload-trigger-wrap" :class="{ disabled }">
    <span class="upload-trigger">
      <slot />
    </span>
    <input ref="inputRef" class="upload-hidden-input" type="file" :accept="accept" :disabled="disabled" @change="onChange" />
  </label>
</template>

<script setup>
const props = defineProps({
  accept: { type: String, default: '' },
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['select'])

const onChange = (event) => {
  const file = event.target.files?.[0]
  if (file) emit('select', file)
  event.target.value = ''
}
</script>

<template>
  <div class="article-rich-editor" :class="{ 'is-disabled': disabled }">
    <div v-if="!editorReady" class="article-rich-editor__loading" role="status">
      <LoaderCircle :size="18" class="is-spinning" aria-hidden="true" />
      编辑器加载中
    </div>
    <div ref="editorMount" class="article-rich-editor__surface" :aria-busy="!editorReady"></div>
    <div v-if="pendingUploads" class="article-rich-editor__upload-status" role="status">
      <LoaderCircle :size="16" class="is-spinning" aria-hidden="true" />
      {{ pendingUploads }} 个文件上传中
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { LoaderCircle } from 'lucide-vue-next'

const props = defineProps({
  modelValue: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  placeholder: { type: String, default: '开始写正文，输入 / 可以插入图片、视频、附件和内容卡片' }
})

const emit = defineEmits(['update:modelValue', 'pending-change', 'ready'])
const editorMount = ref(null)
const editorReady = ref(false)
const pendingUploads = ref(0)
let editorController = null
let unmounted = false

const setPendingUploads = (value) => {
  pendingUploads.value = Math.max(0, Number(value) || 0)
  if (!unmounted) emit('pending-change', pendingUploads.value)
}

onMounted(async () => {
  try {
    const { mountKoenigEditor } = await import('../editor/koenigEditorBridge.jsx')
    if (unmounted || !editorMount.value) return

    editorController = mountKoenigEditor(editorMount.value, {
      value: props.modelValue,
      disabled: props.disabled,
      placeholder: props.placeholder,
      onChange: (html) => {
        if (!unmounted && html !== props.modelValue) emit('update:modelValue', html)
      },
      onPendingChange: setPendingUploads,
      onReady: (editor) => {
        if (unmounted) return
        editorReady.value = true
        emit('ready', editor)
      }
    })
  } catch (error) {
    console.error(error)
    ElMessage.error('编辑器初始化失败，请刷新页面后重试')
  }
})

watch(() => props.modelValue, (value) => editorController?.setValue(value))
watch(() => props.disabled, (value) => editorController?.setDisabled(value))
watch(() => props.placeholder, (value) => editorController?.setPlaceholder(value))

onBeforeUnmount(() => {
  unmounted = true
  emit('pending-change', 0)
  pendingUploads.value = 0
  editorController?.destroy()
  editorController = null
})

defineExpose({
  focus: () => editorController?.focus(),
  getEditor: () => editorController?.getEditor() || null
})
</script>

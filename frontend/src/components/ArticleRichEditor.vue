<template>
  <div class="article-rich-editor" :class="{ 'is-disabled': disabled, 'is-immersive': immersive }">
    <div v-if="!editorReady" class="article-rich-editor__loading" role="status">
      <LoaderCircle :size="18" class="is-spinning" aria-hidden="true" />
      编辑器加载中
    </div>
    <QuillEditor
      v-else
      ref="quillComponent"
      v-model:content="content"
      theme="snow"
      content-type="html"
      class="article-rich-editor__surface"
      :toolbar="toolbarOptions"
      :enable="!disabled"
      :placeholder="placeholder"
      @ready="onEditorReady"
      @selectionChange="onSelectionChange"
    >
      <template #toolbar>
        <div :id="toolbarId" class="article-rich-editor__toolbar" aria-label="文章编辑工具栏">
          <div class="article-rich-editor__formats">
            <span class="ql-formats">
              <select class="ql-header" title="段落样式" aria-label="段落样式">
                <option value="1">一级标题</option>
                <option value="2">二级标题</option>
                <option value="3">三级标题</option>
                <option selected>正文</option>
              </select>
            </span>
            <span class="ql-formats">
              <button type="button" class="ql-bold" title="加粗" aria-label="加粗"></button>
              <button type="button" class="ql-italic" title="斜体" aria-label="斜体"></button>
              <button type="button" class="ql-underline" title="下划线" aria-label="下划线"></button>
            </span>
            <span class="ql-formats">
              <button type="button" class="ql-list" value="ordered" title="有序列表" aria-label="有序列表"></button>
              <button type="button" class="ql-list" value="bullet" title="无序列表" aria-label="无序列表"></button>
              <button type="button" class="ql-blockquote" title="引用" aria-label="引用"></button>
              <button type="button" class="ql-code-block" title="代码块" aria-label="代码块"></button>
            </span>
            <span class="ql-formats">
              <button type="button" class="ql-link" title="文本链接" aria-label="文本链接"></button>
              <button type="button" class="article-editor-history article-editor-history--undo" title="撤销" aria-label="撤销" :disabled="disabled" @click="undoEditor"></button>
              <button type="button" class="article-editor-history article-editor-history--redo" title="重做" aria-label="重做" :disabled="disabled" @click="redoEditor"></button>
              <button type="button" class="ql-clean" title="清除格式" aria-label="清除格式"></button>
            </span>
            <span class="article-rich-editor__media-tools">
            <button type="button" class="article-editor-tool" title="插入图片" aria-label="插入图片" :disabled="disabled" @click="openPicker('image')">
              <ImagePlus :size="17" aria-hidden="true" />
            </button>
            <button type="button" class="article-editor-tool" title="插入视频" aria-label="插入视频" :disabled="disabled" @click="openPicker('video')">
              <Video :size="17" aria-hidden="true" />
            </button>
            <button type="button" class="article-editor-tool" title="插入附件" aria-label="插入附件" :disabled="disabled" @click="openPicker('file')">
              <Paperclip :size="17" aria-hidden="true" />
            </button>
            <button type="button" class="article-editor-tool article-editor-tool--link" title="插入链接按钮" aria-label="插入链接按钮" :disabled="disabled" @click="openLinkButton">
              <SquareArrowOutUpRight :size="17" aria-hidden="true" />
              <span>链接按钮</span>
            </button>
            <span v-if="pendingUploads" class="article-rich-editor__upload-status" role="status">
              <LoaderCircle :size="16" class="is-spinning" aria-hidden="true" />
              {{ pendingUploads }} 个文件上传中
            </span>
            </span>
          </div>

          <input ref="imageInput" class="upload-native-input" type="file" accept="image/*" :disabled="disabled" @change="(event) => handlePickerChange(event, 'image')" />
          <input ref="videoInput" class="upload-native-input" type="file" accept="video/*" :disabled="disabled" @change="(event) => handlePickerChange(event, 'video')" />
          <input ref="fileInput" class="upload-native-input" type="file" :disabled="disabled" @change="(event) => handlePickerChange(event, 'file')" />
        </div>
      </template>
    </QuillEditor>

    <el-dialog v-model="linkButtonVisible" title="插入链接按钮" width="460px" append-to-body @closed="clearLinkBookmark">
      <el-form label-position="top">
        <el-form-item label="按钮文字"><el-input v-model="linkButtonForm.text" maxlength="60" placeholder="例如：查看文档" /></el-form-item>
        <el-form-item label="链接地址"><el-input v-model="linkButtonForm.href" placeholder="https://example.com 或 /article/1" /></el-form-item>
        <el-form-item label="按钮样式">
          <el-radio-group v-model="linkButtonForm.style">
            <el-radio-button value="primary">主要</el-radio-button>
            <el-radio-button value="secondary">次要</el-radio-button>
            <el-radio-button value="download">下载</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item><el-checkbox v-model="linkButtonForm.newWindow">在新窗口打开</el-checkbox></el-form-item>
      </el-form>
      <template #footer>
        <button class="btn-ghost" type="button" @click="linkButtonVisible = false">取消</button>
        <button class="btn-primary" type="button" @click="confirmLinkButton">插入</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, shallowRef, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { QuillEditor } from '@vueup/vue-quill'
import { ImagePlus, LoaderCircle, Paperclip, SquareArrowOutUpRight, Video } from 'lucide-vue-next'
import { uploadApi } from '../api/blog'
import {
  ensureRichTextFormats,
  fileSnippet,
  getSafeInsertIndex,
  imageSnippet,
  insertHtmlSnippet,
  insertLinkButton,
  insertUploadPlaceholder,
  removeUploadPlaceholder,
  replaceUploadPlaceholder,
  setupRichTextEditor,
  videoSnippet
} from '../utils/richText'

const props = defineProps({
  modelValue: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  immersive: { type: Boolean, default: false },
  placeholder: { type: String, default: '开始写正文，支持标题、列表、引用、代码、图片、视频、附件和链接。' }
})

const emit = defineEmits(['update:modelValue', 'pending-change', 'ready'])
const toolbarId = `article-editor-toolbar-${Math.random().toString(36).slice(2, 10)}`
const toolbarOptions = { container: `#${toolbarId}` }
const content = computed({ get: () => props.modelValue, set: (value) => emit('update:modelValue', value || '') })
const editorReady = ref(false)
const quillComponent = ref(null)
const editorRef = shallowRef(null)
const lastSelection = ref(null)
const pendingUploads = ref(0)
const unmounted = ref(false)
const imageInput = ref(null)
const videoInput = ref(null)
const fileInput = ref(null)
const pickerInputs = { image: imageInput, video: videoInput, file: fileInput }
const activeBookmarks = new Set()
const linkButtonVisible = ref(false)
const linkBookmark = ref(null)
const linkButtonForm = reactive({ text: '', href: '', style: 'primary', newWindow: true })
let editorCleanup = null
let bookmarkChangeHandler = null

const transformBookmark = (bookmark, delta) => {
  if (!bookmark?.active) return
  if (typeof delta?.transformPosition === 'function') {
    bookmark.index = delta.transformPosition(bookmark.index, true)
    return
  }
  let sourceIndex = 0
  let targetIndex = bookmark.index
  for (const operation of delta?.ops || []) {
    if (operation.retain) sourceIndex += operation.retain
    else if (operation.insert != null && sourceIndex <= bookmark.index) targetIndex += typeof operation.insert === 'string' ? operation.insert.length : 1
    else if (operation.delete && sourceIndex < bookmark.index) targetIndex -= Math.min(operation.delete, bookmark.index - sourceIndex)
  }
  bookmark.index = Math.max(0, targetIndex)
}

const createBookmark = (range = null) => {
  const quill = editorRef.value
  if (!quill) return null
  let selection = range || lastSelection.value
  if (!selection) {
    try {
      selection = quill.getSelection?.() || null
    } catch (error) {
      console.warn('Unable to read the current editor selection; using the document end instead.', error)
    }
  }
  const bookmark = { index: getSafeInsertIndex(quill, selection), active: true }
  activeBookmarks.add(bookmark)
  return bookmark
}

const releaseBookmark = (bookmark) => {
  if (!bookmark) return null
  bookmark.active = false
  activeBookmarks.delete(bookmark)
  return bookmark.index
}

const setPendingUploads = (value) => {
  pendingUploads.value = Math.max(0, value)
  if (!unmounted.value) emit('pending-change', pendingUploads.value)
}

const onEditorReady = (quill) => {
  editorCleanup?.()
  if (editorRef.value && bookmarkChangeHandler) editorRef.value.off?.('text-change', bookmarkChangeHandler)
  editorRef.value = quill
  lastSelection.value = null
  editorCleanup = setupRichTextEditor(quill, {
    getLastSelection: () => lastSelection.value,
    onImageFile: (file) => uploadMedia(file, 'image'),
    onImageFiles: (files) => uploadImageBatch(files)
  })
  bookmarkChangeHandler = (delta) => activeBookmarks.forEach((bookmark) => transformBookmark(bookmark, delta))
  quill.on?.('text-change', bookmarkChangeHandler)
  emit('ready', quill)
}

const onSelectionChange = ({ range }) => {
  if (range) lastSelection.value = { index: range.index, length: range.length || 0 }
}

watch(() => props.modelValue, (value) => {
  if (value !== '' || !editorRef.value) return
  const currentHtml = editorRef.value.root?.innerHTML || ''
  if (currentHtml === '<p><br></p>' || currentHtml === '') return
  quillComponent.value?.setHTML?.('', 'api')
  lastSelection.value = null
})

const openPicker = (type) => {
  if (props.disabled || !editorRef.value) return
  const input = pickerInputs[type]?.value
  if (!input) return ElMessage.warning('编辑器还没有准备好，请稍后再试')
  input.value = ''
  input.click()
}

const handlePickerChange = async (event, type) => {
  const file = event?.target?.files?.[0]
  if (event?.target) event.target.value = ''
  if (file) await uploadMedia(file, type)
}

const uploadMedia = async (file, type) => {
  if (props.disabled || !file) return
  if (!editorRef.value) return ElMessage.warning('编辑器还没有准备好，请稍后再试')
  const placeholder = insertUploadPlaceholder(editorRef.value, type)
  if (!placeholder) return ElMessage.warning('无法创建上传占位，请稍后再试')
  setPendingUploads(pendingUploads.value + 1)
  try {
    const res = await uploadApi.file(file)
    if (unmounted.value || !editorRef.value) return
    const { url, name } = res.data
    const snippet = type === 'image' ? imageSnippet(url, name) : type === 'video' ? videoSnippet(url, name) : fileSnippet(url, name)
    replaceUploadPlaceholder(editorRef.value, placeholder.id, snippet)
    ElMessage.success('上传并插入成功')
  } catch (error) {
    if (!unmounted.value && editorRef.value) removeUploadPlaceholder(editorRef.value, placeholder.id)
    console.error(error)
    if (!unmounted.value && !error?.response) ElMessage.error(error?.message || '文件上传失败')
  } finally {
    setPendingUploads(pendingUploads.value - 1)
  }
}

const uploadImageBatch = async (files) => {
  const images = Array.from(files || []).filter(Boolean)
  if (props.disabled || !images.length) return
  if (!editorRef.value) return ElMessage.warning('编辑器还没有准备好，请稍后再试')
  const placeholders = images.map(() => insertUploadPlaceholder(editorRef.value, 'image'))
  if (placeholders.some((placeholder) => !placeholder)) {
    placeholders.filter(Boolean).forEach((placeholder) => removeUploadPlaceholder(editorRef.value, placeholder.id))
    return ElMessage.warning('无法创建图片上传占位，请稍后再试')
  }
  setPendingUploads(pendingUploads.value + images.length)
  try {
    const results = await Promise.allSettled(images.map((file) => uploadApi.file(file)))
    if (unmounted.value || !editorRef.value) return
    let inserted = 0
    results.forEach((result, index) => {
      const placeholder = placeholders[index]
      if (result.status !== 'fulfilled') {
        removeUploadPlaceholder(editorRef.value, placeholder.id)
        return
      }
      const { url, name } = result.value.data
      try {
        replaceUploadPlaceholder(editorRef.value, placeholder.id, imageSnippet(url, name))
        inserted += 1
      } catch (error) {
        removeUploadPlaceholder(editorRef.value, placeholder.id)
        console.error(error)
      }
    })
    if (inserted) {
      ElMessage.success(inserted === images.length ? `${inserted} 张图片上传并插入成功` : `已插入 ${inserted} 张图片`)
    }
    const failed = images.length - inserted
    if (failed) ElMessage.error(`${failed} 张图片上传或插入失败，请重试`)
  } catch (error) {
    if (!unmounted.value && editorRef.value) placeholders.forEach((placeholder) => removeUploadPlaceholder(editorRef.value, placeholder.id))
    console.error(error)
    ElMessage.error('图片上传失败')
  } finally {
    setPendingUploads(pendingUploads.value - images.length)
  }
}

const openLinkButton = () => {
  clearLinkBookmark()
  linkBookmark.value = createBookmark()
  if (!linkBookmark.value) return ElMessage.warning('编辑器还没有准备好，请稍后再试')
  Object.assign(linkButtonForm, { text: '', href: '', style: 'primary', newWindow: true })
  linkButtonVisible.value = true
}

const clearLinkBookmark = () => {
  if (linkBookmark.value) releaseBookmark(linkBookmark.value)
  linkBookmark.value = null
}

const undoEditor = () => editorRef.value?.history?.undo?.()
const redoEditor = () => editorRef.value?.history?.redo?.()

const confirmLinkButton = () => {
  const bookmark = linkBookmark.value
  try {
    const index = releaseBookmark(bookmark)
    linkBookmark.value = null
    const nextIndex = insertLinkButton(editorRef.value, { index, length: 0 }, linkButtonForm)
    if (nextIndex !== null) lastSelection.value = { index: nextIndex, length: 0 }
    linkButtonVisible.value = false
  } catch (error) {
    if (bookmark && !bookmark.active) {
      bookmark.active = true
      activeBookmarks.add(bookmark)
      linkBookmark.value = bookmark
    }
    ElMessage.warning(error.message || '链接按钮填写有误')
  }
}

onMounted(async () => {
  try {
    await ensureRichTextFormats()
    if (!unmounted.value) editorReady.value = true
  } catch (error) {
    console.error(error)
    ElMessage.error('编辑器初始化失败，请刷新页面后重试')
  }
})

onBeforeUnmount(() => {
  emit('pending-change', 0)
  unmounted.value = true
  editorCleanup?.()
  if (editorRef.value && bookmarkChangeHandler) editorRef.value.off?.('text-change', bookmarkChangeHandler)
  editorCleanup = null
  bookmarkChangeHandler = null
  activeBookmarks.forEach((bookmark) => releaseBookmark(bookmark))
  clearLinkBookmark()
  pendingUploads.value = 0
})

defineExpose({
  getQuill: () => editorRef.value
})
</script>

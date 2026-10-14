<template>
  <div class="article-composer" role="dialog" aria-modal="true" aria-label="文章编辑器">
    <header class="article-composer__topbar">
      <div class="article-composer__topbar-side article-composer__topbar-side--start">
        <button class="composer-icon-button" type="button" title="返回文章列表" aria-label="返回文章列表" :disabled="saving" @click="emit('close')">
          <ArrowLeft :size="20" aria-hidden="true" />
        </button>
        <div class="article-composer__identity">
          <strong>{{ editing ? '编辑文章' : '新文章' }}</strong>
          <span :class="['article-composer__save-state', { 'is-dirty': dirty }]">
            <LoaderCircle v-if="saving" :size="13" class="is-spinning" aria-hidden="true" />
            <CloudUpload v-else-if="pendingUploads" :size="13" aria-hidden="true" />
            <CheckCircle2 v-else :size="13" aria-hidden="true" />
            {{ saveStateText }}
          </span>
        </div>
      </div>

      <div class="article-composer__mode-switch" aria-label="编辑器视图">
        <button type="button" :class="{ active: mode === 'edit' }" @click="mode = 'edit'">
          <PencilLine :size="16" aria-hidden="true" />
          <span>编辑</span>
        </button>
        <button type="button" :class="{ active: mode === 'preview' }" @click="mode = 'preview'">
          <Eye :size="16" aria-hidden="true" />
          <span>预览</span>
        </button>
      </div>

      <div class="article-composer__topbar-side article-composer__topbar-side--end">
        <button class="composer-icon-button" type="button" title="文章设置" aria-label="文章设置" :class="{ active: settingsOpen }" @click="settingsOpen = !settingsOpen">
          <Settings2 :size="19" aria-hidden="true" />
        </button>
        <button class="composer-text-button" type="button" :disabled="actionsDisabled" @click="emit('save', 'DRAFT')">保存草稿</button>
        <el-dropdown v-if="admin" trigger="click" :disabled="actionsDisabled" @command="(status) => emit('save', status)">
          <button class="composer-publish-button" type="button" :disabled="actionsDisabled">
            发布
            <ChevronDown :size="16" aria-hidden="true" />
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="PUBLISHED">直接发布</el-dropdown-item>
              <el-dropdown-item command="PENDING">提交审核</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <button v-else class="composer-publish-button" type="button" :disabled="actionsDisabled" @click="emit('save', 'PUBLISHED')">发布</button>
      </div>
    </header>

    <div :class="['article-composer__workspace', { 'settings-collapsed': !settingsOpen }]">
      <main class="article-composer__main">
        <div v-if="loading" class="article-composer__loading" role="status">
          <LoaderCircle :size="22" class="is-spinning" aria-hidden="true" />
          正在载入文章
        </div>

        <template v-else-if="mode === 'edit'">
          <div class="article-composer__document">
            <textarea
              v-model="title"
              class="article-composer__title"
              rows="1"
              maxlength="180"
              placeholder="文章标题"
              aria-label="文章标题"
              :disabled="disabled"
              @input="resizeTitle"
            ></textarea>
            <textarea
              v-model="summary"
              class="article-composer__summary"
              rows="2"
              maxlength="500"
              placeholder="写一段摘要，让读者快速了解这篇文章..."
              aria-label="文章摘要"
              :disabled="disabled"
            ></textarea>

            <ArticleRichEditor
              v-model="content"
              immersive
              :disabled="disabled"
              placeholder="开始写作。输入正文，或从工具栏插入图片、视频和附件..."
              @pending-change="(value) => emit('pending-change', value)"
            />
          </div>
        </template>

        <article v-else class="article-composer__preview">
          <img v-if="coverPreviewSrc" class="article-composer__preview-cover" :src="coverPreviewSrc" :alt="title || '文章封面'" />
          <div class="article-composer__preview-meta">
            <span v-if="categoryName">{{ categoryName }}</span>
            <span>{{ readingTime }} 分钟阅读</span>
          </div>
          <h1>{{ title || '未命名文章' }}</h1>
          <p v-if="summary" class="article-composer__preview-summary">{{ summary }}</p>
          <div v-if="contentText" class="article-body" v-html="previewHtml"></div>
          <div v-else class="article-composer__preview-empty">正文还没有内容</div>
        </article>

        <footer class="article-composer__stats" aria-label="文章统计">
          <span><Type :size="14" aria-hidden="true" />{{ characterCount }} 字</span>
          <span><Clock3 :size="14" aria-hidden="true" />约 {{ readingTime }} 分钟阅读</span>
          <span v-if="title.length">标题 {{ title.length }}/180</span>
        </footer>
      </main>

      <button v-if="settingsOpen" class="article-composer__settings-backdrop" type="button" aria-label="关闭文章设置" @click="settingsOpen = false"></button>
      <aside :class="['article-composer__settings', { open: settingsOpen }]" aria-label="文章设置">
        <div class="article-composer__settings-head">
          <div>
            <strong>文章设置</strong>
            <span>发布信息与展示方式</span>
          </div>
          <button class="composer-icon-button article-composer__settings-close" type="button" title="关闭设置" aria-label="关闭设置" @click="settingsOpen = false">
            <X :size="18" aria-hidden="true" />
          </button>
        </div>

        <div class="article-composer__settings-scroll">
          <section class="composer-setting-section">
            <div class="composer-setting-title">
              <ImageIcon :size="17" aria-hidden="true" />
              <span>封面图</span>
            </div>
            <div class="composer-cover">
              <img v-if="coverPreviewSrc" :src="coverPreviewSrc" alt="文章封面" />
              <div v-else class="composer-cover__empty">
                <ImagePlus :size="25" aria-hidden="true" />
                <span>添加文章封面</span>
              </div>
              <button v-if="coverUrl" class="composer-cover__remove" type="button" title="移除封面" aria-label="移除封面" :disabled="disabled" @click="coverUrl = ''">
                <X :size="15" aria-hidden="true" />
              </button>
            </div>
            <div class="composer-cover__actions">
              <FileUploadButton accept="image/jpeg,image/png,image/gif,image/webp" :disabled="disabled || pendingUploads > 0" @select="(file) => emit('upload-cover', file)">
                <LoaderCircle v-if="pendingUploads > 0" :size="15" class="is-spinning" aria-hidden="true" />
                <Upload v-else :size="15" aria-hidden="true" />
                {{ pendingUploads > 0 ? '上传中...' : '上传封面' }}
              </FileUploadButton>
            </div>
            <el-input v-model="coverUrl" size="small" clearable placeholder="或粘贴图片地址" :disabled="disabled" />
          </section>

          <section class="composer-setting-section">
            <label class="composer-field-label" for="composer-category">分类</label>
            <el-select id="composer-category" v-model="categoryId" placeholder="选择分类" clearable filterable :disabled="disabled">
              <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </section>

          <section class="composer-setting-section">
            <label class="composer-field-label" for="composer-tags">标签</label>
            <el-select id="composer-tags" v-model="tagIds" placeholder="选择标签" multiple clearable filterable collapse-tags collapse-tags-tooltip :disabled="disabled">
              <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
            </el-select>
          </section>

          <section v-if="admin" class="composer-setting-section">
            <div class="composer-setting-title">
              <Star :size="17" aria-hidden="true" />
              <span>首页推荐</span>
            </div>
            <div class="composer-recommend-row">
              <div>
                <strong>推荐此文章</strong>
                <span>仅已发布文章会在首页展示</span>
              </div>
              <el-switch v-model="recommended" :active-value="1" :inactive-value="0" :disabled="disabled" />
            </div>
            <div v-if="recommended === 1" class="composer-sort-field">
              <label class="composer-field-label">推荐顺序</label>
              <el-input-number v-model="recommendSort" :min="0" :step="1" controls-position="right" :disabled="disabled" />
              <span>数字越小越靠前</span>
            </div>
          </section>

          <section class="composer-setting-section composer-setting-section--summary">
            <strong>发布检查</strong>
            <ul>
              <li :class="{ complete: Boolean(title.trim()) }"><Check :size="15" aria-hidden="true" />已填写标题</li>
              <li :class="{ complete: Boolean(contentText) }"><Check :size="15" aria-hidden="true" />已填写正文</li>
              <li :class="{ complete: Boolean(coverUrl) }"><Check :size="15" aria-hidden="true" />已设置封面</li>
              <li :class="{ complete: Boolean(categoryId) }"><Check :size="15" aria-hidden="true" />已选择分类</li>
            </ul>
          </section>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  ArrowLeft,
  Check,
  CheckCircle2,
  ChevronDown,
  Clock3,
  CloudUpload,
  Eye,
  Image as ImageIcon,
  ImagePlus,
  LoaderCircle,
  PencilLine,
  Settings2,
  Star,
  Type,
  Upload,
  X
} from 'lucide-vue-next'
import { normalizeAssetUrl } from '../utils/assets'
import { toDisplayHtml } from '../utils/richText'
import ArticleRichEditor from './ArticleRichEditor.vue'
import FileUploadButton from './FileUploadButton.vue'

const props = defineProps({
  article: { type: Object, required: true },
  categories: { type: Array, default: () => [] },
  tags: { type: Array, default: () => [] },
  editing: { type: Boolean, default: false },
  admin: { type: Boolean, default: false },
  dirty: { type: Boolean, default: false },
  saving: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  pendingUploads: { type: Number, default: 0 },
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['close', 'save', 'upload-cover', 'pending-change', 'update-field'])
const mode = ref('edit')
const settingsOpen = ref(true)
let previousBodyOverflow = ''

const fieldModel = (key, fallback) => computed({
  get: () => props.article?.[key] ?? fallback,
  set: (value) => emit('update-field', key, value)
})

const title = fieldModel('title', '')
const summary = fieldModel('summary', '')
const content = fieldModel('content', '')
const coverUrl = fieldModel('coverUrl', '')
const categoryId = fieldModel('categoryId', null)
const tagIds = fieldModel('tagIds', [])
const recommended = fieldModel('recommended', 0)
const recommendSort = fieldModel('recommendSort', 0)
const actionsDisabled = computed(() => props.disabled || props.saving || props.loading || props.pendingUploads > 0)
const coverPreviewSrc = computed(() => normalizeAssetUrl(coverUrl.value))
const previewHtml = computed(() => toDisplayHtml(content.value, 'HTML'))
const contentText = computed(() => String(content.value || '')
  .replace(/<style[\s\S]*?<\/style>/gi, ' ')
  .replace(/<script[\s\S]*?<\/script>/gi, ' ')
  .replace(/<[^>]+>/g, ' ')
  .replace(/&nbsp;|&#160;/gi, ' ')
  .replace(/&[a-z]+;|&#\d+;/gi, ' ')
  .replace(/\s+/g, ' ')
  .trim())
const characterCount = computed(() => contentText.value.replace(/\s/g, '').length)
const readingTime = computed(() => Math.max(1, Math.ceil(characterCount.value / 500)))
const categoryName = computed(() => props.categories.find((item) => item.id === categoryId.value)?.name || '')
const saveStateText = computed(() => {
  if (props.saving) return '正在保存...'
  if (props.pendingUploads) return `${props.pendingUploads} 个文件上传中`
  if (props.dirty) return '有未保存的修改'
  return '所有更改已保存'
})

const resizeTitle = (event) => {
  const target = event?.target
  if (!target) return
  target.style.height = 'auto'
  target.style.height = `${target.scrollHeight}px`
}

const handleKeydown = (event) => {
  if (event.key !== 'Escape') return
  if (settingsOpen.value && window.matchMedia('(max-width: 900px)').matches) settingsOpen.value = false
  else emit('close')
}

onMounted(() => {
  previousBodyOverflow = document.body.style.overflow
  document.body.style.overflow = 'hidden'
  settingsOpen.value = !window.matchMedia('(max-width: 900px)').matches
  window.addEventListener('keydown', handleKeydown)
  nextTick(() => document.querySelector('.article-composer__title')?.dispatchEvent(new Event('input')))
})

onBeforeUnmount(() => {
  document.body.style.overflow = previousBodyOverflow
  window.removeEventListener('keydown', handleKeydown)
})
</script>

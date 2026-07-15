<template>
  <section class="admin-card">
    <div class="toolbar article-admin-toolbar">
      <div>
        <h2>内容管理</h2>
        <p class="section-subtitle">管理员可以新增、编辑、审核、发布、下架和推荐文章。</p>
      </div>
      <div class="admin-filters article-admin-actions">
        <el-input v-model="query.keyword" placeholder="搜索标题、摘要或内容" class="filter-input" clearable @keyup.enter="search" @clear="search" />
        <el-select v-model="query.status" placeholder="状态" clearable class="filter-select" @change="search">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <button class="btn-ghost" type="button" :disabled="loading" @click="search">查询</button>
        <button class="btn-primary" type="button" @click="openEditor()">新增文章</button>
        <button class="btn-ghost danger-action" type="button" :disabled="!selected.length || loading" @click="removeSelected">批量删除</button>
      </div>
    </div>

    <div class="table-scroll">
      <el-table v-loading="loading" :data="rows" border style="min-width: 1120px" @selection-change="selected = $event">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
        <el-table-column label="状态" width="120">
          <template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="首页推荐" width="130">
          <template #default="{ row }">
            <el-tag v-if="row.recommended === 1" type="success">已推荐 #{{ row.recommendSort || 0 }}</el-tag>
            <span v-else class="review-note">未推荐</span>
          </template>
        </el-table-column>
        <el-table-column label="审核说明" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reviewReason || '-' }}</template>
        </el-table-column>
        <el-table-column prop="viewCount" label="阅读" width="90" />
        <el-table-column prop="likeCount" label="点赞" width="90" />
        <el-table-column prop="favoriteCount" label="收藏" width="90" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="420" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <el-button size="small" @click="openPreview(row)">预览</el-button>
              <el-button size="small" type="primary" @click="openEditor(row)">编辑</el-button>
              <el-button v-if="canPublish(row.status)" size="small" type="success" @click="changeStatus(row, 'PUBLISHED')">发布</el-button>
              <el-button v-if="row.status === 'PUBLISHED' && row.recommended !== 1" size="small" type="success" @click="setRecommendation(row, true)">设推荐</el-button>
              <el-button v-if="row.status === 'PUBLISHED' && row.recommended === 1" size="small" @click="setRecommendation(row, true)">改排序</el-button>
              <el-button v-if="row.recommended === 1" size="small" @click="setRecommendation(row, false)">取消推荐</el-button>
              <el-button v-if="row.status === 'PENDING'" size="small" type="warning" @click="changeStatus(row, 'REJECTED')">驳回</el-button>
              <el-button v-if="row.status === 'PUBLISHED'" size="small" type="warning" @click="changeStatus(row, 'OFFLINE')">下架</el-button>
              <el-button size="small" type="danger" @click="remove(row.id)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="pager">
      <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="query.size" v-model:current-page="query.current" @current-change="load" />
    </div>

    <el-dialog v-model="editorVisible" :title="editingId ? '编辑文章' : '新增文章'" width="1020px" class="article-editor-dialog" destroy-on-close append-to-body>
      <el-form label-position="top">
        <el-form-item label="标题">
          <el-input v-model="form.title" maxlength="180" show-word-limit placeholder="请输入文章标题" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="用于首页和搜索结果展示" />
        </el-form-item>

        <div class="writer-meta-grid">
          <el-form-item label="分类">
            <el-select v-model="form.categoryId" placeholder="选择分类" clearable filterable>
              <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="标签">
            <el-select v-model="form.tagIds" placeholder="选择标签" multiple clearable filterable collapse-tags collapse-tags-tooltip>
              <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="首页推荐">
            <div class="recommend-setting-row">
              <el-switch v-model="form.recommended" :active-value="1" :inactive-value="0" active-text="推荐" inactive-text="不推荐" />
              <el-input-number v-model="form.recommendSort" :min="0" :step="1" controls-position="right" placeholder="排序" />
            </div>
          </el-form-item>
        </div>

        <el-form-item label="封面图">
          <div class="cover-picker">
            <img v-if="form.coverUrl" :src="coverPreviewSrc" alt="文章封面" />
            <div v-else class="cover-empty">封面预览</div>
            <div class="cover-fields">
              <el-input v-model="form.coverUrl" placeholder="上传封面或粘贴图片地址" />
              <el-upload :show-file-list="false" :http-request="(options) => uploadFile(options, 'cover')" accept="image/*">
                <button class="btn-ghost" type="button">上传封面</button>
              </el-upload>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="正文">
          <div class="rich-editor-shell">
            <QuillEditor
              v-model:content="form.content"
              theme="snow"
              content-type="html"
              class="rich-editor"
              :toolbar="richToolbar"
              @ready="onEditorReady"
              @selectionChange="onSelectionChange"
              placeholder="开始写正文，支持字体、字号、颜色、对齐、图片、视频和链接。"
            />
          </div>
        </el-form-item>
      </el-form>

      <div class="upload-row writer-tools">
        <button class="btn-ghost" type="button" @click="openUpload('image')">插入图片</button>
        <button class="btn-ghost" type="button" @click="openUpload('video')">插入视频</button>
        <button class="btn-ghost" type="button" @click="openUpload('file')">插入附件</button>
        <input ref="imageInput" class="hidden-file-input" type="file" accept="image/*" @change="(event) => uploadSelectedFile(event, 'image')" />
        <input ref="videoInput" class="hidden-file-input" type="file" accept="video/*" @change="(event) => uploadSelectedFile(event, 'video')" />
        <input ref="fileInput" class="hidden-file-input" type="file" @change="(event) => uploadSelectedFile(event, 'file')" />
      </div>

      <template #footer>
        <button class="btn-ghost" type="button" @click="editorVisible = false">取消</button>
        <button class="btn-ghost" type="button" :disabled="saving" @click="saveArticle('DRAFT')">保存草稿</button>
        <button class="btn-ghost" type="button" :disabled="saving" @click="saveArticle('PENDING')">提交审核</button>
        <button class="btn-primary" type="button" :disabled="saving" @click="saveArticle('PUBLISHED')">发布文章</button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" title="文章预览" width="900px" class="article-preview-dialog" append-to-body>
      <div v-if="previewArticle.id" class="admin-preview">
        <img v-if="previewArticle.coverUrl" class="cover detail-cover" :src="previewCoverSrc" :alt="previewArticle.title" />
        <div class="meta detail-meta">
          <span>{{ statusText(previewArticle.status) }}</span>
          <span>{{ previewArticle.createdAt || '-' }}</span>
          <span>{{ previewArticle.viewCount || 0 }} 阅读</span>
          <span>{{ previewArticle.likeCount || 0 }} 点赞</span>
          <span>{{ previewArticle.favoriteCount || 0 }} 收藏</span>
        </div>
        <h1>{{ previewArticle.title }}</h1>
        <p v-if="previewArticle.summary" class="detail-summary">{{ previewArticle.summary }}</p>
        <el-alert
          v-if="previewArticle.reviewReason"
          class="review-alert"
          :title="`${statusText(previewArticle.status)}：${previewArticle.reviewReason}`"
          :description="previewArticle.reviewedAt ? `处理时间：${previewArticle.reviewedAt}` : ''"
          type="warning"
          :closable="false"
        />
        <div class="article-body" v-html="previewHtml"></div>
      </div>
      <template #footer>
        <button class="btn-ghost" type="button" @click="previewVisible = false">关闭</button>
        <button class="btn-ghost" type="button" @click="openEditor(previewArticle)">编辑</button>
        <button v-if="canPublish(previewArticle.status)" class="btn-primary" type="button" @click="changeStatus(previewArticle, 'PUBLISHED')">发布</button>
        <button v-if="previewArticle.status === 'PENDING'" class="btn-ghost danger-action" type="button" @click="changeStatus(previewArticle, 'REJECTED')">驳回</button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { QuillEditor } from '@vueup/vue-quill'
import { adminApi, articleApi, uploadApi } from '../../api/blog'
import { normalizeAssetUrl } from '../../utils/assets'
import { ensureRichTextFormats, fileSnippet, imageSnippet, insertHtmlSnippet, isEmptyHtml, richToolbar, toDisplayHtml, toEditableHtml, videoSnippet } from '../../utils/richText'

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '待审核', value: 'PENDING' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '已下架', value: 'OFFLINE' }
]

const emptyForm = () => ({
  title: '',
  summary: '',
  coverUrl: '',
  content: '',
  contentType: 'HTML',
  categoryId: null,
  tagIds: [],
  recommended: 0,
  recommendSort: 0
})

const rows = ref([])
const selected = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const previewVisible = ref(false)
const editorVisible = ref(false)
const previewArticle = ref({})
const categories = ref([])
const tags = ref([])
const editingId = ref(null)
const editorRef = ref(null)
const lastSelection = ref(null)
const imageInput = ref(null)
const videoInput = ref(null)
const fileInput = ref(null)
const form = reactive(emptyForm())
const query = reactive({ current: 1, size: 10, keyword: '', status: '' })
const previewHtml = computed(() => toDisplayHtml(previewArticle.value.content, previewArticle.value.contentType))
const previewCoverSrc = computed(() => normalizeAssetUrl(previewArticle.value.coverUrl))
const coverPreviewSrc = computed(() => normalizeAssetUrl(form.coverUrl))
const uploadInputs = {
  image: imageInput,
  video: videoInput,
  file: fileInput
}

const load = async () => {
  loading.value = true
  try {
    const res = await articleApi.page({
      ...query,
      keyword: query.keyword?.trim() || undefined,
      status: query.status || undefined
    })
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    console.error(error)
    ElMessage.error('文章列表加载失败')
  } finally {
    loading.value = false
  }
}

const loadMeta = async () => {
  try {
    const [categoryRes, tagRes] = await Promise.all([
      adminApi.categories({ current: 1, size: 200 }),
      adminApi.tags({ current: 1, size: 200 })
    ])
    categories.value = categoryRes.data.records || []
    tags.value = tagRes.data.records || []
  } catch (error) {
    console.error(error)
  }
}

const search = () => {
  query.current = 1
  load()
}

const openPreview = async (row) => {
  previewArticle.value = row
  previewVisible.value = true
  try {
    const res = await articleApi.adminDetail(row.id)
    previewArticle.value = res.data || row
  } catch (error) {
    console.error(error)
  }
}

const openEditor = async (row) => {
  resetForm()
  previewVisible.value = false
  await ensureRichTextFormats()
  lastSelection.value = null
  editorVisible.value = true
  if (!row?.id) return
  try {
    const res = await articleApi.adminDetail(row.id)
    const detail = res.data || row
    editingId.value = detail.id
    Object.assign(form, {
      title: detail.title || '',
      summary: detail.summary || '',
      coverUrl: detail.coverUrl || '',
      content: toEditableHtml(detail.content, detail.contentType),
      contentType: 'HTML',
      categoryId: detail.categoryId || null,
      tagIds: detail.tags?.map((tag) => tag.id) || [],
      recommended: detail.recommended || 0,
      recommendSort: detail.recommendSort || 0
    })
  } catch (error) {
    console.error(error)
  }
}

const resetForm = () => {
  editingId.value = null
  lastSelection.value = null
  Object.assign(form, emptyForm())
}

const onEditorReady = (quill) => {
  editorRef.value = quill
  lastSelection.value = null
}

const onSelectionChange = ({ range }) => {
  if (range) lastSelection.value = range
}

const getInsertIndex = () => {
  const quill = editorRef.value
  if (quill) {
    const selection = quill.getSelection(true) || lastSelection.value
    if (selection) return selection.index
    return Math.max(quill.getLength() - 1, 0)
  }
  return 0
}

const saveArticle = async (status) => {
  if (!form.title.trim()) {
    ElMessage.warning('请填写文章标题')
    return
  }
  if (['PENDING', 'PUBLISHED'].includes(status) && isEmptyHtml(form.content)) {
    ElMessage.warning('发布或提交前请先写正文')
    return
  }
  saving.value = true
  try {
    const payload = { ...form, contentType: 'HTML', status }
    if (editingId.value) {
      await articleApi.adminUpdate(editingId.value, payload)
    } else {
      await articleApi.adminSave(payload)
    }
    ElMessage.success(status === 'PUBLISHED' ? '文章已发布' : '文章已保存')
    editorVisible.value = false
    resetForm()
    await load()
  } catch (error) {
    console.error(error)
  } finally {
    saving.value = false
  }
}

const uploadFile = async (options, type) => {
  try {
    const res = await uploadApi.file(options.file)
    const { url, name } = res.data
    if (type === 'cover') {
      form.coverUrl = url
    } else if (type === 'image') {
      insertSnippet(imageSnippet(url, name))
    } else if (type === 'video') {
      insertSnippet(videoSnippet(url, name))
    } else {
      insertSnippet(fileSnippet(url, name))
    }
    options.onSuccess?.(res)
    ElMessage.success('上传成功')
  } catch (error) {
    console.error(error)
    options.onError?.(error)
  }
}

const openUpload = (type) => {
  uploadInputs[type]?.value?.click()
}

const uploadSelectedFile = async (event, type) => {
  const input = event.target
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  await uploadFile({ file }, type)
}

const insertSnippet = (text) => {
  const quill = editorRef.value
  if (quill && insertHtmlSnippet(quill, getInsertIndex(), text)) return
  form.content = `${form.content || ''}${text}`
}

const canPublish = (status) => ['PENDING', 'REJECTED', 'OFFLINE', 'DRAFT'].includes(status)

const changeStatus = async (row, status) => {
  let reason = ''
  if (['REJECTED', 'OFFLINE'].includes(status)) {
    const action = status === 'REJECTED' ? '驳回' : '下架'
    const result = await ElMessageBox.prompt(`请填写${action}原因，作者会在用户中心看到这条说明。`, `${action}文章`, {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '例如：标题不够清晰、正文缺少必要信息、包含不适合公开展示的内容等',
      inputValidator: (value) => Boolean(value && value.trim()) || `${action}原因不能为空`
    })
    reason = result.value.trim()
  }
  try {
    await articleApi.updateStatus(row.id, status, reason || undefined)
    ElMessage.success(`文章已${statusText(status)}`)
    previewVisible.value = false
    await load()
  } catch (error) {
    console.error(error)
  }
}

const setRecommendation = async (row, recommended) => {
  let sort = row.recommendSort || 0
  if (recommended) {
    const result = await ElMessageBox.prompt('数字越小越靠前，只能推荐已发布文章。', '首页推荐排序', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputValue: String(sort),
      inputPattern: /^\d+$/,
      inputErrorMessage: '请输入 0 或正整数'
    })
    sort = Number(result.value || 0)
  }
  try {
    await articleApi.updateRecommendation(row.id, recommended, sort)
    ElMessage.success(recommended ? '已设为首页推荐' : '已取消首页推荐')
    await load()
  } catch (error) {
    console.error(error)
  }
}

const remove = async (id) => {
  await removeIds([id], '确认删除这篇文章吗？删除后前台将不可见。')
}

const removeSelected = async () => {
  await removeIds(selected.value.map((item) => item.id), `确认删除选中的 ${selected.value.length} 篇文章吗？`)
}

const removeIds = async (ids, message) => {
  if (!ids.length) return
  await ElMessageBox.confirm(message, '删除文章', { type: 'warning' })
  try {
    await articleApi.remove(ids)
    ElMessage.success('文章已删除')
    selected.value = []
    await load()
  } catch (error) {
    console.error(error)
  }
}

const statusText = (status) => ({
  DRAFT: '草稿',
  PENDING: '待审核',
  PUBLISHED: '已发布',
  REJECTED: '已驳回',
  OFFLINE: '已下架'
}[status] || status || '-')

const statusType = (status) => ({
  PUBLISHED: 'success',
  PENDING: 'warning',
  REJECTED: 'danger',
  OFFLINE: 'info',
  DRAFT: 'info'
}[status] || 'info')

onMounted(() => {
  loadMeta()
  load()
})
</script>

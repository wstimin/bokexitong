<template>
  <section class="admin-card">
    <div class="toolbar">
      <div class="admin-filters">
        <el-input v-model="query.keyword" placeholder="搜索标题、摘要或内容" style="width: 240px" clearable @keyup.enter="search" @clear="search" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 150px" @change="search">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <button class="btn-ghost" :disabled="loading" @click="search">查询</button>
      </div>
      <button class="btn-ghost danger-action" :disabled="!selected.length || loading" @click="removeSelected">批量删除</button>
    </div>

    <div class="table-scroll">
      <el-table v-loading="loading" :data="rows" border style="min-width: 1080px" @selection-change="selected = $event">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
        <el-table-column label="状态" width="120">
          <template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="审核说明" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reviewReason || '-' }}</template>
        </el-table-column>
        <el-table-column prop="viewCount" label="阅读" width="90" />
        <el-table-column prop="likeCount" label="点赞" width="90" />
        <el-table-column prop="favoriteCount" label="收藏" width="90" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <el-button size="small" @click="openPreview(row)">预览</el-button>
              <el-button v-if="canPublish(row.status)" size="small" type="success" @click="changeStatus(row, 'PUBLISHED')">通过发布</el-button>
              <el-button v-if="row.status === 'PENDING'" size="small" type="warning" @click="changeStatus(row, 'REJECTED')">驳回</el-button>
              <el-button v-if="row.status === 'PUBLISHED'" size="small" type="warning" @click="changeStatus(row, 'OFFLINE')">下架</el-button>
              <el-button size="small" type="danger" @click="remove(row.id)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="pager">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="query.size" v-model:current-page="query.current" @current-change="load" />
    </div>

    <el-dialog v-model="previewVisible" title="文章预览" width="860px" class="article-preview-dialog">
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
        <div class="markdown" v-html="previewHtml"></div>
      </div>
      <template #footer>
        <button class="btn-ghost" @click="previewVisible = false">关闭</button>
        <button v-if="canPublish(previewArticle.status)" class="btn-primary" @click="changeStatus(previewArticle, 'PUBLISHED')">通过发布</button>
        <button v-if="previewArticle.status === 'PENDING'" class="btn-ghost danger-action" @click="changeStatus(previewArticle, 'REJECTED')">驳回</button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { marked } from 'marked'
import { articleApi } from '../../api/blog'
import { normalizeAssetUrl } from '../../utils/assets'

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '待审核', value: 'PENDING' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '已下架', value: 'OFFLINE' }
]

const rows = ref([])
const selected = ref([])
const total = ref(0)
const loading = ref(false)
const previewVisible = ref(false)
const previewArticle = ref({})
const query = reactive({ current: 1, size: 10, keyword: '', status: '' })
const previewHtml = computed(() => marked(previewArticle.value.content || ''))
const previewCoverSrc = computed(() => normalizeAssetUrl(previewArticle.value.coverUrl))

const load = async () => {
  loading.value = true
  try {
    const res = await articleApi.page({ ...query, keyword: query.keyword?.trim() || undefined, status: query.status || undefined })
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const search = () => {
  query.current = 1
  load()
}

const openPreview = async (row) => {
  const res = await articleApi.adminDetail(row.id)
  previewArticle.value = res.data || row
  previewVisible.value = true
}

const canPublish = (status) => ['PENDING', 'REJECTED', 'OFFLINE'].includes(status)

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
  await articleApi.updateStatus(row.id, status, reason || undefined)
  ElMessage.success(`文章已${statusText(status)}`)
  previewVisible.value = false
  load()
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
  await articleApi.remove(ids)
  ElMessage.success('文章已删除')
  selected.value = []
  load()
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

onMounted(load)
</script>

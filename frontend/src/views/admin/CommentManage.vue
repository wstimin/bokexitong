<template>
  <section class="admin-card">
    <div class="toolbar">
      <div class="admin-filters">
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 150px" @change="search">
          <el-option label="待审核" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
        <button class="btn-ghost" :disabled="loading" @click="search">刷新</button>
      </div>
      <button class="btn-ghost danger-action" :disabled="!selected.length || loading" @click="removeSelected">批量删除</button>
    </div>

    <div class="table-scroll">
      <el-table v-loading="loading" :data="rows" border style="min-width: 960px" @selection-change="selected = $event">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="content" label="评论内容" min-width="300" show-overflow-tooltip />
        <el-table-column prop="articleId" label="文章 ID" width="100" />
        <el-table-column prop="userId" label="用户 ID" width="100" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="审核说明" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reviewReason || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <el-button size="small" type="success" @click="audit(row.id, 'APPROVED')">通过</el-button>
              <el-button size="small" type="warning" @click="audit(row.id, 'REJECTED')">驳回</el-button>
              <el-button size="small" type="danger" @click="remove(row.id)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="pager">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="query.size" v-model:current-page="query.current" @current-change="load" />
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '../../api/blog'

const rows = ref([])
const selected = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 10, status: '' })

const load = async () => {
  loading.value = true
  try {
    const res = await adminApi.comments({ ...query, status: query.status || undefined })
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

const audit = async (id, status) => {
  let reason = ''
  if (status === 'REJECTED') {
    const result = await ElMessageBox.prompt('请填写驳回原因，用户会在个人中心看到这条说明。', '驳回评论', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '例如：评论内容重复、与文章无关、包含不适合公开展示的内容等',
      inputValidator: (value) => Boolean(value && value.trim()) || '驳回原因不能为空'
    })
    reason = result.value.trim()
  }
  await adminApi.auditComment(id, status, reason || undefined)
  ElMessage.success(`评论已${statusText(status)}`)
  load()
}

const remove = async (id) => {
  await removeIds([id], '确认删除这条评论吗？')
}

const removeSelected = async () => {
  await removeIds(selected.value.map((item) => item.id), `确认删除选中的 ${selected.value.length} 条评论吗？`)
}

const removeIds = async (ids, message) => {
  if (!ids.length) return
  await ElMessageBox.confirm(message, '删除评论', { type: 'warning' })
  await Promise.all(ids.map((id) => adminApi.deleteComment(id)))
  ElMessage.success('评论已删除')
  selected.value = []
  load()
}

const statusText = (status) => ({ APPROVED: '通过', REJECTED: '驳回', PENDING: '待审核' }[status] || status || '-')
const statusType = (status) => ({ APPROVED: 'success', REJECTED: 'danger', PENDING: 'warning' }[status] || 'info')

onMounted(load)
</script>

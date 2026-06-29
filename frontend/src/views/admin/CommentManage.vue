<template>
  <section class="admin-card">
    <div class="toolbar">
      <div class="admin-filters">
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px" @change="load">
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
        <button class="btn-ghost" @click="load">刷新</button>
      </div>
      <button class="btn-ghost danger-action" :disabled="!selected.length" @click="removeSelected">批量删除</button>
    </div>
    <el-table :data="rows" border @selection-change="selected = $event">
      <el-table-column type="selection" width="48" />
      <el-table-column prop="content" label="评论内容" min-width="260" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }"><el-tag :type="row.status === 'APPROVED' ? 'success' : 'warning'">{{ statusText(row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button size="small" @click="audit(row.id, 'APPROVED')">通过</el-button>
          <el-button size="small" type="warning" @click="audit(row.id, 'REJECTED')">驳回</el-button>
          <el-button size="small" type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
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
const query = reactive({ current: 1, size: 10, status: '' })
const load = async () => {
  const res = await adminApi.comments({ ...query })
  rows.value = res.data.records || []
  total.value = res.data.total || 0
}
const audit = async (id, status) => { await adminApi.auditComment(id, status); load() }
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
  load()
}
const statusText = (status) => ({ APPROVED: '已通过', REJECTED: '已驳回' }[status] || status || '-')
onMounted(load)
</script>

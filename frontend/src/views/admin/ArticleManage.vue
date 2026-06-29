<template>
  <section class="admin-card">
    <div class="toolbar">
      <div class="admin-filters">
        <el-input v-model="query.keyword" placeholder="搜索标题" style="width: 220px" clearable @keyup.enter="load" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px" @change="load">
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="草稿" value="DRAFT" />
        </el-select>
        <button class="btn-ghost" @click="load">查询</button>
      </div>
      <button class="btn-ghost danger-action" :disabled="!selected.length" @click="removeSelected">批量删除</button>
    </div>
    <el-table :data="rows" border @selection-change="selected = $event">
      <el-table-column type="selection" width="48" />
      <el-table-column prop="title" label="标题" min-width="220" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }"><el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'">{{ statusText(row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="viewCount" label="浏览" width="90" />
      <el-table-column prop="likeCount" label="点赞" width="90" />
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
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
import { articleApi } from '../../api/blog'

const rows = ref([])
const selected = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, keyword: '', status: '' })
const load = async () => {
  const res = await articleApi.page({ ...query })
  rows.value = res.data.records || []
  total.value = res.data.total || 0
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
  load()
}
const statusText = (status) => ({ PUBLISHED: '已发布', DRAFT: '草稿' }[status] || status || '-')
onMounted(load)
</script>

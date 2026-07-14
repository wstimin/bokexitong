<template>
  <section class="admin-card">
    <div class="toolbar user-toolbar">
      <div>
        <h2>操作日志</h2>
        <p class="section-subtitle">查看管理员对文章、评论、用户、站点配置等关键资源的操作记录。</p>
      </div>
      <div class="admin-filters">
        <el-input v-model="query.keyword" placeholder="搜索操作人或详情" class="filter-input" clearable @keyup.enter="search" @clear="search" />
        <el-select v-model="query.action" placeholder="动作" clearable class="filter-select" @change="search">
          <el-option label="新增" value="CREATE" />
          <el-option label="更新" value="UPDATE" />
          <el-option label="审核" value="AUDIT" />
          <el-option label="删除" value="DELETE" />
          <el-option label="启用" value="ENABLE" />
          <el-option label="禁用" value="DISABLE" />
          <el-option label="重置密码" value="RESET_PASSWORD" />
        </el-select>
        <el-select v-model="query.targetType" placeholder="对象" clearable class="filter-select" @change="search">
          <el-option label="文章" value="ARTICLE" />
          <el-option label="评论" value="COMMENT" />
          <el-option label="用户" value="USER" />
          <el-option label="分类" value="CATEGORY" />
          <el-option label="标签" value="TAG" />
          <el-option label="图片" value="IMAGE" />
          <el-option label="站点设置" value="SITE_SETTING" />
        </el-select>
        <button class="btn-ghost" :disabled="loading" @click="search">查询</button>
      </div>
    </div>

    <div class="table-scroll">
      <el-table v-loading="loading" :data="rows" border style="min-width: 1040px">
        <el-table-column label="动作" width="120">
          <template #default="{ row }"><el-tag :type="actionType(row.action)">{{ actionText(row.action) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="对象" width="130">
          <template #default="{ row }">{{ targetText(row.targetType) }}</template>
        </el-table-column>
        <el-table-column prop="targetId" label="对象 ID" width="110" />
        <el-table-column prop="operatorName" label="操作人" width="150" show-overflow-tooltip />
        <el-table-column prop="detail" label="详情" min-width="300" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="操作时间" width="190" />
      </el-table>
    </div>

    <div class="pager">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="query.size" v-model:current-page="query.current" @current-change="load" />
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { adminApi } from '../../api/blog'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 10, keyword: '', action: '', targetType: '' })

const cleanQuery = () => ({
  ...query,
  keyword: query.keyword?.trim() || undefined,
  action: query.action || undefined,
  targetType: query.targetType || undefined
})

const load = async () => {
  loading.value = true
  try {
    const res = await adminApi.operationLogs(cleanQuery())
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

const actionText = (action) => ({
  CREATE: '新增',
  UPDATE: '更新',
  AUDIT: '审核',
  DELETE: '删除',
  ENABLE: '启用',
  DISABLE: '禁用',
  RESET_PASSWORD: '重置密码'
}[action] || action || '-')

const actionType = (action) => ({
  CREATE: 'success',
  UPDATE: 'primary',
  AUDIT: 'warning',
  DELETE: 'danger',
  ENABLE: 'success',
  DISABLE: 'warning',
  RESET_PASSWORD: 'info'
}[action] || 'info')

const targetText = (targetType) => ({
  ARTICLE: '文章',
  COMMENT: '评论',
  USER: '用户',
  CATEGORY: '分类',
  TAG: '标签',
  IMAGE: '图片',
  SITE_SETTING: '站点设置'
}[targetType] || targetType || '-')

onMounted(load)
</script>

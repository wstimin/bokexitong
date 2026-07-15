<template>
  <section class="admin-card">
    <div class="toolbar user-toolbar">
      <div><h2>操作日志</h2><p class="section-subtitle">查看管理员对文章、评论、用户、站点配置等关键资源的操作记录。</p></div>
      <div class="admin-filters">
        <el-input v-model="query.keyword" placeholder="搜索操作人或详情" class="filter-input" clearable @keyup.enter="search" @clear="search" />
        <el-select v-model="query.action" placeholder="动作" clearable class="filter-select" @change="search"><el-option v-for="item in actionOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select>
        <el-select v-model="query.targetType" placeholder="对象" clearable class="filter-select" @change="search"><el-option v-for="item in targetOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select>
        <button class="btn-ghost" type="button" :disabled="loading" @click="search">查询</button>
      </div>
    </div>

    <div class="table-scroll">
      <el-table v-loading="loading" :data="rows" border style="min-width: 1040px">
        <el-table-column label="动作" width="120"><template #default="{ row }"><el-tag :type="actionType(row.action)">{{ actionText(row.action) }}</el-tag></template></el-table-column>
        <el-table-column label="对象" width="130"><template #default="{ row }">{{ targetText(row.targetType) }}</template></el-table-column>
        <el-table-column prop="targetId" label="对象 ID" width="110" />
        <el-table-column prop="operatorName" label="操作人" width="150" show-overflow-tooltip />
        <el-table-column prop="detail" label="详情" min-width="300" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="操作时间" width="190" />
      </el-table>
    </div>

    <div class="pager"><el-pagination background layout="prev, pager, next" :total="total" :page-size="query.size" v-model:current-page="query.current" @current-change="load" /></div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { adminApi } from '../../api/blog'

const actionOptions = [{ label: '新增', value: 'CREATE' }, { label: '更新', value: 'UPDATE' }, { label: '审核', value: 'AUDIT' }, { label: '删除', value: 'DELETE' }, { label: '测试', value: 'TEST' }, { label: '启用', value: 'ENABLE' }, { label: '禁用', value: 'DISABLE' }, { label: '重置密码', value: 'RESET_PASSWORD' }]
const targetOptions = [{ label: '文章', value: 'ARTICLE' }, { label: '评论', value: 'COMMENT' }, { label: '用户', value: 'USER' }, { label: '分类', value: 'CATEGORY' }, { label: '标签', value: 'TAG' }, { label: '图片', value: 'IMAGE' }, { label: '站点设置', value: 'SITE_SETTING' }, { label: '邮件设置', value: 'MAIL_SETTING' }]
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 10, keyword: '', action: '', targetType: '' })
const cleanQuery = () => ({ ...query, keyword: query.keyword?.trim() || undefined, action: query.action || undefined, targetType: query.targetType || undefined })
const load = async () => { loading.value = true; try { const res = await adminApi.operationLogs(cleanQuery()); rows.value = res.data.records || []; total.value = res.data.total || 0 } catch (error) { console.error(error) } finally { loading.value = false } }
const search = () => { query.current = 1; load() }
const actionText = (action) => Object.fromEntries(actionOptions.map((item) => [item.value, item.label]))[action] || action || '-'
const targetText = (targetType) => Object.fromEntries(targetOptions.map((item) => [item.value, item.label]))[targetType] || targetType || '-'
const actionType = (action) => ({ CREATE: 'success', UPDATE: 'primary', AUDIT: 'warning', DELETE: 'danger', TEST: 'info', ENABLE: 'success', DISABLE: 'warning', RESET_PASSWORD: 'info' }[action] || 'info')
onMounted(load)
</script>

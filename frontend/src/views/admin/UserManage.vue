<template>
  <section class="admin-card">
    <div class="toolbar">
      <div class="admin-filters">
        <el-input v-model="query.keyword" placeholder="搜索用户名 / 昵称 / 邮箱" style="width: 240px" clearable @keyup.enter="load" />
        <el-select v-model="query.role" placeholder="角色" clearable style="width: 130px" @change="load">
          <el-option label="管理员" value="ADMIN" />
          <el-option label="普通用户" value="USER" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="load">
          <el-option label="正常" :value="1" />
          <el-option label="封禁" :value="0" />
        </el-select>
        <button class="btn-ghost" @click="load">查询</button>
      </div>
    </div>
    <el-table :data="rows" border>
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column label="角色" width="120">
        <template #default="{ row }"><el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'">{{ roleText(row.role) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'warning'">{{ row.status === 1 ? '正常' : '封禁' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="setStatus(row.id, row.status === 1 ? 0 : 1)">{{ row.status === 1 ? '封禁' : '解封' }}</el-button>
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
const total = ref(0)
const query = reactive({ current: 1, size: 10, keyword: '', role: '', status: null })
const load = async () => {
  const res = await adminApi.users({ ...query })
  rows.value = res.data.records || []
  total.value = res.data.total || 0
}
const setStatus = async (id, status) => { await adminApi.userStatus(id, status); load() }
const remove = async (id) => {
  await ElMessageBox.confirm('确认删除这个用户吗？', '删除用户', { type: 'warning' })
  await adminApi.deleteUser(id)
  ElMessage.success('用户已删除')
  load()
}
const roleText = (role) => ({ ADMIN: '管理员', USER: '普通用户' }[role] || role || '-')
onMounted(load)
</script>

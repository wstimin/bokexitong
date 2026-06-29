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
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column label="角色" width="120">
        <template #default="{ row }"><el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'">{{ roleText(row.role) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'warning'">{{ row.status === 1 ? '正常' : '封禁' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" @click="openPassword(row)">重置密码</el-button>
          <el-button size="small" @click="setStatus(row.id, row.status === 1 ? 0 : 1)">{{ row.status === 1 ? '封禁' : '解封' }}</el-button>
          <el-button size="small" type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="query.size" v-model:current-page="query.current" @current-change="load" />
    </div>

    <el-dialog v-model="editVisible" title="编辑用户" width="520px">
      <el-form label-position="top">
        <el-form-item label="昵称"><el-input v-model="editForm.nickname" /></el-form-item>
        <el-form-item label="头像 URL"><el-input v-model="editForm.avatar" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="editForm.email" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.role" style="width: 100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="普通用户" value="USER" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态"><el-switch v-model="statusBool" active-text="正常" inactive-text="封禁" /></el-form-item>
      </el-form>
      <template #footer>
        <button class="btn-ghost" @click="editVisible = false">取消</button>
        <button class="btn-primary" @click="saveEdit">保存</button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordVisible" title="重置密码" width="420px">
      <el-form label-position="top">
        <el-form-item label="新密码"><el-input v-model="newPassword" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <button class="btn-ghost" @click="passwordVisible = false">取消</button>
        <button class="btn-primary" @click="resetPassword">保存</button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '../../api/blog'

const rows = ref([])
const total = ref(0)
const editVisible = ref(false)
const passwordVisible = ref(false)
const currentId = ref(null)
const newPassword = ref('')
const query = reactive({ current: 1, size: 10, keyword: '', role: '', status: null })
const editForm = reactive({ nickname: '', avatar: '', email: '', role: 'USER', status: 1 })
const statusBool = computed({
  get: () => editForm.status === 1,
  set: (value) => { editForm.status = value ? 1 : 0 }
})
const load = async () => {
  const res = await adminApi.users({ ...query })
  rows.value = res.data.records || []
  total.value = res.data.total || 0
}
const openEdit = (row) => {
  currentId.value = row.id
  Object.assign(editForm, { nickname: row.nickname || '', avatar: row.avatar || '', email: row.email || '', role: row.role || 'USER', status: row.status ?? 1 })
  editVisible.value = true
}
const saveEdit = async () => {
  await adminApi.updateUser(currentId.value, { ...editForm })
  editVisible.value = false
  ElMessage.success('用户资料已更新')
  load()
}
const openPassword = (row) => {
  currentId.value = row.id
  newPassword.value = ''
  passwordVisible.value = true
}
const resetPassword = async () => {
  await adminApi.resetUserPassword(currentId.value, { newPassword: newPassword.value })
  passwordVisible.value = false
  ElMessage.success('密码已重置')
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

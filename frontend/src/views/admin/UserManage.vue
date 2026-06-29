<template>
  <section class="admin-card user-manage-card">
    <div class="toolbar user-toolbar">
      <div>
        <h2>用户权限管理</h2>
        <p class="section-subtitle">管理用户资料、角色、状态和登录密码</p>
      </div>
      <div class="admin-filters">
        <el-input
          v-model="query.keyword"
          placeholder="搜索用户名 / 昵称 / 邮箱"
          class="filter-input"
          clearable
          @keyup.enter="searchUsers"
          @clear="searchUsers"
        />
        <el-select v-model="query.role" placeholder="角色" clearable class="filter-select" @change="searchUsers">
          <el-option label="管理员" value="ADMIN" />
          <el-option label="普通用户" value="USER" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable class="filter-select" @change="searchUsers">
          <el-option label="正常" :value="1" />
          <el-option label="封禁" :value="0" />
        </el-select>
        <button class="btn-ghost" :disabled="loading" @click="searchUsers">查询</button>
      </div>
    </div>

    <div class="table-scroll">
      <el-table v-loading="loading" :data="rows" border style="min-width: 980px">
        <el-table-column prop="username" label="用户名" min-width="130" show-overflow-tooltip />
        <el-table-column prop="nickname" label="昵称" min-width="130" show-overflow-tooltip />
        <el-table-column prop="email" label="邮箱" min-width="190" show-overflow-tooltip />
        <el-table-column label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'">{{ roleText(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'">{{ row.status === 1 ? '正常' : '封禁' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <div class="action-row">
              <el-button size="small" :disabled="actionLoading" @click="openEdit(row)">编辑</el-button>
              <el-button size="small" :disabled="actionLoading" @click="openPassword(row)">重置密码</el-button>
              <el-button size="small" :disabled="actionLoading" @click="setStatus(row.id, row.status === 1 ? 0 : 1)">
                {{ row.status === 1 ? '封禁' : '解封' }}
              </el-button>
              <el-button size="small" type="danger" :disabled="actionLoading" @click="remove(row.id)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="pager">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="query.size"
        v-model:current-page="query.current"
        @current-change="load"
      />
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
        <button class="btn-ghost" :disabled="actionLoading" @click="editVisible = false">取消</button>
        <button class="btn-primary" :disabled="actionLoading" @click="saveEdit">保存</button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordVisible" title="重置密码" width="420px">
      <el-form label-position="top">
        <el-form-item label="新密码"><el-input v-model="newPassword" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <button class="btn-ghost" :disabled="actionLoading" @click="passwordVisible = false">取消</button>
        <button class="btn-primary" :disabled="actionLoading" @click="resetPassword">保存</button>
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
const loading = ref(false)
const actionLoading = ref(false)
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

const cleanQuery = () => ({
  ...query,
  keyword: query.keyword?.trim() || undefined,
  role: query.role || undefined,
  status: query.status ?? undefined
})

const load = async () => {
  loading.value = true
  try {
    const res = await adminApi.users(cleanQuery())
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const searchUsers = () => {
  query.current = 1
  load()
}

const openEdit = (row) => {
  currentId.value = row.id
  Object.assign(editForm, {
    nickname: row.nickname || '',
    avatar: row.avatar || '',
    email: row.email || '',
    role: row.role || 'USER',
    status: row.status ?? 1
  })
  editVisible.value = true
}

const saveEdit = async () => {
  actionLoading.value = true
  try {
    await adminApi.updateUser(currentId.value, { ...editForm })
    editVisible.value = false
    ElMessage.success('用户资料已更新')
    await load()
  } finally {
    actionLoading.value = false
  }
}

const openPassword = (row) => {
  currentId.value = row.id
  newPassword.value = ''
  passwordVisible.value = true
}

const resetPassword = async () => {
  if (!newPassword.value) {
    ElMessage.warning('请输入新密码')
    return
  }
  actionLoading.value = true
  try {
    await adminApi.resetUserPassword(currentId.value, { newPassword: newPassword.value })
    passwordVisible.value = false
    ElMessage.success('密码已重置')
  } finally {
    actionLoading.value = false
  }
}

const setStatus = async (id, status) => {
  actionLoading.value = true
  try {
    await adminApi.userStatus(id, status)
    await load()
  } finally {
    actionLoading.value = false
  }
}

const remove = async (id) => {
  await ElMessageBox.confirm('确认删除这个用户吗？删除后无法恢复。', '删除用户', { type: 'warning' })
  actionLoading.value = true
  try {
    await adminApi.deleteUser(id)
    ElMessage.success('用户已删除')
    await load()
  } finally {
    actionLoading.value = false
  }
}

const roleText = (role) => ({ ADMIN: '管理员', USER: '普通用户' }[role] || role || '-')

onMounted(load)
</script>

<template>
  <section class="admin-card">
    <el-table :data="rows" border>
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="role" label="角色" width="120" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button size="small" @click="setStatus(row.id, row.status === 1 ? 0 : 1)">{{ row.status === 1 ? '封禁' : '解封' }}</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { adminApi } from '../../api/blog'

const rows = ref([])
const load = async () => { rows.value = (await adminApi.users({ current: 1, size: 50 })).data.records || [] }
const setStatus = async (id, status) => { await adminApi.userStatus(id, status); load() }
onMounted(load)
</script>

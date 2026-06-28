<template>
  <section class="admin-card">
    <el-table :data="rows" border>
      <el-table-column prop="content" label="评论内容" min-width="260" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column label="操作" width="190">
        <template #default="{ row }">
          <el-button size="small" @click="audit(row.id, 'APPROVED')">通过</el-button>
          <el-button size="small" type="warning" @click="audit(row.id, 'REJECTED')">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { adminApi } from '../../api/blog'

const rows = ref([])
const load = async () => { rows.value = (await adminApi.comments({ current: 1, size: 50 })).data.records || [] }
const audit = async (id, status) => { await adminApi.auditComment(id, status); load() }
onMounted(load)
</script>

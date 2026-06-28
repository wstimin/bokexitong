<template>
  <section class="admin-card">
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="搜索标题" style="width: 240px" @keyup.enter="load" />
      <button class="btn-ghost" @click="load">查询</button>
    </div>
    <el-table :data="rows" border>
      <el-table-column prop="title" label="标题" min-width="220" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="viewCount" label="浏览" width="90" />
      <el-table-column prop="likeCount" label="点赞" width="90" />
      <el-table-column prop="createdAt" label="创建时间" width="180" />
    </el-table>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { articleApi } from '../../api/blog'

const rows = ref([])
const query = reactive({ keyword: '' })
const load = async () => {
  const res = await articleApi.page({ current: 1, size: 20, keyword: query.keyword })
  rows.value = res.data.records || []
}
onMounted(load)
</script>

<template>
  <div class="editor-grid" style="padding: 0">
    <section class="admin-card">
      <div class="toolbar">
        <h2>分类管理</h2>
        <button class="btn-primary" @click="saveCategory">新增分类</button>
      </div>
      <el-form label-position="top">
        <el-form-item label="分类名称"><el-input v-model="category.name" placeholder="例如：技术笔记" /></el-form-item>
        <el-form-item label="分类描述"><el-input v-model="category.description" placeholder="这个分类主要收录什么内容" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="category.sort" :min="0" /></el-form-item>
      </el-form>
      <el-table :data="categories" border>
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="90" />
        <el-table-column label="操作" width="110">
          <template #default="{ row }"><el-button size="small" type="danger" @click="removeCategory(row.id)">删除</el-button></template>
        </el-table-column>
      </el-table>
    </section>

    <section class="admin-card">
      <div class="toolbar">
        <h2>标签管理</h2>
        <button class="btn-primary" @click="saveTag">新增标签</button>
      </div>
      <el-form label-position="top">
        <el-form-item label="标签名称"><el-input v-model="tag.name" placeholder="例如：Vue" /></el-form-item>
        <el-form-item label="标签颜色"><el-color-picker v-model="tag.color" /></el-form-item>
      </el-form>
      <el-table :data="tags" border>
        <el-table-column prop="name" label="名称" />
        <el-table-column label="颜色" width="130">
          <template #default="{ row }"><span class="anime-tag" :style="{ color: row.color }">{{ row.color }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="110">
          <template #default="{ row }"><el-button size="small" type="danger" @click="removeTag(row.id)">删除</el-button></template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '../../api/blog'

const categories = ref([])
const tags = ref([])
const category = reactive({ name: '', description: '', sort: 0 })
const tag = reactive({ name: '', color: '#ff77b7' })

const load = async () => {
  categories.value = (await adminApi.categories({ current: 1, size: 50 })).data.records || []
  tags.value = (await adminApi.tags({ current: 1, size: 50 })).data.records || []
}

const saveCategory = async () => {
  if (!category.name.trim()) return ElMessage.warning('请填写分类名称')
  await adminApi.saveCategory({ ...category })
  category.name = ''
  category.description = ''
  category.sort = 0
  ElMessage.success('分类已保存')
  load()
}

const saveTag = async () => {
  if (!tag.name.trim()) return ElMessage.warning('请填写标签名称')
  await adminApi.saveTag({ ...tag })
  tag.name = ''
  ElMessage.success('标签已保存')
  load()
}

const removeCategory = async (id) => {
  await ElMessageBox.confirm('确认删除这个分类吗？', '删除分类', { type: 'warning' })
  await adminApi.deleteCategory(id)
  ElMessage.success('分类已删除')
  load()
}

const removeTag = async (id) => {
  await ElMessageBox.confirm('确认删除这个标签吗？', '删除标签', { type: 'warning' })
  await adminApi.deleteTag(id)
  ElMessage.success('标签已删除')
  load()
}

onMounted(load)
</script>

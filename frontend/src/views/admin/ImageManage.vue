<template>
  <section class="admin-card">
    <div class="toolbar">
      <div style="display: flex; gap: 10px; align-items: center">
        <el-select v-model="query.type" placeholder="用途筛选" clearable style="width: 170px" @change="load">
          <el-option label="首页横幅 HERO" value="HERO" />
          <el-option label="文章封面 COVER" value="COVER" />
          <el-option label="头像 AVATAR" value="AVATAR" />
          <el-option label="推荐图 RECOMMEND" value="RECOMMEND" />
        </el-select>
        <button class="btn-ghost" @click="load">刷新</button>
      </div>
      <button class="btn-primary" @click="open()">新增图片 URL</button>
    </div>

    <el-table :data="rows" border>
      <el-table-column label="预览" width="140">
        <template #default="{ row }"><img class="image-preview" :src="row.url" :alt="row.title" /></template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="150" />
      <el-table-column prop="type" label="用途" width="130" />
      <el-table-column prop="url" label="图片 URL" min-width="260" show-overflow-tooltip />
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column label="启用" width="90">
        <template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="open(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" title="图片 URL" width="560px">
      <el-form label-position="top">
        <el-form-item label="标题"><el-input v-model="form.title" placeholder="例如：首页二次元横幅" /></el-form-item>
        <el-form-item label="图片 URL"><el-input v-model="form.url" placeholder="https://example.com/banner.jpg" /></el-form-item>
        <el-form-item label="用途">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="首页横幅 HERO" value="HERO" />
            <el-option label="文章封面 COVER" value="COVER" />
            <el-option label="头像 AVATAR" value="AVATAR" />
            <el-option label="推荐图 RECOMMEND" value="RECOMMEND" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="enabledBool" /></el-form-item>
      </el-form>
      <img v-if="form.url" class="cover" :src="form.url" alt="图片预览" />
      <template #footer>
        <button class="btn-ghost" @click="visible = false">取消</button>
        <button class="btn-primary" @click="save">保存</button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { adminApi } from '../../api/blog'

const rows = ref([])
const visible = ref(false)
const query = reactive({ type: '' })
const form = reactive({ id: null, title: '', url: '', type: 'HERO', description: '', sort: 0, enabled: 1 })
const enabledBool = computed({
  get: () => form.enabled === 1,
  set: (value) => { form.enabled = value ? 1 : 0 }
})

const load = async () => {
  const res = await adminApi.images({ current: 1, size: 50, type: query.type })
  rows.value = res.data.records || []
}
const open = (row) => {
  Object.assign(form, row || { id: null, title: '', url: '', type: 'HERO', description: '', sort: 0, enabled: 1 })
  visible.value = true
}
const save = async () => {
  await adminApi.saveImage({ ...form })
  visible.value = false
  load()
}
const remove = async (id) => {
  await ElMessageBox.confirm('确认删除这条图片链接吗？')
  await adminApi.deleteImage(id)
  load()
}

onMounted(load)
</script>

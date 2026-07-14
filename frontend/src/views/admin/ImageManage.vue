<template>
  <section class="admin-card">
    <div class="toolbar">
      <div class="admin-filters">
        <el-select v-model="query.type" placeholder="用途筛选" clearable style="width: 170px" @change="load">
          <el-option v-for="item in imageTypes" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <button class="btn-ghost" :disabled="loading" @click="load">刷新</button>
      </div>
      <div class="hero-actions">
        <el-upload :show-file-list="false" :http-request="uploadAndOpen" accept="image/*">
          <button class="btn-ghost" type="button">上传图片</button>
        </el-upload>
        <button class="btn-primary" @click="open()">新增图片 URL</button>
      </div>
    </div>

    <div class="table-scroll">
      <el-table v-loading="loading" :data="rows" border style="min-width: 980px">
        <el-table-column label="预览" width="140">
          <template #default="{ row }">
            <img class="image-preview" :src="assetUrl(row.url)" :alt="row.title || '图片预览'" />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
        <el-table-column label="用途" width="160">
          <template #default="{ row }">{{ typeText(row.type) }}</template>
        </el-table-column>
        <el-table-column prop="url" label="图片 URL" min-width="260" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="启用" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <el-button size="small" @click="open(row)">编辑</el-button>
              <el-button size="small" @click="copyUrl(row.url)">复制</el-button>
              <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="visible" :title="form.id ? '编辑图片' : '新增图片'" width="580px">
      <el-form label-position="top">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="例如：首页横幅、站点 Logo" />
        </el-form-item>
        <el-form-item label="图片 URL">
          <div class="inline-field">
            <el-input v-model="form.url" placeholder="上传图片或粘贴图片地址" />
            <el-upload :show-file-list="false" :http-request="uploadIntoForm" accept="image/*">
              <button class="btn-ghost" type="button">上传</button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="用途">
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="item in imageTypes" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="enabledBool" /></el-form-item>
      </el-form>
      <img v-if="form.url" class="cover" :src="assetUrl(form.url)" alt="图片预览" />
      <template #footer>
        <button class="btn-ghost" @click="visible = false">取消</button>
        <button class="btn-primary" @click="save">保存</button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi, uploadApi } from '../../api/blog'
import { useSiteStore } from '../../stores/site'
import { normalizeAssetUrl } from '../../utils/assets'

const imageTypes = [
  { label: '站点 Logo LOGO', value: 'LOGO' },
  { label: '首页横幅 HERO', value: 'HERO' },
  { label: '全站背景 BACKGROUND', value: 'BACKGROUND' },
  { label: '文章封面 COVER', value: 'COVER' },
  { label: '头像 AVATAR', value: 'AVATAR' },
  { label: '推荐图 RECOMMEND', value: 'RECOMMEND' }
]

const rows = ref([])
const visible = ref(false)
const loading = ref(false)
const site = useSiteStore()
const query = reactive({ type: '' })
const form = reactive({ id: null, title: '', url: '', type: 'LOGO', description: '', sort: 0, enabled: 1 })
const enabledBool = computed({
  get: () => form.enabled === 1,
  set: (value) => { form.enabled = value ? 1 : 0 }
})

const emptyForm = () => ({ id: null, title: '', url: '', type: query.type || 'LOGO', description: '', sort: 0, enabled: 1 })

const load = async () => {
  loading.value = true
  try {
    const res = await adminApi.images({ current: 1, size: 50, type: query.type || undefined })
    rows.value = res.data.records || []
  } finally {
    loading.value = false
  }
}

const open = (row) => {
  Object.assign(form, row || emptyForm())
  visible.value = true
}

const save = async () => {
  if (!form.title?.trim() || !form.url?.trim()) {
    ElMessage.warning('请填写标题和图片 URL')
    return
  }
  await adminApi.saveImage({ ...form })
  if (form.type === 'LOGO') await site.loadSite(true)
  visible.value = false
  ElMessage.success('图片资源已保存')
  load()
}

const remove = async (row) => {
  await ElMessageBox.confirm('确认删除这条图片资源吗？', '删除图片', { type: 'warning' })
  await adminApi.deleteImage(row.id)
  if (row.type === 'LOGO') await site.loadSite(true)
  ElMessage.success('图片资源已删除')
  load()
}

const uploadIntoForm = async (options) => {
  const res = await uploadApi.file(options.file)
  form.url = res.data.url
  if (!form.title) form.title = fileTitle(res.data.name)
  ElMessage.success('上传成功')
}

const uploadAndOpen = async (options) => {
  Object.assign(form, emptyForm())
  const res = await uploadApi.file(options.file)
  form.url = res.data.url
  form.title = fileTitle(res.data.name)
  visible.value = true
  ElMessage.success('已上传，请确认用途后保存')
}

const copyUrl = async (url) => {
  if (!navigator.clipboard) {
    ElMessage.warning('当前浏览器不支持自动复制，请手动复制 URL')
    return
  }
  await navigator.clipboard.writeText(url)
  ElMessage.success('图片 URL 已复制')
}

const assetUrl = (url) => normalizeAssetUrl(url)
const fileTitle = (name) => String(name || '图片').replace(/\.[^.]+$/, '')
const typeText = (type) => imageTypes.find((item) => item.value === type)?.label || type || '-'

onMounted(load)
</script>

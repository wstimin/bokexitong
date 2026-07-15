<template>
  <section class="admin-card">
    <div class="toolbar">
      <div class="admin-filters">
        <el-select v-model="query.type" placeholder="用途筛选" clearable style="width: 170px" @change="load">
          <el-option v-for="item in imageTypes" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <button class="btn-ghost" type="button" :disabled="loading" @click="load">刷新</button>
      </div>
      <div class="hero-actions">
        <el-upload :show-file-list="false" :http-request="uploadAndOpen" accept="image/*">
          <button class="btn-ghost" type="button">上传图片</button>
        </el-upload>
        <button class="btn-primary" type="button" @click="open()">新增图片 URL</button>
      </div>
    </div>

    <div v-loading="loading" class="image-resource-grid">
      <article v-for="row in rows" :key="row.id" class="image-resource-card">
        <img class="image-resource-thumb" :src="assetUrl(row.url)" :alt="row.title || '图片预览'" />
        <div class="image-resource-body">
          <div class="image-resource-title">
            <h3>{{ row.title || '未命名图片' }}</h3>
            <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </div>
          <p>{{ typeText(row.type) }} · 排序 {{ row.sort ?? 0 }}</p>
          <div class="image-url" :title="row.url">{{ row.url }}</div>
          <div class="action-row image-actions">
            <el-button size="small" @click="open(row)">编辑</el-button>
            <el-button size="small" @click="copyUrl(row.url)">复制 URL</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </div>
        </div>
      </article>
    </div>
    <el-empty v-if="!loading && rows.length === 0" description="暂无图片资源" />

    <el-dialog v-model="visible" :title="form.id ? '编辑图片' : '新增图片'" width="620px" class="image-form-dialog">
      <div class="image-dialog-layout">
        <div class="image-dialog-preview">
          <img v-if="form.url" :src="assetUrl(form.url)" alt="图片预览" />
          <div v-else>暂无预览</div>
        </div>
        <el-form label-position="top" class="image-dialog-form">
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
      </div>
      <template #footer>
        <button class="btn-ghost" type="button" @click="visible = false">取消</button>
        <button class="btn-primary" type="button" @click="save">保存</button>
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

const emptyForm = () => ({ id: null, title: '', url: '', type: query.type || 'COVER', description: '', sort: 0, enabled: 1 })

const load = async () => {
  loading.value = true
  try {
    const res = await adminApi.images({ current: 1, size: 50, type: query.type || undefined })
    rows.value = res.data.records || []
  } catch (error) {
    console.error(error)
    ElMessage.error('图片资源加载失败')
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
  try {
    await adminApi.saveImage({ ...form })
    if (form.type === 'LOGO') await site.loadSite(true)
    visible.value = false
    ElMessage.success('图片资源已保存')
    await load()
  } catch (error) {
    console.error(error)
  }
}

const remove = async (row) => {
  await ElMessageBox.confirm('确认删除这条图片资源吗？', '删除图片', { type: 'warning' })
  try {
    await adminApi.deleteImage(row.id)
    if (row.type === 'LOGO') await site.loadSite(true)
    ElMessage.success('图片资源已删除')
    await load()
  } catch (error) {
    console.error(error)
  }
}

const uploadIntoForm = async (options) => {
  try {
    const res = await uploadApi.file(options.file)
    form.url = res.data.url
    if (!form.title) form.title = fileTitle(res.data.name)
    ElMessage.success('上传成功')
  } catch (error) {
    console.error(error)
  }
}

const uploadAndOpen = async (options) => {
  try {
    const res = await uploadApi.file(options.file)
    const payload = {
      ...emptyForm(),
      title: fileTitle(res.data.name),
      url: res.data.url
    }
    await adminApi.saveImage(payload)
    ElMessage.success('图片已上传并保存到资源库')
    await load()
  } catch (error) {
    console.error(error)
  }
}

const copyUrl = async (url) => {
  if (!navigator.clipboard) {
    ElMessage.warning('当前浏览器不支持自动复制，请手动复制 URL')
    return
  }
  try {
    await navigator.clipboard.writeText(url)
    ElMessage.success('图片 URL 已复制')
  } catch (error) {
    console.error(error)
  }
}

const assetUrl = (url) => normalizeAssetUrl(url)
const fileTitle = (name) => String(name || '图片').replace(/\.[^.]+$/, '')
const typeText = (type) => imageTypes.find((item) => item.value === type)?.label || type || '-'

onMounted(load)
</script>

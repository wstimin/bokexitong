<template>
  <div class="page">
    <PortalNav />
    <main class="shell user-dashboard">
      <section class="form-panel account-panel">
        <div class="section-title"><h2>用户中心</h2></div>
        <el-tabs v-model="accountTab">
          <el-tab-pane label="资料" name="profile">
            <el-form label-position="top">
              <el-form-item label="昵称"><el-input v-model="profile.nickname" /></el-form-item>
              <el-form-item label="头像 URL">
                <div class="inline-field">
                  <el-input v-model="profile.avatar" placeholder="上传头像或粘贴图片地址" />
                  <el-upload :show-file-list="false" :http-request="(options) => uploadProfileFile(options, 'avatar')" accept="image/*">
                    <button class="btn-ghost" type="button">上传</button>
                  </el-upload>
                </div>
              </el-form-item>
              <el-form-item label="邮箱"><el-input v-model="profile.email" /></el-form-item>
            </el-form>
            <button class="btn-primary" @click="saveProfile">保存资料</button>
          </el-tab-pane>
          <el-tab-pane label="密码" name="password">
            <el-form label-position="top">
              <el-form-item label="原密码"><el-input v-model="password.oldPassword" type="password" show-password /></el-form-item>
              <el-form-item label="新密码"><el-input v-model="password.newPassword" type="password" show-password /></el-form-item>
            </el-form>
            <button class="btn-primary" @click="changePassword">修改密码</button>
          </el-tab-pane>
        </el-tabs>
      </section>

      <section class="form-panel writer-panel">
        <div class="section-title">
          <h2>{{ editingId ? '编辑文章' : '发布文章' }}</h2>
          <span class="anime-tag">Markdown</span>
        </div>
        <el-form label-position="top">
          <el-form-item label="标题"><el-input v-model="articleForm.title" /></el-form-item>
          <el-form-item label="摘要"><el-input v-model="articleForm.summary" type="textarea" :rows="2" /></el-form-item>
          <el-form-item label="封面图">
            <div class="inline-field">
              <el-input v-model="articleForm.coverUrl" placeholder="上传封面或粘贴图片地址" />
              <el-upload :show-file-list="false" :http-request="(options) => uploadArticleFile(options, 'cover')" accept="image/*">
                <button class="btn-ghost" type="button">上传封面</button>
              </el-upload>
            </div>
          </el-form-item>
          <el-form-item label="正文">
            <el-input v-model="articleForm.content" type="textarea" :rows="15" placeholder="支持 Markdown，上传图片/视频/附件后会自动插入链接" />
          </el-form-item>
        </el-form>
        <div class="upload-row">
          <el-upload :show-file-list="false" :http-request="(options) => uploadArticleFile(options, 'image')" accept="image/*">
            <button class="btn-ghost" type="button">上传图片</button>
          </el-upload>
          <el-upload :show-file-list="false" :http-request="(options) => uploadArticleFile(options, 'video')" accept="video/*">
            <button class="btn-ghost" type="button">上传视频</button>
          </el-upload>
          <el-upload :show-file-list="false" :http-request="(options) => uploadArticleFile(options, 'file')">
            <button class="btn-ghost" type="button">上传附件</button>
          </el-upload>
        </div>
        <div class="hero-actions" style="margin-top: 14px">
          <button class="btn-primary" @click="saveArticle('PUBLISHED')">{{ editingId ? '保存并发布' : '发布' }}</button>
          <button class="btn-ghost" @click="saveArticle('DRAFT')">保存草稿</button>
          <button v-if="editingId" class="btn-ghost" @click="resetArticleForm">取消编辑</button>
        </div>
      </section>

      <section class="form-panel my-articles">
        <div class="toolbar">
          <h2>我的文章</h2>
          <button class="btn-ghost" @click="loadMine">刷新</button>
        </div>
        <el-table :data="mineRows" border>
          <el-table-column prop="title" label="标题" min-width="220" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }"><el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'">{{ statusText(row.status) }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="更新时间" width="180" />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="editArticle(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="removeArticle(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </main>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PortalNav from '../../components/PortalNav.vue'
import { articleApi, uploadApi, userApi } from '../../api/blog'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const accountTab = ref('profile')
const profile = reactive({ nickname: '', avatar: '', email: '' })
const password = reactive({ oldPassword: '', newPassword: '' })
const articleForm = reactive({ title: '', summary: '', coverUrl: '', content: '', contentType: 'MARKDOWN' })
const mineRows = ref([])
const editingId = ref(null)

const loadProfile = async () => {
  const res = await userApi.me()
  Object.assign(profile, { nickname: res.data.nickname || '', avatar: res.data.avatar || '', email: res.data.email || '' })
  auth.setUser(res.data)
}
const saveProfile = async () => {
  const res = await userApi.updateProfile({ ...profile })
  auth.setUser(res.data)
  ElMessage.success('资料已保存')
}
const changePassword = async () => {
  await userApi.changePassword({ ...password })
  password.oldPassword = ''
  password.newPassword = ''
  ElMessage.success('密码已修改')
}
const saveArticle = async (status) => {
  if (!articleForm.title.trim()) {
    ElMessage.warning('请填写文章标题')
    return
  }
  const payload = { ...articleForm, status }
  if (editingId.value) {
    await articleApi.update(editingId.value, payload)
  } else {
    await articleApi.save(payload)
  }
  ElMessage.success(status === 'PUBLISHED' ? '文章已发布' : '草稿已保存')
  resetArticleForm()
  loadMine()
}
const resetArticleForm = () => {
  editingId.value = null
  Object.assign(articleForm, { title: '', summary: '', coverUrl: '', content: '', contentType: 'MARKDOWN' })
}
const editArticle = (row) => {
  editingId.value = row.id
  Object.assign(articleForm, {
    title: row.title || '',
    summary: row.summary || '',
    coverUrl: row.coverUrl || '',
    content: row.content || '',
    contentType: row.contentType || 'MARKDOWN'
  })
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
const removeArticle = async (row) => {
  await ElMessageBox.confirm(`确认删除《${row.title}》吗？`, '删除文章', { type: 'warning' })
  await articleApi.removeMine(row.id)
  if (editingId.value === row.id) {
    resetArticleForm()
  }
  ElMessage.success('文章已删除')
  loadMine()
}
const uploadProfileFile = async (options, field) => {
  const res = await uploadApi.file(options.file)
  profile[field] = res.data.url
  ElMessage.success('上传成功')
}
const uploadArticleFile = async (options, type) => {
  const res = await uploadApi.file(options.file)
  const { url, name } = res.data
  if (type === 'cover') {
    articleForm.coverUrl = url
  } else if (type === 'image') {
    articleForm.content += `\n\n![${name}](${url})\n`
  } else if (type === 'video') {
    articleForm.content += `\n\n<video src="${url}" controls style="max-width:100%"></video>\n`
  } else {
    articleForm.content += `\n\n[${name}](${url})\n`
  }
  ElMessage.success('上传成功')
}
const loadMine = async () => {
  const res = await articleApi.mine({ current: 1, size: 20 })
  mineRows.value = res.data.records || []
}
const statusText = (status) => ({ PUBLISHED: '已发布', DRAFT: '草稿' }[status] || status || '-')

onMounted(() => {
  loadProfile()
  loadMine()
})
</script>

<template>
  <div class="page">
    <PortalNav />
    <main class="shell user-dashboard">
      <section class="form-panel user-quick-panel">
        <div class="section-title writer-title">
          <div>
            <h2>我的工作台</h2>
            <p class="section-subtitle">常用操作都在这里，写作、管理内容和账户设置可以直接进入。</p>
          </div>
          <button class="btn-primary" type="button" @click="focusWriter">写新文章</button>
        </div>
        <div class="user-action-grid">
          <button class="shortcut-item user-action-card" type="button" @click="focusWriter">
            <strong>写文章</strong>
            <span>编辑正文、插入图片、视频和附件。</span>
          </button>
          <button class="shortcut-item user-action-card" type="button" @click="switchActivity('articles')">
            <strong>我的文章</strong>
            <span>查看草稿、审核状态和发布记录。</span>
          </button>
          <button class="shortcut-item user-action-card" type="button" @click="switchActivity('favorites')">
            <strong>我的收藏</strong>
            <span>回看收藏过的公开文章。</span>
          </button>
          <button class="shortcut-item user-action-card" type="button" @click="switchActivity('comments')">
            <strong>我的评论</strong>
            <span>查看评论审核结果和关联文章。</span>
          </button>
          <button class="shortcut-item user-action-card" type="button" @click="switchAccount('profile')">
            <strong>个人资料</strong>
            <span>修改昵称、头像和邮箱。</span>
          </button>
          <button class="shortcut-item user-action-card" type="button" @click="switchAccount('password')">
            <strong>修改密码</strong>
            <span>定期更新登录密码。</span>
          </button>
        </div>
      </section>

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

      <section ref="writerPanel" class="form-panel writer-panel">
        <div class="section-title writer-title">
          <div>
            <h2>{{ editingId ? '编辑文章' : '写新文章' }}</h2>
            <p class="section-subtitle">保存草稿只有你自己可见，提交审核后由管理员发布。</p>
          </div>
          <span class="anime-tag">Markdown</span>
        </div>

        <el-form label-position="top">
          <el-form-item label="标题">
            <el-input v-model="articleForm.title" maxlength="180" show-word-limit placeholder="给文章起一个清楚的标题" />
          </el-form-item>
          <el-form-item label="摘要">
            <el-input v-model="articleForm.summary" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="用一两句话介绍这篇文章" />
          </el-form-item>

          <div class="writer-meta-grid">
            <el-form-item label="分类">
              <el-select v-model="articleForm.categoryId" placeholder="选择分类" clearable filterable>
                <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="标签">
              <el-select v-model="articleForm.tagIds" placeholder="选择标签" multiple clearable filterable collapse-tags collapse-tags-tooltip>
                <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
              </el-select>
            </el-form-item>
          </div>

          <el-form-item label="封面图">
            <div class="cover-picker">
              <img v-if="articleForm.coverUrl" :src="coverPreviewSrc" alt="文章封面" />
              <div v-else class="cover-empty">封面预览</div>
              <div class="cover-fields">
                <el-input v-model="articleForm.coverUrl" placeholder="上传封面或粘贴图片地址" />
                <el-upload :show-file-list="false" :http-request="(options) => uploadArticleFile(options, 'cover')" accept="image/*">
                  <button class="btn-ghost" type="button">上传封面</button>
                </el-upload>
              </div>
            </div>
          </el-form-item>

          <el-form-item label="正文">
            <el-tabs v-model="writerMode" class="writer-mode-tabs">
              <el-tab-pane label="编辑" name="edit">
                <el-input v-model="articleForm.content" type="textarea" :rows="18" placeholder="从这里开始写正文。可以直接写普通文字，也可以用下方按钮插入图片、视频、附件、小标题和列表。" />
              </el-tab-pane>
              <el-tab-pane label="预览" name="preview">
                <div class="writer-preview-box markdown" v-html="previewHtml"></div>
              </el-tab-pane>
            </el-tabs>
          </el-form-item>
        </el-form>

        <div class="upload-row writer-tools">
          <button class="btn-ghost" type="button" @click="insertSnippet('**加粗文字**')">加粗</button>
          <button class="btn-ghost" type="button" @click="insertSnippet('\n\n> 引用内容\n')">引用</button>
          <el-upload :show-file-list="false" :http-request="(options) => uploadArticleFile(options, 'image')" accept="image/*">
            <button class="btn-ghost" type="button">插入图片</button>
          </el-upload>
          <el-upload :show-file-list="false" :http-request="(options) => uploadArticleFile(options, 'video')" accept="video/*">
            <button class="btn-ghost" type="button">插入视频</button>
          </el-upload>
          <el-upload :show-file-list="false" :http-request="(options) => uploadArticleFile(options, 'file')">
            <button class="btn-ghost" type="button">插入附件</button>
          </el-upload>
          <button class="btn-ghost" type="button" @click="insertSnippet('\n\n## 小标题\n')">小标题</button>
          <button class="btn-ghost" type="button" @click="insertSnippet('\n\n- 列表项\n')">列表</button>
          <button class="btn-ghost" type="button" @click="insertSnippet('\n\n```\n代码写在这里\n```\n')">代码块</button>
        </div>

        <div class="hero-actions writer-actions">
          <button class="btn-primary" @click="saveArticle('PENDING')">提交审核</button>
          <button class="btn-ghost" @click="saveArticle('DRAFT')">保存草稿</button>
          <button v-if="editingId" class="btn-ghost" @click="resetArticleForm">取消编辑</button>
        </div>
      </section>

      <section ref="activityPanel" class="form-panel my-articles">
        <div class="toolbar">
          <h2>我的内容</h2>
          <button class="btn-ghost" @click="refreshActivity">刷新</button>
        </div>
        <el-tabs v-model="activityTab" class="user-activity-tabs">
          <el-tab-pane label="我的文章" name="articles">
            <div class="content-filters">
              <el-input v-model="articleQuery.keyword" class="filter-input" placeholder="搜索我的文章" clearable @keyup.enter="loadMine(1)" @clear="loadMine(1)" />
              <el-select v-model="articleQuery.status" class="filter-select" placeholder="全部状态" clearable @change="loadMine(1)">
                <el-option label="草稿" value="DRAFT" />
                <el-option label="待审核" value="PENDING" />
                <el-option label="已发布" value="PUBLISHED" />
                <el-option label="已驳回" value="REJECTED" />
                <el-option label="已下架" value="OFFLINE" />
              </el-select>
              <button class="btn-ghost" @click="loadMine(1)">查询</button>
            </div>
            <div class="table-scroll">
              <el-table :data="mineRows" border style="min-width: 940px">
                <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
                <el-table-column label="状态" width="110">
                  <template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag></template>
                </el-table-column>
                <el-table-column label="处理说明" min-width="220" show-overflow-tooltip>
                  <template #default="{ row }">
                    <span :class="row.reviewReason ? 'review-note has-note' : 'review-note'">{{ row.reviewReason || '-' }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="更新时间" width="180">
                  <template #default="{ row }">{{ formatDate(row.updatedAt || row.createdAt) }}</template>
                </el-table-column>
                <el-table-column label="操作" width="220" fixed="right">
                  <template #default="{ row }">
                    <div class="action-row">
                      <el-button size="small" @click="editArticle(row)">编辑</el-button>
                      <el-button size="small" type="danger" @click="removeArticle(row)">删除</el-button>
                    </div>
                  </template>
                </el-table-column>
              </el-table>
            </div>
            <div class="pager">
              <el-pagination
                v-model:current-page="articlePage.current"
                v-model:page-size="articlePage.size"
                background
                layout="total, sizes, prev, pager, next"
                :page-sizes="[10, 20, 30]"
                :total="articlePage.total"
                @size-change="loadMine(1)"
                @current-change="loadMine"
              />
            </div>
          </el-tab-pane>

          <el-tab-pane label="我的收藏" name="favorites">
            <div class="table-scroll">
              <el-table :data="favoriteRows" border style="min-width: 860px">
                <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
                <el-table-column prop="categoryName" label="分类" width="130" show-overflow-tooltip />
                <el-table-column label="发布时间" width="180">
                  <template #default="{ row }">{{ formatDate(row.publishedAt || row.createdAt) }}</template>
                </el-table-column>
                <el-table-column label="数据" min-width="180">
                  <template #default="{ row }">
                    <span class="meta">阅读 {{ row.viewCount || 0 }} · 点赞 {{ row.likeCount || 0 }} · 收藏 {{ row.favoriteCount || 0 }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="120" fixed="right">
                  <template #default="{ row }">
                    <el-button size="small" @click="openArticle(row.id)">阅读</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
            <div class="pager">
              <el-pagination
                v-model:current-page="favoritePage.current"
                v-model:page-size="favoritePage.size"
                background
                layout="total, sizes, prev, pager, next"
                :page-sizes="[10, 20, 30]"
                :total="favoritePage.total"
                @size-change="loadFavorites(1)"
                @current-change="loadFavorites"
              />
            </div>
          </el-tab-pane>

          <el-tab-pane label="我的评论" name="comments">
            <div class="table-scroll">
              <el-table :data="commentRows" border style="min-width: 1040px">
                <el-table-column prop="content" label="评论内容" min-width="260" show-overflow-tooltip />
                <el-table-column label="状态" width="110">
                  <template #default="{ row }"><el-tag :type="commentStatusType(row.status)">{{ commentStatusText(row.status) }}</el-tag></template>
                </el-table-column>
                <el-table-column label="审核说明" min-width="200" show-overflow-tooltip>
                  <template #default="{ row }">
                    <span :class="row.reviewReason ? 'review-note has-note' : 'review-note'">{{ row.reviewReason || '-' }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="articleTitle" label="文章" min-width="220" show-overflow-tooltip />
                <el-table-column label="文章状态" width="110">
                  <template #default="{ row }"><el-tag :type="statusType(row.articleStatus)">{{ statusText(row.articleStatus) }}</el-tag></template>
                </el-table-column>
                <el-table-column label="评论时间" width="180">
                  <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
                </el-table-column>
                <el-table-column label="操作" width="120" fixed="right">
                  <template #default="{ row }">
                    <el-button size="small" :disabled="row.articleStatus !== 'PUBLISHED'" @click="openArticle(row.articleId)">查看</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
            <div class="pager">
              <el-pagination
                v-model:current-page="commentPage.current"
                v-model:page-size="commentPage.size"
                background
                layout="total, sizes, prev, pager, next"
                :page-sizes="[10, 20, 30]"
                :total="commentPage.total"
                @size-change="loadComments(1)"
                @current-change="loadComments"
              />
            </div>
          </el-tab-pane>
        </el-tabs>
      </section>
    </main>
    <PortalFooter />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { marked } from 'marked'
import PortalNav from '../../components/PortalNav.vue'
import PortalFooter from '../../components/PortalFooter.vue'
import { articleApi, portalApi, uploadApi, userApi } from '../../api/blog'
import { useAuthStore } from '../../stores/auth'
import { normalizeAssetUrl } from '../../utils/assets'

const auth = useAuthStore()
const router = useRouter()
const accountTab = ref('profile')
const activityTab = ref('articles')
const writerMode = ref('edit')
const writerPanel = ref(null)
const activityPanel = ref(null)
const profile = reactive({ nickname: '', avatar: '', email: '' })
const password = reactive({ oldPassword: '', newPassword: '' })
const articleForm = reactive({ title: '', summary: '', coverUrl: '', content: '', contentType: 'MARKDOWN', categoryId: null, tagIds: [] })
const articleQuery = reactive({ keyword: '', status: '' })
const mineRows = ref([])
const favoriteRows = ref([])
const commentRows = ref([])
const articlePage = reactive({ current: 1, size: 10, total: 0 })
const favoritePage = reactive({ current: 1, size: 10, total: 0 })
const commentPage = reactive({ current: 1, size: 10, total: 0 })
const categories = ref([])
const tags = ref([])
const editingId = ref(null)
const previewHtml = computed(() => marked(articleForm.content || ''))
const coverPreviewSrc = computed(() => normalizeAssetUrl(articleForm.coverUrl))

const loadProfile = async () => {
  const res = await userApi.me()
  Object.assign(profile, { nickname: res.data.nickname || '', avatar: res.data.avatar || '', email: res.data.email || '' })
  auth.setUser(res.data)
}

const loadMeta = async () => {
  const res = await portalApi.home()
  categories.value = res.data.categories || []
  tags.value = res.data.tags || []
}

const saveProfile = async () => {
  const res = await userApi.updateProfile({ ...profile })
  auth.setUser(res.data)
  ElMessage.success('资料已保存')
}

const changePassword = async () => {
  if (!password.oldPassword || !password.newPassword) {
    ElMessage.warning('请填写原密码和新密码')
    return
  }
  await userApi.changePassword({ ...password })
  auth.clearPasswordRequired()
  password.oldPassword = ''
  password.newPassword = ''
  ElMessage.success('密码已修改')
}

const saveArticle = async (status) => {
  if (!articleForm.title.trim()) {
    ElMessage.warning('请填写文章标题')
    return
  }
  if (status === 'PENDING' && !articleForm.content.trim()) {
    ElMessage.warning('提交审核前请先填写正文')
    return
  }
  const payload = { ...articleForm, status }
  if (editingId.value) {
    await articleApi.update(editingId.value, payload)
  } else {
    await articleApi.save(payload)
  }
  ElMessage.success(status === 'PENDING' ? '文章已提交审核' : '草稿已保存')
  resetArticleForm()
  loadMine(1)
}

const resetArticleForm = () => {
  editingId.value = null
  Object.assign(articleForm, { title: '', summary: '', coverUrl: '', content: '', contentType: 'MARKDOWN', categoryId: null, tagIds: [] })
}

const editArticle = (row) => {
  editingId.value = row.id
  Object.assign(articleForm, {
    title: row.title || '',
    summary: row.summary || '',
    coverUrl: row.coverUrl || '',
    content: row.content || '',
    contentType: row.contentType || 'MARKDOWN',
    categoryId: row.categoryId || null,
    tagIds: row.tags?.map((tag) => tag.id) || []
  })
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const removeArticle = async (row) => {
  await ElMessageBox.confirm(`确认删除《${row.title}》吗？`, '删除文章', { type: 'warning' })
  await articleApi.removeMine(row.id)
  if (editingId.value === row.id) resetArticleForm()
  ElMessage.success('文章已删除')
  loadMine(Math.min(articlePage.current, pageCount(articlePage.total - 1, articlePage.size)))
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
    insertSnippet(`\n\n![${name}](${url})\n`)
  } else if (type === 'video') {
    insertSnippet(`\n\n<video src="${url}" controls style="max-width:100%"></video>\n`)
  } else {
    insertSnippet(`\n\n[${name}](${url})\n`)
  }
  ElMessage.success('上传成功')
}

const insertSnippet = (text) => {
  articleForm.content = `${articleForm.content || ''}${text}`
}

const loadMine = async (page = articlePage.current) => {
  articlePage.current = page
  const res = await articleApi.mine({
    current: articlePage.current,
    size: articlePage.size,
    keyword: articleQuery.keyword?.trim() || undefined,
    status: articleQuery.status || undefined
  })
  mineRows.value = res.data.records || []
  articlePage.total = res.data.total || 0
}

const loadFavorites = async (page = favoritePage.current) => {
  favoritePage.current = page
  const res = await userApi.favorites({ current: favoritePage.current, size: favoritePage.size })
  favoriteRows.value = res.data.records || []
  favoritePage.total = res.data.total || 0
}

const loadComments = async (page = commentPage.current) => {
  commentPage.current = page
  const res = await userApi.comments({ current: commentPage.current, size: commentPage.size })
  commentRows.value = res.data.records || []
  commentPage.total = res.data.total || 0
}

const refreshActivity = () => {
  loadMine(articlePage.current)
  loadFavorites(favoritePage.current)
  loadComments(commentPage.current)
}

const focusWriter = () => {
  resetArticleForm()
  writerPanel.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const switchActivity = (tab) => {
  activityTab.value = tab
  refreshActivity()
  activityPanel.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const switchAccount = (tab) => {
  accountTab.value = tab
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const pageCount = (total, size) => Math.max(1, Math.ceil(Math.max(total, 0) / size))

const openArticle = (id) => {
  if (!id) return
  router.push(`/article/${id}`)
}

const formatDate = (date) => String(date || '').slice(0, 16) || '-'

const statusText = (status) => ({
  DRAFT: '草稿',
  PENDING: '待审核',
  PUBLISHED: '已发布',
  REJECTED: '已驳回',
  OFFLINE: '已下架',
  DELETED: '已删除'
}[status] || status || '-')

const statusType = (status) => ({
  PUBLISHED: 'success',
  PENDING: 'warning',
  REJECTED: 'danger',
  OFFLINE: 'info',
  DELETED: 'danger',
  DRAFT: 'info'
}[status] || 'info')

const commentStatusText = (status) => ({
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回'
}[status] || status || '-')

const commentStatusType = (status) => ({
  APPROVED: 'success',
  PENDING: 'warning',
  REJECTED: 'danger'
}[status] || 'info')

onMounted(() => {
  loadProfile()
  loadMeta()
  refreshActivity()
})
</script>

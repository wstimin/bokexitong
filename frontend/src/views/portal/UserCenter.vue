<template>
  <div class="page">
    <PortalNav />
    <main class="shell user-workbench">
      <aside class="form-panel user-workbench-sidebar">
        <div class="user-profile-card">
          <img v-if="avatarPreviewSrc" class="user-avatar" :src="avatarPreviewSrc" alt="用户头像" />
          <div v-else class="user-avatar user-avatar-empty">{{ profile.nickname?.slice(0, 1) || '用' }}</div>
          <div>
            <strong>{{ profile.nickname || '用户' }}</strong>
            <span>{{ profile.email || '未填写邮箱' }}</span>
          </div>
        </div>
        <nav class="user-workbench-menu">
          <button
            v-for="item in workbenchMenus"
            :key="item.key"
            type="button"
            :class="['user-menu-item', { active: activeSection === item.key }]"
            @click="setSection(item.key)"
          >
            <strong>{{ item.title }}</strong>
            <span>{{ item.desc }}</span>
          </button>
        </nav>
      </aside>

      <div class="user-workbench-main">
        <section v-if="activeSection === 'dashboard'" class="form-panel user-section-panel">
          <div class="section-title writer-title">
            <div>
              <h2>我的工作台</h2>
              <p class="section-subtitle">在这里完成写作、资料维护和文章管理。</p>
            </div>
            <button class="btn-primary" type="button" @click="focusWriter">写新文章</button>
          </div>
          <div class="user-action-grid">
            <button class="shortcut-item user-action-card" type="button" @click="focusWriter">
              <strong>写文章</strong>
              <span>支持字体、字号、颜色、对齐、图片、视频和附件。</span>
            </button>
            <button class="shortcut-item user-action-card" type="button" @click="setSection('articles')">
              <strong>我的文章</strong>
              <span>查看草稿、审核状态和发布记录。</span>
            </button>
            <button class="shortcut-item user-action-card" type="button" @click="setSection('favorites')">
              <strong>我的收藏</strong>
              <span>回看收藏过的公开文章。</span>
            </button>
            <button class="shortcut-item user-action-card" type="button" @click="setSection('comments')">
              <strong>我的评论</strong>
              <span>查看评论审核结果和关联文章。</span>
            </button>
            <button class="shortcut-item user-action-card" type="button" @click="setSection('profile')">
              <strong>个人资料</strong>
              <span>修改昵称、头像和邮箱。</span>
            </button>
            <button class="shortcut-item user-action-card" type="button" @click="setSection('password')">
              <strong>修改密码</strong>
              <span>定期更新登录密码。</span>
            </button>
          </div>
        </section>

        <section v-if="activeSection === 'profile'" class="form-panel user-section-panel account-panel">
          <div class="section-title writer-title">
            <div>
              <h2>个人资料</h2>
              <p class="section-subtitle">维护昵称、头像和邮箱信息。</p>
            </div>
          </div>
          <el-form label-position="top" class="user-form-grid">
            <el-form-item label="昵称"><el-input v-model="profile.nickname" /></el-form-item>
            <el-form-item label="邮箱"><el-input v-model="profile.email" /></el-form-item>
            <el-form-item label="头像 URL" class="field-full">
              <div class="inline-field">
                <el-input v-model="profile.avatar" placeholder="上传头像或粘贴图片地址" />
                <el-upload :show-file-list="false" :http-request="(options) => uploadProfileFile(options, 'avatar')" accept="image/*">
                  <button class="btn-ghost" type="button">上传头像</button>
                </el-upload>
              </div>
            </el-form-item>
          </el-form>
          <button class="btn-primary" type="button" @click="saveProfile">保存资料</button>
        </section>

        <section v-if="activeSection === 'password'" class="form-panel user-section-panel account-panel">
          <div class="section-title writer-title">
            <div>
              <h2>修改密码</h2>
              <p class="section-subtitle">更新登录密码可以提升账号安全。</p>
            </div>
          </div>
          <el-form label-position="top" class="user-form-grid">
            <el-form-item label="原密码"><el-input v-model="password.oldPassword" type="password" show-password /></el-form-item>
            <el-form-item label="新密码"><el-input v-model="password.newPassword" type="password" show-password /></el-form-item>
          </el-form>
          <button class="btn-primary" type="button" @click="changePassword">修改密码</button>
        </section>

        <section v-if="activeSection === 'write'" class="form-panel user-section-panel writer-panel">
          <div class="section-title writer-title">
            <div>
              <h2>{{ editingId ? '编辑文章' : '写新文章' }}</h2>
              <p class="section-subtitle">草稿只有你自己可见，发布后会自动检测违禁词，合规内容可直接公开。</p>
            </div>
            <span class="anime-tag">富文本</span>
          </div>

          <el-form label-position="top">
            <el-form-item label="标题">
              <el-input v-model="articleForm.title" maxlength="180" show-word-limit placeholder="给文章起一个清晰的标题" />
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
              <div class="rich-editor-shell">
                <QuillEditor
                  v-model:content="articleForm.content"
                  theme="snow"
                  content-type="html"
                  class="rich-editor"
                  :toolbar="richToolbar"
                  @ready="onEditorReady"
                  @selectionChange="onSelectionChange"
                  placeholder="从这里开始写正文，可以调整字体、字号、颜色、对齐，也可以插入图片、视频和链接。"
                />
              </div>
            </el-form-item>
          </el-form>

          <div class="upload-row writer-tools">
            <button class="btn-ghost" type="button" @click="openArticleUpload('image')">插入图片</button>
            <button class="btn-ghost" type="button" @click="openArticleUpload('video')">插入视频</button>
            <button class="btn-ghost" type="button" @click="openArticleUpload('file')">插入附件</button>
            <input ref="articleImageInput" class="hidden-file-input" type="file" accept="image/*" @change="(event) => uploadArticleSelectedFile(event, 'image')" />
            <input ref="articleVideoInput" class="hidden-file-input" type="file" accept="video/*" @change="(event) => uploadArticleSelectedFile(event, 'video')" />
            <input ref="articleFileInput" class="hidden-file-input" type="file" @change="(event) => uploadArticleSelectedFile(event, 'file')" />
          </div>

          <div class="hero-actions writer-actions">
            <button class="btn-primary" type="button" @click="saveArticle('PUBLISHED')">发布文章</button>
            <button class="btn-ghost" type="button" @click="saveArticle('DRAFT')">保存草稿</button>
            <button v-if="editingId" class="btn-ghost" type="button" @click="resetArticleForm">取消编辑</button>
          </div>
        </section>

        <section v-if="activeSection === 'articles'" class="form-panel user-section-panel my-articles">
          <div class="toolbar">
            <div>
              <h2>我的文章</h2>
              <p class="section-subtitle">查看草稿、审核状态和发布记录。</p>
            </div>
            <button class="btn-ghost" type="button" @click="loadMine(articlePage.current)">刷新</button>
          </div>
          <div class="content-filters">
            <el-input v-model="articleQuery.keyword" class="filter-input" placeholder="搜索我的文章" clearable @keyup.enter="loadMine(1)" @clear="loadMine(1)" />
            <el-select v-model="articleQuery.status" class="filter-select" placeholder="全部状态" clearable @change="loadMine(1)">
              <el-option label="草稿" value="DRAFT" />
              <el-option label="待审核" value="PENDING" />
              <el-option label="已发布" value="PUBLISHED" />
              <el-option label="已驳回" value="REJECTED" />
              <el-option label="已下架" value="OFFLINE" />
            </el-select>
            <button class="btn-ghost" type="button" @click="loadMine(1)">查询</button>
          </div>
          <div class="table-scroll">
            <el-table :data="mineRows" border style="min-width: 940px">
              <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
              <el-table-column label="状态" width="110">
                <template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag></template>
              </el-table-column>
              <el-table-column label="审核说明" min-width="220" show-overflow-tooltip>
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
        </section>

        <section v-if="activeSection === 'favorites'" class="form-panel user-section-panel my-articles">
          <div class="toolbar">
            <div>
              <h2>我的收藏</h2>
              <p class="section-subtitle">回看收藏过的公开文章。</p>
            </div>
            <button class="btn-ghost" type="button" @click="loadFavorites(favoritePage.current)">刷新</button>
          </div>
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
        </section>

        <section v-if="activeSection === 'comments'" class="form-panel user-section-panel my-articles">
          <div class="toolbar">
            <div>
              <h2>我的评论</h2>
              <p class="section-subtitle">查看评论审核结果和关联文章。</p>
            </div>
            <button class="btn-ghost" type="button" @click="loadComments(commentPage.current)">刷新</button>
          </div>
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
        </section>
      </div>
    </main>
    <PortalFooter />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { QuillEditor } from '@vueup/vue-quill'
import PortalNav from '../../components/PortalNav.vue'
import PortalFooter from '../../components/PortalFooter.vue'
import { articleApi, portalApi, uploadApi, userApi } from '../../api/blog'
import { useAuthStore } from '../../stores/auth'
import { normalizeAssetUrl } from '../../utils/assets'
import { ensureRichTextFormats, fileSnippet, imageSnippet, insertHtmlSnippet, isEmptyHtml, richToolbar, toEditableHtml, videoSnippet } from '../../utils/richText'

const auth = useAuthStore()
const router = useRouter()
const activeSection = ref('dashboard')
const profile = reactive({ nickname: '', avatar: '', email: '' })
const password = reactive({ oldPassword: '', newPassword: '' })
const articleForm = reactive({ title: '', summary: '', coverUrl: '', content: '', contentType: 'HTML', categoryId: null, tagIds: [] })
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
const editorRef = ref(null)
const lastSelection = ref(null)
const articleImageInput = ref(null)
const articleVideoInput = ref(null)
const articleFileInput = ref(null)
const avatarPreviewSrc = computed(() => profile.avatar ? normalizeAssetUrl(profile.avatar) : '')
const coverPreviewSrc = computed(() => normalizeAssetUrl(articleForm.coverUrl))
const articleUploadInputs = {
  image: articleImageInput,
  video: articleVideoInput,
  file: articleFileInput
}
const workbenchMenus = [
  { key: 'dashboard', title: '工作台', desc: '常用入口和账号概览' },
  { key: 'write', title: '写文章', desc: '创作、排版和插入素材' },
  { key: 'articles', title: '我的文章', desc: '草稿、审核和发布记录' },
  { key: 'favorites', title: '我的收藏', desc: '收藏过的公开文章' },
  { key: 'comments', title: '我的评论', desc: '评论状态和关联文章' },
  { key: 'profile', title: '个人资料', desc: '昵称、头像和邮箱' },
  { key: 'password', title: '修改密码', desc: '更新登录密码' }
]

const loadProfile = async () => {
  try {
    const res = await userApi.me()
    Object.assign(profile, { nickname: res.data.nickname || '', avatar: res.data.avatar || '', email: res.data.email || '' })
    auth.setUser(res.data)
  } catch (error) {
    console.error(error)
  }
}

const loadMeta = async () => {
  try {
    const res = await portalApi.home()
    categories.value = res.data.categories || []
    tags.value = res.data.tags || []
  } catch (error) {
    console.error(error)
  }
}

const saveProfile = async () => {
  try {
    const res = await userApi.updateProfile({ ...profile })
    auth.setUser(res.data)
    ElMessage.success('资料已保存')
  } catch (error) {
    console.error(error)
  }
}

const changePassword = async () => {
  if (!password.oldPassword || !password.newPassword) {
    ElMessage.warning('请填写原密码和新密码')
    return
  }
  try {
    await userApi.changePassword({ ...password })
    auth.clearPasswordRequired()
    password.oldPassword = ''
    password.newPassword = ''
    ElMessage.success('密码已修改')
  } catch (error) {
    console.error(error)
  }
}

const saveArticle = async (status) => {
  if (!articleForm.title.trim()) {
    ElMessage.warning('请填写文章标题')
    return
  }
  if (status !== 'DRAFT' && isEmptyHtml(articleForm.content)) {
    ElMessage.warning('发布前请先写正文')
    return
  }
  const payload = { ...articleForm, contentType: 'HTML', status }
  try {
    let res
    if (editingId.value) {
      res = await articleApi.update(editingId.value, payload)
    } else {
      res = await articleApi.save(payload)
    }
    const savedArticle = res?.data || {}
    if (status === 'DRAFT') {
      ElMessage.success('草稿已保存')
    } else if (savedArticle.status === 'PUBLISHED') {
      ElMessage.success('文章已发布')
    } else {
      ElMessage.warning(savedArticle.reviewReason ? `内容含违禁词，已转入审核：${savedArticle.reviewReason}` : '内容已转入审核')
    }
    resetArticleForm()
    await loadMine(1)
    activeSection.value = 'articles'
  } catch (error) {
    console.error(error)
  }
}

const resetArticleForm = () => {
  editingId.value = null
  Object.assign(articleForm, { title: '', summary: '', coverUrl: '', content: '', contentType: 'HTML', categoryId: null, tagIds: [] })
}

const editArticle = async (row) => {
  await ensureRichTextFormats()
  editingId.value = row.id
  Object.assign(articleForm, {
    title: row.title || '',
    summary: row.summary || '',
    coverUrl: row.coverUrl || '',
    content: toEditableHtml(row.content, row.contentType),
    contentType: 'HTML',
    categoryId: row.categoryId || null,
    tagIds: row.tags?.map((tag) => tag.id) || []
  })
  activeSection.value = 'write'
}

const onEditorReady = (quill) => {
  editorRef.value = quill
  lastSelection.value = null
}

const onSelectionChange = ({ range }) => {
  if (range) lastSelection.value = range
}

const getInsertIndex = () => {
  const quill = editorRef.value
  if (quill) {
    const selection = quill.getSelection(true) || lastSelection.value
    if (selection) return selection.index
    return Math.max(quill.getLength() - 1, 0)
  }
  return 0
}

const removeArticle = async (row) => {
  await ElMessageBox.confirm(`确认删除《${row.title}》吗？`, '删除文章', { type: 'warning' })
  await articleApi.removeMine(row.id)
  if (editingId.value === row.id) resetArticleForm()
  ElMessage.success('文章已删除')
  loadMine(Math.min(articlePage.current, pageCount(articlePage.total - 1, articlePage.size)))
}

const uploadProfileFile = async (options, field) => {
  try {
    const res = await uploadApi.file(options.file)
    profile[field] = res.data.url
    options.onSuccess?.(res)
    ElMessage.success('上传成功')
  } catch (error) {
    console.error(error)
    options.onError?.(error)
  }
}

const openArticleUpload = (type) => {
  articleUploadInputs[type]?.value?.click()
}

const uploadArticleSelectedFile = async (event, type) => {
  const input = event.target
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  await uploadArticleFile({ file }, type)
}

const uploadArticleFile = async (options, type) => {
  try {
    const res = await uploadApi.file(options.file)
    const { url, name } = res.data
    if (type === 'cover') {
      articleForm.coverUrl = url
    } else if (type === 'image') {
      insertSnippet(imageSnippet(url, name))
    } else if (type === 'video') {
      insertSnippet(videoSnippet(url, name))
    } else {
      insertSnippet(fileSnippet(url, name))
    }
    options.onSuccess?.(res)
    ElMessage.success('上传成功')
  } catch (error) {
    console.error(error)
    options.onError?.(error)
  }
}

const insertSnippet = (text) => {
  const quill = editorRef.value
  if (quill && insertHtmlSnippet(quill, getInsertIndex(), text)) return
  articleForm.content = `${articleForm.content || ''}${text}`
}

const loadMine = async (page = articlePage.current) => {
  articlePage.current = page
  try {
    const res = await articleApi.mine({
      current: articlePage.current,
      size: articlePage.size,
      keyword: articleQuery.keyword?.trim() || undefined,
      status: articleQuery.status || undefined
    })
    mineRows.value = res.data.records || []
    articlePage.total = res.data.total || 0
  } catch (error) {
    console.error(error)
  }
}

const loadFavorites = async (page = favoritePage.current) => {
  favoritePage.current = page
  try {
    const res = await userApi.favorites({ current: favoritePage.current, size: favoritePage.size })
    favoriteRows.value = res.data.records || []
    favoritePage.total = res.data.total || 0
  } catch (error) {
    console.error(error)
  }
}

const loadComments = async (page = commentPage.current) => {
  commentPage.current = page
  try {
    const res = await userApi.comments({ current: commentPage.current, size: commentPage.size })
    commentRows.value = res.data.records || []
    commentPage.total = res.data.total || 0
  } catch (error) {
    console.error(error)
  }
}

const focusWriter = async () => {
  await ensureRichTextFormats()
  resetArticleForm()
  activeSection.value = 'write'
}

const setSection = async (section) => {
  if (section === 'write') await ensureRichTextFormats()
  activeSection.value = section
  if (section === 'articles') loadMine(articlePage.current)
  if (section === 'favorites') loadFavorites(favoritePage.current)
  if (section === 'comments') loadComments(commentPage.current)
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
  loadMine()
})
</script>

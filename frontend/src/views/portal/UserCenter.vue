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
          <button v-for="item in workbenchMenus" :key="item.key" type="button" :class="['user-menu-item', { active: activeSection === item.key }]" @click="setSection(item.key)">
            <strong>{{ item.title }}</strong>
            <span>{{ item.desc }}</span>
          </button>
        </nav>
      </aside>

      <div class="user-workbench-main">
        <section v-if="activeSection === 'dashboard'" class="form-panel user-section-panel">
          <div class="section-title writer-title">
            <div><h2>我的工作台</h2><p class="section-subtitle">在这里完成写作、资料维护和文章管理。</p></div>
            <button class="btn-primary" type="button" @click="focusWriter">写新文章</button>
          </div>
          <div class="user-action-grid">
            <button v-for="item in actionCards" :key="item.key" class="shortcut-item user-action-card" type="button" @click="item.action">
              <strong>{{ item.title }}</strong>
              <span>{{ item.desc }}</span>
            </button>
          </div>
        </section>

        <section v-if="activeSection === 'profile'" class="form-panel user-section-panel account-panel">
          <div class="section-title writer-title"><div><h2>个人资料</h2><p class="section-subtitle">维护昵称、头像和邮箱信息。</p></div></div>
          <el-form label-position="top" class="user-form-grid">
            <el-form-item label="昵称"><el-input v-model="profile.nickname" /></el-form-item>
            <el-form-item label="邮箱"><el-input v-model="profile.email" /></el-form-item>
            <el-form-item label="头像 URL" class="field-full">
              <div class="inline-field"><el-input v-model="profile.avatar" placeholder="上传头像或粘贴图片地址" /><FileUploadButton accept="image/*" @select="(file) => uploadProfileFile({ file }, 'avatar')">上传头像</FileUploadButton></div>
            </el-form-item>
          </el-form>
          <button class="btn-primary" type="button" @click="saveProfile">保存资料</button>
        </section>

        <section v-if="activeSection === 'password'" class="form-panel user-section-panel account-panel">
          <div class="section-title writer-title"><div><h2>修改密码</h2><p class="section-subtitle">更新登录密码可以提升账号安全。</p></div></div>
          <el-form label-position="top" class="user-form-grid">
            <el-form-item label="原密码"><el-input v-model="password.oldPassword" type="password" show-password /></el-form-item>
            <el-form-item label="新密码"><el-input v-model="password.newPassword" type="password" show-password /></el-form-item>
          </el-form>
          <button class="btn-primary" type="button" @click="changePassword">修改密码</button>
        </section>

        <section v-if="activeSection === 'write'" class="form-panel user-section-panel writer-panel">
          <div class="section-title writer-title">
            <div><h2>{{ editingId ? '编辑文章' : '写新文章' }}</h2><p class="section-subtitle">草稿只有你自己可见，发布后会自动检测违禁词，合规内容可直接公开。</p></div>
            <span class="anime-tag">富文本</span>
          </div>

          <el-form label-position="top" :disabled="articleSaving">
            <el-form-item label="标题"><el-input v-model="articleForm.title" maxlength="180" show-word-limit placeholder="给文章起一个清晰的标题" /></el-form-item>
            <el-form-item label="摘要"><el-input v-model="articleForm.summary" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="用一两句话介绍这篇文章" /></el-form-item>
            <div class="writer-meta-grid">
              <el-form-item label="分类"><el-select v-model="articleForm.categoryId" placeholder="选择分类" clearable filterable><el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
              <el-form-item label="标签"><el-select v-model="articleForm.tagIds" placeholder="选择标签" multiple clearable filterable collapse-tags collapse-tags-tooltip><el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" /></el-select></el-form-item>
            </div>
            <el-form-item label="封面图">
              <div class="cover-picker">
                <img v-if="articleForm.coverUrl" :src="coverPreviewSrc" alt="文章封面" />
                <div v-else class="cover-empty">封面预览</div>
                <div class="cover-fields"><el-input v-model="articleForm.coverUrl" placeholder="上传封面或粘贴图片地址" /><FileUploadButton accept="image/*" :disabled="articleSaving" @select="(file) => uploadArticleFile({ file }, 'cover')">上传封面</FileUploadButton></div>
              </div>
            </el-form-item>
            <el-form-item label="正文">
              <div class="rich-editor-shell"><QuillEditor v-model:content="articleForm.content" theme="snow" content-type="html" class="rich-editor" :toolbar="richToolbar" :enable="!articleSaving" @ready="onEditorReady" @selectionChange="onSelectionChange" placeholder="从这里开始写正文，可以调整字体、字号、颜色、对齐，也可以插入图片、视频和链接。" /></div>
            </el-form-item>
          </el-form>

          <div class="upload-row writer-tools">
            <button class="upload-trigger" type="button" :disabled="articleSaving" @click="openWriterUpload('image')">插入图片</button>
            <input ref="writerImageInput" class="upload-native-input" type="file" accept="image/*" :disabled="articleSaving" @change="(event) => handleWriterUpload(event, 'image')" />
            <button class="upload-trigger" type="button" :disabled="articleSaving" @click="openWriterUpload('video')">插入视频</button>
            <input ref="writerVideoInput" class="upload-native-input" type="file" accept="video/*" :disabled="articleSaving" @change="(event) => handleWriterUpload(event, 'video')" />
            <button class="upload-trigger" type="button" :disabled="articleSaving" @click="openWriterUpload('file')">插入附件</button>
            <input ref="writerFileInput" class="upload-native-input" type="file" :disabled="articleSaving" @change="(event) => handleWriterUpload(event, 'file')" />
            <span v-if="pendingUploads" class="upload-status">{{ pendingUploads }} 个文件上传中</span>
          </div>
          <div class="hero-actions writer-actions">
            <button class="btn-primary" type="button" :disabled="articleSaving || pendingUploads > 0" @click="saveArticle('PUBLISHED')">发布文章</button>
            <button class="btn-ghost" type="button" :disabled="articleSaving || pendingUploads > 0" @click="saveArticle('DRAFT')">保存草稿</button>
            <button v-if="editingId" class="btn-ghost" type="button" :disabled="articleSaving || pendingUploads > 0" @click="cancelArticleEdit">取消编辑</button>
          </div>
        </section>

        <section v-if="activeSection === 'articles'" class="form-panel user-section-panel my-articles">
          <div class="toolbar"><div><h2>我的文章</h2><p class="section-subtitle">查看草稿、审核状态和发布记录。</p></div><button class="btn-ghost" type="button" @click="loadMine(articlePage.current)">刷新</button></div>
          <div class="content-filters"><el-input v-model="articleQuery.keyword" class="filter-input" placeholder="搜索我的文章" clearable @keyup.enter="loadMine(1)" @clear="loadMine(1)" /><el-select v-model="articleQuery.status" class="filter-select" placeholder="全部状态" clearable @change="loadMine(1)"><el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select><button class="btn-ghost" type="button" @click="loadMine(1)">查询</button></div>
          <ArticleTable :rows="mineRows" :status-text="statusText" :status-type="statusType" @edit="editArticle" @remove="removeArticle" />
          <div class="pager"><el-pagination v-model:current-page="articlePage.current" v-model:page-size="articlePage.size" background layout="total, sizes, prev, pager, next" :page-sizes="[10, 20, 30]" :total="articlePage.total" @size-change="loadMine(1)" @current-change="loadMine" /></div>
        </section>

        <section v-if="activeSection === 'favorites'" class="form-panel user-section-panel my-articles">
          <div class="toolbar"><div><h2>我的收藏</h2><p class="section-subtitle">回看收藏过的公开文章。</p></div><button class="btn-ghost" type="button" @click="loadFavorites(favoritePage.current)">刷新</button></div>
          <div class="table-scroll"><el-table :data="favoriteRows" border style="min-width: 860px"><el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip /><el-table-column prop="categoryName" label="分类" width="130" show-overflow-tooltip /><el-table-column label="发布时间" width="180"><template #default="{ row }">{{ formatDate(row.publishedAt || row.createdAt) }}</template></el-table-column><el-table-column label="数据" min-width="180"><template #default="{ row }"><span class="meta">阅读 {{ row.viewCount || 0 }} · 点赞 {{ row.likeCount || 0 }} · 收藏 {{ row.favoriteCount || 0 }}</span></template></el-table-column><el-table-column label="操作" width="120" fixed="right"><template #default="{ row }"><el-button size="small" @click="openArticle(row.id)">阅读</el-button></template></el-table-column></el-table></div>
          <div class="pager"><el-pagination v-model:current-page="favoritePage.current" v-model:page-size="favoritePage.size" background layout="total, sizes, prev, pager, next" :page-sizes="[10, 20, 30]" :total="favoritePage.total" @size-change="loadFavorites(1)" @current-change="loadFavorites" /></div>
        </section>

        <section v-if="activeSection === 'comments'" class="form-panel user-section-panel my-articles">
          <div class="toolbar"><div><h2>我的评论</h2><p class="section-subtitle">查看评论审核结果和关联文章。</p></div><button class="btn-ghost" type="button" @click="loadComments(commentPage.current)">刷新</button></div>
          <div class="table-scroll"><el-table :data="commentRows" border style="min-width: 1040px"><el-table-column prop="content" label="评论内容" min-width="260" show-overflow-tooltip /><el-table-column label="状态" width="110"><template #default="{ row }"><el-tag :type="commentStatusType(row.status)">{{ commentStatusText(row.status) }}</el-tag></template></el-table-column><el-table-column label="审核说明" min-width="200" show-overflow-tooltip><template #default="{ row }"><span :class="row.reviewReason ? 'review-note has-note' : 'review-note'">{{ row.reviewReason || '-' }}</span></template></el-table-column><el-table-column prop="articleTitle" label="文章" min-width="220" show-overflow-tooltip /><el-table-column label="文章状态" width="110"><template #default="{ row }"><el-tag :type="statusType(row.articleStatus)">{{ statusText(row.articleStatus) }}</el-tag></template></el-table-column><el-table-column label="评论时间" width="180"><template #default="{ row }">{{ formatDate(row.createdAt) }}</template></el-table-column><el-table-column label="操作" width="120" fixed="right"><template #default="{ row }"><el-button size="small" :disabled="row.articleStatus !== 'PUBLISHED'" @click="openArticle(row.articleId)">查看</el-button></template></el-table-column></el-table></div>
          <div class="pager"><el-pagination v-model:current-page="commentPage.current" v-model:page-size="commentPage.size" background layout="total, sizes, prev, pager, next" :page-sizes="[10, 20, 30]" :total="commentPage.total" @size-change="loadComments(1)" @current-change="loadComments" /></div>
        </section>
      </div>
    </main>
    <el-dialog v-model="linkButtonVisible" title="插入链接按钮" width="460px" append-to-body>
      <el-form label-position="top">
        <el-form-item label="按钮文字"><el-input v-model="linkButtonForm.text" maxlength="60" placeholder="例如：查看文档" /></el-form-item>
        <el-form-item label="链接地址"><el-input v-model="linkButtonForm.href" placeholder="https://example.com 或 /article/1" /></el-form-item>
        <el-form-item label="按钮样式"><el-radio-group v-model="linkButtonForm.style"><el-radio-button value="primary">主要</el-radio-button><el-radio-button value="secondary">次要</el-radio-button><el-radio-button value="download">下载</el-radio-button></el-radio-group></el-form-item>
        <el-form-item><el-checkbox v-model="linkButtonForm.newWindow">在新窗口打开</el-checkbox></el-form-item>
      </el-form>
      <template #footer><button class="btn-ghost" type="button" @click="linkButtonVisible = false">取消</button><button class="btn-primary" type="button" @click="confirmLinkButton">插入</button></template>
    </el-dialog>
    <PortalFooter />
  </div>
</template>

<script setup>
import { computed, defineComponent, h, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { onBeforeRouteLeave, useRouter } from 'vue-router'
import { ElButton, ElMessage, ElMessageBox, ElTable, ElTableColumn, ElTag } from 'element-plus'
import { QuillEditor } from '@vueup/vue-quill'
import FileUploadButton from '../../components/FileUploadButton.vue'
import PortalNav from '../../components/PortalNav.vue'
import PortalFooter from '../../components/PortalFooter.vue'
import { articleApi, portalApi, uploadApi, userApi } from '../../api/blog'
import { useAuthStore } from '../../stores/auth'
import { normalizeAssetUrl } from '../../utils/assets'
import { ensureRichTextFormats, fileSnippet, imageSnippet, insertLinkButton, insertUploadPlaceholder, isEmptyHtml, removeUploadPlaceholder, replaceUploadPlaceholder, richToolbar, setupRichTextEditor, toEditableHtml, videoSnippet } from '../../utils/richText'

const ArticleTable = defineComponent({
  props: { rows: { type: Array, default: () => [] }, statusText: Function, statusType: Function },
  emits: ['edit', 'remove'],
  setup(props, { emit }) {
    return () => h('div', { class: 'table-scroll' }, h(ElTable, { data: props.rows, border: true, style: 'min-width: 940px' }, {
      default: () => [
        h(ElTableColumn, { prop: 'title', label: '标题', minWidth: 220, showOverflowTooltip: true }),
        h(ElTableColumn, { label: '状态', width: 110 }, { default: ({ row }) => h(ElTag, { type: props.statusType(row.status) }, () => props.statusText(row.status)) }),
        h(ElTableColumn, { label: '审核说明', minWidth: 220, showOverflowTooltip: true }, { default: ({ row }) => h('span', { class: row.reviewReason ? 'review-note has-note' : 'review-note' }, row.reviewReason || '-') }),
        h(ElTableColumn, { label: '更新时间', width: 180 }, { default: ({ row }) => formatDate(row.updatedAt || row.createdAt) }),
        h(ElTableColumn, { label: '操作', width: 220, fixed: 'right' }, { default: ({ row }) => h('div', { class: 'action-row' }, [h(ElButton, { size: 'small', onClick: () => emit('edit', row) }, () => '编辑'), h(ElButton, { size: 'small', type: 'danger', onClick: () => emit('remove', row) }, () => '删除')]) })
      ]
    }))
  }
})

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
const articleBaseline = ref('')
const pendingUploads = ref(0)
const articleSaving = ref(false)
const linkButtonVisible = ref(false)
const linkButtonRange = ref(null)
const linkButtonForm = reactive({ text: '', href: '', style: 'primary', newWindow: true })
const writerImageInput = ref(null)
const writerVideoInput = ref(null)
const writerFileInput = ref(null)
const writerUploadInputs = { image: writerImageInput, video: writerVideoInput, file: writerFileInput }
const avatarPreviewSrc = computed(() => profile.avatar ? normalizeAssetUrl(profile.avatar) : '')
const coverPreviewSrc = computed(() => normalizeAssetUrl(articleForm.coverUrl))
const statusOptions = [{ label: '草稿', value: 'DRAFT' }, { label: '待审核', value: 'PENDING' }, { label: '已发布', value: 'PUBLISHED' }, { label: '已驳回', value: 'REJECTED' }, { label: '已下架', value: 'OFFLINE' }]
const workbenchMenus = [
  { key: 'dashboard', title: '工作台', desc: '常用入口和账号概览' }, { key: 'write', title: '写文章', desc: '创作、排版和插入素材' }, { key: 'articles', title: '我的文章', desc: '草稿、审核和发布记录' }, { key: 'favorites', title: '我的收藏', desc: '收藏过的公开文章' }, { key: 'comments', title: '我的评论', desc: '评论状态和关联文章' }, { key: 'profile', title: '个人资料', desc: '昵称、头像和邮箱' }, { key: 'password', title: '修改密码', desc: '更新登录密码' }
]
const actionCards = computed(() => [
  { key: 'write', title: '写文章', desc: '支持字体、字号、颜色、对齐、图片、视频和附件。', action: focusWriter },
  { key: 'articles', title: '我的文章', desc: '查看草稿、审核状态和发布记录。', action: () => setSection('articles') },
  { key: 'favorites', title: '我的收藏', desc: '回看收藏过的公开文章。', action: () => setSection('favorites') },
  { key: 'comments', title: '我的评论', desc: '查看评论审核结果和关联文章。', action: () => setSection('comments') },
  { key: 'profile', title: '个人资料', desc: '修改昵称、头像和邮箱。', action: () => setSection('profile') },
  { key: 'password', title: '修改密码', desc: '定期更新登录密码。', action: () => setSection('password') }
])
const articleSignature = () => JSON.stringify({ ...articleForm, tagIds: [...articleForm.tagIds] })
const hasUnsavedArticle = computed(() => activeSection.value === 'write' && articleSignature() !== articleBaseline.value && Boolean(articleForm.title.trim() || articleForm.summary.trim() || articleForm.coverUrl || !isEmptyHtml(articleForm.content)))
const markArticleSaved = () => { articleBaseline.value = articleSignature() }
const confirmDiscardArticle = async () => {
  if (articleSaving.value) { ElMessage.warning('文章正在保存，请稍后再离开写作页'); return false }
  if (pendingUploads.value > 0) { ElMessage.warning('请等待文件上传完成后再离开写作页'); return false }
  if (!hasUnsavedArticle.value) return true
  try {
    await ElMessageBox.confirm('当前文章还有未保存的修改，确定离开吗？', '未保存的文章', { type: 'warning', confirmButtonText: '放弃修改', cancelButtonText: '继续编辑' })
    return true
  } catch {
    return false
  }
}
const cancelArticleEdit = async () => { if (await confirmDiscardArticle()) resetArticleForm() }

const loadProfile = async () => { try { const res = await userApi.me(); Object.assign(profile, { nickname: res.data.nickname || '', avatar: res.data.avatar || '', email: res.data.email || '' }); auth.setUser(res.data) } catch (error) { console.error(error) } }
const loadMeta = async () => { try { const res = await portalApi.home(); categories.value = res.data.categories || []; tags.value = res.data.tags || [] } catch (error) { console.error(error) } }
const saveProfile = async () => { try { const res = await userApi.updateProfile({ ...profile }); auth.setUser(res.data); ElMessage.success('资料已保存') } catch (error) { console.error(error) } }
const changePassword = async () => { if (!password.oldPassword || !password.newPassword) return ElMessage.warning('请填写原密码和新密码'); try { await userApi.changePassword({ ...password }); auth.clearPasswordRequired(); password.oldPassword = ''; password.newPassword = ''; ElMessage.success('密码已修改') } catch (error) { console.error(error) } }

const saveArticle = async (status) => {
  if (articleSaving.value) return
  if (pendingUploads.value > 0) return ElMessage.warning('请等待文件上传完成后再保存')
  if (!articleForm.title.trim()) return ElMessage.warning('请填写文章标题')
  if (status !== 'DRAFT' && isEmptyHtml(articleForm.content)) return ElMessage.warning('发布前请先写正文')
  const payload = { ...articleForm, contentType: 'HTML', status }
  articleSaving.value = true
  try {
    const res = editingId.value ? await articleApi.update(editingId.value, payload) : await articleApi.save(payload)
    const savedArticle = res?.data || {}
    if (status === 'DRAFT') ElMessage.success('草稿已保存')
    else if (savedArticle.status === 'PUBLISHED') ElMessage.success('文章已发布')
    else ElMessage.warning(savedArticle.reviewReason ? `内容含违禁词，已转入审核：${savedArticle.reviewReason}` : '内容已转入审核')
    resetArticleForm(); await loadMine(1); activeSection.value = 'articles'
  } catch (error) { console.error(error) } finally { articleSaving.value = false }
}
const resetArticleForm = () => { editingId.value = null; editorRef.value = null; lastSelection.value = null; Object.assign(articleForm, { title: '', summary: '', coverUrl: '', content: '', contentType: 'HTML', categoryId: null, tagIds: [] }); markArticleSaved() }
const editArticle = async (row) => { if (!(await confirmDiscardArticle())) return; await ensureRichTextFormats(); editingId.value = row.id; Object.assign(articleForm, { title: row.title || '', summary: row.summary || '', coverUrl: row.coverUrl || '', content: toEditableHtml(row.content, row.contentType), contentType: 'HTML', categoryId: row.categoryId || null, tagIds: row.tags?.map((tag) => tag.id) || [] }); markArticleSaved(); activeSection.value = 'write' }
const onEditorReady = (quill) => { editorRef.value = quill; lastSelection.value = null; setupRichTextEditor(quill, { onImageFile: (file, range) => uploadArticleFile({ file }, 'image', range), onLinkButton: openLinkButton, getLastSelection: () => lastSelection.value }) }
const onSelectionChange = ({ range }) => { if (range) lastSelection.value = range }
const removeArticle = async (row) => { await ElMessageBox.confirm(`确认删除《${row.title}》吗？`, '删除文章', { type: 'warning' }); await articleApi.removeMine(row.id); if (editingId.value === row.id) resetArticleForm(); ElMessage.success('文章已删除'); loadMine(Math.min(articlePage.current, pageCount(articlePage.total - 1, articlePage.size))) }
const uploadProfileFile = async (options, field) => { try { const res = await uploadApi.file(options.file); profile[field] = res.data.url; options.onSuccess?.(res); ElMessage.success('上传成功') } catch (error) { console.error(error); options.onError?.(error) } }
const uploadArticleFile = async (options, type, requestedRange = null) => { if (articleSaving.value) return ElMessage.warning('文章正在保存，请稍后再上传文件'); const quill = editorRef.value; const range = requestedRange || quill?.getSelection?.() || lastSelection.value; const placeholderId = type === 'cover' ? null : insertUploadPlaceholder(quill, type, range); if (type !== 'cover' && !placeholderId) return ElMessage.warning('编辑器还没有准备好，请稍后再试'); pendingUploads.value += 1; try { const res = await uploadApi.file(options.file); const { url, name } = res.data; if (type === 'cover') articleForm.coverUrl = url; else { const snippet = type === 'image' ? imageSnippet(url, name) : type === 'video' ? videoSnippet(url, name) : fileSnippet(url, name); const nextIndex = replaceUploadPlaceholder(quill, placeholderId, snippet); lastSelection.value = { index: nextIndex, length: 0 } } options.onSuccess?.(res); ElMessage.success('上传成功') } catch (error) { console.error(error); if (placeholderId) removeUploadPlaceholder(quill, placeholderId); if (error?.code === 'EDITOR_INSERT_FAILED') ElMessage.error(error.message); options.onError?.(error) } finally { pendingUploads.value = Math.max(0, pendingUploads.value - 1) } }
const openWriterUpload = (type) => { if (articleSaving.value) return; const quill = editorRef.value; const currentRange = quill?.getSelection?.(); if (currentRange) lastSelection.value = currentRange; const input = writerUploadInputs[type]?.value; if (!input) return; input.value = ''; input.click() }
const handleWriterUpload = async (event, type) => { const file = event?.target?.files?.[0]; if (event?.target) event.target.value = ''; if (!file) return; await uploadArticleFile({ file }, type) }
const openLinkButton = () => { const quill = editorRef.value; linkButtonRange.value = quill?.getSelection?.() || lastSelection.value; Object.assign(linkButtonForm, { text: '', href: '', style: 'primary', newWindow: true }); linkButtonVisible.value = true }
const confirmLinkButton = () => { try { const nextIndex = insertLinkButton(editorRef.value, linkButtonRange.value, linkButtonForm); if (nextIndex !== null) lastSelection.value = { index: nextIndex, length: 0 }; linkButtonVisible.value = false } catch (error) { ElMessage.warning(error.message || '链接按钮填写有误') } }
const loadMine = async (page = articlePage.current) => { articlePage.current = page; try { const res = await articleApi.mine({ current: articlePage.current, size: articlePage.size, keyword: articleQuery.keyword?.trim() || undefined, status: articleQuery.status || undefined }); mineRows.value = res.data.records || []; articlePage.total = res.data.total || 0 } catch (error) { console.error(error) } }
const loadFavorites = async (page = favoritePage.current) => { favoritePage.current = page; try { const res = await userApi.favorites({ current: favoritePage.current, size: favoritePage.size }); favoriteRows.value = res.data.records || []; favoritePage.total = res.data.total || 0 } catch (error) { console.error(error) } }
const loadComments = async (page = commentPage.current) => { commentPage.current = page; try { const res = await userApi.comments({ current: commentPage.current, size: commentPage.size }); commentRows.value = res.data.records || []; commentPage.total = res.data.total || 0 } catch (error) { console.error(error) } }
async function focusWriter() { if (!(await confirmDiscardArticle())) return; await ensureRichTextFormats(); resetArticleForm(); activeSection.value = 'write' }
const setSection = async (section) => { if (section !== 'write' && !(await confirmDiscardArticle())) return; if (activeSection.value === 'write' && section !== 'write') resetArticleForm(); if (section === 'write') await ensureRichTextFormats(); activeSection.value = section; if (section === 'articles') loadMine(articlePage.current); if (section === 'favorites') loadFavorites(favoritePage.current); if (section === 'comments') loadComments(commentPage.current) }
const pageCount = (total, size) => Math.max(1, Math.ceil(Math.max(total, 0) / size))
const openArticle = (id) => { if (id) router.push(`/article/${id}`) }
const formatDate = (date) => String(date || '').slice(0, 16) || '-'
const statusText = (status) => ({ DRAFT: '草稿', PENDING: '待审核', PUBLISHED: '已发布', REJECTED: '已驳回', OFFLINE: '已下架', DELETED: '已删除' }[status] || status || '-')
const statusType = (status) => ({ PUBLISHED: 'success', PENDING: 'warning', REJECTED: 'danger', OFFLINE: 'info', DELETED: 'danger', DRAFT: 'info' }[status] || 'info')
const commentStatusText = (status) => ({ PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }[status] || status || '-')
const commentStatusType = (status) => ({ APPROVED: 'success', PENDING: 'warning', REJECTED: 'danger' }[status] || 'info')
const handleBeforeUnload = (event) => { if (!hasUnsavedArticle.value && pendingUploads.value === 0 && !articleSaving.value) return; event.preventDefault(); event.returnValue = '' }
onBeforeRouteLeave(async () => await confirmDiscardArticle())
onMounted(() => { markArticleSaved(); window.addEventListener('beforeunload', handleBeforeUnload); loadProfile(); loadMeta(); loadMine() })
onBeforeUnmount(() => window.removeEventListener('beforeunload', handleBeforeUnload))
</script>

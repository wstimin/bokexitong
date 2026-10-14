<template>
  <section class="admin-card settings-panel">
    <div class="toolbar user-toolbar">
      <div>
        <h2>站点设置</h2>
        <p class="section-subtitle">配置站点名称、Logo、首页文案、全站背景、SEO、登录入口和违禁词规则。</p>
      </div>
      <div class="settings-save-actions">
        <span :class="['settings-save-state', `is-${saveState}`]">{{ saveStateText }}</span>
        <button class="btn-primary" type="button" :disabled="loading || pathSaving" @click="save">{{ saveState === 'saving' ? '保存并校验中…' : '保存并校验' }}</button>
      </div>
    </div>

    <el-form label-position="top" class="settings-form grouped-settings-form">
      <div class="setting-group">
        <div class="setting-group-info">
          <h3>基础信息</h3>
          <p>控制站点名称、Logo 和浏览器图标。</p>
        </div>
        <div class="setting-group-fields">
          <el-form-item label="站点名称">
            <el-input v-model="form.siteName" maxlength="40" show-word-limit placeholder="请输入站点名称" />
          </el-form-item>
          <el-form-item label="站点 Logo / 浏览器图标" class="field-full">
            <div class="cover-picker settings-logo-picker">
              <img v-if="form.logoUrl" :src="logoPreviewSrc" alt="站点 Logo" />
              <div v-else class="cover-empty">Logo 预览</div>
              <div class="cover-fields">
                <el-input v-model="form.logoUrl" placeholder="上传 Logo 或粘贴图片地址" />
                <FileUploadButton accept="image/*" @select="uploadLogoFile">上传 Logo</FileUploadButton>
              </div>
            </div>
          </el-form-item>
        </div>
      </div>

      <div class="setting-group">
        <div class="setting-group-info">
          <h3>首页展示</h3>
          <p>首页大标题、说明和全站背景图直接显示，不叠加额外遮罩。</p>
        </div>
        <div class="setting-group-fields">
          <el-form-item label="首页徽标文案">
            <el-input v-model="form.heroBadge" maxlength="40" show-word-limit placeholder="请输入首页徽标文案" />
          </el-form-item>
          <el-form-item label="首页大标题">
            <el-input v-model="form.heroTitle" maxlength="80" show-word-limit placeholder="请输入首页大标题" />
          </el-form-item>
          <el-form-item label="首页说明" class="field-full">
            <el-input v-model="form.heroSubtitle" type="textarea" :rows="4" maxlength="220" show-word-limit placeholder="写一段给访客看的介绍" />
          </el-form-item>
          <el-form-item label="主标题颜色">
            <div class="color-setting-field">
              <el-color-picker v-model="form.heroTitleColor" :predefine="heroColorOptions" />
              <span>{{ form.heroTitleColor }}</span>
            </div>
          </el-form-item>
          <el-form-item label="介绍文字颜色">
            <div class="color-setting-field">
              <el-color-picker v-model="form.heroSubtitleColor" :predefine="heroColorOptions" />
              <span>{{ form.heroSubtitleColor }}</span>
            </div>
          </el-form-item>
          <el-form-item label="文字阴影" class="field-full">
            <el-radio-group v-model="form.heroTextShadow">
              <el-radio-button value="dark">深色阴影</el-radio-button>
              <el-radio-button value="light">浅色阴影</el-radio-button>
              <el-radio-button value="none">关闭阴影</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="全站背景图 URL" class="field-full">
            <div class="inline-field">
              <el-input v-model="form.backgroundUrl" placeholder="上传背景图或粘贴图片地址，留空使用默认背景" />
              <FileUploadButton accept="image/*" @select="uploadBackgroundFile">上传背景</FileUploadButton>
            </div>
          </el-form-item>
        </div>
      </div>

      <div class="setting-group">
        <div class="setting-group-info">
          <h3>内容审核</h3>
          <p>文章和评论共用同一份违禁词。命中后直接驳回，不再进入待审队列。</p>
        </div>
        <div class="setting-group-fields">
          <el-form-item label="违禁词设置" class="field-full">
            <el-input
              v-model="form.forbiddenWords"
              type="textarea"
              :rows="4"
              maxlength="1000"
              show-word-limit
              placeholder="一行或用逗号分隔，例如：赌博,色情,毒品,诈骗"
            />
          </el-form-item>
          <el-form-item label="页脚审核与版权说明" class="field-full">
            <el-input v-model="form.contactHtml" type="textarea" :rows="6" maxlength="2000" show-word-limit placeholder="填写审核规则、版权说明或站点公告；保存后显示在前台页脚的“站点说明”区域" />
            <div class="setting-content-preview">
              <strong>前台显示预览</strong>
              <div v-if="contactPreviewHtml" class="portal-contact-html" v-html="contactPreviewHtml"></div>
              <span v-else>暂未填写站点说明</span>
            </div>
          </el-form-item>
        </div>
      </div>

      <div class="setting-group">
        <div class="setting-group-info">
          <h3>SEO 与入口</h3>
          <p>用于搜索引擎、页脚文字和后台登录路径。</p>
        </div>
        <div class="setting-group-fields">
          <el-form-item label="SEO 描述" class="field-full">
            <el-input v-model="form.seoDescription" type="textarea" :rows="2" maxlength="220" show-word-limit placeholder="用于搜索引擎描述" />
          </el-form-item>
          <el-form-item label="SEO 关键词" class="field-full">
            <el-input v-model="form.seoKeywords" maxlength="180" show-word-limit placeholder="例如：博客,技术文章,生活记录" />
          </el-form-item>
          <el-form-item label="备案号">
            <el-input v-model="form.icpBeian" maxlength="80" show-word-limit placeholder="例如：粤ICP备xxxxxx号" />
          </el-form-item>
          <el-form-item label="页脚文字">
            <el-input v-model="form.footerText" maxlength="120" show-word-limit placeholder="例如：Copyright 2026 博客系统" />
          </el-form-item>
          <el-form-item label="后台登录路径" class="field-full">
            <div class="admin-login-path-field">
              <div class="inline-field admin-login-path-actions">
                <el-input v-model="form.adminLoginPath" maxlength="80" placeholder="例如：/admin/login 或 /shiyeotimin/login" @keyup.enter="saveAdminLoginPath" />
                <button class="btn-primary" type="button" :disabled="loading || pathSaving" @click="saveAdminLoginPath">
                  {{ pathSaving ? '正在切换…' : '保存并切换入口' }}
                </button>
              </div>
              <p>此项独立保存且必须以 /login 结尾。保存后登录页和全部后台页面都会切换到新前缀，旧入口与旧后台地址随即失效。</p>
            </div>
          </el-form-item>
          <el-form-item label="开放注册" class="field-full setting-switch-field">
            <el-switch v-model="form.allowRegister" active-text="允许新用户注册" inactive-text="关闭公开注册" />
          </el-form-item>
        </div>
      </div>
    </el-form>

    <div class="settings-preview" :style="previewStyle">
      <span class="eyebrow">{{ form.heroBadge || '博客' }}</span>
      <h1>{{ form.heroTitle || form.siteName || '博客系统' }}</h1>
      <p>{{ form.heroSubtitle || '用透明卡片浏览公开文章，点开后阅读完整内容。登录后可进入用户中心创作和管理自己的文章。' }}</p>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi, portalApi, uploadApi } from '../../api/blog'
import FileUploadButton from '../../components/FileUploadButton.vue'
import { useAuthStore } from '../../stores/auth'
import { useSiteStore } from '../../stores/site'
import { normalizeAssetUrl } from '../../utils/assets'
import { sanitizeContactHtml } from '../../utils/sanitize'
import {
  findSettingMismatches,
  findMissingRequiredSiteField,
  PUBLIC_SITE_SETTING_KEYS,
  SITE_SETTINGS_SYNC_KEY
} from '../../utils/siteSettings'
import {
  createHeroTextStyle,
  DEFAULT_HERO_SUBTITLE_COLOR,
  DEFAULT_HERO_TEXT_SHADOW,
  DEFAULT_HERO_TITLE_COLOR,
  normalizeHeroColor,
  normalizeHeroTextShadow
} from '../../utils/heroAppearance'

const site = useSiteStore()
const auth = useAuthStore()
const loading = ref(false)
const pathSaving = ref(false)
const saveState = ref('idle')
const savedAt = ref('')
const form = reactive({
  siteName: '',
  logoUrl: '',
  heroTitle: '',
  heroSubtitle: '',
  heroBadge: '',
  heroTitleColor: DEFAULT_HERO_TITLE_COLOR,
  heroSubtitleColor: DEFAULT_HERO_SUBTITLE_COLOR,
  heroTextShadow: DEFAULT_HERO_TEXT_SHADOW,
  backgroundUrl: '',
  forbiddenWords: '',
  seoDescription: '',
  seoKeywords: '',
  icpBeian: '',
  footerText: '',
  contactHtml: '',
  adminLoginPath: '/admin/login',
  allowRegister: true
})

const heroColorOptions = ['#f7f5f2', '#f0ebe3', '#2c2a26', '#5c574e', '#f1dfa6', '#d8dfeb']
const previewStyle = computed(() => ({
  ...(form.backgroundUrl ? { backgroundImage: `url("${normalizeAssetUrl(form.backgroundUrl)}")` } : {}),
  ...createHeroTextStyle(form.heroTitleColor, form.heroSubtitleColor, form.heroTextShadow)
}))
const logoPreviewSrc = computed(() => normalizeAssetUrl(form.logoUrl))
const contactPreviewHtml = computed(() => sanitizeContactHtml(form.contactHtml).trim())
const saveStateText = computed(() => {
  if (saveState.value === 'saving') return '正在写入数据库并同步前台…'
  if (saveState.value === 'saved') return savedAt.value ? `已保存并验证 · ${savedAt.value}` : '已保存并验证'
  if (saveState.value === 'dirty') return '有尚未保存的修改'
  if (saveState.value === 'error') return '保存校验未通过，请重试'
  return '修改后请保存，系统会核对前台结果'
})
const settingsSignature = () => JSON.stringify({
  siteName: form.siteName,
  logoUrl: form.logoUrl,
  heroTitle: form.heroTitle,
  heroSubtitle: form.heroSubtitle,
  heroBadge: form.heroBadge,
  heroTitleColor: form.heroTitleColor,
  heroSubtitleColor: form.heroSubtitleColor,
  heroTextShadow: form.heroTextShadow,
  backgroundUrl: form.backgroundUrl,
  forbiddenWords: form.forbiddenWords,
  seoDescription: form.seoDescription,
  seoKeywords: form.seoKeywords,
  icpBeian: form.icpBeian,
  footerText: form.footerText,
  contactHtml: form.contactHtml,
  allowRegister: form.allowRegister
})
watch(settingsSignature, () => {
  if (saveState.value === 'saved') saveState.value = 'dirty'
})
const settingsVerificationError = (message) => Object.assign(new Error(message), { settingsVerification: true })

const apply = (settings = {}) => {
  form.siteName = settings.siteName || '博客系统'
  form.logoUrl = settings.logoUrl || ''
  form.heroTitle = settings.heroTitle || form.siteName
  form.heroSubtitle = settings.heroSubtitle || '用透明卡片浏览公开文章，点开后阅读完整内容。登录后可进入用户中心创作和管理自己的文章。'
  form.heroBadge = settings.heroBadge || '博客'
  form.heroTitleColor = normalizeHeroColor(settings.heroTitleColor, DEFAULT_HERO_TITLE_COLOR)
  form.heroSubtitleColor = normalizeHeroColor(settings.heroSubtitleColor, DEFAULT_HERO_SUBTITLE_COLOR)
  form.heroTextShadow = normalizeHeroTextShadow(settings.heroTextShadow)
  form.backgroundUrl = settings.backgroundUrl || ''
  form.forbiddenWords = settings.forbiddenWords || '赌博,色情,毒品,诈骗'
  form.seoDescription = settings.seoDescription || ''
  form.seoKeywords = settings.seoKeywords || ''
  form.icpBeian = settings.icpBeian || ''
  form.footerText = settings.footerText || ''
  form.contactHtml = settings.contactHtml || ''
  form.adminLoginPath = settings.adminLoginPath || '/admin/login'
  form.allowRegister = settings.allowRegister !== 'false'
}

const load = async () => {
  loading.value = true
  try {
    const res = await adminApi.siteSettings()
    apply(res.data)
    site.setAdminLoginPath(form.adminLoginPath)
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const save = async () => {
  const missingField = findMissingRequiredSiteField(form)
  if (missingField) return ElMessage.warning(`请填写${missingField}`)
  loading.value = true
  saveState.value = 'saving'
  try {
    const payload = { ...form, allowRegister: String(form.allowRegister) }
    const adminLoginPathDraft = payload.adminLoginPath
    delete payload.adminLoginPath
    const res = await adminApi.saveSiteSettings(payload)
    const savedSettings = res.data || {}
    const saveMismatches = findSettingMismatches(payload, savedSettings, Object.keys(payload))
    if (saveMismatches.length) throw settingsVerificationError(`数据库保存校验失败：${saveMismatches.join('、')}`)

    const publicRes = await portalApi.site({
      params: { _ts: Date.now() },
      headers: { 'Cache-Control': 'no-cache' }
    })
    const publicSettings = publicRes.data?.settings || {}
    const publicKeys = PUBLIC_SITE_SETTING_KEYS.filter((key) => Object.hasOwn(payload, key))
    const publicMismatches = findSettingMismatches(savedSettings, publicSettings, publicKeys)
    if (publicMismatches.length) throw settingsVerificationError(`前台同步校验失败：${publicMismatches.join('、')}`)

    apply(savedSettings)
    form.adminLoginPath = adminLoginPathDraft
    site.applySettings(publicRes.data || {})
    savedAt.value = new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false }).format(new Date())
    saveState.value = 'saved'
    localStorage.setItem(SITE_SETTINGS_SYNC_KEY, String(Date.now()))
    ElMessage.success('站点设置已写入数据库，并通过前台接口校验；后台登录路径仍使用独立按钮保存')
  } catch (error) {
    console.error(error)
    saveState.value = 'error'
    if (error.settingsVerification) ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

const saveAdminLoginPath = async () => {
  if (pathSaving.value || loading.value) return
  pathSaving.value = true
  try {
    const res = await adminApi.saveAdminLoginPath({ adminLoginPath: form.adminLoginPath })
    const nextAdminLoginPath = res.data?.adminLoginPath
    if (!nextAdminLoginPath) throw new Error('服务器未返回新的后台登录路径')
    form.adminLoginPath = nextAdminLoginPath
    site.setAdminLoginPath(nextAdminLoginPath)
    auth.logout('admin')
    sessionStorage.setItem('blog_admin_login_path_updated', nextAdminLoginPath)
    window.location.replace(nextAdminLoginPath)
  } catch (error) {
    console.error(error)
  } finally {
    pathSaving.value = false
  }
}

const uploadBackground = async (file) => {
  try {
    const res = await uploadApi.file(file)
    form.backgroundUrl = res.data.url
    ElMessage.success('背景图已上传')
  } catch (error) {
    console.error(error)
  }
}

const uploadLogo = async (file) => {
  try {
    const res = await uploadApi.file(file)
    form.logoUrl = res.data.url
    ElMessage.success('Logo 已上传')
  } catch (error) {
    console.error(error)
  }
}

const uploadBackgroundFile = (file) => uploadBackground(file)
const uploadLogoFile = (file) => uploadLogo(file)

onMounted(load)
</script>

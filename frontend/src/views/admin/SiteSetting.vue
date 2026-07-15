<template>
  <section class="admin-card settings-panel">
    <div class="toolbar user-toolbar">
      <div>
        <h2>站点设置</h2>
        <p class="section-subtitle">配置站点名称、Logo、浏览器图标、首页文案、全站背景、SEO 和前台展示信息。</p>
      </div>
      <button class="btn-primary" :disabled="loading" @click="save">保存设置</button>
    </div>

    <el-form label-position="top" class="settings-form grouped-settings-form">
      <div class="setting-group">
        <div class="setting-group-info">
          <h3>基础信息</h3>
          <p>控制站点名称、顶部 Logo 和浏览器标签图标。</p>
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
                <el-input v-model="form.logoUrl" placeholder="上传 Logo 或粘贴图片地址；保存后同步顶部 Logo 和浏览器图标" />
                <el-upload :show-file-list="false" :http-request="uploadLogo" accept="image/*">
                  <button class="btn-ghost" type="button">上传 Logo</button>
                </el-upload>
              </div>
            </div>
          </el-form-item>
        </div>
      </div>

      <div class="setting-group">
        <div class="setting-group-info">
          <h3>首页展示</h3>
          <p>首页首屏文案和全站背景图，保存后前台立即使用。</p>
        </div>
        <div class="setting-group-fields">
          <el-form-item label="首页徽标文字">
            <el-input v-model="form.heroBadge" maxlength="40" show-word-limit placeholder="请输入首页徽标文字" />
          </el-form-item>
          <el-form-item label="首页大标题">
            <el-input v-model="form.heroTitle" maxlength="80" show-word-limit placeholder="请输入首页大标题" />
          </el-form-item>
          <el-form-item label="首页说明" class="field-full">
            <el-input v-model="form.heroSubtitle" type="textarea" :rows="4" maxlength="220" show-word-limit placeholder="写一段给访客看的介绍" />
          </el-form-item>
          <el-form-item label="全站背景图 URL" class="field-full">
            <div class="inline-field">
              <el-input v-model="form.backgroundUrl" placeholder="上传背景图或粘贴图片地址；留空使用默认背景" />
              <el-upload :show-file-list="false" :http-request="uploadBackground" accept="image/*">
                <button class="btn-ghost" type="button">上传背景</button>
              </el-upload>
            </div>
          </el-form-item>
        </div>
      </div>

      <div class="setting-group">
        <div class="setting-group-info">
          <h3>上线信息</h3>
          <p>用于搜索引擎、备案展示和站点页脚信息。</p>
        </div>
        <div class="setting-group-fields">
          <el-form-item label="SEO 描述" class="field-full">
            <el-input v-model="form.seoDescription" type="textarea" :rows="2" maxlength="220" show-word-limit placeholder="用于搜索引擎描述，不填写则使用首页说明" />
          </el-form-item>
          <el-form-item label="SEO 关键词" class="field-full">
            <el-input v-model="form.seoKeywords" maxlength="180" show-word-limit placeholder="例如：博客,技术文章,生活记录" />
          </el-form-item>
          <el-form-item label="备案号">
            <el-input v-model="form.icpBeian" maxlength="80" show-word-limit placeholder="例如：粤ICP备xxxx号" />
          </el-form-item>
          <el-form-item label="页脚文字">
            <el-input v-model="form.footerText" maxlength="120" show-word-limit placeholder="例如：Copyright 2026 博客系统" />
          </el-form-item>
        </div>
      </div>

      <div class="setting-group">
        <div class="setting-group-info">
          <h3>前台展示与入口</h3>
          <p>填写前台页脚展示内容，并设置管理员后台登录入口。</p>
        </div>
        <div class="setting-group-fields">
          <el-form-item label="联系信息 / 自定义展示内容" class="field-full">
            <el-input
              v-model="form.contactHtml"
              type="textarea"
              :rows="6"
              maxlength="2000"
              show-word-limit
              placeholder="可填写邮箱、微信、商务合作、站长简介等内容。支持基础 HTML 链接和换行，不执行脚本。"
            />
          </el-form-item>
          <el-form-item label="后台登录路径">
            <el-input v-model="form.adminLoginPath" maxlength="80" placeholder="例如：/admin/login 或 /manage-login" />
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
      <p>{{ form.heroSubtitle || '用卡片浏览公开文章，点开后再阅读完整内容。' }}</p>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi, uploadApi } from '../../api/blog'
import { useSiteStore } from '../../stores/site'
import { normalizeAssetUrl } from '../../utils/assets'

const site = useSiteStore()
const loading = ref(false)
const form = reactive({
  siteName: '',
  logoUrl: '',
  heroTitle: '',
  heroSubtitle: '',
  heroBadge: '',
  backgroundUrl: '',
  seoDescription: '',
  seoKeywords: '',
  icpBeian: '',
  footerText: '',
  contactHtml: '',
  adminLoginPath: '/admin/login',
  allowRegister: true
})

const previewStyle = computed(() => form.backgroundUrl
  ? { backgroundImage: `linear-gradient(120deg, rgba(38, 29, 57, .78), rgba(70, 50, 94, .42)), url("${normalizeAssetUrl(form.backgroundUrl)}")` }
  : {})
const logoPreviewSrc = computed(() => normalizeAssetUrl(form.logoUrl))

const apply = (settings = {}) => {
  form.siteName = settings.siteName || '博客系统'
  form.logoUrl = settings.logoUrl || ''
  form.heroTitle = settings.heroTitle || form.siteName
  form.heroSubtitle = settings.heroSubtitle || '用卡片浏览公开文章，点开后再阅读完整内容。登录后可以进入用户中心创作、管理自己的文章。'
  form.heroBadge = settings.heroBadge || '博客'
  form.backgroundUrl = settings.backgroundUrl || ''
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
  } finally {
    loading.value = false
  }
}

const save = async () => {
  loading.value = true
  try {
    const payload = {
      ...form,
      allowRegister: String(form.allowRegister)
    }
    const res = await adminApi.saveSiteSettings(payload)
    apply(res.data)
    await site.loadSite(true)
    ElMessage.success('站点设置已保存')
  } finally {
    loading.value = false
  }
}

const uploadBackground = async (options) => {
  const res = await uploadApi.file(options.file)
  form.backgroundUrl = res.data.url
  ElMessage.success('背景图已上传')
}

const uploadLogo = async (options) => {
  const res = await uploadApi.file(options.file)
  form.logoUrl = res.data.url
  ElMessage.success('Logo 已上传')
}

onMounted(load)
</script>

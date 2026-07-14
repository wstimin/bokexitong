<template>
  <section class="admin-card settings-panel">
    <div class="toolbar user-toolbar">
      <div>
        <h2>站点设置</h2>
        <p class="section-subtitle">配置站点名称、Logo、浏览器图标、SEO、首页文案和全站背景。</p>
      </div>
      <button class="btn-primary" :disabled="loading" @click="save">保存设置</button>
    </div>

    <el-form label-position="top" class="settings-form">
      <el-form-item label="站点名称">
        <el-input v-model="form.siteName" maxlength="40" show-word-limit placeholder="请输入站点名称" />
      </el-form-item>
      <el-form-item label="站点 Logo / 浏览器图标">
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
      <el-form-item label="首页徽标文字">
        <el-input v-model="form.heroBadge" maxlength="40" show-word-limit placeholder="请输入首页徽标文字" />
      </el-form-item>
      <el-form-item label="首页大标题">
        <el-input v-model="form.heroTitle" maxlength="80" show-word-limit placeholder="请输入首页大标题" />
      </el-form-item>
      <el-form-item label="首页说明">
        <el-input v-model="form.heroSubtitle" type="textarea" :rows="4" maxlength="220" show-word-limit placeholder="写一段给访客看的介绍" />
      </el-form-item>
      <el-form-item label="全站背景图 URL">
        <div class="inline-field">
          <el-input v-model="form.backgroundUrl" placeholder="上传背景图或粘贴图片地址；留空使用默认背景" />
          <el-upload :show-file-list="false" :http-request="uploadBackground" accept="image/*">
            <button class="btn-ghost" type="button">上传</button>
          </el-upload>
        </div>
      </el-form-item>
      <el-divider content-position="left">上线信息</el-divider>
      <el-form-item label="SEO 描述">
        <el-input v-model="form.seoDescription" type="textarea" :rows="2" maxlength="220" show-word-limit placeholder="用于搜索引擎描述，不填写则使用首页说明" />
      </el-form-item>
      <el-form-item label="SEO 关键词">
        <el-input v-model="form.seoKeywords" maxlength="180" show-word-limit placeholder="例如：博客,技术文章,生活记录" />
      </el-form-item>
      <div class="settings-grid two-cols">
        <el-form-item label="备案号">
          <el-input v-model="form.icpBeian" maxlength="80" show-word-limit placeholder="例如：粤ICP备xxxx号" />
        </el-form-item>
        <el-form-item label="页脚文字">
          <el-input v-model="form.footerText" maxlength="120" show-word-limit placeholder="例如：© 2026 博客系统" />
        </el-form-item>
      </div>
      <el-form-item label="开放注册">
        <el-switch v-model="form.allowRegister" active-text="允许新用户注册" inactive-text="关闭公开注册" />
      </el-form-item>

      <el-divider content-position="left">邮箱验证码</el-divider>
      <el-form-item label="启用后台邮箱配置">
        <el-switch v-model="form.mailEnabled" active-text="启用" inactive-text="关闭" />
      </el-form-item>
      <div class="settings-grid two-cols">
        <el-form-item label="SMTP 服务器">
          <el-input v-model="form.mailHost" placeholder="例如 smtp.qq.com / smtp.gmail.com" />
        </el-form-item>
        <el-form-item label="SMTP 端口">
          <el-input-number v-model="form.mailPort" :min="1" :max="65535" style="width: 100%" />
        </el-form-item>
        <el-form-item label="SMTP 账号">
          <el-input v-model="form.mailUsername" placeholder="通常是完整邮箱地址" />
        </el-form-item>
        <el-form-item label="SMTP 密码 / 授权码">
          <el-input v-model="form.mailPassword" type="password" show-password placeholder="留空表示不修改已有密码" />
        </el-form-item>
        <el-form-item label="邮件发件名称">
          <el-input v-model="form.mailFromName" maxlength="40" show-word-limit placeholder="显示在验证码邮件中" />
        </el-form-item>
      </div>
      <div class="settings-switch-row">
        <el-checkbox v-model="form.mailSmtpAuth">SMTP 认证</el-checkbox>
        <el-checkbox v-model="form.mailStarttlsEnable">STARTTLS</el-checkbox>
        <el-checkbox v-model="form.mailSslEnable">SSL</el-checkbox>
      </div>
      <el-form-item label="测试收件邮箱">
        <div class="inline-field">
          <el-input v-model="testEmail" placeholder="先保存邮箱配置，再发送测试邮件" />
          <button class="btn-ghost" type="button" :disabled="loading" @click="sendTestMail">发送测试</button>
        </div>
      </el-form-item>
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
const testEmail = ref('')
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
  allowRegister: true,
  mailEnabled: false,
  mailHost: '',
  mailPort: 587,
  mailUsername: '',
  mailPassword: '',
  mailFromName: '',
  mailSmtpAuth: true,
  mailStarttlsEnable: true,
  mailSslEnable: false
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
  form.allowRegister = settings.allowRegister !== 'false'
  form.mailEnabled = settings.mailEnabled === 'true'
  form.mailHost = settings.mailHost || ''
  form.mailPort = Number(settings.mailPort || 587)
  form.mailUsername = settings.mailUsername || ''
  form.mailPassword = ''
  form.mailFromName = settings.mailFromName || form.siteName || '博客系统'
  form.mailSmtpAuth = settings.mailSmtpAuth !== 'false'
  form.mailStarttlsEnable = settings.mailStarttlsEnable !== 'false'
  form.mailSslEnable = settings.mailSslEnable === 'true'
}

const load = async () => {
  loading.value = true
  try {
    const res = await adminApi.settings()
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
      allowRegister: String(form.allowRegister),
      mailEnabled: String(form.mailEnabled),
      mailPort: String(form.mailPort || 587),
      mailSmtpAuth: String(form.mailSmtpAuth),
      mailStarttlsEnable: String(form.mailStarttlsEnable),
      mailSslEnable: String(form.mailSslEnable)
    }
    const res = await adminApi.saveSettings(payload)
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

const sendTestMail = async () => {
  if (!testEmail.value.trim()) {
    ElMessage.warning('请填写测试收件邮箱')
    return
  }
  loading.value = true
  try {
    await adminApi.testMail({ email: testEmail.value.trim() })
    ElMessage.success('测试邮件已发送')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

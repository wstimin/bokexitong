<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <RouterLink class="brand" to="/">
        <img v-if="site.logoSrc" class="brand-logo" :src="site.logoSrc" :alt="site.name" />
        <span v-else class="brand-mark"></span>
        <span><strong>{{ site.name }}</strong><small>Control room</small></span>
      </RouterLink>
      <nav class="admin-menu">
        <span class="admin-menu__label">Workspace</span>
        <RouterLink v-for="(item, index) in menuItems" :key="item.to" :to="item.to">
          <span class="admin-menu__index">{{ String(index + 1).padStart(2, '0') }}</span>
          {{ item.label }}
        </RouterLink>
      </nav>
      <div class="admin-sidebar__foot">
        <span>System</span>
        <strong>v{{ version }}</strong>
      </div>
    </aside>
    <main class="admin-main">
      <div class="admin-header">
        <h1>{{ title }}</h1>
        <div class="hero-actions">
          <ThemeToggle scope="admin" />
          <button class="btn-ghost" type="button" @click="openPasswordDialog">修改密码</button>
          <RouterLink class="btn-ghost" to="/">打开前台首页</RouterLink>
          <button class="btn-ghost danger-action" type="button" @click="logout">退出登录</button>
        </div>
      </div>
      <el-alert v-if="auth.passwordChangeRequired" class="admin-alert" title="当前管理员仍在使用默认密码，请立即修改。" type="error" :closable="false" />
      <RouterView />
    </main>

    <el-dialog
      v-model="passwordVisible"
      class="password-dialog"
      width="460px"
      append-to-body
      align-center
      destroy-on-close
      :close-on-click-modal="false"
      @closed="resetPasswordForm"
    >
      <template #header>
        <div class="password-dialog__heading">
          <span class="password-dialog__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path d="M7 10V8a5 5 0 0 1 10 0v2M6 10h12v10H6zM12 14v2" /></svg>
          </span>
          <div>
            <h2>修改登录密码</h2>
            <p>设置一个不易被猜到的新密码，保护后台账号安全。</p>
          </div>
        </div>
      </template>

      <el-form class="password-dialog__form" label-position="top" @keyup.enter="changePassword">
        <el-form-item label="当前密码">
          <el-input
            v-model="password.oldPassword"
            type="password"
            show-password
            autocomplete="current-password"
            maxlength="64"
            placeholder="请输入当前登录密码"
          />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input
            v-model="password.newPassword"
            type="password"
            show-password
            autocomplete="new-password"
            maxlength="64"
            placeholder="至少 6 位，建议包含字母、数字和符号"
          />
          <div v-if="password.newPassword" class="password-strength" aria-live="polite">
            <div class="password-strength__track"><span :style="{ width: `${passwordStrength.percent}%` }" :class="passwordStrength.className"></span></div>
            <span>{{ passwordStrength.label }}</span>
          </div>
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input
            v-model="password.confirmPassword"
            type="password"
            show-password
            autocomplete="new-password"
            maxlength="64"
            placeholder="请再次输入新密码"
          />
          <p v-if="password.confirmPassword && !passwordMatches" class="password-dialog__error">两次输入的新密码不一致</p>
        </el-form-item>
      </el-form>

      <div class="password-dialog__tips">
        <span :class="{ 'is-met': password.newPassword.length >= 6 }">至少 6 位</span>
        <span :class="{ 'is-met': hasLetterAndNumber }">包含字母和数字</span>
        <span :class="{ 'is-met': hasSymbol }">包含符号更安全</span>
      </div>

      <template #footer>
        <div class="password-dialog__footer">
          <span>修改成功后请使用新密码登录。</span>
          <div>
            <button class="btn-ghost" type="button" :disabled="passwordSaving" @click="passwordVisible = false">取消</button>
            <button class="btn-primary" type="button" :disabled="!canChangePassword || passwordSaving" @click="changePassword">
              {{ passwordSaving ? '正在保存…' : '确认修改' }}
            </button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { userApi } from '../../api/blog'
import { useAuthStore } from '../../stores/auth'
import { useSiteStore } from '../../stores/site'
import ThemeToggle from '../../components/ThemeToggle.vue'
import { portalApi } from '../../api/blog'
import { adminPagePath } from '../../utils/adminPath'

const route = useRoute()
const router = useRouter()
const site = useSiteStore()
const auth = useAuthStore()
const names = {
  dashboard: '数据看板',
  settings: '站点设置',
  'mail-settings': '邮件设置',
  'system-update': '版本与更新',
  backup: '备份与恢复',
  articles: '内容管理',
  taxonomies: '分类标签',
  images: '图片资源管理',
  comments: '评论审核',
  users: '用户权限',
  logs: '操作日志'
}
const title = computed(() => names[route.meta.adminPage] || '后台管理')
const passwordVisible = ref(false)
const passwordSaving = ref(false)
const version = ref('1.1.33')
const password = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const passwordMatches = computed(() => password.newPassword === password.confirmPassword)
const hasLetterAndNumber = computed(() => /[A-Za-z]/.test(password.newPassword) && /\d/.test(password.newPassword))
const hasSymbol = computed(() => /[^A-Za-z0-9]/.test(password.newPassword))
const passwordStrength = computed(() => {
  let score = 0
  if (password.newPassword.length >= 6) score += 1
  if (password.newPassword.length >= 10) score += 1
  if (hasLetterAndNumber.value) score += 1
  if (hasSymbol.value) score += 1
  if (score <= 1) return { label: '强度较弱', percent: 33, className: 'is-weak' }
  if (score <= 3) return { label: '强度适中', percent: 66, className: 'is-medium' }
  return { label: '强度良好', percent: 100, className: 'is-strong' }
})
const canChangePassword = computed(() => Boolean(
  password.oldPassword
  && password.newPassword.length >= 6
  && passwordMatches.value
  && password.oldPassword !== password.newPassword
))

onMounted(() => {
  site.loadSite().catch((error) => console.error(error))
  portalApi.version().then((res) => { version.value = res.data?.version || version.value }).catch(() => {})
})

const adminPath = (page) => adminPagePath(site.adminLoginPath, page)
const menuItems = computed(() => [
  { to: adminPath('dashboard'), label: '数据看板' },
  { to: adminPath('settings'), label: '站点设置' },
  { to: adminPath('mail-settings'), label: '邮件设置' },
  { to: adminPath('articles'), label: '内容管理' },
  { to: adminPath('taxonomies'), label: '分类标签' },
  { to: adminPath('images'), label: '图片资源' },
  { to: adminPath('comments'), label: '评论审核' },
  { to: adminPath('users'), label: '用户权限' },
  { to: adminPath('logs'), label: '操作日志' },
  { to: adminPath('backup'), label: '备份恢复' },
  { to: adminPath('system-update'), label: '版本与更新' }
])

const resetPasswordForm = () => {
  password.oldPassword = ''
  password.newPassword = ''
  password.confirmPassword = ''
}

const openPasswordDialog = () => {
  resetPasswordForm()
  passwordVisible.value = true
}

const changePassword = async () => {
  if (!password.oldPassword || !password.newPassword) {
    ElMessage.warning('请填写原密码和新密码')
    return
  }
  if (password.newPassword.length < 6) {
    ElMessage.warning('新密码至少需要 6 位')
    return
  }
  if (!passwordMatches.value) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  if (password.oldPassword === password.newPassword) {
    ElMessage.warning('新密码不能与当前密码相同')
    return
  }
  passwordSaving.value = true
  try {
    await userApi.changePassword({ oldPassword: password.oldPassword, newPassword: password.newPassword })
    auth.clearPasswordRequired()
    passwordVisible.value = false
    ElMessage.success('密码已修改')
  } catch (error) {
    console.error(error)
  } finally {
    passwordSaving.value = false
  }
}

const logout = () => {
  auth.logout('admin')
  router.push(site.adminLoginPath || '/admin/login')
}
</script>

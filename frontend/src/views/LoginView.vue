<template>
  <main class="login-page">
    <section class="login-box">
      <RouterLink class="brand" to="/">
        <img v-if="site.logoSrc" class="brand-logo" :src="site.logoSrc" :alt="site.name" />
        <span v-else class="brand-mark"></span>
        <span>{{ site.name }}</span>
      </RouterLink>

      <div class="auth-title">
        <h1>{{ pageTitle }}</h1>
        <p>{{ pageHint }}</p>
      </div>

      <el-alert v-if="isAdminLogin" class="auth-alert" title="管理员入口仅供后台账号登录。" type="info" :closable="false" />
      <el-alert v-if="isRegister && !site.allowRegister" class="auth-alert" title="本站暂未开放注册。" type="warning" :closable="false" />

      <el-form label-position="top" @keyup.enter="submit">
        <template v-if="isLogin">
          <el-form-item label="用户名或邮箱">
            <el-input v-model="form.account" placeholder="请输入用户名或邮箱" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" show-password placeholder="不少于 6 位" />
          </el-form-item>
        </template>

        <template v-else-if="isRegister">
          <el-form-item label="用户名">
            <el-input v-model="form.username" maxlength="20" placeholder="3-20 位字母、数字或下划线" :disabled="!site.allowRegister" />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="form.nickname" maxlength="30" placeholder="不填则默认使用用户名" :disabled="!site.allowRegister" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="form.email" type="email" placeholder="用于接收验证码" :disabled="!site.allowRegister" />
          </el-form-item>
          <el-form-item label="邮箱验证码">
            <div class="code-row">
              <el-input v-model="form.code" maxlength="6" placeholder="6 位验证码" :disabled="!site.allowRegister" />
              <button class="btn-ghost code-button" type="button" :disabled="!canSendRegisterCode" @click="sendCode('REGISTER')">
                {{ registerCountdown ? `${registerCountdown}s` : '获取验证码' }}
              </button>
            </div>
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" show-password placeholder="不少于 6 位" :disabled="!site.allowRegister" />
          </el-form-item>
          <el-form-item label="确认密码">
            <el-input v-model="form.confirmPassword" type="password" show-password placeholder="再次输入密码" :disabled="!site.allowRegister" />
          </el-form-item>
        </template>

        <template v-else>
          <el-form-item label="绑定邮箱">
            <el-input v-model="form.email" type="email" placeholder="请输入账号绑定的邮箱" />
          </el-form-item>
          <el-form-item label="邮箱验证码">
            <div class="code-row">
              <el-input v-model="form.code" maxlength="6" placeholder="6 位验证码" />
              <button class="btn-ghost code-button" type="button" :disabled="!canSendResetCode" @click="sendCode('RESET_PASSWORD')">
                {{ resetCountdown ? `${resetCountdown}s` : '获取验证码' }}
              </button>
            </div>
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="form.password" type="password" show-password placeholder="不少于 6 位" />
          </el-form-item>
          <el-form-item label="确认新密码">
            <el-input v-model="form.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
          </el-form-item>
        </template>
      </el-form>

      <button class="btn-primary auth-submit" type="button" :disabled="submitDisabled" @click="submit">
        {{ submitText }}
      </button>

      <div class="auth-actions">
        <button v-if="!isLogin" class="auth-switch" type="button" @click="setMode('login')">已有账号，去登录</button>
        <button v-if="isLogin && site.allowRegister && !isAdminLogin" class="auth-switch" type="button" @click="setMode('register')">没有账号，去注册</button>
        <button v-if="!isForgot && !isAdminLogin" class="auth-switch" type="button" @click="setMode('forgot')">忘记密码</button>
        <RouterLink v-if="isAdminLogin" class="auth-switch" to="/login">前台用户登录</RouterLink>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { authApi } from '../api/blog'
import { useAuthStore } from '../stores/auth'
import { useSiteStore } from '../stores/site'

const form = reactive({ account: '', username: '', nickname: '', email: '', code: '', password: '', confirmPassword: '' })
const auth = useAuthStore()
const site = useSiteStore()
const router = useRouter()
const route = useRoute()
const loading = ref(false)
const sendingScene = ref('')
const registerCountdown = ref(0)
const resetCountdown = ref(0)

const isAdminLogin = computed(() => route.meta.loginType === 'ADMIN')
const mode = computed(() => (isAdminLogin.value ? 'login' : route.query.mode || 'login'))
const isLogin = computed(() => mode.value === 'login')
const isRegister = computed(() => !isAdminLogin.value && mode.value === 'register')
const isForgot = computed(() => !isAdminLogin.value && mode.value === 'forgot')
const pageTitle = computed(() => (isAdminLogin.value ? '后台登录' : isRegister.value ? '注册账号' : isForgot.value ? '重置密码' : '用户登录'))
const pageHint = computed(() => {
  if (isAdminLogin.value) return '管理员账号用于进入后台管理文章、评论、用户和站点设置。'
  if (isRegister.value) return site.allowRegister ? '使用邮箱验证码完成注册，注册后即可进入用户中心。' : '站点暂未开放注册。'
  if (isForgot.value) return '通过绑定邮箱验证身份后设置新密码。'
  return '登录后可进入用户中心，管理文章、收藏和评论。'
})
const submitText = computed(() => (isRegister.value ? '注册并进入' : isForgot.value ? '重置密码' : isAdminLogin.value ? '进入后台' : '进入用户中心'))
const submitDisabled = computed(() => loading.value || (isRegister.value && !site.allowRegister))
const canSendRegisterCode = computed(() => site.allowRegister && !sendingScene.value && !registerCountdown.value)
const canSendResetCode = computed(() => !sendingScene.value && !resetCountdown.value)

onMounted(() => {
  site.loadSite().catch((error) => console.error(error))
})

const redirectTo = () => route.query.redirect || (isAdminLogin.value ? '/admin/dashboard' : '/user')

const setMode = (nextMode) => {
  const query = { ...route.query }
  if (nextMode === 'login') delete query.mode
  else query.mode = nextMode
  router.replace({ path: '/login', query })
}

const startCountdown = (target) => {
  target.value = 60
  const timer = window.setInterval(() => {
    target.value -= 1
    if (target.value <= 0) window.clearInterval(timer)
  }, 1000)
}

const sendCode = async (scene) => {
  if (!form.email.trim()) {
    ElMessage.warning('请先填写邮箱')
    return
  }
  sendingScene.value = scene
  try {
    await authApi.sendEmailCode({ email: form.email, scene })
    startCountdown(scene === 'REGISTER' ? registerCountdown : resetCountdown)
    ElMessage.success('验证码已发送，请查收邮件')
  } catch (error) {
    console.error(error)
  } finally {
    sendingScene.value = ''
  }
}

const login = async () => {
  if (!form.account.trim() || !form.password.trim()) {
    ElMessage.warning('请填写账号和密码')
    return
  }
  await auth.login({ account: form.account, password: form.password, loginType: isAdminLogin.value ? 'ADMIN' : 'USER' })
  if (auth.passwordChangeRequired) ElMessage.warning('当前管理员仍在使用默认密码，请尽快修改。')
  router.push(redirectTo())
}

const ensurePasswordConfirmed = () => {
  if (!form.password.trim()) {
    ElMessage.warning('请填写密码')
    return false
  }
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return false
  }
  return true
}

const register = async () => {
  if (!site.allowRegister) {
    ElMessage.warning('站点暂未开放注册')
    return
  }
  if (!form.username.trim() || !form.email.trim() || !form.code.trim()) {
    ElMessage.warning('请填写用户名、邮箱和验证码')
    return
  }
  if (!ensurePasswordConfirmed()) return
  await authApi.register({ username: form.username, nickname: form.nickname, email: form.email, code: form.code, password: form.password })
  await auth.login({ account: form.username, password: form.password, loginType: 'USER' })
  ElMessage.success('注册成功')
  router.push(redirectTo())
}

const resetPassword = async () => {
  if (!form.email.trim() || !form.code.trim()) {
    ElMessage.warning('请填写邮箱和验证码')
    return
  }
  if (!ensurePasswordConfirmed()) return
  await authApi.resetPassword({ email: form.email, code: form.code, newPassword: form.password })
  ElMessage.success('密码已重置，请重新登录')
  form.account = form.email
  form.password = ''
  form.confirmPassword = ''
  form.code = ''
  setMode('login')
}

const submit = async () => {
  if (submitDisabled.value) return
  loading.value = true
  try {
    if (isRegister.value) await register()
    else if (isForgot.value) await resetPassword()
    else await login()
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}
</script>

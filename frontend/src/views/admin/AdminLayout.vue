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
          <ThemeToggle />
          <button class="btn-ghost" type="button" @click="passwordVisible = true">修改密码</button>
          <RouterLink class="btn-ghost" to="/">打开前台首页</RouterLink>
          <button class="btn-ghost danger-action" type="button" @click="logout">退出登录</button>
        </div>
      </div>
      <el-alert v-if="auth.passwordChangeRequired" class="admin-alert" title="当前管理员仍在使用默认密码，请立即修改。" type="error" :closable="false" />
      <RouterView />
    </main>

    <el-dialog v-model="passwordVisible" title="修改密码" width="420px">
      <el-form label-position="top">
        <el-form-item label="原密码">
          <el-input v-model="password.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="password.newPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <button class="btn-ghost" type="button" @click="passwordVisible = false">取消</button>
        <button class="btn-primary" type="button" @click="changePassword">保存</button>
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

const route = useRoute()
const router = useRouter()
const site = useSiteStore()
const auth = useAuthStore()
const names = {
  '/admin/dashboard': '数据看板',
  '/admin/settings': '站点设置',
  '/admin/mail-settings': '邮件设置',
  '/admin/system-update': '版本与更新',
  '/admin/articles': '内容管理',
  '/admin/taxonomies': '分类标签',
  '/admin/images': '图片资源管理',
  '/admin/comments': '评论审核',
  '/admin/users': '用户权限',
  '/admin/logs': '操作日志'
}
const title = computed(() => names[route.path] || '后台管理')
const passwordVisible = ref(false)
const version = ref('1.1.6')
const password = reactive({ oldPassword: '', newPassword: '' })

onMounted(() => {
  site.loadSite().catch((error) => console.error(error))
  portalApi.version().then((res) => { version.value = res.data?.version || version.value }).catch(() => {})
})

const menuItems = [
  { to: '/admin/dashboard', label: '数据看板' },
  { to: '/admin/settings', label: '站点设置' },
  { to: '/admin/mail-settings', label: '邮件设置' },
  { to: '/admin/articles', label: '内容管理' },
  { to: '/admin/taxonomies', label: '分类标签' },
  { to: '/admin/images', label: '图片资源' },
  { to: '/admin/comments', label: '评论审核' },
  { to: '/admin/users', label: '用户权限' },
  { to: '/admin/logs', label: '操作日志' },
  { to: '/admin/system-update', label: '版本与更新' }
]

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
    passwordVisible.value = false
    ElMessage.success('密码已修改')
  } catch (error) {
    console.error(error)
  }
}

const logout = () => {
  auth.logout('admin')
  router.push(site.adminLoginPath || '/admin/login')
}
</script>

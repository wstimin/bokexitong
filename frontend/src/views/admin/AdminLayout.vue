<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <RouterLink class="brand" to="/">
        <img v-if="site.logoSrc" class="brand-logo" :src="site.logoSrc" :alt="site.name" />
        <span v-else class="brand-mark"></span>
        <span>博客后台</span>
      </RouterLink>
      <nav class="admin-menu">
        <RouterLink to="/admin/dashboard">数据看板</RouterLink>
        <RouterLink to="/admin/settings">站点设置</RouterLink>
        <RouterLink to="/admin/mail-settings">邮箱设置</RouterLink>
        <RouterLink to="/admin/articles">内容管理</RouterLink>
        <RouterLink to="/admin/taxonomies">分类标签</RouterLink>
        <RouterLink to="/admin/images">图片资源</RouterLink>
        <RouterLink to="/admin/comments">评论审核</RouterLink>
        <RouterLink to="/admin/users">用户权限</RouterLink>
        <RouterLink to="/admin/logs">操作日志</RouterLink>
      </nav>
    </aside>
    <main class="admin-main">
      <div class="admin-header">
        <h1>{{ title }}</h1>
        <div class="hero-actions">
          <button class="btn-ghost" @click="passwordVisible = true">修改密码</button>
          <RouterLink class="btn-ghost" to="/">打开前台首页</RouterLink>
          <button class="btn-ghost danger-action" @click="logout">退出登录</button>
        </div>
      </div>
      <el-alert v-if="auth.passwordChangeRequired" class="admin-alert" title="当前管理员仍在使用默认密码，请立即修改。生产环境会禁止默认密码启动。" type="error" :closable="false" />
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
        <button class="btn-ghost" @click="passwordVisible = false">取消</button>
        <button class="btn-primary" @click="changePassword">保存</button>
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

const route = useRoute()
const router = useRouter()
const site = useSiteStore()
const auth = useAuthStore()
const names = {
  '/admin/dashboard': '数据看板',
  '/admin/settings': '站点设置',
  '/admin/mail-settings': '邮箱设置',
  '/admin/articles': '内容管理',
  '/admin/taxonomies': '分类标签',
  '/admin/images': '图片资源管理',
  '/admin/comments': '评论审核',
  '/admin/users': '用户权限',
  '/admin/logs': '操作日志'
}
const title = computed(() => names[route.path] || '后台管理')
const passwordVisible = ref(false)
const password = reactive({ oldPassword: '', newPassword: '' })

onMounted(() => site.loadSite())

const changePassword = async () => {
  if (!password.oldPassword || !password.newPassword) {
    ElMessage.warning('请填写原密码和新密码')
    return
  }
  await userApi.changePassword({ ...password })
  auth.clearPasswordRequired()
  password.oldPassword = ''
  password.newPassword = ''
  passwordVisible.value = false
  ElMessage.success('密码已修改')
}

const logout = () => {
  auth.logout('admin')
  router.push('/admin/login')
}
</script>

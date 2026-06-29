<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <RouterLink class="brand" to="/"><span class="brand-mark"></span><span>星绘后台</span></RouterLink>
      <nav class="admin-menu">
        <RouterLink to="/admin/dashboard">仪表盘</RouterLink>
        <RouterLink to="/admin/articles">文章管理</RouterLink>
        <RouterLink to="/admin/taxonomies">分类标签</RouterLink>
        <RouterLink to="/admin/images">图片链接</RouterLink>
        <RouterLink to="/admin/comments">评论审核</RouterLink>
        <RouterLink to="/admin/users">用户权限</RouterLink>
      </nav>
    </aside>
    <main class="admin-main">
      <div class="admin-header">
        <h1>{{ title }}</h1>
        <div class="hero-actions">
          <button class="btn-ghost" @click="passwordVisible = true">修改密码</button>
          <RouterLink class="btn-ghost" to="/">返回前台</RouterLink>
        </div>
      </div>
      <RouterView />
    </main>
    <el-dialog v-model="passwordVisible" title="修改密码" width="420px">
      <el-form label-position="top">
        <el-form-item label="原密码"><el-input v-model="password.oldPassword" type="password" show-password /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="password.newPassword" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <button class="btn-ghost" @click="passwordVisible = false">取消</button>
        <button class="btn-primary" @click="changePassword">保存</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { userApi } from '../../api/blog'

const route = useRoute()
const names = {
  '/admin/dashboard': '仪表盘',
  '/admin/articles': '文章管理',
  '/admin/taxonomies': '分类标签',
  '/admin/images': '图片链接管理',
  '/admin/comments': '评论审核',
  '/admin/users': '用户权限'
}
const title = computed(() => names[route.path] || '后台管理')
const passwordVisible = ref(false)
const password = reactive({ oldPassword: '', newPassword: '' })
const changePassword = async () => {
  await userApi.changePassword({ ...password })
  password.oldPassword = ''
  password.newPassword = ''
  passwordVisible.value = false
  ElMessage.success('密码已修改')
}
</script>

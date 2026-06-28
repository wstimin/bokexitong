<template>
  <main class="login-page">
    <section class="login-box">
      <RouterLink class="brand" to="/"><span class="brand-mark"></span><span>星绘 Blog</span></RouterLink>
      <h1>登录</h1>
      <el-form label-position="top">
        <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" type="password" show-password /></el-form-item>
      </el-form>
      <button class="btn-primary" style="width: 100%" @click="login">进入系统</button>
    </section>
  </main>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const form = reactive({ username: 'admin', password: '123456' })
const auth = useAuthStore()
const router = useRouter()
const login = async () => {
  await auth.login(form)
  router.push(auth.isAdmin ? '/admin/dashboard' : '/')
}
</script>

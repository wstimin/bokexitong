<template>
  <main class="login-page install-page">
    <section class="login-box install-box">
      <div class="auth-title">
        <h1>首次安装</h1>
        <p>填写基础信息后完成初始化，之后会自动进入正常站点。</p>
      </div>

      <el-form label-position="top">
        <el-form-item label="站点名称">
          <el-input v-model="form.siteName" placeholder="博客系统" />
        </el-form-item>
        <el-form-item label="访问域名">
          <el-input v-model="form.domain" placeholder="example.com" />
        </el-form-item>
        <el-form-item label="管理员密码">
          <el-input v-model="form.adminPassword" type="password" show-password placeholder="至少 8 位" />
        </el-form-item>
      </el-form>

      <button class="btn-primary auth-submit" type="button" :disabled="loading" @click="submit">
        {{ loading ? '正在安装...' : '完成安装' }}
      </button>
    </section>
  </main>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { installApi } from '../api/blog'

const router = useRouter()
const loading = ref(false)
const form = reactive({
  siteName: '博客系统',
  domain: '',
  adminPassword: ''
})

onMounted(async () => {
  try {
    const res = await installApi.status()
    if (res.data?.installed) {
      router.replace('/')
    }
  } catch (error) {
    console.error(error)
  }
})

const submit = async () => {
  if (!form.adminPassword.trim()) {
    ElMessage.warning('请把必填项填完整')
    return
  }
  loading.value = true
  try {
    await installApi.install({ ...form })
    ElMessage.success('安装完成')
    router.replace('/')
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}
</script>

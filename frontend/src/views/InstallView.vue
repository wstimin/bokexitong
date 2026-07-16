<template>
  <main class="login-page install-page">
    <section class="login-box install-box">
      <div class="auth-title">
        <h1>网页安装向导</h1>
        <p>填写数据库和站点信息，系统会自动初始化并重启进入正常站点。</p>
      </div>

      <el-form label-position="top">
        <div class="install-section">
          <h2>数据库连接</h2>
          <div class="install-grid">
            <el-form-item label="数据库主机">
              <el-input v-model="form.dbHost" placeholder="mysql 或 127.0.0.1" />
            </el-form-item>
            <el-form-item label="端口">
              <el-input v-model="form.dbPort" placeholder="3306" />
            </el-form-item>
            <el-form-item label="数据库名">
              <el-input v-model="form.dbName" placeholder="personal_blog" />
            </el-form-item>
            <el-form-item label="数据库用户">
              <el-input v-model="form.dbUsername" placeholder="root" />
            </el-form-item>
          </div>
          <el-form-item label="数据库密码">
            <el-input v-model="form.dbPassword" type="password" show-password placeholder="请输入数据库密码" />
          </el-form-item>
        </div>

        <div class="install-section">
          <h2>站点信息</h2>
          <div class="install-grid">
            <el-form-item label="站点名称">
              <el-input v-model="form.siteName" placeholder="博客系统" />
            </el-form-item>
            <el-form-item label="访问域名">
              <el-input v-model="form.domain" placeholder="example.com" />
            </el-form-item>
          </div>
          <el-form-item label="管理员密码">
            <el-input v-model="form.adminPassword" type="password" show-password placeholder="至少 8 位" />
          </el-form-item>
        </div>
      </el-form>

      <button class="btn-primary auth-submit" type="button" :disabled="loading" @click="submit">
        {{ loading ? '正在初始化...' : '开始安装' }}
      </button>
      <p v-if="restarting" class="install-restart-tip">配置已保存，服务正在自动重启。稍等几秒后会进入站点首页。</p>
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
const restarting = ref(false)
const form = reactive({
  siteName: '博客系统',
  domain: '',
  adminPassword: '',
  dbHost: 'mysql',
  dbPort: '3306',
  dbName: 'personal_blog',
  dbUsername: 'root',
  dbPassword: ''
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
  if (!form.dbHost.trim() || !form.dbPort.trim() || !form.dbName.trim() || !form.dbUsername.trim() || !form.adminPassword.trim()) {
    ElMessage.warning('请把必填项填完整')
    return
  }
  if (form.adminPassword.trim().length < 8) {
    ElMessage.warning('管理员密码至少 8 位')
    return
  }
  loading.value = true
  try {
    await installApi.install({ ...form })
    restarting.value = true
    ElMessage.success('安装配置已保存，正在重启服务')
    window.setTimeout(() => router.replace('/'), 6500)
  } catch (error) {
    console.error(error)
  } finally {
    if (!restarting.value) {
      loading.value = false
    }
  }
}
</script>

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
              <el-input v-model="form.dbHost" placeholder="127.0.0.1 或数据库内网地址" />
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
const sleep = (milliseconds) => new Promise((resolve) => window.setTimeout(resolve, milliseconds))
const form = reactive({
  siteName: '博客系统',
  domain: '',
  adminPassword: '',
  dbHost: '127.0.0.1',
  dbPort: '3306',
  dbName: 'personal_blog',
  dbUsername: 'root',
  dbPassword: ''
})

onMounted(() => {
  // Do not block the form on a database check. This is especially important
  // on a fresh 1Panel/BT deployment where the datasource is not configured yet.
  void installApi.status({ silentError: true })
    .then((res) => {
      if (res.data?.installed) {
        router.replace('/')
      }
    })
    .catch((error) => {
      console.error(error)
    })
})

const waitForService = async () => {
  await sleep(4000)
  const deadline = Date.now() + 60000

  while (Date.now() < deadline) {
    try {
      const res = await installApi.status({ timeout: 4000, silentError: true })
      if (res.data?.installed) {
        window.location.replace('/')
        return
      }
    } catch (error) {
      console.debug('Waiting for the service to restart', error)
    }
    await sleep(1200)
  }

  restarting.value = false
  loading.value = false
  ElMessage.warning('服务重启时间较长，请稍后刷新页面；如果仍无法访问，请查看 Java 服务日志')
}

const isRestartDisconnect = (error) => {
  const status = error.response?.status
  return status === 502 || status === 503 || status === 504 || error.code === 'ECONNABORTED' || !error.response
}

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
    let responseInterrupted = false
    try {
      await installApi.install({ ...form })
    } catch (error) {
      if (!isRestartDisconnect(error)) {
        throw error
      }
      responseInterrupted = true
      console.debug('Install response was interrupted by the service restart', error)
    }
    restarting.value = true
    if (responseInterrupted) {
      ElMessage.info('连接暂时中断，正在确认安装结果并等待服务恢复')
    } else {
      ElMessage.success('安装配置已保存，正在重启服务')
    }
    await waitForService()
  } catch (error) {
    console.error(error)
  } finally {
    if (!restarting.value) {
      loading.value = false
    }
  }
}
</script>

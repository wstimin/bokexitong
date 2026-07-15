<template>
  <section class="admin-card settings-panel">
    <div class="toolbar user-toolbar">
      <div>
        <h2>邮件设置</h2>
        <p class="section-subtitle">配置注册、找回密码和测试邮件使用的 SMTP 服务。</p>
      </div>
      <button class="btn-primary" type="button" :disabled="loading" @click="save">保存邮件设置</button>
    </div>

    <el-alert class="settings-alert" title="启用邮箱验证码前，请先填写真实的 SMTP 服务、账号和授权码。密码留空保存时会保留服务器中已有密码。" type="info" :closable="false" />

    <el-form label-position="top" class="settings-form">
      <el-form-item label="启用邮箱验证码">
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

      <el-divider content-position="left">发送测试</el-divider>
      <el-form-item label="测试接收邮箱">
        <div class="inline-field">
          <el-input v-model="testEmail" placeholder="先保存邮件配置，再发送测试邮件" />
          <button class="btn-ghost" type="button" :disabled="loading" @click="sendTestMail">发送测试</button>
        </div>
      </el-form-item>
    </el-form>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '../../api/blog'

const loading = ref(false)
const testEmail = ref('')
const form = reactive({ mailEnabled: false, mailHost: '', mailPort: 587, mailUsername: '', mailPassword: '', mailFromName: '', mailSmtpAuth: true, mailStarttlsEnable: true, mailSslEnable: false })

const apply = (settings = {}) => {
  form.mailEnabled = settings.mailEnabled === 'true'
  form.mailHost = settings.mailHost || ''
  form.mailPort = Number(settings.mailPort || 587)
  form.mailUsername = settings.mailUsername || ''
  form.mailPassword = ''
  form.mailFromName = settings.mailFromName || '博客系统'
  form.mailSmtpAuth = settings.mailSmtpAuth !== 'false'
  form.mailStarttlsEnable = settings.mailStarttlsEnable !== 'false'
  form.mailSslEnable = settings.mailSslEnable === 'true'
}

const load = async () => {
  loading.value = true
  try {
    const res = await adminApi.mailSettings()
    apply(res.data)
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const save = async () => {
  loading.value = true
  try {
    const payload = {
      ...form,
      mailEnabled: String(form.mailEnabled),
      mailPort: String(form.mailPort || 587),
      mailSmtpAuth: String(form.mailSmtpAuth),
      mailStarttlsEnable: String(form.mailStarttlsEnable),
      mailSslEnable: String(form.mailSslEnable)
    }
    const res = await adminApi.saveMailSettings(payload)
    apply(res.data)
    ElMessage.success('邮件设置已保存')
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const sendTestMail = async () => {
  if (!testEmail.value.trim()) {
    ElMessage.warning('请填写测试接收邮箱')
    return
  }
  loading.value = true
  try {
    await adminApi.testMail({ email: testEmail.value.trim() })
    ElMessage.success('测试邮件已发送')
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

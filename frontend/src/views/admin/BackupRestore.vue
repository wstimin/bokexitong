<template>
  <section class="backup-center">
    <div class="backup-hero admin-card">
      <div>
        <span class="section-kicker">Portable site archive</span>
        <h2>把整座博客，完整地带走。</h2>
        <p>备份包含数据库、文章、本地图片和附件，可在任何部署方式的新服务器中恢复。</p>
      </div>
      <div class="backup-seal" aria-label="跨部署备份">
        <span>FORMAT</span><strong>.bkbak</strong><small>跨部署 · 可校验</small>
      </div>
    </div>

    <div class="backup-grid">
      <section class="admin-card backup-card">
        <div class="backup-card__head">
          <div><span class="section-kicker">Create backup</span><h3>下载整站备份</h3></div>
          <span class="backup-icon" aria-hidden="true">↓</span>
        </div>
        <p class="backup-copy">数据库记录和 <code>uploads</code> 中的头像、背景、封面、正文图片及附件会一起打包。</p>
        <label class="backup-field">
          <span>备份密码（可选）</span>
          <el-input v-model="exportPassword" type="password" show-password maxlength="64" placeholder="至少 8 位；不填写则不加密" />
        </label>
        <div class="backup-note">
          <strong>建议设置密码</strong>
          <span>备份可能包含账号、邮箱和邮件配置。密码只用于本次加密，不会保存到服务器。</span>
        </div>
        <button class="btn-primary backup-main-action" type="button" :disabled="exporting" @click="downloadBackup">
          {{ exporting ? '正在整理数据库和图片…' : '创建并下载备份' }}
        </button>
      </section>

      <section class="admin-card backup-card restore-card">
        <div class="backup-card__head">
          <div><span class="section-kicker">Restore site</span><h3>上传并恢复</h3></div>
          <span class="backup-icon is-restore" aria-hidden="true">↥</span>
        </div>
        <input ref="fileInput" class="backup-file-input" type="file" accept=".bkbak" @change="selectFile" />
        <button class="backup-dropzone" type="button" :disabled="busy" @click="fileInput?.click()">
          <span class="backup-dropzone__mark">B</span>
          <span v-if="restoreFile"><strong>{{ restoreFile.name }}</strong><small>{{ formatBytes(restoreFile.size) }}</small></span>
          <span v-else><strong>选择 .bkbak 备份包</strong><small>大文件会自动分片上传，不受普通图片上传限制</small></span>
        </button>
        <label class="backup-field">
          <span>备份密码</span>
          <el-input v-model="restorePassword" type="password" show-password maxlength="64" placeholder="未加密的备份可留空" />
        </label>
        <div v-if="uploading || uploadProgress > 0" class="backup-progress" aria-live="polite">
          <div><strong>{{ progressLabel }}</strong><span>{{ uploadProgress }}%</span></div>
          <div class="backup-progress__track"><span :style="{ transform: `scaleX(${uploadProgress / 100})` }"></span></div>
        </div>
        <button v-if="!inspection" class="btn-ghost backup-main-action" type="button" :disabled="!restoreFile || busy" @click="uploadAndInspect">
          {{ inspecting ? '正在校验备份完整性…' : uploadId ? '重新校验备份' : '上传并校验' }}
        </button>

        <div v-else class="backup-inspection">
          <div class="backup-inspection__title"><span>✓</span><strong>完整性校验通过</strong></div>
          <dl>
            <div><dt>来源版本</dt><dd>v{{ inspection.manifest.appVersion }}</dd></div>
            <div><dt>创建时间</dt><dd>{{ formatDate(inspection.manifest.createdAt) }}</dd></div>
            <div><dt>数据库记录</dt><dd>{{ inspection.totalRows }} 条</dd></div>
            <div><dt>本地文件</dt><dd>{{ inspection.manifest.uploadFiles }} 个</dd></div>
            <div><dt>图片附件大小</dt><dd>{{ formatBytes(inspection.manifest.uploadBytes) }}</dd></div>
            <div><dt>加密状态</dt><dd>{{ inspection.encrypted ? '已加密' : '未加密' }}</dd></div>
          </dl>
          <label class="backup-field confirmation-field">
            <span>输入“确认恢复”</span>
            <el-input v-model="confirmation" placeholder="确认恢复" autocomplete="off" />
          </label>
          <button class="btn-primary danger-restore backup-main-action" type="button" :disabled="confirmation !== '确认恢复' || restoring" @click="restoreBackup">
            {{ restoring ? '正在恢复数据库和图片…' : '恢复这份整站备份' }}
          </button>
        </div>
      </section>
    </div>

    <div class="backup-safety admin-card">
      <div><span>01</span><strong>跨部署</strong><small>1Panel、宝塔、Docker 和 JAR 使用同一种备份格式。</small></div>
      <div><span>02</span><strong>图片不遗漏</strong><small>本地上传文件逐个计算 SHA-256，恢复前必须全部校验通过。</small></div>
      <div><span>03</span><strong>安全恢复</strong><small>先验证版本与内容，再事务恢复数据库并原子替换上传目录。</small></div>
    </div>
    <p class="backup-external-tip">说明：第三方外链图片只会保留原网址；上传到本站的本地图片和附件会完整进入备份包。</p>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { backupApi } from '../../api/blog'
import { useAuthStore } from '../../stores/auth'

const CHUNK_SIZE = 5 * 1024 * 1024
const fileInput = ref(null)
const exportPassword = ref('')
const restorePassword = ref('')
const confirmation = ref('')
const restoreFile = ref(null)
const uploadId = ref('')
const inspection = ref(null)
const uploadProgress = ref(0)
const progressStage = ref('准备上传')
const exporting = ref(false)
const uploading = ref(false)
const inspecting = ref(false)
const restoring = ref(false)
const auth = useAuthStore()

const busy = computed(() => uploading.value || inspecting.value || restoring.value)
const progressLabel = computed(() => progressStage.value)
const formatBytes = (value) => {
  const bytes = Number(value || 0)
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 ** 2) return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1024 ** 3) return `${(bytes / 1024 ** 2).toFixed(1)} MB`
  return `${(bytes / 1024 ** 3).toFixed(2)} GB`
}
const formatDate = (value) => value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '—'
const backupFilename = () => {
  const stamp = new Date().toISOString().replace(/[-:]/g, '').replace('T', '-').slice(0, 15)
  return `bokexitong-${stamp}.bkbak`
}

const downloadBackup = async () => {
  if (exportPassword.value && exportPassword.value.trim().length < 8) return ElMessage.warning('备份密码至少需要 8 位，不能只包含空格')
  exporting.value = true
  try {
    const data = await backupApi.exportBackup({ password: exportPassword.value })
    const blob = data instanceof Blob ? data : new Blob([data], { type: 'application/octet-stream' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = backupFilename()
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.setTimeout(() => URL.revokeObjectURL(url), 1500)
    ElMessage.success('整站备份已生成，正在下载')
  } finally {
    exporting.value = false
  }
}

const cancelCurrentUpload = async () => {
  const current = uploadId.value
  uploadId.value = ''
  inspection.value = null
  uploadProgress.value = 0
  confirmation.value = ''
  if (current) await backupApi.cancelUpload(current).catch(() => {})
}

const selectFile = async (event) => {
  await cancelCurrentUpload()
  restoreFile.value = event.target.files?.[0] || null
  if (restoreFile.value && !restoreFile.value.name.toLowerCase().endsWith('.bkbak')) {
    restoreFile.value = null
    event.target.value = ''
    ElMessage.warning('请选择 .bkbak 整站备份文件')
  }
}

const uploadAndInspect = async () => {
  if (!restoreFile.value) return
  try {
    if (!uploadId.value) {
      uploading.value = true
      progressStage.value = '正在分片上传备份'
      const totalChunks = Math.ceil(restoreFile.value.size / CHUNK_SIZE)
      const started = await backupApi.beginUpload({ filename: restoreFile.value.name, size: restoreFile.value.size, totalChunks })
      uploadId.value = started.data.uploadId
      for (let index = 0; index < totalChunks; index += 1) {
        const chunk = restoreFile.value.slice(index * CHUNK_SIZE, Math.min(restoreFile.value.size, (index + 1) * CHUNK_SIZE))
        const response = await backupApi.uploadChunk(uploadId.value, index, chunk)
        uploadProgress.value = response.data.progress
      }
      uploading.value = false
    }
    inspecting.value = true
    progressStage.value = '正在校验版本、数据库和图片'
    const response = await backupApi.inspectUpload(uploadId.value, { password: restorePassword.value })
    inspection.value = response.data
    uploadProgress.value = 100
    ElMessage.success('备份完整性校验通过')
  } catch (error) {
    console.error(error)
  } finally {
    uploading.value = false
    inspecting.value = false
  }
}

const restoreBackup = async () => {
  if (!inspection.value || confirmation.value !== '确认恢复') return
  await ElMessageBox.confirm(
    `即将用 v${inspection.value.manifest.appVersion} 的备份覆盖当前博客数据库和本地图片。系统会先创建恢复前快照，确认继续吗？`,
    '最后确认整站恢复',
    { confirmButtonText: '确认恢复', cancelButtonText: '取消', type: 'warning', closeOnClickModal: false }
  )
  restoring.value = true
  progressStage.value = '正在事务恢复数据库并替换图片'
  try {
    const response = await backupApi.restoreUpload(uploadId.value, { confirmation: confirmation.value })
    const loginPath = response.data.adminLoginPath || '/admin/login'
    ElMessage.success(`已恢复 ${response.data.restoredRows} 条数据和 ${response.data.restoredFiles} 个文件`)
    uploadId.value = ''
    auth.logout('all')
    localStorage.removeItem('blog_site_settings_cache')
    sessionStorage.removeItem('blog_welcome_gate_entered')
    window.setTimeout(() => window.location.assign(loginPath), 800)
  } catch (error) {
    console.error(error)
    restoring.value = false
  }
}

onBeforeUnmount(() => {
  if (uploadId.value && !restoring.value) backupApi.cancelUpload(uploadId.value).catch(() => {})
})
</script>

<style scoped>
.backup-center { display: grid; gap: 22px; }
.backup-hero { min-height: 210px; display: flex; align-items: center; justify-content: space-between; gap: 32px; padding: 34px 38px; background: radial-gradient(circle at 82% 22%, color-mix(in srgb, var(--accent) 22%, transparent), transparent 34%), var(--bg-elevated); }
.backup-hero h2 { margin: 0; color: var(--text-primary); font: 500 clamp(28px, 4vw, 46px)/1.18 Georgia, 'Songti SC', serif; letter-spacing: .04em; }
.backup-hero p { max-width: 660px; margin: 16px 0 0; color: var(--text-secondary); line-height: 1.8; }
.backup-seal { flex: 0 0 150px; aspect-ratio: 1; border-radius: 50%; display: grid; place-content: center; text-align: center; border: 1px solid color-mix(in srgb, var(--accent) 55%, var(--border)); box-shadow: inset 0 0 0 8px var(--bg-secondary), 0 18px 45px color-mix(in srgb, var(--accent) 15%, transparent); }
.backup-seal span, .backup-seal small { color: var(--text-tertiary); font-size: 9px; letter-spacing: .16em; }
.backup-seal strong { margin: 5px 0; color: var(--text-primary); font: 600 20px/1 Georgia, serif; }
.backup-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 22px; align-items: start; }
.backup-card { display: grid; gap: 20px; padding: 28px; }
.backup-card__head { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; }
.backup-card__head h3 { margin: 0; color: var(--text-primary); font-size: 22px; }
.backup-icon { width: 42px; height: 42px; border-radius: 50%; display: grid; place-items: center; color: var(--accent); border: 1px solid color-mix(in srgb, var(--accent) 45%, var(--border)); font-size: 22px; }
.backup-icon.is-restore { transform: rotate(180deg); }
.backup-copy { margin: -4px 0 0; color: var(--text-secondary); line-height: 1.8; }
.backup-copy code { color: var(--accent); }
.backup-field { display: grid; gap: 8px; }
.backup-field > span { color: var(--text-secondary); font-size: 13px; font-weight: 700; }
.backup-note { display: grid; gap: 5px; padding: 14px 16px; border: 1px solid var(--border); border-radius: 12px; background: var(--bg-secondary); }
.backup-note strong { color: var(--text-primary); font-size: 13px; }
.backup-note span { color: var(--text-tertiary); font-size: 12px; line-height: 1.65; }
.backup-main-action { width: 100%; min-height: 44px; justify-content: center; }
.backup-file-input { display: none; }
.backup-dropzone { width: 100%; min-height: 116px; padding: 20px; display: flex; align-items: center; gap: 16px; text-align: left; color: var(--text-primary); border: 1px dashed color-mix(in srgb, var(--accent) 46%, var(--border)); border-radius: 14px; background: color-mix(in srgb, var(--accent) 5%, var(--bg-secondary)); cursor: pointer; transition: border-color 160ms ease, transform 160ms ease, background 160ms ease; }
.backup-dropzone:hover:not(:disabled) { transform: translateY(-2px); border-color: var(--accent); background: color-mix(in srgb, var(--accent) 9%, var(--bg-secondary)); }
.backup-dropzone__mark { flex: 0 0 48px; height: 48px; border-radius: 14px; display: grid; place-items: center; color: var(--accent); background: var(--bg-elevated); border: 1px solid var(--border); font: 600 22px/1 Georgia, serif; }
.backup-dropzone > span:last-child { min-width: 0; display: grid; gap: 6px; }
.backup-dropzone strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.backup-dropzone small { color: var(--text-tertiary); line-height: 1.5; }
.backup-progress { display: grid; gap: 9px; }
.backup-progress > div:first-child { display: flex; justify-content: space-between; color: var(--text-secondary); font-size: 12px; }
.backup-progress__track { height: 7px; overflow: hidden; border-radius: 999px; background: var(--bg-secondary); }
.backup-progress__track span { display: block; width: 100%; height: 100%; transform-origin: left; border-radius: inherit; background: linear-gradient(90deg, var(--accent), #8bd8d4, #d99fc0); transition: transform 180ms ease; }
.backup-inspection { display: grid; gap: 18px; padding-top: 4px; }
.backup-inspection__title { display: flex; align-items: center; gap: 9px; color: #5fae88; }
.backup-inspection__title span { width: 24px; height: 24px; display: grid; place-items: center; border-radius: 50%; background: color-mix(in srgb, #5fae88 14%, transparent); }
.backup-inspection dl { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); margin: 0; overflow: hidden; border: 1px solid var(--border); border-radius: 12px; background: var(--border); gap: 1px; }
.backup-inspection dl div { min-width: 0; padding: 13px; background: var(--bg-secondary); }
.backup-inspection dt { color: var(--text-tertiary); font-size: 10px; font-weight: 800; letter-spacing: .06em; }
.backup-inspection dd { margin: 5px 0 0; overflow: hidden; text-overflow: ellipsis; color: var(--text-primary); font-size: 13px; font-weight: 700; }
.confirmation-field { padding-top: 4px; }
.danger-restore { background: #b85757; border-color: #b85757; }
.backup-safety { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 0; padding: 0; }
.backup-safety > div { position: relative; min-height: 128px; padding: 24px 26px; display: grid; align-content: center; gap: 7px; }
.backup-safety > div + div { border-left: 1px solid var(--border); }
.backup-safety span { position: absolute; top: 15px; right: 18px; color: color-mix(in srgb, var(--accent) 42%, transparent); font: 500 28px/1 Georgia, serif; }
.backup-safety strong { color: var(--text-primary); }
.backup-safety small { color: var(--text-tertiary); line-height: 1.65; }
.backup-external-tip { margin: -4px 4px 0; color: var(--text-tertiary); font-size: 12px; text-align: center; }
@media (max-width: 900px) { .backup-grid { grid-template-columns: 1fr; } .backup-hero { align-items: flex-start; } .backup-seal { flex-basis: 118px; } }
@media (max-width: 680px) { .backup-hero { padding: 26px 22px; } .backup-seal { display: none; } .backup-card { padding: 22px 18px; } .backup-safety { grid-template-columns: 1fr; } .backup-safety > div + div { border-left: 0; border-top: 1px solid var(--border); } .backup-inspection dl { grid-template-columns: 1fr; } }
</style>

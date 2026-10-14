<template>
  <section class="update-center">
    <div class="update-hero admin-card">
      <div>
        <span class="section-kicker">System release</span>
        <h2>保持博客优雅而新鲜。</h2>
        <p>系统会从受信任的发布清单检查新版本，并由部署层更新代理完成校验、替换和重启。</p>
      </div>
      <div class="version-orbit" aria-label="当前版本">
        <span>Current</span>
        <strong>v{{ info.currentVersion || '—' }}</strong>
        <small>{{ info.buildTime || 'development' }}</small>
      </div>
    </div>

    <div class="update-grid">
      <section class="admin-card update-card">
        <div class="update-card__head">
          <div>
            <span class="section-kicker">Update channel</span>
            <h3>稳定版通道</h3>
          </div>
          <span class="status-pill" :class="statusClass">{{ statusLabel }}</span>
        </div>
        <dl class="system-facts">
          <div><dt>当前版本</dt><dd>v{{ info.currentVersion || '—' }}</dd></div>
          <div><dt>最新版本</dt><dd>{{ latestVersion }}</dd></div>
          <div><dt>部署方式</dt><dd>{{ modeLabel }}</dd></div>
          <div><dt>更新代理</dt><dd>{{ info.agentAvailable ? '已连接' : '未启用' }}</dd></div>
        </dl>
        <div class="update-message">{{ message }}</div>
        <div class="update-actions">
          <button class="btn-ghost" type="button" :disabled="checking || installing" @click="checkUpdate">
            {{ checking ? '正在检查…' : '检查更新' }}
          </button>
          <button class="btn-primary" type="button" :disabled="!canInstall || installing" @click="installUpdate">
            {{ installing ? '正在交接更新…' : '一键更新' }}
          </button>
        </div>
      </section>

      <section class="admin-card release-notes">
        <span class="section-kicker">Release notes</span>
        <h3>{{ checked.latestVersion ? `v${checked.latestVersion}` : '更新说明' }}</h3>
        <p>{{ checked.notes || '检查更新后，这里会显示新版本的更新内容。' }}</p>
        <small v-if="checked.releaseDate">发布时间 · {{ checked.releaseDate }}</small>
        <div class="safety-note">
          <strong>安全更新链路</strong>
          <span>HTTPS 清单 → SHA-256 完整性校验 → 部署代理 → 服务重启</span>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi, portalApi } from '../../api/blog'

const info = ref({})
const checked = ref({})
const checking = ref(false)
const installing = ref(false)

const activeState = computed(() => info.value.state || 'IDLE')
const canInstall = computed(() => checked.value.available && info.value.agentAvailable)
const latestVersion = computed(() => checked.value.latestVersion ? `v${checked.value.latestVersion}` : '尚未检查')
const modeLabel = computed(() => ({ docker: 'Docker Compose', web: '面板 / Java 包', disabled: '未配置' }[info.value.mode] || info.value.mode || '未知'))
const message = computed(() => checked.value.message || info.value.message || '准备检查新版本')
const statusLabel = computed(() => {
  if (checked.value.available) return '有新版本'
  return ({ REQUESTED: '等待接管', DOWNLOADING: '下载中', INSTALLING: '安装中', RESTARTING: '重启中', FAILED: '更新失败' }[activeState.value] || '运行正常')
})
const statusClass = computed(() => ({
  'is-ready': checked.value.available,
  'is-progress': ['REQUESTED', 'DOWNLOADING', 'INSTALLING', 'RESTARTING'].includes(activeState.value),
  'is-error': activeState.value === 'FAILED'
}))

const loadStatus = async () => {
  const res = await adminApi.updateStatus()
  info.value = res.data || {}
}

const checkUpdate = async () => {
  checking.value = true
  try {
    await loadStatus()
    const res = await adminApi.checkUpdate()
    checked.value = res.data || {}
    ElMessage.success(checked.value.message || '检查完成')
  } finally {
    checking.value = false
  }
}

const installUpdate = async () => {
  await ElMessageBox.confirm(
    `将从 v${info.value.currentVersion} 更新到 v${checked.value.latestVersion}，服务会短暂重启。确认继续吗？`,
    '开始系统更新',
    { confirmButtonText: '确认更新', cancelButtonText: '稍后再说', type: 'warning' }
  )
  installing.value = true
  try {
    await adminApi.installUpdate()
    info.value.state = 'REQUESTED'
    info.value.message = '更新任务已提交，部署代理正在接管。页面将在服务恢复后重新连接。'
    ElMessage.success('更新任务已提交')
    await waitForUpdatedVersion(checked.value.latestVersion)
  } catch (error) {
    installing.value = false
    throw error
  }
}

const sleep = (milliseconds) => new Promise((resolve) => window.setTimeout(resolve, milliseconds))
const waitForUpdatedVersion = async (targetVersion) => {
  const deadline = Date.now() + 180_000
  await sleep(3500)
  while (Date.now() < deadline) {
    try {
      const response = await portalApi.version()
      if (response.data?.version === targetVersion) {
        window.location.reload()
        return
      }
    } catch (error) {
      console.debug('Waiting for updated service', error)
    }
    await sleep(1800)
  }
  installing.value = false
  info.value.message = '更新耗时较长，请稍后刷新页面查看版本状态。'
  ElMessage.warning(info.value.message)
}

onMounted(() => loadStatus().catch((error) => console.error(error)))
</script>

import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import {
  createHeroTextStyle,
  normalizeHeroColor,
  normalizeHeroTextShadow
} from '../src/utils/heroAppearance.js'
import {
  adminBasePathFromLogin,
  adminPagePath,
  isAdminPagePath
} from '../src/utils/adminPath.js'

const readSource = (relativePath) => readFile(new URL(relativePath, import.meta.url), 'utf8')

test('hero colors only accept six-digit hex values', () => {
  assert.equal(normalizeHeroColor('#A1B2C3'), '#a1b2c3')
  assert.equal(normalizeHeroColor('red'), '#f7f5f2')
  assert.equal(normalizeHeroColor('url(javascript:alert(1))'), '#f7f5f2')
})

test('hero shadow mode uses the supported presets', () => {
  assert.equal(normalizeHeroTextShadow('light'), 'light')
  assert.equal(normalizeHeroTextShadow('custom'), 'dark')
})

test('hero style exposes normalized CSS variables', () => {
  assert.deepEqual(createHeroTextStyle('#111827', '#F8FAFC', 'none'), {
    '--hero-title-color': '#111827',
    '--hero-subtitle-color': '#f8fafc',
    '--hero-title-shadow': 'none',
    '--hero-subtitle-shadow': 'none'
  })
})

test('admin login path uses a dedicated save action and hard redirect', async () => {
  const layoutSource = await readSource('../src/views/admin/AdminLayout.vue')
  const settingSource = await readSource('../src/views/admin/SiteSetting.vue')
  const storeSource = await readSource('../src/stores/site.js')
  const apiSource = await readSource('../src/api/blog.js')

  assert.match(layoutSource, /router\.push\(site\.adminLoginPath \|\| '\/admin\/login'\)/)
  assert.match(settingSource, /site\.setAdminLoginPath\(form\.adminLoginPath\)/)
  assert.match(settingSource, /delete payload\.adminLoginPath/)
  assert.match(settingSource, /adminApi\.saveAdminLoginPath\(\{ adminLoginPath: form\.adminLoginPath \}\)/)
  assert.match(settingSource, /auth\.logout\('admin'\)/)
  assert.match(settingSource, /window\.location\.replace\(nextAdminLoginPath\)/)
  assert.match(apiSource, /saveAdminLoginPath: \(data\) => http\.put\('\/admin\/site-settings\/admin-login-path', data\)/)
  assert.match(storeSource, /setAdminLoginPath\(path\)/)
})

test('admin login entry always refreshes server settings and rejects stale paths', async () => {
  const routerSource = await readSource('../src/router/index.js')
  const loginSource = await readSource('../src/views/LoginView.vue')
  const storeSource = await readSource('../src/stores/site.js')

  assert.doesNotMatch(routerSource, /path: '\/admin\/login', component:/)
  assert.doesNotMatch(routerSource, /path: '\/admin',/)
  assert.match(routerSource, /path: '\/:adminLoginBase\(\.\*\)\/login'/)
  assert.match(routerSource, /path: '\/:adminBase\(\.\*\)'/)
  assert.match(routerSource, /const checksAdminRoute = Boolean\(to\.meta\.dynamicAdminLogin \|\| to\.meta\.dynamicAdminArea\)/)
  assert.match(routerSource, /if \(requestedBasePath !== adminBasePath\) return '\/'/)
  assert.match(loginSource, /const siteLoaded = await site\.loadSite\(true\)/)
  assert.match(loginSource, /window\.location\.pathname !== site\.adminLoginPath/)
  assert.match(loginSource, /window\.location\.replace\('\/'\)/)
  assert.match(loginSource, /adminLoginPath: isAdminLogin\.value \? window\.location\.pathname : undefined/)
  assert.match(storeSource, /if \(siteLoadPromise\) return siteLoadPromise/)
})

test('custom login path also controls every admin page route', () => {
  assert.equal(adminBasePathFromLogin('/shiyeotimin/login'), '/shiyeotimin')
  assert.equal(adminPagePath('/shiyeotimin/login', 'settings'), '/shiyeotimin/settings')
  assert.equal(adminPagePath('/private/control/login', 'users'), '/private/control/users')
  assert.equal(isAdminPagePath('/shiyeotimin/settings', '/shiyeotimin/login'), true)
  assert.equal(isAdminPagePath('/admin/settings', '/shiyeotimin/login'), false)
})

test('admin password dialog is readable, validates confirmation, and exposes progress', async () => {
  const layoutSource = await readSource('../src/views/admin/AdminLayout.vue')
  const themeSource = await readSource('../src/assets/theme.css')

  assert.match(layoutSource, /class="password-dialog"/)
  assert.match(layoutSource, /append-to-body/)
  assert.match(layoutSource, /v-model="password\.confirmPassword"/)
  assert.match(layoutSource, /autocomplete="current-password"/)
  assert.match(layoutSource, /autocomplete="new-password"/)
  assert.match(layoutSource, /const passwordStrength = computed/)
  assert.match(layoutSource, /password\.oldPassword === password\.newPassword/)
  assert.match(layoutSource, /:disabled="!canChangePassword \|\| passwordSaving"/)
  assert.match(themeSource, /\.el-dialog__title, \.el-message-box__title \{ color: var\(--text-primary\) !important;/)
  assert.match(themeSource, /\.password-dialog \.el-form-item__label|\.el-dialog \.el-form-item__label/)
  assert.match(themeSource, /\.password-dialog__tips span\.is-met/)
})

test('dark theme gives Element controls themed surfaces and readable text', async () => {
  const source = await readSource('../src/assets/theme.css')

  assert.match(source, /\.el-input, \.el-textarea, \.el-select, \.el-input-number \{[\s\S]*?--el-input-text-color: var\(--text-primary\);/)
  assert.match(source, /\.el-input__inner, \.el-textarea__inner, \.el-select__selected-item \{[\s\S]*?-webkit-text-fill-color: var\(--text-primary\);/)
  assert.match(source, /\.el-input__inner::placeholder, \.el-textarea__inner::placeholder \{[\s\S]*?opacity: \.72;/)
  assert.match(source, /\.el-select__placeholder \{[\s\S]*?color: var\(--text-primary\) !important;[\s\S]*?opacity: 1;/)
  assert.match(source, /\.el-select__placeholder\.is-transparent \{[\s\S]*?color: var\(--text-tertiary\) !important;[\s\S]*?opacity: \.72;/)
  assert.match(source, /\.el-radio-button__inner \{[\s\S]*?background: var\(--bg-secondary\) !important;/)
  assert.match(source, /\.el-radio-button__original-radio:checked \+ \.el-radio-button__inner \{[\s\S]*?background: var\(--accent\) !important;/)
  assert.match(source, /\.el-textarea \.el-input__count[\s\S]*?color: var\(--text-tertiary\) !important;/)
  assert.match(source, /:root \.el-table \{[\s\S]*?--el-table-bg-color: var\(--bg-secondary\);/)
  assert.match(source, /--el-table-header-bg-color: var\(--bg-tertiary\);/)
  assert.match(source, /\.el-table__empty-block \{ background-color: var\(--el-table-bg-color\); \}/)
  assert.match(source, /\.el-table \.el-loading-mask \{[\s\S]*?background-color: color-mix\(in srgb, var\(--bg-secondary\) 88%, transparent\) !important;/)
  assert.match(source, /:root \.el-loading-mask \{[\s\S]*?background-color: color-mix\(in srgb, var\(--bg-secondary\) 88%, transparent\) !important;/)
  assert.match(source, /\.el-loading-spinner \.path \{ stroke: var\(--accent\) !important; \}/)
  assert.match(source, /\.el-table__fixed-right-patch \{ background: var\(--el-table-header-bg-color\) !important; \}/)
  assert.match(source, /:root\[data-theme="dark"\] \.el-button \{[\s\S]*?background: var\(--control-bg\) !important;[\s\S]*?border-color: var\(--control-border\) !important;/)
  assert.match(source, /:root\[data-theme="dark"\] \.el-button--success \{[\s\S]*?background: var\(--success-soft\) !important;/)
  assert.match(source, /:root\[data-theme="dark"\] \.el-button--danger \{[\s\S]*?background: var\(--danger-soft\) !important;/)
  assert.match(source, /:root\[data-theme="dark"\] \.el-tag--success \{[\s\S]*?--el-tag-bg-color: var\(--success-soft\);/)
  assert.match(source, /:root\[data-theme="dark"\] \.el-tag--danger \{[\s\S]*?--el-tag-bg-color: var\(--danger-soft\);/)
  assert.match(source, /:root\[data-theme="dark"\] \.el-checkbox__inner \{[\s\S]*?background: var\(--control-bg\) !important;/)
  assert.match(source, /:root\[data-theme="dark"\] \.el-switch \{[\s\S]*?--el-switch-on-color: var\(--accent\);/)
  assert.match(source, /:root\[data-theme="dark"\] \.el-color-picker__trigger \{[\s\S]*?background: var\(--control-bg\) !important;/)
  assert.match(source, /:root\[data-theme="dark"\] \.el-alert \{[\s\S]*?background: var\(--control-bg\) !important;/)
  assert.match(source, /:root\[data-theme="dark"\] \.el-message,[\s\S]*?background: color-mix\(in srgb, var\(--bg-elevated\) 94%, var\(--bg-primary\)\) !important;/)
})

test('directional controls and multi-select tags stay readable in both themes', async () => {
  const source = await readSource('../src/assets/theme.css')

  assert.match(source, /:root \.el-select__selection \.el-tag \{[\s\S]*?background: var\(--accent-soft\) !important;[\s\S]*?border-color: var\(--border-strong\) !important;/)
  assert.match(source, /:root \.el-select__caret,[\s\S]*?color: var\(--text-secondary\) !important;/)
  assert.match(source, /:root \.el-input-number__increase,[\s\S]*?color: var\(--text-primary\) !important;[\s\S]*?background: var\(--bg-tertiary\) !important;/)
  assert.match(source, /:root \.el-pagination \.btn-prev,[\s\S]*?:root \.el-pagination \.btn-next,[\s\S]*?color: var\(--text-primary\);/)
  assert.match(source, /:root \.el-pagination \.btn-prev:disabled,[\s\S]*?color: var\(--text-tertiary\) !important;/)
})

test('recommendation card keeps its surface while only the border glow moves', async () => {
  const source = await readSource('../src/assets/theme.css')
  const panelRule = source.match(/\.recommend-panel \{([\s\S]*?)\n\}/)?.[1] || ''
  const borderRule = source.match(/\.recommend-panel::before,\s*\n\.recommend-panel::after \{([\s\S]*?)\n\}/)?.[1] || ''

  assert.match(panelRule, /background: var\(--bg-elevated\);/)
  assert.doesNotMatch(panelRule, /linear-gradient/)
  assert.match(borderRule, /mask-composite: exclude;/)
  assert.match(borderRule, /animation: recommend-border-flow 8s linear infinite;/)
})

test('system update shows staged progress and keeps restart polling silent', async () => {
  const apiSource = await readSource('../src/api/blog.js')
  const viewSource = await readSource('../src/views/admin/SystemUpdate.vue')
  const themeSource = await readSource('../src/assets/theme.css')

  assert.match(apiSource, /version: \(config = \{\}\) => http\.get\('\/portal\/version', config\)/)
  assert.match(apiSource, /updateStatus: \(config = \{\}\) => http\.get\('\/admin\/update\/status', config\)/)
  assert.match(apiSource, /installUpdate: \(\) => http\.post\('\/admin\/update\/install', null, \{ timeout: 30000, silentRestartError: true \}\)/)
  assert.match(viewSource, /role="progressbar"/)
  assert.match(viewSource, /const restartRequestConfig = \{ timeout: 5000, silentRestartError: true \}/)
  assert.match(viewSource, /Promise\.allSettled\(\[/)
  assert.match(viewSource, /updateProgress\.value = 100/)
  assert.match(themeSource, /\.update-progress__fill\.is-active::after \{ opacity: 1; animation: update-progress-shimmer 1\.25s linear infinite; \}/)
  assert.match(themeSource, /@media \(prefers-reduced-motion: reduce\)[\s\S]*?\.update-progress__fill\.is-active::after \{ animation: none;/)
})

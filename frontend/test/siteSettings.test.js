import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import {
  createHeroTextStyle,
  normalizeHeroColor,
  normalizeHeroTextShadow
} from '../src/utils/heroAppearance.js'

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

test('admin logout and settings save use the configured login path', async () => {
  const layoutSource = await readSource('../src/views/admin/AdminLayout.vue')
  const settingSource = await readSource('../src/views/admin/SiteSetting.vue')
  const storeSource = await readSource('../src/stores/site.js')

  assert.match(layoutSource, /router\.push\(site\.adminLoginPath \|\| '\/admin\/login'\)/)
  assert.match(settingSource, /site\.setAdminLoginPath\(form\.adminLoginPath\)/)
  assert.match(storeSource, /setAdminLoginPath\(path\)/)
})

test('dark theme gives Element controls themed surfaces and readable text', async () => {
  const source = await readSource('../src/assets/theme.css')

  assert.match(source, /\.el-input, \.el-textarea, \.el-select, \.el-input-number \{[\s\S]*?--el-input-text-color: var\(--text-primary\);/)
  assert.match(source, /\.el-input__inner, \.el-textarea__inner, \.el-select__selected-item \{[\s\S]*?-webkit-text-fill-color: var\(--text-primary\);/)
  assert.match(source, /\.el-input__inner::placeholder, \.el-textarea__inner::placeholder \{[\s\S]*?opacity: \.72;/)
  assert.match(source, /\.el-radio-button__inner \{[\s\S]*?background: var\(--bg-secondary\) !important;/)
  assert.match(source, /\.el-radio-button__original-radio:checked \+ \.el-radio-button__inner \{[\s\S]*?background: var\(--accent\) !important;/)
  assert.match(source, /\.el-textarea \.el-input__count[\s\S]*?color: var\(--text-tertiary\) !important;/)
  assert.match(source, /--el-table-header-bg-color: var\(--bg-secondary\);/)
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

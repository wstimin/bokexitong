import assert from 'node:assert/strict'
import test from 'node:test'

const values = new Map([['blog_theme', 'dark']])
const root = { dataset: {}, style: {} }

globalThis.localStorage = {
  getItem: (key) => values.get(key) ?? null,
  setItem: (key, value) => values.set(key, String(value)),
  removeItem: (key) => values.delete(key)
}
globalThis.window = {
  matchMedia: () => ({
    matches: false,
    addEventListener: () => {}
  })
}

const {
  activateThemeScope,
  initializeTheme,
  setTheme,
  toggleTheme,
  useTheme
} = await import('../src/utils/theme.js')

globalThis.document = { documentElement: root }

test('portal and admin themes persist independently after legacy migration', () => {
  initializeTheme('portal')

  assert.equal(values.has('blog_theme'), false)
  assert.equal(values.get('blog_portal_theme'), 'dark')
  assert.equal(values.get('blog_admin_theme'), 'dark')
  assert.equal(root.dataset.theme, 'dark')

  setTheme('light', 'portal')
  assert.equal(values.get('blog_portal_theme'), 'light')
  assert.equal(values.get('blog_admin_theme'), 'dark')
  assert.equal(root.dataset.theme, 'light')

  activateThemeScope('admin')
  assert.equal(root.dataset.theme, 'dark')

  toggleTheme('admin')
  assert.equal(values.get('blog_admin_theme'), 'light')
  assert.equal(values.get('blog_portal_theme'), 'light')
})

test('theme consumers bind to their requested scope', () => {
  setTheme('dark', 'portal')
  setTheme('light', 'admin')

  assert.equal(useTheme('portal').theme.value, 'dark')
  assert.equal(useTheme('admin').theme.value, 'light')
})

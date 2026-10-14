import { readonly, ref } from 'vue'

const LEGACY_STORAGE_KEY = 'blog_theme'
const STORAGE_KEYS = {
  portal: 'blog_portal_theme',
  admin: 'blog_admin_theme'
}
const themes = {
  portal: ref('light'),
  admin: ref('light')
}
let activeScope = 'portal'
let mediaQuery

const systemTheme = () => window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
const normalizeScope = (scope) => scope === 'admin' ? 'admin' : 'portal'
const normalizeTheme = (value) => value === 'dark' ? 'dark' : 'light'

const applyTheme = (value) => {
  const next = normalizeTheme(value)
  document.documentElement.dataset.theme = next
  document.documentElement.style.colorScheme = next
}

const handleSystemTheme = () => {
  const next = systemTheme()
  for (const scope of Object.keys(STORAGE_KEYS)) {
    if (!localStorage.getItem(STORAGE_KEYS[scope])) themes[scope].value = next
  }
  if (!localStorage.getItem(STORAGE_KEYS[activeScope])) applyTheme(themes[activeScope].value)
}

const migrateLegacyTheme = () => {
  const legacy = localStorage.getItem(LEGACY_STORAGE_KEY)
  if (!legacy) return
  for (const key of Object.values(STORAGE_KEYS)) {
    if (!localStorage.getItem(key)) localStorage.setItem(key, normalizeTheme(legacy))
  }
  localStorage.removeItem(LEGACY_STORAGE_KEY)
}

export const initializeTheme = (scope = 'portal') => {
  migrateLegacyTheme()
  const fallback = systemTheme()
  for (const [name, key] of Object.entries(STORAGE_KEYS)) {
    themes[name].value = normalizeTheme(localStorage.getItem(key) || fallback)
  }
  activeScope = normalizeScope(scope)
  applyTheme(themes[activeScope].value)
  mediaQuery = window.matchMedia?.('(prefers-color-scheme: dark)')
  mediaQuery?.addEventListener?.('change', handleSystemTheme)
}

export const activateThemeScope = (scope = 'portal') => {
  activeScope = normalizeScope(scope)
  applyTheme(themes[activeScope].value)
}

export const setTheme = (value, scope = activeScope) => {
  const normalizedScope = normalizeScope(scope)
  const next = normalizeTheme(value)
  themes[normalizedScope].value = next
  localStorage.setItem(STORAGE_KEYS[normalizedScope], next)
  if (normalizedScope === activeScope) applyTheme(next)
}

export const toggleTheme = (scope = activeScope) => {
  const normalizedScope = normalizeScope(scope)
  setTheme(themes[normalizedScope].value === 'dark' ? 'light' : 'dark', normalizedScope)
}

export const useTheme = (scope = 'portal') => {
  const normalizedScope = normalizeScope(scope)
  return {
    theme: readonly(themes[normalizedScope]),
    activateTheme: () => activateThemeScope(normalizedScope),
    setTheme: (value) => setTheme(value, normalizedScope),
    toggleTheme: () => toggleTheme(normalizedScope)
  }
}

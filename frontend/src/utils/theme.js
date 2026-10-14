import { readonly, ref } from 'vue'

const STORAGE_KEY = 'blog_theme'
const theme = ref('light')
let mediaQuery

const systemTheme = () => window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'

const applyTheme = (value) => {
  const next = value === 'dark' ? 'dark' : 'light'
  theme.value = next
  document.documentElement.dataset.theme = next
  document.documentElement.style.colorScheme = next
}

const handleSystemTheme = () => {
  if (!localStorage.getItem(STORAGE_KEY)) applyTheme(systemTheme())
}

export const initializeTheme = () => {
  const saved = localStorage.getItem(STORAGE_KEY)
  applyTheme(saved || systemTheme())
  mediaQuery = window.matchMedia?.('(prefers-color-scheme: dark)')
  mediaQuery?.addEventListener?.('change', handleSystemTheme)
}

export const setTheme = (value) => {
  applyTheme(value)
  localStorage.setItem(STORAGE_KEY, theme.value)
}

export const toggleTheme = () => setTheme(theme.value === 'dark' ? 'light' : 'dark')
export const useTheme = () => ({ theme: readonly(theme), setTheme, toggleTheme })

export const DEFAULT_ADMIN_LOGIN_PATH = '/admin/login'

export const normalizeAdminLoginPath = (value) => {
  const text = String(value || '').trim()
  if (!text) return DEFAULT_ADMIN_LOGIN_PATH
  const withSlash = text.startsWith('/') ? text : `/${text}`
  return withSlash.replace(/\/+$/, '') || DEFAULT_ADMIN_LOGIN_PATH
}

export const adminBasePathFromLogin = (loginPath) => {
  const normalized = normalizeAdminLoginPath(loginPath)
  return normalized.endsWith('/login')
    ? (normalized.slice(0, -'/login'.length) || '/admin')
    : `${normalized}-admin`
}

export const adminPagePath = (loginPath, page = 'dashboard') => {
  const suffix = String(page || 'dashboard').replace(/^\/+/, '')
  return `${adminBasePathFromLogin(loginPath)}/${suffix}`
}

export const isAdminPagePath = (path, loginPath) => {
  const normalizedPath = String(path || '').replace(/\/+$/, '') || '/'
  const normalizedLogin = normalizeAdminLoginPath(loginPath)
  const basePath = adminBasePathFromLogin(normalizedLogin)
  return normalizedPath === normalizedLogin
    || normalizedPath === basePath
    || normalizedPath.startsWith(`${basePath}/`)
}

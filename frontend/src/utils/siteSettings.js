export const SITE_SETTINGS_SYNC_KEY = 'blog_site_settings_updated_at'

export const PUBLIC_SITE_SETTING_KEYS = [
  'siteName', 'heroTitle', 'heroSubtitle', 'heroBadge', 'heroTitleColor', 'heroSubtitleColor',
  'heroTextShadow', 'backgroundUrl', 'logoUrl', 'seoDescription', 'seoKeywords', 'icpBeian',
  'footerText', 'contactHtml', 'allowRegister'
]

const FIELD_LABELS = {
  siteName: '站点名称',
  heroTitle: '首页大标题',
  heroSubtitle: '首页说明',
  heroBadge: '首页徽标文案'
}

const normalizeSettingValue = (key, value) => {
  const normalized = String(value ?? '').trim()
  if (key === 'heroTitleColor' || key === 'heroSubtitleColor' || key === 'heroTextShadow' || key === 'allowRegister') {
    return normalized.toLowerCase()
  }
  return normalized
}

export const findSettingMismatches = (expected = {}, actual = {}, keys = Object.keys(expected)) => keys.filter(
  (key) => normalizeSettingValue(key, expected[key]) !== normalizeSettingValue(key, actual[key])
)

export const findMissingRequiredSiteField = (settings = {}) => {
  const missingKey = Object.keys(FIELD_LABELS).find((key) => !String(settings[key] ?? '').trim())
  return missingKey ? FIELD_LABELS[missingKey] : ''
}

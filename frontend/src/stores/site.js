import { defineStore } from 'pinia'
import { portalApi } from '../api/blog'
import { normalizeAssetUrl } from '../utils/assets'
import { normalizeAdminLoginPath } from '../utils/adminPath'
import {
  DEFAULT_HERO_SUBTITLE_COLOR,
  DEFAULT_HERO_TEXT_SHADOW,
  DEFAULT_HERO_TITLE_COLOR,
  normalizeHeroColor,
  normalizeHeroTextShadow
} from '../utils/heroAppearance'
import { SITE_SETTINGS_SYNC_KEY } from '../utils/siteSettings'

export { SITE_SETTINGS_SYNC_KEY }

const SITE_NAME = '博客系统'
const HERO_SUBTITLE = '用清透的卡片浏览公开文章，点开后阅读完整内容。登录后可以进入用户中心创作和管理自己的文章。'
const SITE_CACHE_KEY = 'blog_site_settings_cache'
let siteLoadPromise = null

const readCachedSite = () => {
  if (typeof localStorage === 'undefined') return null
  try {
    const cached = JSON.parse(localStorage.getItem(SITE_CACHE_KEY) || 'null')
    return cached && typeof cached === 'object' ? cached : null
  } catch {
    return null
  }
}

const cachedSite = readCachedSite()
const cachedSettings = cachedSite?.settings || {}
const cachedLogo = Array.isArray(cachedSite?.logo) ? cachedSite.logo[0] : cachedSite?.logo

const setFavicon = (url) => {
  const href = normalizeAssetUrl(url)
  if (!href || typeof document === 'undefined') return
  let icon = document.querySelector('link[rel="icon"]')
  if (!icon) {
    icon = document.createElement('link')
    icon.rel = 'icon'
    document.head.appendChild(icon)
  }
  icon.href = href
}

const setMeta = (name, content) => {
  if (typeof document === 'undefined') return
  let meta = document.querySelector(`meta[name="${name}"]`)
  if (!meta) {
    meta = document.createElement('meta')
    meta.name = name
    document.head.appendChild(meta)
  }
  meta.content = content || ''
}

export const useSiteStore = defineStore('site', {
  state: () => ({
    name: cachedSettings.siteName || SITE_NAME,
    heroTitle: cachedSettings.heroTitle || SITE_NAME,
    heroSubtitle: cachedSettings.heroSubtitle || HERO_SUBTITLE,
    heroBadge: cachedSettings.heroBadge || '博客',
    heroTitleColor: normalizeHeroColor(cachedSettings.heroTitleColor, DEFAULT_HERO_TITLE_COLOR),
    heroSubtitleColor: normalizeHeroColor(cachedSettings.heroSubtitleColor, DEFAULT_HERO_SUBTITLE_COLOR),
    heroTextShadow: normalizeHeroTextShadow(cachedSettings.heroTextShadow),
    allowRegister: cachedSettings.allowRegister !== 'false',
    logoUrl: cachedSettings.logoUrl || cachedLogo?.url || '',
    backgroundUrl: cachedSettings.backgroundUrl || '',
    seoDescription: cachedSettings.seoDescription || '',
    seoKeywords: cachedSettings.seoKeywords || '',
    icpBeian: cachedSettings.icpBeian || '',
    footerText: cachedSettings.footerText || '',
    contactHtml: cachedSettings.contactHtml || '',
    welcomeEnabled: cachedSettings.welcomeEnabled !== 'false',
    welcomeAvatarUrl: cachedSettings.welcomeAvatarUrl || '',
    welcomeBackgroundUrl: cachedSettings.welcomeBackgroundUrl || '',
    welcomeDisplayName: cachedSettings.welcomeDisplayName || 'Lily',
    welcomeTitle: cachedSettings.welcomeTitle || '愿你在这里，遇见温柔的光',
    welcomeSubtitle: cachedSettings.welcomeSubtitle || '每一次相遇，都值得被认真收藏',
    welcomeDescription: cachedSettings.welcomeDescription || '在字里行间，慢慢认识我。',
    welcomeButtonText: cachedSettings.welcomeButtonText || '进入博客',
    welcomeAccentColor: cachedSettings.welcomeAccentColor || '#c9a227',
    welcomeAnimationEnabled: cachedSettings.welcomeAnimationEnabled !== 'false',
    adminLoginPath: localStorage.getItem('blog_admin_login_path') || '/admin/login',
    loaded: Boolean(cachedSite),
    loading: false
  }),
  getters: {
    logoSrc: (state) => normalizeAssetUrl(state.logoUrl),
    backgroundSrc: (state) => normalizeAssetUrl(state.backgroundUrl)
  },
  actions: {
    applySettings(data = {}) {
      const logo = Array.isArray(data.logo) ? data.logo[0] : data.logo
      const settings = data.settings || data
      this.name = settings.siteName || SITE_NAME
      this.heroTitle = settings.heroTitle || this.name
      this.heroSubtitle = settings.heroSubtitle || HERO_SUBTITLE
      this.heroBadge = settings.heroBadge || '博客'
      this.heroTitleColor = normalizeHeroColor(settings.heroTitleColor, DEFAULT_HERO_TITLE_COLOR)
      this.heroSubtitleColor = normalizeHeroColor(settings.heroSubtitleColor, DEFAULT_HERO_SUBTITLE_COLOR)
      this.heroTextShadow = normalizeHeroTextShadow(settings.heroTextShadow)
      this.allowRegister = settings.allowRegister !== 'false'
      this.backgroundUrl = settings.backgroundUrl || ''
      this.logoUrl = settings.logoUrl || logo?.url || ''
      this.seoDescription = settings.seoDescription || ''
      this.seoKeywords = settings.seoKeywords || ''
      this.icpBeian = settings.icpBeian || ''
      this.footerText = settings.footerText || ''
      this.contactHtml = settings.contactHtml || ''
      this.welcomeEnabled = settings.welcomeEnabled !== 'false'
      this.welcomeAvatarUrl = settings.welcomeAvatarUrl || ''
      this.welcomeBackgroundUrl = settings.welcomeBackgroundUrl || ''
      this.welcomeDisplayName = settings.welcomeDisplayName || 'Lily'
      this.welcomeTitle = settings.welcomeTitle || '愿你在这里，遇见温柔的光'
      this.welcomeSubtitle = settings.welcomeSubtitle || '每一次相遇，都值得被认真收藏'
      this.welcomeDescription = settings.welcomeDescription || '在字里行间，慢慢认识我。'
      this.welcomeButtonText = settings.welcomeButtonText || '进入博客'
      this.welcomeAccentColor = settings.welcomeAccentColor || '#c9a227'
      this.welcomeAnimationEnabled = settings.welcomeAnimationEnabled !== 'false'
      this.setAdminLoginPath(settings.adminLoginPath)
      this.loaded = true
      this.applyHead()
      if (typeof localStorage !== 'undefined') {
        localStorage.setItem(SITE_CACHE_KEY, JSON.stringify({ settings, logo: data.logo || null }))
      }
    },
    setAdminLoginPath(path) {
      this.adminLoginPath = normalizeAdminLoginPath(path)
      localStorage.setItem('blog_admin_login_path', this.adminLoginPath)
    },
    async loadSite(force = false) {
      if (siteLoadPromise) {
        const activeResult = await siteLoadPromise
        if (!force) return activeResult
      }
      if (this.loaded && !force) return true
      this.loading = true
      siteLoadPromise = portalApi.site(force ? {
        params: { _ts: Date.now() },
        headers: { 'Cache-Control': 'no-cache' }
      } : {})
        .then((res) => {
          this.applySettings(res.data || {})
          return true
        })
        .catch((error) => {
          console.error(error)
          return false
        })
        .finally(() => {
          this.applyHead()
          this.loading = false
          siteLoadPromise = null
        })
      return siteLoadPromise
    },
    applyHead() {
      if (typeof document === 'undefined') return
      document.title = this.name
      setFavicon(this.logoUrl)
      setMeta('description', this.seoDescription || this.heroSubtitle)
      setMeta('keywords', this.seoKeywords)
      document.documentElement.style.setProperty('--site-background-image', this.backgroundSrc ? `url("${this.backgroundSrc}")` : 'none')
    }
  }
})

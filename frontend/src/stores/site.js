import { defineStore } from 'pinia'
import { portalApi } from '../api/blog'
import { normalizeAssetUrl } from '../utils/assets'

const SITE_NAME = '博客系统'
const HERO_SUBTITLE = '用卡片浏览公开文章，点开后再阅读完整内容。登录后可以进入用户中心创作、管理自己的文章。'

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

export const useSiteStore = defineStore('site', {
  state: () => ({
    name: SITE_NAME,
    heroTitle: SITE_NAME,
    heroSubtitle: HERO_SUBTITLE,
    heroBadge: '博客',
    allowRegister: true,
    logoUrl: '',
    backgroundUrl: '',
    seoDescription: '',
    seoKeywords: '',
    icpBeian: '',
    footerText: '',
    contactHtml: '',
    adminLoginPath: '/admin/login',
    loaded: false,
    loading: false
  }),
  getters: {
    logoSrc: (state) => normalizeAssetUrl(state.logoUrl),
    backgroundSrc: (state) => normalizeAssetUrl(state.backgroundUrl)
  },
  actions: {
    async loadSite(force = false) {
      if (this.loading || (this.loaded && !force)) return
      this.loading = true
      try {
        const res = await portalApi.home()
        const logo = Array.isArray(res.data.logo) ? res.data.logo[0] : res.data.logo
        const settings = res.data.settings || {}
        this.name = settings.siteName || SITE_NAME
        this.heroTitle = settings.heroTitle || this.name
        this.heroSubtitle = settings.heroSubtitle || HERO_SUBTITLE
        this.heroBadge = settings.heroBadge || '博客'
        this.allowRegister = settings.allowRegister !== 'false'
        this.backgroundUrl = settings.backgroundUrl || ''
        this.logoUrl = settings.logoUrl || logo?.url || ''
        this.seoDescription = settings.seoDescription || ''
        this.seoKeywords = settings.seoKeywords || ''
        this.icpBeian = settings.icpBeian || ''
        this.footerText = settings.footerText || ''
        this.contactHtml = settings.contactHtml || ''
        this.adminLoginPath = settings.adminLoginPath || '/admin/login'
        this.loaded = true
        this.applyHead()
      } catch (error) {
        this.applyHead()
      } finally {
        this.loading = false
      }
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

export const DEFAULT_HERO_TITLE_COLOR = '#ffffff'
export const DEFAULT_HERO_SUBTITLE_COLOR = '#ffffff'
export const DEFAULT_HERO_TEXT_SHADOW = 'dark'

const HERO_SHADOW_STYLES = {
  none: {
    '--hero-title-shadow': 'none',
    '--hero-subtitle-shadow': 'none'
  },
  dark: {
    '--hero-title-shadow': '0 4px 18px rgba(0, 0, 0, .55)',
    '--hero-subtitle-shadow': '0 2px 10px rgba(0, 0, 0, .58)'
  },
  light: {
    '--hero-title-shadow': '0 3px 16px rgba(255, 255, 255, .8)',
    '--hero-subtitle-shadow': '0 2px 9px rgba(255, 255, 255, .82)'
  }
}

export const normalizeHeroColor = (value, fallback = DEFAULT_HERO_TITLE_COLOR) => {
  const color = String(value || '').trim()
  return /^#[0-9a-f]{6}$/i.test(color) ? color.toLowerCase() : fallback
}

export const normalizeHeroTextShadow = (value) => (
  Object.prototype.hasOwnProperty.call(HERO_SHADOW_STYLES, value) ? value : DEFAULT_HERO_TEXT_SHADOW
)

export const createHeroTextStyle = (titleColor, subtitleColor, shadowMode) => ({
  '--hero-title-color': normalizeHeroColor(titleColor, DEFAULT_HERO_TITLE_COLOR),
  '--hero-subtitle-color': normalizeHeroColor(subtitleColor, DEFAULT_HERO_SUBTITLE_COLOR),
  ...HERO_SHADOW_STYLES[normalizeHeroTextShadow(shadowMode)]
})

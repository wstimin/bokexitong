const safeEmbeddedImagePattern = /^data:image\/(?:png|jpe?g|gif|webp);base64,[a-z0-9+/=\r\n]+$/i

export const isSafeEmbeddedImageUrl = (value) => safeEmbeddedImagePattern.test(String(value || '').trim())

export const isSafeRelativeUrl = (value) => {
  const url = String(value || '').trim()
  return /^\/(?!\/)[^\s]*$/.test(url) || /^\.\.?\/[^\s]*$/.test(url)
}

export const isSafeHttpUrl = (value) => /^https?:\/\/[^\s]+$/i.test(String(value || '').trim())

export const isSafeMediaUrl = (value) => isSafeHttpUrl(value) || isSafeRelativeUrl(value)

export const isSafeLinkUrl = (value) => {
  const url = String(value || '').trim()
  return isSafeMediaUrl(url) || /^mailto:[^\s]+$/i.test(url) || /^#[^\s]*$/.test(url)
}

export const normalizeLinkButtonUrl = (value) => {
  const url = String(value || '').trim()
  if (isSafeHttpUrl(url) || /^\/(?!\/)[^\s]*$/.test(url)) return url
  throw new Error('链接地址只支持 http、https 或站内绝对路径')
}

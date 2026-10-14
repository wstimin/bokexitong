export const MAX_UPLOAD_SIZE_BYTES = 50 * 1024 * 1024
export const MAX_UPLOAD_SIZE_LABEL = '50MB'

const COVER_EXTENSIONS = new Set(['jpg', 'jpeg', 'png', 'gif', 'webp'])
const COVER_CONTENT_TYPES = new Set(['image/jpeg', 'image/png', 'image/gif', 'image/webp'])

const extensionOf = (name = '') => {
  const value = String(name).trim().toLowerCase()
  const dot = value.lastIndexOf('.')
  return dot >= 0 ? value.slice(dot + 1) : ''
}

export const formatFileSize = (bytes = 0) => {
  const size = Number(bytes) || 0
  if (size < 1024) return `${size}B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)}KB`
  return `${(size / 1024 / 1024).toFixed(1)}MB`
}

export const validateCoverImage = (file) => {
  if (!file) return '请选择要上传的封面图片'
  if (!file.size) return '封面图片内容为空，请重新选择'
  if (file.size > MAX_UPLOAD_SIZE_BYTES) {
    return `封面图片大小为 ${formatFileSize(file.size)}，超过 ${MAX_UPLOAD_SIZE_LABEL} 限制，请压缩后重试`
  }

  const extension = extensionOf(file.name)
  const contentType = String(file.type || '').trim().toLowerCase()
  if (!COVER_EXTENSIONS.has(extension) || (contentType && !COVER_CONTENT_TYPES.has(contentType))) {
    return '封面仅支持 JPG、PNG、GIF 或 WebP 图片'
  }
  return ''
}

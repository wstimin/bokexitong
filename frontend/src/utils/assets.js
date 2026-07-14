export const normalizeAssetUrl = (url) => {
  if (!url) return ''
  if (/^(https?:)?\/\//.test(url) || url.startsWith('data:') || url.startsWith('blob:')) return url
  if (url.startsWith('/api/')) return url
  return url.startsWith('/') ? `/api${url}` : url
}

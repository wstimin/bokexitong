import { loadQuill } from '@vueup/vue-quill'
import { marked } from 'marked'
import { normalizeAssetUrl } from './assets'

const fontWhitelist = ['system', 'songti', 'heiti', 'kaiti', 'serif', 'mono']
const sizeWhitelist = ['12px', '14px', '16px', '18px', '20px', '24px', '28px', '32px']

let richTextRegistered = false

export const ensureRichTextFormats = async () => {
  if (richTextRegistered) return

  const Quill = await loadQuill()

  const Font = Quill.import('formats/font')
  Font.whitelist = fontWhitelist
  Quill.register(Font, true)

  const Size = Quill.import('attributors/style/size')
  Size.whitelist = sizeWhitelist
  Quill.register(Size, true)

  richTextRegistered = true
}

export const richToolbar = [
  [{ font: fontWhitelist }, { size: sizeWhitelist }],
  ['bold', 'italic', 'underline', 'strike'],
  [{ color: [] }, { background: [] }],
  [{ align: [] }],
  [{ header: [1, 2, 3, 4, false] }],
  [{ list: 'ordered' }, { list: 'bullet' }],
  ['blockquote', 'code-block'],
  ['link', 'image'],
  ['clean']
]

export const isRichTextContentType = (contentType) => {
  const normalized = String(contentType || '').toUpperCase()
  return normalized === 'HTML' || normalized === 'RICH_TEXT'
}

export const toEditableHtml = (content, contentType) => {
  const raw = String(content || '')
  return isRichTextContentType(contentType) ? raw : marked(raw)
}

export const toDisplayHtml = (content, contentType) => {
  const raw = String(content || '')
  const html = isRichTextContentType(contentType) ? raw : marked(raw)
  return normalizeHtmlAssets(html)
}

export const isEmptyHtml = (value) => {
  const plain = String(value || '')
    .replace(/<[^>]*>/g, '')
    .replace(/&nbsp;/g, ' ')
    .trim()
  return !plain
}

export const escapeHtml = (value) => String(value || '')
  .replaceAll('&', '&amp;')
  .replaceAll('<', '&lt;')
  .replaceAll('>', '&gt;')
  .replaceAll('"', '&quot;')
  .replaceAll("'", '&#39;')

export const appendHtmlSnippet = (current, snippet) => `${current || ''}${snippet}`

export const insertHtmlSnippet = (quill, index, snippet) => {
  if (!quill || typeof index !== 'number' || Number.isNaN(index)) return false
  quill.clipboard.dangerouslyPasteHTML(index, snippet, 'user')
  return true
}

export const imageSnippet = (url, name) => {
  const src = normalizeAssetUrl(url)
  return `<p><img class="article-inline-image" src="${escapeHtml(src)}" alt="${escapeHtml(name || '图片')}" /></p>`
}

export const videoSnippet = (url, name) => {
  const href = normalizeAssetUrl(url)
  return `<p><a class="article-inline-file" href="${escapeHtml(href)}" target="_blank" rel="noopener noreferrer">${escapeHtml(name || '视频文件')}</a></p>`
}

export const fileSnippet = (url, name) => {
  const href = normalizeAssetUrl(url)
  return `<p><a class="article-inline-file" href="${escapeHtml(href)}" target="_blank" rel="noopener noreferrer">${escapeHtml(name || '附件')}</a></p>`
}

const normalizeHtmlAssets = (html) => String(html || '')
  .replace(/(src|href)=(['"])(\/[^'"\s>]+)\2/g, (_, attr, quote, url) => `${attr}=${quote}${escapeHtml(normalizeAssetUrl(url))}${quote}`)

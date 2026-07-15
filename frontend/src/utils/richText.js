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

export const richToolbarLabels = {
  font: '字体',
  size: '字号',
  bold: '加粗',
  italic: '斜体',
  underline: '下划线',
  strike: '删除线',
  color: '文字颜色',
  background: '背景色',
  align: '对齐',
  header: '标题',
  list: '列表',
  blockquote: '引用',
  'code-block': '代码块',
  link: '链接',
  image: '图片',
  clean: '清除格式'
}

const toolbarSelector = '.ql-toolbar.ql-snow'

const setTitle = (el, title) => {
  if (!el) return
  el.setAttribute('title', title)
  el.setAttribute('aria-label', title)
}

export const localizeRichTextToolbar = (editorRoot) => {
  const root = editorRoot?.container || editorRoot?.$el || editorRoot
  if (!root?.querySelector) return

  const toolbar = root.querySelector(toolbarSelector)
  if (!toolbar) return

  toolbar.querySelectorAll('button').forEach((button) => {
    const format = button.className.match(/ql-([a-z-]+)/)?.[1]
    const title = richToolbarLabels[format]
    if (title) setTitle(button, title)
  })

  toolbar.querySelectorAll('.ql-picker').forEach((picker) => {
    const format = Array.from(picker.classList)
      .find((cls) => cls.startsWith('ql-') && cls !== 'ql-picker' && cls !== 'ql-expanded')
      ?.replace(/^ql-/, '')
    const title = richToolbarLabels[format]
    if (!title) return

    const label = picker.querySelector('.ql-picker-label')
    setTitle(label, title)
    label?.setAttribute('data-label', title)
    picker.querySelectorAll('.ql-picker-item').forEach((item) => {
      setTitle(item, title)
      item.setAttribute('data-label', title)
    })
  })
}

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

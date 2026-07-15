import { loadQuill } from '@vueup/vue-quill'
import { marked } from 'marked'
import { normalizeAssetUrl } from './assets'

const fontWhitelist = ['system', 'songti', 'heiti', 'kaiti', 'serif', 'mono']
const sizeWhitelist = ['12px', '14px', '16px', '18px', '20px', '24px', '28px', '32px']

let richTextRegistered = false
let toolbarLocalizationTimer = null

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

const pickerValueLabels = {
  font: {
    system: '系统字体',
    songti: '宋体',
    heiti: '黑体',
    kaiti: '楷体',
    serif: '衬线',
    mono: '等宽'
  },
  header: {
    '': '正文',
    1: '标题 1',
    2: '标题 2',
    3: '标题 3',
    4: '标题 4'
  },
  list: {
    ordered: '有序列表',
    bullet: '无序列表'
  },
  align: {
    '': '默认对齐',
    center: '居中',
    right: '右对齐',
    justify: '两端对齐'
  },
  color: {
    color: '文字颜色',
    background: '背景色'
  }
}

const sizeLabels = {
  '12px': '12px',
  '14px': '14px',
  '16px': '16px',
  '18px': '18px',
  '20px': '20px',
  '24px': '24px',
  '28px': '28px',
  '32px': '32px'
}

const toolbarSelector = '.ql-toolbar.ql-snow'

const pickerLabelTexts = {
  font: {
    system: '系统字体',
    songti: '宋体',
    heiti: '黑体',
    kaiti: '楷体',
    serif: '衬线',
    mono: '等宽'
  },
  size: {
    '12px': '12px',
    '14px': '14px',
    '16px': '16px',
    '18px': '18px',
    '20px': '20px',
    '24px': '24px',
    '28px': '28px',
    '32px': '32px'
  },
  header: {
    '': '正文',
    1: '标题 1',
    2: '标题 2',
    3: '标题 3',
    4: '标题 4'
  },
  list: {
    ordered: '有序列表',
    bullet: '无序列表'
  },
  align: {
    '': '默认对齐',
    center: '居中',
    right: '右对齐',
    justify: '两端对齐'
  },
  color: '文字颜色',
  background: '背景色'
}

const setTitle = (el, title) => {
  if (!el) return
  el.setAttribute('title', title)
  el.setAttribute('aria-label', title)
}

const setPickerText = (picker, values, fallback) => {
  picker.querySelectorAll('.ql-picker-item').forEach((item) => {
    const value = item.getAttribute('data-value') ?? ''
    const text = values[value] || fallback || item.textContent || ''
    item.textContent = text
    item.setAttribute('data-label', text)
    setTitle(item, text)
  })
}

const getPickerText = (format, value) => {
  if (format === 'font') return pickerLabelTexts.font[value] || '字体'
  if (format === 'size') return pickerLabelTexts.size[value] || '字号'
  if (format === 'header') return pickerLabelTexts.header[value] || '正文'
  if (format === 'list') return pickerLabelTexts.list[value] || '列表'
  if (format === 'align') return pickerLabelTexts.align[value] || '对齐'
  if (format === 'color') return pickerLabelTexts.color
  if (format === 'background') return pickerLabelTexts.background
  return richToolbarLabels[format] || ''
}

const localizePicker = (picker, format) => {
  const label = picker.querySelector('.ql-picker-label')
  const title = richToolbarLabels[format]
  if (label && title) {
    const value = label.getAttribute('data-value') ?? ''
    const labelText = getPickerText(format, value)
    setTitle(label, labelText || title)
    label.setAttribute('data-label', labelText || title)
  }

  if (format === 'font') setPickerText(picker, pickerLabelTexts.font, '字体')
  else if (format === 'size') setPickerText(picker, pickerLabelTexts.size, '字号')
  else if (format === 'header') setPickerText(picker, pickerLabelTexts.header, '正文')
  else if (format === 'list') setPickerText(picker, pickerLabelTexts.list, '列表')
  else if (format === 'align') setPickerText(picker, pickerLabelTexts.align, '对齐')
  else if (format === 'color') {
    setTitle(picker, pickerLabelTexts.color)
  } else if (format === 'background') {
    setTitle(picker, pickerLabelTexts.background)
  }
}

const refreshToolbar = (toolbar) => {
  toolbar.querySelectorAll('button').forEach((button) => {
    const classes = Array.from(button.classList)
    const format = classes.find((cls) => cls.startsWith('ql-') && cls !== 'ql-active')?.replace(/^ql-/, '')
    const title = richToolbarLabels[format]
    if (title) setTitle(button, title)
  })

  toolbar.querySelectorAll('.ql-picker').forEach((picker) => {
    const format = Array.from(picker.classList)
      .find((cls) => cls.startsWith('ql-') && cls !== 'ql-picker' && cls !== 'ql-expanded')
      ?.replace(/^ql-/, '')
    if (!format) return
    localizePicker(picker, format)
  })
}

export const localizeRichTextToolbar = (editorRoot) => {
  const container = editorRoot?.container || editorRoot?.$el || editorRoot
  if (!container) return

  const locateToolbar = () => {
    const root = container.parentElement || container
    return root.querySelector?.(toolbarSelector) || container.previousElementSibling || null
  }

  const applyLocalization = () => {
    const toolbar = locateToolbar()
    if (!toolbar) return false
    refreshToolbar(toolbar)
    return true
  }

  if (toolbarLocalizationTimer) {
    window.clearTimeout(toolbarLocalizationTimer)
    toolbarLocalizationTimer = null
  }

  if (!applyLocalization()) {
    toolbarLocalizationTimer = window.setTimeout(() => {
      applyLocalization()
      toolbarLocalizationTimer = null
    }, 0)
    return
  }

  toolbarLocalizationTimer = window.setTimeout(() => {
    applyLocalization()
    toolbarLocalizationTimer = null
  }, 0)
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

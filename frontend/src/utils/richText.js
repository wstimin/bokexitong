import { loadQuill } from '@vueup/vue-quill'
import DOMPurify from 'dompurify'
import { marked } from 'marked'
import { normalizeAssetUrl } from './assets'

const fontWhitelist = ['system', 'songti', 'heiti', 'kaiti', 'serif', 'mono']
const sizeWhitelist = ['12px', '14px', '16px', '18px', '20px', '24px', '28px', '32px']

let richTextRegistered = false
let registeredQuill = null
let toolbarLocalizationTimer = null
const autoLinkTimers = new WeakMap()

const urlPattern = /(?:https?:\/\/|www\.)[^\s<>"']+/gi
const trailingPunctuation = /[),.;!?，。；！？）】》]+$/
const skippedAutoLinkTags = new Set(['A', 'CODE', 'PRE', 'SCRIPT', 'STYLE', 'TEXTAREA'])

const normalizeLinkUrl = (value) => value.toLowerCase().startsWith('www.') ? `https://${value}` : value

const trimUrlPunctuation = (value) => {
  const trailing = value.match(trailingPunctuation)?.[0] || ''
  return { url: trailing ? value.slice(0, -trailing.length) : value, trailing }
}

export const ensureRichTextFormats = async () => {
  if (richTextRegistered) return

  const Quill = await loadQuill()
  registeredQuill = Quill

  const Font = Quill.import('formats/font')
  Font.whitelist = fontWhitelist
  Quill.register(Font, true)

  const Size = Quill.import('attributors/style/size')
  Size.whitelist = sizeWhitelist
  Quill.register(Size, true)

  const BlockEmbed = Quill.import('blots/block/embed')
  class UploadedVideoBlot extends BlockEmbed {
    static blotName = 'uploadedVideo'
    static tagName = 'video'

    static create(value) {
      const node = super.create()
      node.setAttribute('src', String(value || ''))
      node.setAttribute('controls', 'controls')
      node.setAttribute('preload', 'metadata')
      node.setAttribute('class', 'article-inline-video')
      return node
    }

    static value(node) {
      return node.getAttribute('src') || ''
    }
  }
  Quill.register(UploadedVideoBlot, true)

  class UploadPlaceholderBlot extends BlockEmbed {
    static blotName = 'uploadPlaceholder'
    static tagName = 'div'
    static className = 'article-upload-placeholder'

    static create(value) {
      const node = super.create()
      const uploadId = String(value?.id || '')
      const type = String(value?.type || 'file')
      node.setAttribute('data-upload-id', uploadId)
      node.setAttribute('data-upload-type', type)
      node.setAttribute('contenteditable', 'false')
      node.setAttribute('role', 'status')
      node.textContent = `${mediaTypeLabel(type)}上传中...`
      return node
    }

    static value(node) {
      return {
        id: node.getAttribute('data-upload-id') || '',
        type: node.getAttribute('data-upload-type') || 'file'
      }
    }
  }
  Quill.register(UploadPlaceholderBlot, true)

  class ArticleLinkButtonBlot extends BlockEmbed {
    static blotName = 'articleLinkButton'
    static tagName = 'div'
    static className = 'article-link-button-block'

    static create(value) {
      const node = super.create()
      const style = ['primary', 'secondary', 'download'].includes(value?.style) ? value.style : 'primary'
      const link = document.createElement('a')
      link.setAttribute('href', String(value?.href || ''))
      link.setAttribute('class', `article-link-button article-link-button--${style}`)
      link.setAttribute('rel', 'noopener noreferrer')
      if (value?.newWindow !== false) link.setAttribute('target', '_blank')
      if (style === 'download') link.setAttribute('download', '')
      link.textContent = String(value?.text || '查看链接')
      node.setAttribute('contenteditable', 'false')
      node.appendChild(link)
      return node
    }

    static value(node) {
      const link = node.querySelector('a')
      const styleClass = Array.from(link?.classList || []).find((item) => item.startsWith('article-link-button--'))
      return {
        href: link?.getAttribute('href') || '',
        text: link?.textContent || '查看链接',
        style: styleClass?.replace('article-link-button--', '') || 'primary',
        newWindow: link?.getAttribute('target') === '_blank'
      }
    }
  }
  Quill.register(ArticleLinkButtonBlot, true)

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
  ['link', 'linkButton'],
  ['undo', 'redo'],
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
  linkButton: '链接按钮',
  undo: '撤销',
  redo: '重做',
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

const setPickerTitles = (picker, values, fallback) => {
  picker.querySelectorAll('.ql-picker-item').forEach((item) => {
    const value = item.getAttribute('data-value') ?? ''
    const text = values[value] || fallback || item.textContent || ''
    item.removeAttribute('data-label')
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
    if (format === 'align' || format === 'color' || format === 'background') {
      label.removeAttribute('data-label')
    } else {
      label.setAttribute('data-label', labelText || title)
    }
  }

  if (format === 'font') setPickerText(picker, pickerLabelTexts.font, '字体')
  else if (format === 'size') setPickerText(picker, pickerLabelTexts.size, '字号')
  else if (format === 'header') setPickerText(picker, pickerLabelTexts.header, '正文')
  else if (format === 'list') setPickerText(picker, pickerLabelTexts.list, '列表')
  else if (format === 'align') setPickerTitles(picker, pickerLabelTexts.align, '对齐')
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

const autoLinkEditor = (quill) => {
  if (!quill?.getText || !quill?.formatText) return
  const text = quill.getText(0, Math.max(quill.getLength?.() || 0, 0))
  for (const match of text.matchAll(urlPattern)) {
    const { url } = trimUrlPunctuation(match[0])
    if (!url) continue
    const index = match.index || 0
    const formats = quill.getFormat?.(index, url.length) || {}
    if (formats.link || formats['code-block'] || formats.code) continue
    quill.formatText(index, url.length, 'link', normalizeLinkUrl(url), 'api')
  }
}

export const setupRichTextEditor = (quill, options = {}) => {
  if (!quill) return
  quill.__blogEditorOptions = options
  localizeRichTextToolbar(quill)
  const toolbar = quill.getModule?.('toolbar')
  toolbar?.addHandler?.('undo', () => quill.history?.undo?.())
  toolbar?.addHandler?.('redo', () => quill.history?.redo?.())
  toolbar?.addHandler?.('linkButton', () => quill.__blogEditorOptions?.onLinkButton?.())

  if (quill.__blogAutoLinkReady) return
  quill.__blogAutoLinkReady = true
  quill.on?.('text-change', (_delta, _oldDelta, source) => {
    if (source !== 'user') return
    const previous = autoLinkTimers.get(quill)
    if (previous) window.clearTimeout(previous)
    const timer = window.setTimeout(() => {
      autoLinkTimers.delete(quill)
      autoLinkEditor(quill)
    }, 80)
    autoLinkTimers.set(quill, timer)
  })

  const uploadClipboardImage = (event) => {
    const files = Array.from(event.clipboardData?.files || event.dataTransfer?.files || [])
    const images = files.filter((file) => file.type?.startsWith('image/'))
    if (!images.length) return
    event.preventDefault()
    const range = event.type === 'drop'
      ? getRangeFromPoint(quill, event.clientX, event.clientY)
      : quill.getSelection?.() || quill.__blogEditorOptions?.getLastSelection?.() || null
    images.forEach((image, offset) => {
      const nextRange = range ? { index: range.index + (offset * 2), length: 0 } : null
      quill.__blogEditorOptions?.onImageFile?.(image, nextRange)
    })
  }
  quill.root?.addEventListener('paste', uploadClipboardImage, true)
  quill.root?.addEventListener('drop', uploadClipboardImage, true)
}

export const getSafeInsertIndex = (quill, range) => {
  if (!quill) return 0
  const end = Math.max((quill.getLength?.() || 1) - 1, 0)
  const requested = Number(range?.index)
  return Number.isFinite(requested) ? Math.min(Math.max(requested, 0), end) : end
}

const mediaTypeLabel = (type) => ({ image: '图片', video: '视频', file: '附件' }[type] || '文件')

const createUploadId = () => globalThis.crypto?.randomUUID?.() || `upload-${Date.now()}-${Math.random().toString(16).slice(2)}`

const createEditorInsertError = (message) => Object.assign(new Error(message), { code: 'EDITOR_INSERT_FAILED' })

export const insertUploadPlaceholder = (quill, type, range) => {
  if (!quill?.insertEmbed) return null
  const id = createUploadId()
  const index = getSafeInsertIndex(quill, range || quill.getSelection?.())
  quill.insertEmbed(index, 'uploadPlaceholder', { id, type }, 'user')
  quill.insertText(index + 1, '\n', 'user')
  quill.setSelection?.(index + 2, 0, 'silent')
  return id
}

const findUploadPlaceholder = (quill, uploadId) => {
  if (!quill?.root || !uploadId || !registeredQuill) return null
  const nodes = quill.root.querySelectorAll('[data-upload-id]')
  const node = Array.from(nodes).find((item) => item.getAttribute('data-upload-id') === uploadId)
  if (!node) return null
  const blot = registeredQuill.find(node)
  if (!blot) return null
  return { node, index: quill.getIndex(blot) }
}

export const replaceUploadPlaceholder = (quill, uploadId, snippet) => {
  const placeholder = findUploadPlaceholder(quill, uploadId)
  if (!placeholder) {
    throw createEditorInsertError('文件已上传，但原插入位置已被删除，请重新插入')
  }
  quill.deleteText(placeholder.index, 1, 'user')
  const nextIndex = insertHtmlSnippet(quill, placeholder.index, snippet)
  if (nextIndex === null) {
    throw createEditorInsertError('文件已上传，但插入正文失败，请重新插入')
  }
  return nextIndex
}

export const removeUploadPlaceholder = (quill, uploadId) => {
  const placeholder = findUploadPlaceholder(quill, uploadId)
  if (!placeholder) return false
  quill.deleteText(placeholder.index, 1, 'user')
  return true
}

const getRangeFromPoint = (quill, clientX, clientY) => {
  if (!quill?.root || !Number.isFinite(clientX) || !Number.isFinite(clientY)) return null
  const nativeRange = document.caretRangeFromPoint?.(clientX, clientY)
    || (() => {
      const position = document.caretPositionFromPoint?.(clientX, clientY)
      if (!position) return null
      const range = document.createRange()
      range.setStart(position.offsetNode, position.offset)
      range.collapse(true)
      return range
    })()
  if (!nativeRange || !quill.root.contains(nativeRange.startContainer)) return null
  const selection = window.getSelection?.()
  if (!selection) return null
  selection.removeAllRanges()
  selection.addRange(nativeRange)
  return quill.getSelection?.() || null
}

export const isRichTextContentType = (contentType) => {
  const normalized = String(contentType || '').toUpperCase()
  return normalized === 'HTML' || normalized === 'RICH_TEXT'
}

export const toEditableHtml = (content, contentType) => {
  const raw = String(content || '')
  return sanitizeRichTextHtml(isRichTextContentType(contentType) ? raw : marked(raw))
}

export const toDisplayHtml = (content, contentType) => {
  const raw = String(content || '')
  const html = isRichTextContentType(contentType) ? raw : marked(raw)
  return autoLinkHtml(normalizeHtmlAssets(sanitizeRichTextHtml(html)))
}

export const sanitizeRichTextHtml = (html) => DOMPurify.sanitize(String(html || ''), {
  ADD_TAGS: ['video', 'source'],
  ADD_ATTR: ['controls', 'preload', 'data-list', 'target', 'download'],
  ALLOW_DATA_ATTR: true,
  FORBID_TAGS: ['form', 'input', 'button', 'iframe', 'object', 'embed', 'svg', 'math'],
  FORBID_ATTR: ['srcdoc']
})

const autoLinkHtml = (html) => {
  if (typeof document === 'undefined') return html
  const container = document.createElement('div')
  container.innerHTML = html
  const walker = document.createTreeWalker(container, NodeFilter.SHOW_TEXT)
  const textNodes = []
  while (walker.nextNode()) textNodes.push(walker.currentNode)

  textNodes.forEach((node) => {
    if (!node.parentElement || skippedAutoLinkTags.has(node.parentElement.tagName)) return
    const text = node.nodeValue || ''
    urlPattern.lastIndex = 0
    if (!urlPattern.test(text)) return
    urlPattern.lastIndex = 0
    const fragment = document.createDocumentFragment()
    let cursor = 0
    for (const match of text.matchAll(urlPattern)) {
      const index = match.index || 0
      const { url, trailing } = trimUrlPunctuation(match[0])
      fragment.append(document.createTextNode(text.slice(cursor, index)))
      if (url) {
        const anchor = document.createElement('a')
        anchor.href = normalizeLinkUrl(url)
        anchor.target = '_blank'
        anchor.rel = 'noopener noreferrer'
        anchor.textContent = url
        fragment.append(anchor)
      }
      if (trailing) fragment.append(document.createTextNode(trailing))
      cursor = index + match[0].length
    }
    fragment.append(document.createTextNode(text.slice(cursor)))
    node.replaceWith(fragment)
  })
  container.querySelectorAll('a[href]').forEach((anchor) => {
    const href = anchor.getAttribute('href') || ''
    if (/^https?:\/\//i.test(href)) {
      anchor.target = '_blank'
      anchor.rel = 'noopener noreferrer'
    }
  })
  return container.innerHTML
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

const parseInsertSnippet = (snippet) => {
  if (typeof DOMParser === 'undefined') return null
  const doc = new DOMParser().parseFromString(String(snippet || ''), 'text/html')
  const img = doc.querySelector('img')
  if (img) {
    return {
      type: 'image',
      src: img.getAttribute('src') || ''
    }
  }

  const video = doc.querySelector('video')
  if (video) {
    return {
      type: 'video',
      src: video.getAttribute('src') || video.querySelector('source')?.getAttribute('src') || ''
    }
  }

  const link = doc.querySelector('a')
  if (link) {
    const text = (link.textContent || '').trim()
    return {
      type: link.getAttribute('href') ? 'link' : null,
      href: link.getAttribute('href') || '',
      text: text || '附件'
    }
  }

  return null
}

export const insertHtmlSnippet = (quill, index, snippet) => {
  if (!quill || typeof index !== 'number' || Number.isNaN(index)) return null
  const parsed = parseInsertSnippet(snippet)
  try {
    const safeIndex = Math.min(Math.max(index, 0), Math.max((quill.getLength?.() || 1) - 1, 0))

    if (parsed?.type === 'image' && parsed.src && typeof quill.insertEmbed === 'function') {
      quill.insertEmbed(safeIndex, 'image', parsed.src, 'user')
      quill.insertText(safeIndex + 1, '\n', 'user')
      quill.setSelection?.(safeIndex + 2, 0, 'silent')
      quill.focus?.()
      return safeIndex + 2
    }

    if (parsed?.type === 'video' && parsed.src && typeof quill.insertEmbed === 'function') {
      quill.insertEmbed(safeIndex, 'uploadedVideo', parsed.src, 'user')
      quill.insertText(safeIndex + 1, '\n', 'user')
      quill.setSelection?.(safeIndex + 2, 0, 'silent')
      quill.focus?.()
      return safeIndex + 2
    }

    if (parsed?.type === 'link' && parsed.href && typeof quill.insertText === 'function') {
      quill.insertText(safeIndex, parsed.text, 'link', parsed.href, 'user')
      quill.insertText(safeIndex + parsed.text.length, '\n', 'user')
      const nextIndex = safeIndex + parsed.text.length + 1
      quill.setSelection?.(nextIndex, 0, 'silent')
      quill.focus?.()
      return nextIndex
    }

    if (snippet && quill.clipboard?.dangerouslyPasteHTML) {
      quill.clipboard.dangerouslyPasteHTML(safeIndex, sanitizeRichTextHtml(snippet), 'user')
      quill.setSelection?.(safeIndex + 1, 0, 'silent')
      quill.focus?.()
      return safeIndex + 1
    }
  } catch (error) {
    console.error(error)
  }

  return null
}

export const normalizeLinkButtonUrl = (value) => {
  const url = String(value || '').trim()
  if (/^https?:\/\/[^\s]+$/i.test(url)) return url
  if (/^\/(?!\/)[^\s]*$/.test(url)) return url
  throw new Error('链接地址只支持 http、https 或站内路径')
}

export const insertLinkButton = (quill, range, values) => {
  if (!quill?.insertEmbed) return null
  const href = normalizeLinkButtonUrl(values?.href)
  const text = String(values?.text || '').trim()
  if (!text) throw new Error('请填写按钮文字')
  const index = getSafeInsertIndex(quill, range || quill.getSelection?.())
  quill.insertEmbed(index, 'articleLinkButton', {
    href,
    text,
    style: values?.style,
    newWindow: values?.newWindow
  }, 'user')
  quill.insertText(index + 1, '\n', 'user')
  quill.setSelection?.(index + 2, 0, 'silent')
  quill.focus?.()
  return index + 2
}

export const imageSnippet = (url, name) => {
  const src = normalizeAssetUrl(url)
  return `<p><img class="article-inline-image" src="${escapeHtml(src)}" alt="${escapeHtml(name || '图片')}" /></p>`
}

export const videoSnippet = (url, name) => {
  const src = normalizeAssetUrl(url)
  return `<video class="article-inline-video" src="${escapeHtml(src)}" controls preload="metadata" title="${escapeHtml(name || '视频')}"></video><p><br></p>`
}

export const fileSnippet = (url, name) => {
  const href = normalizeAssetUrl(url)
  return `<p><a class="article-inline-file" href="${escapeHtml(href)}" target="_blank" rel="noopener noreferrer">${escapeHtml(name || '附件')}</a></p>`
}

const normalizeHtmlAssets = (html) => String(html || '')
  .replace(/(src|href)=(['"])(\/[^'"\s>]+)\2/g, (_, attr, quote, url) => `${attr}=${quote}${escapeHtml(normalizeAssetUrl(url))}${quote}`)

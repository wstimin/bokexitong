import DOMPurify from 'dompurify'

export const sanitizeContactHtml = (html = '') => DOMPurify.sanitize(String(html), {
  ALLOWED_TAGS: ['a', 'p', 'br', 'strong', 'em', 'span'],
  ALLOWED_ATTR: ['href', 'target', 'rel']
})

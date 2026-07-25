import test from 'node:test'
import assert from 'node:assert/strict'
import {
  isSafeEmbeddedImageUrl,
  isSafeLinkUrl,
  isSafeMediaUrl,
  normalizeLinkButtonUrl
} from '../src/utils/richTextUrls.js'

test('media URLs accept supported remote and relative locations', () => {
  for (const url of [
    'https://example.com/image.png',
    'http://example.com/video.mp4',
    '/uploads/image.png',
    './image.png',
    '../image.png'
  ]) {
    assert.equal(isSafeMediaUrl(url), true, url)
  }
})

test('media URLs reject executable, opaque, protocol-relative, and malformed locations', () => {
  for (const url of [
    '//evil.example/image.png',
    'javascript:alert(1)',
    'blob:https://example.com/id',
    'data:image/svg+xml,<svg></svg>',
    'https://example.com/has space.png'
  ]) {
    assert.equal(isSafeMediaUrl(url), false, url)
  }
})

test('embedded images only allow raster base64 data URLs', () => {
  assert.equal(isSafeEmbeddedImageUrl('data:image/png;base64,aGVsbG8='), true)
  assert.equal(isSafeEmbeddedImageUrl('data:image/webp;base64,UklGRg=='), true)
  assert.equal(isSafeEmbeddedImageUrl('data:image/svg+xml;base64,PHN2Zz4='), false)
  assert.equal(isSafeEmbeddedImageUrl('data:text/html;base64,PGgxPng8L2gxPg=='), false)
})

test('ordinary links additionally allow mail and document anchors', () => {
  assert.equal(isSafeLinkUrl('mailto:editor@example.com'), true)
  assert.equal(isSafeLinkUrl('#section'), true)
  assert.equal(isSafeLinkUrl('//evil.example'), false)
  assert.equal(isSafeLinkUrl('javascript:alert(1)'), false)
})

test('link buttons only accept HTTP(S) and root-relative site paths', () => {
  assert.equal(normalizeLinkButtonUrl(' https://example.com/docs '), 'https://example.com/docs')
  assert.equal(normalizeLinkButtonUrl('/article/1'), '/article/1')

  for (const url of [
    '//evil.example',
    './article/1',
    'mailto:editor@example.com',
    '#section',
    'blob:https://example.com/id',
    'javascript:alert(1)'
  ]) {
    assert.throws(() => normalizeLinkButtonUrl(url), /http/)
  }
})

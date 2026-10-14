import test from 'node:test'
import assert from 'node:assert/strict'
import { MAX_UPLOAD_SIZE_BYTES, formatFileSize, validateCoverImage } from '../src/utils/upload.js'

test('cover image validation accepts supported images', () => {
  assert.equal(validateCoverImage({ name: 'cover.webp', type: 'image/webp', size: 1024 }), '')
  assert.equal(validateCoverImage({ name: 'cover.JPG', type: 'image/jpeg', size: 1024 }), '')
})

test('cover image validation reports oversized and unsupported files', () => {
  assert.match(validateCoverImage({ name: 'large.png', type: 'image/png', size: MAX_UPLOAD_SIZE_BYTES + 1 }), /超过 50MB 限制/)
  assert.match(validateCoverImage({ name: 'cover.svg', type: 'image/svg+xml', size: 1024 }), /仅支持 JPG、PNG、GIF 或 WebP/)
  assert.equal(formatFileSize(2 * 1024 * 1024), '2.0MB')
})

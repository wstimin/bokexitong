import test from 'node:test'
import assert from 'node:assert/strict'
import { getDocumentAppendIndex } from '../src/utils/richTextPosition.js'

test('upload append index stays after existing article text', () => {
  const quill = {
    getContents: () => ({ ops: [{ insert: '第一段\n第二段\n' }] })
  }

  assert.equal(getDocumentAppendIndex(quill), 7)
})

test('upload append index advances past existing embeds', () => {
  const quill = {
    getContents: () => ({
      ops: [
        { insert: '正文\n' },
        { insert: { uploadPlaceholder: { id: 'first', type: 'image' } } },
        { insert: '\n' }
      ]
    })
  }

  assert.equal(getDocumentAppendIndex(quill), 4)
})

test('upload append index falls back to Quill length when Delta is unavailable', () => {
  assert.equal(getDocumentAppendIndex({ getLength: () => 12 }), 11)
  assert.equal(getDocumentAppendIndex(null), 0)
})

import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const readSource = (relativePath) => readFile(new URL(relativePath, import.meta.url), 'utf8')

test('editor exposes Quill and cleans up listeners on unmount', async () => {
  const source = await readSource('../src/components/ArticleRichEditor.vue')
  assert.match(source, /defineExpose\(\s*\{[\s\S]*getQuill:/)
  assert.match(source, /editorCleanup\?\.\(\)/)
  assert.match(source, /off\?\.\('text-change', bookmarkChangeHandler\)/)
  assert.match(source, /emit\('pending-change', 0\)/)
})

test('link button keeps its dialog, style choices, and insertion command', async () => {
  const source = await readSource('../src/components/ArticleRichEditor.vue')
  assert.match(source, /@click="openLinkButton"/)
  assert.match(source, /value="primary"/)
  assert.match(source, /value="secondary"/)
  assert.match(source, /value="download"/)
  assert.match(source, /insertLinkButton\(editorRef\.value/)
})

test('article detail loading blocks editing and ignores stale requests', async () => {
  const source = await readSource('../src/views/admin/ArticleManage.vue')
  assert.match(source, /detailLoading/)
  assert.match(source, /editorRequestVersion/)
  assert.match(source, /requestVersion !== editorRequestVersion/)
  assert.match(source, /:disabled="[^"]*detailLoading/)
})

test('rich text setup removes browser listeners during cleanup', async () => {
  const source = await readSource('../src/utils/richText.js')
  assert.match(source, /removeEventListener\('paste', uploadClipboardImage, true\)/)
  assert.match(source, /removeEventListener\('drop', uploadClipboardImage, true\)/)
  assert.match(source, /quill\.off\?\.\('text-change', handleTextChange\)/)
})

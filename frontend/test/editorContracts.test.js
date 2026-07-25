import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const readSource = (relativePath) => readFile(new URL(relativePath, import.meta.url), 'utf8')

test('editor mounts Koenig and destroys the React root on unmount', async () => {
  const source = await readSource('../src/components/ArticleRichEditor.vue')
  assert.match(source, /mountKoenigEditor/)
  assert.match(source, /defineExpose\(\s*\{[\s\S]*getEditor:/)
  assert.match(source, /editorController\?\.destroy\(\)/)
  assert.match(source, /emit\('pending-change', 0\)/)
})

test('Koenig bridge keeps HTML storage and the existing upload API', async () => {
  const bridge = await readSource('../src/editor/koenigEditorBridge.jsx')
  const uploader = await readSource('../src/editor/useKoenigUpload.js')
  assert.match(bridge, /KoenigComposer/)
  assert.match(bridge, /KoenigEditor/)
  assert.match(bridge, /HtmlOutputPlugin/)
  assert.match(bridge, /supportedCardTypes/)
  assert.match(uploader, /uploadApi\.file\(file\)/)
  assert.match(uploader, /normalizeAssetUrl/)
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

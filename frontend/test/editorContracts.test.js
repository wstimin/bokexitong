import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const readSource = (relativePath) => readFile(new URL(relativePath, import.meta.url), 'utf8')

test('editor exposes Quill and cleans up listeners on unmount', async () => {
  const source = await readSource('../src/components/ArticleRichEditor.vue')
  assert.match(source, /editorRef = shallowRef\(null\)/)
  assert.match(source, /@click="undoEditor"/)
  assert.match(source, /@click="redoEditor"/)
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

test('media tools upload and insert image, video, and file content', async () => {
  const source = await readSource('../src/components/ArticleRichEditor.vue')
  assert.match(source, /@click="openPicker\('image'\)"/)
  assert.match(source, /@click="openPicker\('video'\)"/)
  assert.match(source, /@click="openPicker\('file'\)"/)
  assert.match(source, /uploadApi\.file\(file\)/)
  assert.match(source, /insertHtmlSnippet\(editorRef\.value/)
})

test('custom toolbar actions bookmark the last Quill selection before reading the native selection', async () => {
  const source = await readSource('../src/components/ArticleRichEditor.vue')
  assert.match(source, /let selection = range \|\| lastSelection\.value/)
  assert.match(source, /try \{\s*selection = quill\.getSelection\?\.\(\) \|\| null/)
  assert.match(source, /getSafeInsertIndex\(quill, selection\)/)
})

test('async media and dialog inserts do not depend on a focused native browser selection', async () => {
  const source = await readSource('../src/utils/richText.js')
  assert.match(source, /const restoreEditorSelection = \(quill, index\) => \{[\s\S]*?quill\.focus\?\.\(\{ preventScroll: true \}\)[\s\S]*?quill\.setSelection\?\.\(index, 0, 'silent'\)/)
  assert.match(source, /restoreEditorSelection\(quill, safeIndex\)/)
  assert.match(source, /restoreEditorSelection\(quill, index\)/)
  assert.match(source, /insertEmbed\(safeIndex, 'uploadedImage',[\s\S]*?, 'api'\)/)
  assert.match(source, /insertEmbed\(safeIndex, 'uploadedVideo', parsed\.src, 'api'\)/)
  assert.match(source, /insertEmbed\(safeIndex, 'uploadedFile',[\s\S]*?, 'api'\)/)
  assert.match(source, /insertEmbed\(index, 'articleLinkButton',[\s\S]*?\}, 'api'\)/)
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

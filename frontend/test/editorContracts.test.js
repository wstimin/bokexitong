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
  assert.match(source, /class="ql-link" title="文本链接（请先选中文字）" aria-label="文本链接"/)
  assert.match(source, /@mousedown="rememberLinkButtonPosition" @click\.stop="openLinkButton"/)
  assert.match(source, /<span>链接按钮<\/span>/)
  assert.match(source, /getSafeInsertIndex,/)
  assert.match(source, /value="primary"/)
  assert.match(source, /value="secondary"/)
  assert.match(source, /value="download"/)
  assert.match(source, /insertLinkButton\(editorRef\.value/)
})

test('text link explains the required selection instead of failing silently', async () => {
  const source = await readSource('../src/components/ArticleRichEditor.vue')

  assert.match(source, /toolbar\?\.addHandler\?\.\('link'/)
  assert.match(source, /if \(!range \|\| range\.length === 0\)/)
  assert.match(source, /ElMessage\.warning\('请先选中要添加链接的文字'\)/)
  assert.match(source, /theme\?\.tooltip\?\.edit\?\.\('link', preview\)/)
})

test('toolbar pickers are not clipped by the formatting row', async () => {
  const source = await readSource('../src/assets/styles.css')
  const formatsRule = source.match(/\.article-rich-editor__formats \{([^}]*)\}/)?.[1] || ''
  const immersiveFormatsRule = source.match(/\.article-composer__document \.article-rich-editor__formats \{([^}]*)\}/)?.[1] || ''

  assert.match(formatsRule, /flex-wrap: wrap/)
  assert.match(formatsRule, /overflow: visible/)
  assert.doesNotMatch(formatsRule, /overflow-y: hidden/)
  assert.match(immersiveFormatsRule, /overflow: visible/)
  assert.match(source, /\.ql-picker-options \{[^}]*z-index: 20;/)
})

test('toolbar exposes every registered font and size option', async () => {
  const editorSource = await readSource('../src/components/ArticleRichEditor.vue')
  const richTextSource = await readSource('../src/utils/richText.js')

  assert.match(editorSource, /<select class="ql-font"[^>]*>[\s\S]*?<\/select>/)
  assert.match(editorSource, /<select class="ql-size"[^>]*>[\s\S]*?<\/select>/)
  for (const font of ['system', 'songti', 'heiti', 'kaiti', 'serif', 'mono']) {
    assert.match(editorSource, new RegExp(`<option value="${font}"`))
  }
  for (const size of ['12px', '14px', '16px', '18px', '20px', '24px', '28px', '32px']) {
    assert.match(editorSource, new RegExp(`<option value="${size}"`))
  }
  assert.match(richTextSource, /fontWhitelist = \['system', 'songti', 'heiti', 'kaiti', 'serif', 'mono'\]/)
  assert.match(richTextSource, /sizeWhitelist = \['12px', '14px', '16px', '18px', '20px', '24px', '28px', '32px'\]/)
})

test('media tools upload and insert image, video, and file content', async () => {
  const source = await readSource('../src/components/ArticleRichEditor.vue')
  assert.match(source, /@click="openPicker\('image'\)"/)
  assert.match(source, /@click="openPicker\('video'\)"/)
  assert.match(source, /@click="openPicker\('file'\)"/)
  assert.match(source, /uploadApi\.file\(file\)/)
  assert.match(source, /insertUploadPlaceholder\(editorRef\.value/)
  assert.match(source, /replaceUploadPlaceholder\(editorRef\.value/)
  assert.match(source, /removeUploadPlaceholder\(editorRef\.value/)
  assert.match(source, /placeholders = images\.map/)
})

test('media uploads always reserve placeholders at the end of the article', async () => {
  const editorSource = await readSource('../src/components/ArticleRichEditor.vue')
  const richTextSource = await readSource('../src/utils/richText.js')
  const placeholderFunction = richTextSource.slice(
    richTextSource.indexOf('export const insertUploadPlaceholder'),
    richTextSource.indexOf('const findUploadPlaceholder')
  )
  assert.match(editorSource, /insertUploadPlaceholder\(editorRef\.value, type\)/)
  assert.match(editorSource, /images\.map\(\(\) => insertUploadPlaceholder\(editorRef\.value, 'image'\)\)/)
  assert.match(editorSource, /onImageFile: \(file\) => uploadMedia\(file, 'image'\)/)
  assert.match(editorSource, /onImageFiles: \(files\) => uploadImageBatch\(files\)/)
  assert.match(richTextSource, /onImageFiles\(images\)/)
  assert.match(richTextSource, /onImageFile\?\.\(image\)/)
  assert.doesNotMatch(editorSource, /pickerBookmarks/)
  assert.doesNotMatch(editorSource, /getSafeInsertIndex\(editorRef\.value, null\)/)
  assert.doesNotMatch(editorSource, /insertUploadPlaceholder\(editorRef\.value,[^\n]+\{ index:/)
  assert.doesNotMatch(editorSource, /uploadMedia\(file, 'image', createBookmark\(range\)\)/)
  assert.doesNotMatch(editorSource, /const uploadImageBatch = async \(files, range\)/)
  assert.doesNotMatch(richTextSource, /onImageFiles\(images, range\)/)
  assert.doesNotMatch(richTextSource, /onImageFile\?\.\(image, range\)/)
  assert.match(placeholderFunction, /export const insertUploadPlaceholder = \(quill, type\)[\s\S]*?const index = getDocumentAppendIndex\(quill\)[\s\S]*?const currentRange = quill\.getSelection\?\.\(\) \|\| null[\s\S]*?restoreSelection\(quill, currentRange\)[\s\S]*?return \{ id, index, nextIndex/)
  assert.doesNotMatch(placeholderFunction, /range/)
  assert.doesNotMatch(placeholderFunction, /setCursorAfterEmbed/)
})

test('upload placeholders are visible in the article body', async () => {
  const source = await readSource('../src/assets/styles.css')
  assert.match(source, /\.article-upload-placeholder \{[^}]*display: flex;/)
  assert.match(source, /\.article-upload-placeholder::before \{[^}]*animation: editor-spin/)
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

test('article editor API routes match backend controller contracts', async () => {
  const source = await readSource('../src/api/blog.js')
  assert.match(source, /adminSave: \(data\) => http\.post\('\/articles\/admin', data, articleSaveConfig\)/)
  assert.match(source, /adminUpdate: \(id, data\) => http\.put\(`\/articles\/admin\/\$\{id\}`, data, articleSaveConfig\)/)
  assert.match(source, /save: \(data\) => http\.post\('\/articles', data, articleSaveConfig\)/)
  assert.match(source, /update: \(id, data\) => http\.put\(`\/articles\/\$\{id\}`, data, articleSaveConfig\)/)
  assert.match(source, /form\.append\('file', file, file\.name \|\| 'file'\)/)
  assert.match(source, /http\.post\('\/uploads', form, \{ timeout: 120000 \}\)/)
})

test('article payload and publish feedback follow the backend response contract', async () => {
  const adminSource = await readSource('../src/views/admin/ArticleManage.vue')
  const userSource = await readSource('../src/views/portal/UserCenter.vue')
  const composerSource = await readSource('../src/components/ArticleComposer.vue')

  assert.match(adminSource, /const payload = \{ \.\.\.form, contentType: 'HTML', status \}/)
  assert.match(userSource, /const payload = \{ \.\.\.articleForm, contentType: 'HTML', status \}/)
  assert.match(composerSource, /fieldModel\('categoryId', null\)/)
  assert.match(composerSource, /fieldModel\('tagIds', \[\]\)/)
  assert.match(composerSource, /fieldModel\('recommended', 0\)/)
  assert.match(composerSource, /fieldModel\('recommendSort', 0\)/)
  assert.match(userSource, /savedArticle\.status === 'PUBLISHED'/)
  assert.match(userSource, /savedArticle\.status === 'REJECTED'/)
  assert.match(userSource, /发布未通过/)
  assert.match(userSource, /文章已提交审核/)
})

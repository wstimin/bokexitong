import React, { useCallback, useMemo } from 'react'
import { createRoot } from 'react-dom/client'
import {
  DEFAULT_NODES,
  HtmlOutputPlugin,
  KoenigComposer,
  KoenigEditor
} from '@tryghost/koenig-lexical'
import { createKoenigFileUploader } from './useKoenigUpload'

const supportedCardTypes = new Set([
  'aside',
  'button',
  'callout',
  'codeblock',
  'file',
  'horizontalrule',
  'image',
  'markdown',
  'toggle',
  'video'
])

const getNodeClass = (nodeConfig) => typeof nodeConfig === 'function' ? nodeConfig : nodeConfig?.replace

// Koenig builds its card menu from registered node metadata. Keeping all nodes
// registered preserves HTML compatibility while hiding cards this blog cannot serve.
DEFAULT_NODES.forEach((nodeConfig) => {
  const NodeClass = getNodeClass(nodeConfig)
  if (!NodeClass || typeof NodeClass.getType !== 'function') return
  if (supportedCardTypes.has(NodeClass.getType())) return
  if (Object.prototype.hasOwnProperty.call(NodeClass, 'kgMenu') || NodeClass.kgMenu) {
    NodeClass.kgMenu = []
  }
})

function KoenigHost({ disabled, html, onChange, onReady, placeholder, uploader }) {
  const registerApi = useCallback((api) => {
    onReady?.(api)
  }, [onReady])
  const composerNodes = useMemo(() => [...DEFAULT_NODES], [])

  return (
    <KoenigComposer fileUploader={uploader} nodes={composerNodes}>
      <div className="article-rich-editor__scroll">
        <div className="article-rich-editor__canvas">
          <KoenigEditor
            isDragEnabled={!disabled}
            isSnippetsEnabled={false}
            placeholderText={placeholder}
            readOnly={disabled}
            registerAPI={registerApi}
          >
            <HtmlOutputPlugin html={html} setHtml={onChange} />
          </KoenigEditor>
        </div>
      </div>
    </KoenigComposer>
  )
}

export const mountKoenigEditor = (element, options = {}) => {
  const root = createRoot(element)
  const state = {
    disabled: Boolean(options.disabled),
    html: options.value || '',
    placeholder: options.placeholder || '开始写正文，输入 / 可以插入图片、视频、附件和内容卡片',
    revision: 0
  }
  let editorApi = null
  let destroyed = false
  let lastEmittedHtml = state.html

  const uploader = createKoenigFileUploader({ onPendingChange: options.onPendingChange })

  const onReady = (api) => {
    editorApi = api
    if (api) options.onReady?.(api.editorInstance || api)
  }

  const onChange = (html) => {
    const nextHtml = html || ''
    lastEmittedHtml = nextHtml
    state.html = nextHtml
    options.onChange?.(nextHtml)
  }

  const render = () => {
    if (destroyed) return
    root.render(
      <KoenigHost
        key={state.revision}
        disabled={state.disabled}
        html={state.html}
        onChange={onChange}
        onReady={onReady}
        placeholder={state.placeholder}
        uploader={uploader}
      />
    )
  }

  render()

  return {
    destroy() {
      if (destroyed) return
      destroyed = true
      uploader.resetPending()
      root.unmount()
      editorApi = null
    },
    focus() {
      editorApi?.focusEditor?.({ position: 'bottom' })
    },
    getEditor() {
      return editorApi?.editorInstance || null
    },
    setDisabled(disabled) {
      const nextDisabled = Boolean(disabled)
      if (nextDisabled === state.disabled) return
      state.disabled = nextDisabled
      render()
    },
    setPlaceholder(placeholder) {
      const nextPlaceholder = placeholder || ''
      if (nextPlaceholder === state.placeholder) return
      state.placeholder = nextPlaceholder
      render()
    },
    setValue(html) {
      const nextHtml = html || ''
      if (nextHtml === state.html || nextHtml === lastEmittedHtml) return
      state.html = nextHtml
      lastEmittedHtml = nextHtml
      editorApi = null
      state.revision += 1
      render()
    }
  }
}

import { useCallback, useState } from 'react'
import { uploadApi } from '../api/blog'
import { normalizeAssetUrl } from '../utils/assets'

export const koenigFileTypes = {
  image: {
    mimeTypes: ['image/gif', 'image/jpg', 'image/jpeg', 'image/png', 'image/svg+xml', 'image/webp'],
    extensions: ['gif', 'jpg', 'jpeg', 'png', 'svg', 'svgz', 'webp']
  },
  video: {
    mimeTypes: ['video/mp4', 'video/webm', 'video/ogg'],
    extensions: ['mp4', 'webm', 'ogv', 'ogg']
  },
  audio: {
    mimeTypes: ['audio/mp3', 'audio/mpeg', 'audio/ogg', 'audio/wav', 'audio/vnd.wav', 'audio/wave', 'audio/x-wav', 'audio/mp4', 'audio/x-m4a'],
    extensions: ['mp3', 'wav', 'ogg', 'm4a']
  },
  mediaThumbnail: {
    mimeTypes: ['image/gif', 'image/jpg', 'image/jpeg', 'image/png', 'image/webp'],
    extensions: ['gif', 'jpg', 'jpeg', 'png', 'webp']
  },
  file: { mimeTypes: [], extensions: [] }
}

const extensionOf = (fileName = '') => fileName.split('.').pop()?.toLowerCase() || ''

const validateFiles = (files, type) => {
  const config = koenigFileTypes[type]
  if (!config || type === 'file') return []

  return files.flatMap((file) => {
    const validExtension = config.extensions.includes(extensionOf(file.name))
    const validMime = !file.type || config.mimeTypes.includes(file.type)
    if (validExtension && validMime) return []

    return [{
      fileName: file.name,
      message: `不支持该文件格式，请上传 ${config.extensions.map((item) => `.${item}`).join('、')}`
    }]
  })
}

export const createKoenigFileUploader = ({ onPendingChange } = {}) => {
  let pendingUploads = 0

  const changePending = (delta) => {
    pendingUploads = Math.max(0, pendingUploads + delta)
    onPendingChange?.(pendingUploads)
  }

  function useFileUpload(type = 'file') {
    const [progress, setProgress] = useState(100)
    const [isLoading, setLoading] = useState(false)
    const [errors, setErrors] = useState([])
    const [filesNumber, setFilesNumber] = useState(0)

    const upload = useCallback(async (inputFiles = []) => {
      const files = Array.from(inputFiles || []).filter(Boolean)
      setFilesNumber(files.length)
      setErrors([])

      if (!files.length) return []

      const validationErrors = validateFiles(files, type)
      if (validationErrors.length) {
        setErrors(validationErrors)
        return null
      }

      setLoading(true)
      setProgress(10)
      changePending(files.length)

      try {
        const settledResults = await Promise.allSettled(files.map((file) => uploadApi.file(file)))
        const uploadErrors = []
        const results = []

        settledResults.forEach((result, index) => {
          const file = files[index]
          if (result.status === 'rejected') {
            uploadErrors.push({
              fileName: file.name,
              message: result.reason?.response?.data?.message || result.reason?.message || '文件上传失败'
            })
            return
          }

          results.push({
            url: normalizeAssetUrl(result.value.data.url),
            fileName: file.name,
            name: result.value.data.name || file.name
          })
        })

        setErrors(uploadErrors)
        setProgress(100)
        return results.length ? results : null
      } finally {
        setLoading(false)
        changePending(-files.length)
      }
    }, [type])

    return { progress, isLoading, upload, errors, filesNumber }
  }

  return {
    fileTypes: koenigFileTypes,
    useFileUpload,
    resetPending() {
      pendingUploads = 0
      onPendingChange?.(0)
    }
  }
}

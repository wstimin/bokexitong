const operationLength = (operation) => {
  const inserted = operation?.insert
  if (typeof inserted === 'string') return inserted.length
  return inserted == null ? 0 : 1
}

export const getDocumentAppendIndex = (quill) => {
  if (!quill) return 0
  const operations = quill.getContents?.()?.ops
  if (Array.isArray(operations)) {
    const deltaLength = operations.reduce((length, operation) => length + operationLength(operation), 0)
    return Math.max(deltaLength - 1, 0)
  }
  return Math.max(Number(quill.getLength?.()) - 1 || 0, 0)
}

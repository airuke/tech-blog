export function date(value, options = {}) {
  return value
    ? new Intl.DateTimeFormat('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        ...options,
      }).format(new Date(value))
    : '尚未发布'
}
export function bytes(n) {
  if (!n) return '0 B'
  const i = Math.min(3, Math.floor(Math.log(n) / Math.log(1024)))
  return `${(n / 1024 ** i).toFixed(i ? 1 : 0)} ${['B', 'KB', 'MB', 'GB'][i]}`
}
export const articleBody = (value) => ({
  title: value.title,
  summary: value.summary || '',
  content: value.content || '',
  cover: value.cover || null,
  categoryId: value.category?.id || null,
  tagIds: value.tags?.map((t) => t.id) || [],
  status: value.status,
  visibility: value.visibility,
  isTop: value.isTop,
  isFavorite: value.isFavorite,
  version: value.version,
})

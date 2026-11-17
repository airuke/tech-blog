<script setup>
import { computed, watch, useId, onBeforeUnmount } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js/lib/common'
import 'highlight.js/styles/github-dark.css'
const props = defineProps({
  content: { type: String, default: '' },
  assets: { type: Array, default: () => [] },
})
const emit = defineEmits(['toc']),
  prefix = useId().replaceAll(':', '')
const renderer = new marked.Renderer()
renderer.code = ({ text, lang }) => {
  const language = (lang || '').split(/\s+/)[0]
  const highlighted =
    language && hljs.getLanguage(language)
      ? hljs.highlight(text, { language }).value
      : text
          .replaceAll('&', '&amp;')
          .replaceAll('<', '&lt;')
          .replaceAll('>', '&gt;')
  return `<pre data-language="${language.replace(/[^a-zA-Z0-9_+#.-]/g, '')}"><code class="hljs">${highlighted}</code></pre>`
}
const rendered = computed(() => {
  const safe = DOMPurify.sanitize(
    marked.parse(props.content, { renderer, gfm: true }),
    {
      FORBID_TAGS: ['form', 'input', 'button', 'iframe', 'style'],
      FORBID_ATTR: ['style'],
    },
  )
  const document = new DOMParser().parseFromString(safe, 'text/html')
  document.querySelectorAll('h1').forEach((heading) => {
    const replacement = document.createElement('h2')
    replacement.append(...heading.childNodes)
    heading.replaceWith(replacement)
  })
  document.querySelectorAll('pre').forEach((pre) => {
    const wrapper = document.createElement('div')
    wrapper.className = 'code-block'
    const toolbar = document.createElement('div')
    toolbar.className = 'code-toolbar'
    const label = document.createElement('span')
    label.textContent = pre.dataset.language || 'CODE'
    const button = document.createElement('button')
    button.type = 'button'; button.dataset.copyCode = ''; button.className = 'code-copy'
    button.textContent = '复制代码'; button.setAttribute('aria-label', '复制此代码块')
    toolbar.append(label, button); pre.replaceWith(wrapper); wrapper.append(toolbar, pre)
  })
  const resources = new Map(
    props.assets.map((item) => [item.url, item.previewUrl]),
  )
  document.querySelectorAll('img').forEach((image) => {
    const src = image.getAttribute('src')
    if (resources.has(src)) image.setAttribute('src', resources.get(src))
    image.setAttribute('loading', 'lazy')
  })
  document.querySelectorAll('a').forEach((link) => {
    link.setAttribute('rel', 'noopener noreferrer')
    if (/^https?:\/\//i.test(link.getAttribute('href') || ''))
      link.setAttribute('target', '_blank')
  })
  const toc = [...document.querySelectorAll('h1,h2,h3')].map(
    (heading, index) => {
      heading.id = `${prefix}-section-${index}`
      return {
        id: heading.id,
        text: heading.textContent,
        level: Number(heading.tagName[1]),
      }
    },
  )
  return { html: document.body.innerHTML, toc }
})
watch(
  () => rendered.value.toc,
  (toc) => emit('toc', toc),
  { immediate: true },
)
const timers = new Set()
async function copyCode(event) {
  const button = event.target.closest('button[data-copy-code]')
  if (!button) return
  try {
    const code = button.closest('.code-block').querySelector('code')
    await navigator.clipboard.writeText(code?.textContent || '')
    button.textContent = '已复制'; button.setAttribute('aria-label', '代码已复制')
  } catch { button.textContent = '复制失败'; button.setAttribute('aria-label', '未能复制代码') }
  const timer = setTimeout(() => { button.textContent = '复制代码'; button.setAttribute('aria-label', '复制此代码块'); timers.delete(timer) }, 2200)
  timers.add(timer)
}
onBeforeUnmount(() => timers.forEach(clearTimeout))
</script>
<template><div class="markdown-content" @click="copyCode" v-html="rendered.html" /></template>

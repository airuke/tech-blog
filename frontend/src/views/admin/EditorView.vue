<script setup>
import { ref, reactive, computed, watch, onBeforeUnmount } from 'vue'
import {
  useRoute,
  useRouter,
  onBeforeRouteLeave,
  onBeforeRouteUpdate,
} from 'vue-router'
import { MdEditor } from 'md-editor-v3'
import DOMPurify from 'dompurify'
import 'md-editor-v3/lib/style.css'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api'
import { articleBody } from '@/utils/format'
import AppIcon from '@/components/AppIcon.vue'
import MarkdownContent from '@/components/MarkdownContent.vue'
import LoadingState from '@/components/LoadingState.vue'
import EmptyState from '@/components/EmptyState.vue'
const route = useRoute(),
  router = useRouter(),
  currentId = ref(''),
  categories = ref([]),
  tags = ref([]),
  assets = ref([]),
  loading = ref(true),
  loadError = ref(''),
  error = ref(''),
  saving = ref(false),
  mode = ref(window.matchMedia('(max-width: 840px)').matches ? 'source' : 'split'),
  coverInput = ref(null),
  imageBusy = ref(false),
  savedAt = ref('')
const form = reactive({
  title: '',
  summary: '',
  content: '',
  cover: null,
  categoryId: null,
  tagIds: [],
  visibility: 'PUBLIC',
  status: 'DRAFT',
  isTop: false,
  isFavorite: false,
  version: null,
})
const snapshot = ref(''),
  body = () => ({ ...form, tagIds: [...form.tagIds] }),
  dirty = computed(
    () => !loading.value && snapshot.value !== JSON.stringify(body()),
  )
const words = computed(() => Array.from(form.content).length),
  cover = computed(
    () =>
      assets.value.find((item) => item.url === form.cover)?.previewUrl ||
      form.cover,
  )
const toolbars = [
  'bold',
  'italic',
  'strikeThrough',
  '-',
  'title',
  'quote',
  'unorderedList',
  'orderedList',
  'task',
  '-',
  'codeRow',
  'code',
  'link',
  'image',
  'table',
  '-',
  'revoke',
  'next',
]
async function load() {
  loading.value = true
  loadError.value = ''
  error.value = ''
  currentId.value = String(route.params.id || '')
  savedAt.value = ''
  Object.assign(form, {
    title: '',
    summary: '',
    content: '',
    cover: null,
    categoryId: null,
    tagIds: [],
    visibility: route.query.visibility === 'PRIVATE' ? 'PRIVATE' : 'PUBLIC',
    status: 'DRAFT',
    isTop: false,
    isFavorite: false,
    version: null,
  })
  try {
    const [c, t, files, article] = await Promise.all([
      api.taxonomy('categories', true),
      api.taxonomy('tags', true),
      api.files({ pageSize: 200 }),
      currentId.value ? api.article(currentId.value) : Promise.resolve(null),
    ])
    categories.value = c
    tags.value = t
    assets.value = files.items
    if (article) Object.assign(form, articleBody(article))
    const references = [
      ...new Set(
        (form.content + ' ' + (form.cover || '')).match(
          /\/api\/files\/[a-f0-9-]{36}/g,
        ) || [],
      ),
    ]
    const missing = references.filter(
      (url) => !assets.value.some((item) => item.url === url),
    )
    const extra = await Promise.all(
      missing.map((url) => api.file(url.split('/').at(-1)).catch(() => null)),
    )
    assets.value.push(...extra.filter(Boolean))
    snapshot.value = JSON.stringify(body())
  } catch (e) {
    loadError.value = e.message
  } finally {
    loading.value = false
  }
}
async function save(status = form.status) {
  if (saving.value) return
  if (!form.title.trim()) {
    error.value = '请填写文章标题。'
    return
  }
  if (status === 'PUBLISHED' && !form.content.trim()) {
    error.value = '请填写正文后再发布。'
    return
  }
  saving.value = true
  error.value = ''
  try {
    const article = await api.saveArticle(currentId.value, {
      ...body(),
      status,
    })
    Object.assign(form, articleBody(article))
    currentId.value = article.id
    snapshot.value = JSON.stringify(body())
    savedAt.value = new Intl.DateTimeFormat('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date())
    await router.replace({ name: 'editor', params: { id: article.id } })
    ElMessage.success(
      status === 'DRAFT'
        ? '草稿已保存'
        : form.visibility === 'PRIVATE'
          ? '已保存到私人知识库'
          : '公开文章已保存',
    )
  } catch (e) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}
async function uploadImages(files, callback) {
  try {
    const uploaded = []
    for (const file of files) {
      if (!file.type.startsWith('image/'))
        throw new Error('正文插图请选择图片文件')
      const resource = await api.upload(file)
      assets.value.unshift(resource)
      uploaded.push(resource.url)
    }
    callback(uploaded)
  } catch (e) {
    error.value = e.message
    ElMessage.error(e.message)
  }
}
async function uploadCover(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  imageBusy.value = true
  try {
    const resource = await api.upload(file)
    if (!resource.contentType.startsWith('image/'))
      throw new Error('封面需要图片文件')
    assets.value.unshift(resource)
    form.cover = resource.url
  } catch (e) {
    error.value = e.message
  } finally {
    imageBusy.value = false
  }
}
async function leave() {
  if (!dirty.value) return true
  try {
    await ElMessageBox.confirm(
      '当前修改还没有保存。离开后这些修改将丢失。',
      '离开编辑器？',
      {
        confirmButtonText: '离开页面',
        cancelButtonText: '继续编辑',
        type: 'warning',
      },
    )
    return true
  } catch {
    return false
  }
}
function beforeUnload(event) {
  if (dirty.value) {
    event.preventDefault()
    event.returnValue = ''
  }
}
window.addEventListener('beforeunload', beforeUnload)
onBeforeUnmount(() => window.removeEventListener('beforeunload', beforeUnload))
onBeforeRouteLeave(leave)
onBeforeRouteUpdate((to) =>
  String(to.params.id || '') === currentId.value ? true : leave(),
)
watch(
  () => route.params.id,
  () => {
    if (String(route.params.id || '') !== currentId.value || loading.value)
      load()
  },
  { immediate: true },
)
</script>
<template>
  <section class="editor-page">
    <header class="editor-heading">
      <div>
        <router-link class="back-link" to="/admin/articles">
          ← 全部文章
        </router-link>
        <p>
          <span class="editor-save-state" :class="{ unsaved: dirty }">{{
            dirty
              ? '有未保存的修改'
              : currentId
                ? `已保存${savedAt ? ` · ${savedAt}` : ''}`
                : '新的文章'
          }}</span
          ><span>{{ words }} 字</span>
        </p>
      </div>
      <div class="editor-actions">
        <button
          class="button secondary"
          :disabled="saving || loading || Boolean(loadError)"
          @click="save('DRAFT')"
        >
          {{ form.status === 'PUBLISHED' ? '撤回为草稿' : '保存草稿' }}</button
        ><button
          class="button primary"
          :disabled="saving || loading || Boolean(loadError)"
          @click="save('PUBLISHED')"
        >
          <AppIcon
            :name="form.visibility === 'PRIVATE' ? 'lock' : 'globe'"
            :size="17"
          />{{
            saving
              ? '正在保存…'
              : form.visibility === 'PRIVATE'
                ? '保存知识库'
                : form.status === 'PUBLISHED'
                  ? '更新公开文章'
                  : '发布文章'
          }}
        </button>
      </div>
    </header>
    <LoadingState v-if="loading" /><EmptyState
      v-else-if="loadError"
      title="暂时无法打开编辑器"
      :text="loadError"
      error
    >
      <button class="button secondary" @click="load">
        重新读取
      </button> </EmptyState
    ><template v-else>
      <p v-if="error" class="inline-error editor-error" role="alert">
        {{ error }}
      </p>
      <div class="editor-layout">
        <section class="surface writing-surface">
          <label class="sr-only" for="article-title">文章标题</label
          ><input
            id="article-title"
            v-model="form.title"
            class="article-title-input"
            maxlength="160"
            placeholder="输入文章标题"
          />
          <div class="editor-modebar">
            <div role="group" aria-label="编辑器视图">
              <button
                v-for="view in [
                  { id: 'source', text: '编辑' },
                  { id: 'split', text: '双栏' },
                  { id: 'preview', text: '预览' },
                ]"
                :key="view.id"
                :class="{ active: mode === view.id }"
                :aria-pressed="mode === view.id"
                @click="mode = view.id"
              >
                {{ view.text }}
              </button>
            </div>
            <span
              >Markdown ·
              {{
                form.visibility === 'PRIVATE' ? '仅自己可见' : '发布后公开'
              }}</span
            >
          </div>
          <div class="editor-content" :class="mode">
            <div v-show="mode !== 'preview'" class="markdown-editor-pane">
              <MdEditor
                id="blog-md-editor"
                v-model="form.content"
                language="zh-CN"
                :preview="false"
                :toolbars="toolbars"
                :sanitize="DOMPurify.sanitize"
                no-mermaid
                no-katex
                no-prettier
                :auto-focus="false"
                @on-upload-img="uploadImages"
                @on-save="save(form.status)"
              />
            </div>
            <div v-if="mode !== 'source'" class="editor-preview-pane">
              <p class="preview-caption">阅读预览</p>
              <MarkdownContent
                v-if="form.content"
                :content="form.content"
                :assets="assets"
              />
              <div v-else class="preview-placeholder">
                <AppIcon name="book" :size="32" />
                <p>正文预览</p>
                <span
                  >支持代码块、表格、图片与附件。</span
                >
              </div>
            </div>
          </div>
          <footer class="editor-bottom">
            {{
              form.visibility === 'PRIVATE'
                ? '私人文章不会出现在公开博客、搜索和归档中。'
                : '保存草稿后，仅博主可见；发布文章后进入公开博客。'
            }}
          </footer>
        </section>
        <aside class="surface article-settings">
          <h2>文章设置</h2>
          <div class="settings-field">
            <label for="article-visibility">可见范围</label
            ><el-select id="article-visibility" v-model="form.visibility">
              <el-option
                label="公开 · 发布后访客可见"
                value="PUBLIC"
              /><el-option label="私有 · 仅博主可见" value="PRIVATE" />
            </el-select>
            <p>草稿始终不会对外显示。</p>
          </div>
          <div class="settings-field">
            <label for="article-category">文章分类</label
            ><el-select
              id="article-category"
              v-model="form.categoryId"
              placeholder="选择分类"
              clearable
            >
              <el-option
                v-for="category in categories"
                :key="category.id"
                :label="category.name"
                :value="category.id"
              /> </el-select
            ><router-link v-if="!categories.length" to="/admin/categories">
              先创建一个分类 →
            </router-link>
          </div>
          <div class="settings-field">
            <label for="article-tags">文章标签</label
            ><el-select
              id="article-tags"
              v-model="form.tagIds"
              multiple
              :multiple-limit="20"
              placeholder="选择标签"
            >
              <el-option
                v-for="tag in tags"
                :key="tag.id"
                :label="tag.name"
                :value="tag.id"
              /> </el-select
            ><router-link v-if="!tags.length" to="/admin/tags">
              去整理标签 →
            </router-link>
          </div>
          <div class="settings-field">
            <label for="article-summary">文章摘要</label
            ><el-input
              id="article-summary"
              v-model="form.summary"
              type="textarea"
              :rows="4"
              maxlength="400"
              show-word-limit
              placeholder="留空时，从正文自动提取摘要"
            />
          </div>
          <div class="settings-field">
            <label>封面图片</label
            ><img
              v-if="cover"
              class="editor-cover"
              :src="cover"
              alt="文章封面预览"
            />
            <div class="cover-actions">
              <button
                class="button secondary small"
                :disabled="imageBusy"
                @click="coverInput.click()"
              >
                <AppIcon name="upload" :size="15" />{{
                  imageBusy ? '上传中…' : cover ? '更换封面' : '上传封面'
                }}</button
              ><button
                v-if="cover"
                class="text-button danger"
                @click="form.cover = null"
              >
                移除
              </button>
            </div>
            <input
              ref="coverInput"
              class="sr-only"
              type="file"
              accept="image/png,image/jpeg,image/gif,image/webp"
              aria-label="选择文章封面"
              @change="uploadCover"
            /><el-select
              v-if="assets.some((a) => a.contentType.startsWith('image/'))"
              v-model="form.cover"
              placeholder="或选择已有图片"
              clearable
            >
              <el-option
                v-for="resource in assets.filter((a) =>
                  a.contentType.startsWith('image/'),
                )"
                :key="resource.id"
                :label="resource.name"
                :value="resource.url"
              />
            </el-select>
            <p>PNG、JPEG、GIF、WebP，最多 20 MiB。</p>
          </div>
          <div class="settings-options">
            <el-checkbox v-model="form.isTop">置顶到公开列表前面</el-checkbox
            ><el-checkbox v-model="form.isFavorite">加入自己的收藏</el-checkbox>
          </div>
        </aside>
      </div>
    </template>
  </section>
</template>

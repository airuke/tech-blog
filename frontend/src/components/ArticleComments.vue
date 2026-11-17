<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { api } from '@/api'
import { date } from '@/utils/format'
const props = defineProps({ articleId: { type: String, required: true } }),
  items = ref([]), total = ref(0), page = ref(1), loading = ref(true),
  listError = ref(''), nickname = ref(''), content = ref(''),
  saving = ref(false), formError = ref(''), notice = ref('')
let serial = 0, disposed = false, submissionId = '', fingerprint = ''
function newSubmissionId() {
  if (crypto.randomUUID) return crypto.randomUUID()
  const bytes = crypto.getRandomValues(new Uint8Array(16))
  bytes[6] = (bytes[6] & 15) | 64
  bytes[8] = (bytes[8] & 63) | 128
  const hex = Array.from(bytes, (value) => value.toString(16).padStart(2, '0')).join('')
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}
async function load() {
  const current = ++serial
  loading.value = true
  listError.value = ''
  try {
    const result = await api.publicComments(props.articleId, { page: page.value, pageSize: 10 })
    if (current === serial) {
      const last = Math.max(1, Math.ceil(Number(result.total) / 10))
      if (page.value > last) { page.value = last; await load(); return }
      items.value = result.items
      total.value = Number(result.total)
    }
  } catch (error) {
    if (current === serial) listError.value = error.message
  } finally { if (current === serial) loading.value = false }
}
async function submit() {
  if (saving.value) return
  formError.value = ''
  notice.value = ''
  const body = { nickname: nickname.value.trim(), content: content.value.trim() }
  if (!body.content) { formError.value = '请填写评论'; return }
  const current = JSON.stringify(body)
  if (fingerprint !== current || !submissionId) {
    submissionId = newSubmissionId()
    fingerprint = current
  }
  saving.value = true
  try {
    const result = await api.submitComment(props.articleId, { ...body, submissionId })
    if (disposed) return
    content.value = ''
    submissionId = ''
    fingerprint = ''
    notice.value = result.message
    page.value = 1
    await load()
  } catch (error) { if (!disposed) formError.value = error.message }
  finally { saving.value = false }
}
function turn(value) { page.value = value; load() }
onMounted(load)
onBeforeUnmount(() => { disposed = true; serial++ })
</script>
<template>
  <section id="comments" class="article-comments" aria-labelledby="comments-heading">
    <header class="comments-heading">
      <h2 id="comments-heading">评论</h2><span v-if="!loading && !listError">{{ total }}</span>
    </header>
    <form class="comment-form" @submit.prevent="submit">
      <div class="comment-field comment-name-field">
        <label for="comment-nickname">昵称（选填）</label>
        <input id="comment-nickname" v-model="nickname" maxlength="40"
          placeholder="留空为访客" autocomplete="nickname" :disabled="saving" />
      </div>
      <div class="comment-field">
        <label for="comment-content">评论内容</label>
        <textarea id="comment-content" v-model="content" rows="4" maxlength="2000"
          placeholder="输入评论" required :disabled="saving" />
      </div>
      <div class="comment-form-footer">
        <span>审核通过后显示 · {{ content.length }}/2000</span>
        <button class="button primary small" type="submit" :disabled="saving">
          {{ saving ? '提交中…' : '提交评论' }}
        </button>
      </div>
      <p v-if="formError" class="inline-error" role="alert">{{ formError }}</p>
      <p v-if="notice" class="comment-notice" role="status">{{ notice }}</p>
    </form>
    <div class="public-comment-list" aria-busy="loading">
      <p v-if="loading" class="comment-empty" role="status">正在加载评论…</p>
      <div v-else-if="listError" class="inline-error" role="alert">
        {{ listError }} <button class="text-button" @click="load">重试</button>
      </div>
      <p v-else-if="!items.length" class="comment-empty">暂无评论</p>
      <template v-else>
        <article v-for="comment in items" :key="comment.id" class="public-comment">
          <div class="comment-meta">
            <strong>{{ comment.nickname }}</strong><span v-if="comment.nickname !== '访客'" class="visitor-badge">访客</span>
            <time :datetime="comment.createdAt">{{ date(comment.createdAt, { hour: '2-digit', minute: '2-digit' }) }}</time>
          </div>
          <p class="comment-text">{{ comment.content }}</p>
          <div v-if="comment.reply" class="comment-reply">
            <div class="comment-meta">
              <strong class="owner-badge">博主回复</strong>
              <time :datetime="comment.reply.updatedAt">{{ date(comment.reply.updatedAt, { hour: '2-digit', minute: '2-digit' }) }}</time>
            </div>
            <p class="comment-text">{{ comment.reply.content }}</p>
          </div>
        </article>
      </template>
    </div>
    <nav v-if="!listError && total > 10" class="pagination" aria-label="评论分页">
      <button :disabled="loading || page === 1" @click="turn(page - 1)">上一页</button>
      <span>{{ page }} / {{ Math.ceil(total / 10) }}</span>
      <button :disabled="loading || page * 10 >= total" @click="turn(page + 1)">下一页</button>
    </nav>
  </section>
</template>

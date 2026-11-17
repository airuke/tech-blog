<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api'
import AppIcon from '@/components/AppIcon.vue'
import LoadingState from '@/components/LoadingState.vue'
import CommentModerationCard from '@/components/CommentModerationCard.vue'
const items = ref([]), total = ref(0), page = ref(1), status = ref('PENDING'),
  counts = ref({ total: 0, pending: 0, approved: 0, rejected: 0 }),
  loading = ref(true), error = ref(''), busy = ref(false), dialog = ref(false),
  editing = ref(null), reply = ref(''), formError = ref('')
const filters = [
  { value: 'PENDING', label: '待审核', count: 'pending' },
  { value: 'APPROVED', label: '已通过', count: 'approved' },
  { value: 'REJECTED', label: '已拒绝', count: 'rejected' },
  { value: '', label: '全部', count: 'total' },
]
let serial = 0
async function load() {
  const current = ++serial
  loading.value = true
  error.value = ''
  try {
    const result = await api.comments({ page: page.value, pageSize: 12, status: status.value || undefined })
    if (current !== serial) return
    const last = Math.max(1, Math.ceil(Number(result.total) / 12))
    if (page.value > last) { page.value = last; await load(); return }
    items.value = result.items
    total.value = Number(result.total)
    counts.value = result.counts
  } catch (failure) { if (current === serial) error.value = failure.message }
  finally { if (current === serial) loading.value = false }
}
function filter(value) { status.value = value; page.value = 1; load() }
function turn(value) { page.value = value; load() }
async function review(comment, value) {
  if (busy.value) return
  busy.value = true
  try {
    await api.reviewComment(comment.id, value, comment.version)
    ElMessage.success(value === 'APPROVED' ? '评论已通过' : '评论已隐藏')
    await load()
  } catch (failure) { ElMessage.error(failure.message); await load() }
  finally { busy.value = false }
}
function edit(comment) {
  editing.value = comment
  reply.value = comment.reply?.content || ''
  formError.value = ''
  dialog.value = true
}
async function saveReply() {
  if (busy.value) return
  if (!reply.value.trim()) { formError.value = '请填写回复'; return }
  busy.value = true
  formError.value = ''
  try {
    await api.replyComment(editing.value.id, reply.value.trim(), editing.value.version)
    dialog.value = false
    ElMessage.success('回复已保存')
    await load()
  } catch (failure) { formError.value = failure.message; if (failure.status === 409) await load() }
  finally { busy.value = false }
}
async function remove(comment, onlyReply = false) {
  if (busy.value) return
  try {
    await ElMessageBox.confirm(onlyReply ? '删除这条博主回复？' : '删除这条评论及其回复？删除后无法恢复。',
      onlyReply ? '删除回复' : '删除评论', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
  } catch { return }
  busy.value = true
  try {
    if (onlyReply) await api.replyComment(comment.id, '', comment.version)
    else await api.deleteComment(comment.id, comment.version)
    ElMessage.success(onlyReply ? '回复已删除' : '评论已删除')
    await load()
  } catch (failure) { ElMessage.error(failure.message); await load() }
  finally { busy.value = false }
}
onMounted(load)
</script>
<template>
  <section class="comments-admin">
    <header class="admin-page-heading">
      <h1>评论管理</h1>
      <button class="button secondary small" :disabled="loading || busy" @click="load">
        <AppIcon name="refresh" :size="16" />刷新
      </button>
    </header>
    <section class="surface" aria-label="评论审核列表">
      <div class="comment-filters" role="group" aria-label="按审核状态筛选">
        <button v-for="item in filters" :key="item.value" :aria-pressed="status === item.value" :disabled="busy"
          @click="filter(item.value)">{{ item.label }}<span>{{ counts[item.count] }}</span></button>
      </div>
      <LoadingState v-if="loading" />
      <div v-else-if="error" class="comments-admin-error inline-error" role="alert">
        {{ error }} <button class="text-button" @click="load">重试</button>
      </div>
      <div v-else-if="!items.length" class="moderation-empty" role="status">
        <AppIcon name="comment" :size="28" />
        <p>{{ status === 'PENDING' ? '暂无待审核评论' : status === 'APPROVED' ? '暂无已通过评论' : status === 'REJECTED' ? '暂无已拒绝评论' : '暂无评论' }}</p>
      </div>
      <template v-else>
        <CommentModerationCard v-for="comment in items" :key="comment.id" :comment="comment" :busy="busy"
          @review="review(comment, $event)" @reply="edit(comment)" @remove="remove(comment)"
          @clear-reply="remove(comment, true)" />
      </template>
      <nav v-if="!loading && !error && total > 12" class="pagination" aria-label="后台评论分页">
        <button :disabled="busy || page === 1" @click="turn(page - 1)">上一页</button>
        <span>{{ page }} / {{ Math.ceil(total / 12) }}</span>
        <button :disabled="busy || page * 12 >= total" @click="turn(page + 1)">下一页</button>
      </nav>
    </section>
    <el-dialog v-model="dialog" :title="editing?.reply ? '编辑回复' : '回复评论'" width="560px"
      :close-on-click-modal="false" :close-on-press-escape="!busy" :show-close="!busy">
      <form class="dialog-form" @submit.prevent="saveReply">
        <blockquote class="reply-context">{{ editing?.nickname }}：{{ editing?.content }}</blockquote>
        <label for="moderation-reply">博主回复</label>
        <el-input id="moderation-reply" v-model="reply" type="textarea" :rows="5" maxlength="2000"
          show-word-limit placeholder="输入回复" required :disabled="busy" />
        <p v-if="formError" class="inline-error" role="alert">{{ formError }}</p>
        <div class="dialog-actions">
          <el-button :disabled="busy" @click="dialog = false">取消</el-button>
          <el-button type="primary" native-type="submit" :loading="busy">保存回复</el-button>
        </div>
      </form>
    </el-dialog>
  </section>
</template>

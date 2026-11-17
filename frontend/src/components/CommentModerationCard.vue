<script setup>
import { date } from '@/utils/format'
import AppIcon from '@/components/AppIcon.vue'
defineProps({ comment: { type: Object, required: true }, busy: Boolean })
defineEmits(['review', 'reply', 'clear-reply', 'remove'])
const labels = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝' }
</script>
<template>
  <article class="moderation-card">
    <header class="moderation-card-header">
      <router-link class="comment-article-link" :to="{ name: 'editor', params: { id: comment.articleId } }">
        <AppIcon name="book" :size="16" /><span>{{ comment.articleTitle }}</span>
      </router-link>
      <span class="comment-status" :class="comment.status.toLowerCase()">{{ labels[comment.status] }}</span>
    </header>
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
    <footer class="moderation-actions">
      <button v-if="comment.status !== 'APPROVED'" class="button primary small" :disabled="busy"
        @click="$emit('review', 'APPROVED')"><AppIcon name="check" :size="16" />通过</button>
      <button v-if="comment.status !== 'REJECTED'" class="button secondary small" :disabled="busy"
        @click="$emit('review', 'REJECTED')">{{ comment.status === 'APPROVED' ? '隐藏' : '拒绝' }}</button>
      <button class="button secondary small" :disabled="busy || comment.status !== 'APPROVED'"
        :title="comment.status !== 'APPROVED' ? '审核通过后可以回复' : undefined"
        @click="$emit('reply')">{{ comment.reply ? '编辑回复' : '回复' }}</button>
      <button v-if="comment.reply && comment.status === 'APPROVED'" class="text-button" :disabled="busy"
        @click="$emit('clear-reply')">删除回复</button>
      <button class="button secondary small danger" :disabled="busy" @click="$emit('remove')">
        <AppIcon name="trash" :size="16" />删除
      </button>
    </footer>
  </article>
</template>

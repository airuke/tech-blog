<script setup>
import { ref, computed, onMounted } from 'vue'
import { api } from '@/api'
import { date } from '@/utils/format'
import AppIcon from '@/components/AppIcon.vue'
import LoadingState from '@/components/LoadingState.vue'
import EmptyState from '@/components/EmptyState.vue'
const groups = ref([]),
  loading = ref(true),
  error = ref(''),
  total = computed(() =>
    groups.value.reduce((n, group) => n + group.articles.length, 0),
  )
async function load() {
  loading.value = true
  error.value = ''
  try {
    groups.value = await api.archives()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>
<template>
  <section class="archive-page">
    <header class="page-intro">
      <p class="eyebrow">ARTICLE ARCHIVE</p>
      <h1>文章归档</h1>
      <p>按发布时间浏览 {{ total }} 篇公开文章。</p>
    </header>
    <LoadingState v-if="loading" /><EmptyState
      v-else-if="error"
      title="归档暂时无法读取"
      :text="error"
      error
    >
      <button class="button secondary" @click="load">
        重新读取
      </button> </EmptyState
    ><EmptyState
      v-else-if="!groups.length"
      icon="archive"
      title="还没有归档文章"
      text="文章发布后会按月份自动归档。"
    >
      <router-link class="text-link" to="/">
        返回首页<AppIcon name="arrow" :size="17" />
      </router-link>
    </EmptyState>
    <div v-else class="archive-timeline">
      <section
        v-for="group in groups"
        :key="`${group.year}-${group.month}`"
        class="archive-month"
      >
        <header>
          <strong>{{ group.year }}</strong
          ><span
            >{{ String(group.month).padStart(2, '0') }} 月 ·
            {{ group.articles.length }} 篇</span
          >
        </header>
        <router-link
          v-for="article in group.articles"
          :key="article.id"
          :to="{ name: 'article', params: { id: article.id } }"
        >
          <time>{{ date(article.publishedAt, { year: undefined }) }}</time>
          <h2>{{ article.title }}</h2>
          <AppIcon name="arrow" :size="18" />
        </router-link>
      </section>
    </div>
  </section>
</template>

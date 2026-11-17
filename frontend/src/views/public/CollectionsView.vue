<script setup>
import { ref, watch } from 'vue'
import { api } from '@/api'
import AppIcon from '@/components/AppIcon.vue'
import LoadingState from '@/components/LoadingState.vue'
import EmptyState from '@/components/EmptyState.vue'
const props = defineProps({ kind: { type: String, required: true } }),
  items = ref([]),
  loading = ref(true),
  error = ref('')
async function load() {
  loading.value = true
  error.value = ''
  try {
    items.value = await api.taxonomy(props.kind)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
watch(() => props.kind, load, { immediate: true })
</script>
<template>
  <section class="collection-page">
    <header class="page-intro">
      <p class="eyebrow">
        {{
          kind === 'categories' ? 'INDEX / TOPICS' : 'INDEX / TAGS'
        }}
      </p>
      <h1>
        {{
          kind === 'categories'
            ? '文章分类'
            : '技术标签'
        }}
      </h1>
      <p>
        {{
          kind === 'categories'
            ? '按技术领域或项目方向浏览文章。'
            : '按技术栈、算法名称与实验主题查找文章。'
        }}
      </p>
    </header>
    <LoadingState v-if="loading" /><EmptyState
      v-else-if="error"
      title="暂时无法读取"
      :text="error"
      error
    >
      <button class="button secondary" @click="load">
        重新读取
      </button> </EmptyState
    ><EmptyState
      v-else-if="!items.length"
      :icon="kind === 'categories' ? 'folder' : 'tag'"
      :title="
        kind === 'categories' ? '暂无公开分类' : '暂无公开标签'
      "

    >
      <router-link class="text-link" to="/">
        返回文章首页<AppIcon name="arrow" :size="17" />
      </router-link>
    </EmptyState>
    <div v-else class="collection-list">
      <router-link
        v-for="(item, index) in items"
        :key="item.id"
        :to="{
          name: 'home',
          query: { [kind === 'categories' ? 'categoryId' : 'tagId']: item.id },
        }"
      >
        <span class="collection-index">{{
          String(index + 1).padStart(2, '0')
        }}</span>
        <div>
          <h2>{{ kind === 'tags' ? '# ' : '' }}{{ item.name }}</h2>
          <p>{{ item.description || '' }}</p>
        </div>
        <span>{{ item.articleCount }} 篇</span><AppIcon name="arrow" />
      </router-link>
    </div>
  </section>
</template>

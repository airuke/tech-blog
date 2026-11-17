<script setup>
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '@/api'
import { useSite } from '@/stores/site'
import { date } from '@/utils/format'
import MarkdownContent from '@/components/MarkdownContent.vue'
import AppIcon from '@/components/AppIcon.vue'
import LoadingState from '@/components/LoadingState.vue'
import EmptyState from '@/components/EmptyState.vue'
import ArticleComments from '@/components/ArticleComments.vue'
import ArticleActions from '@/components/ArticleActions.vue'
const route = useRoute(), site = useSite()
const article = ref(null), loading = ref(true), error = ref(''), headings = ref([]), activeHeading = ref(''), progress = ref(0)
let serial = 0, frame = 0
async function load() {
  const current = ++serial
  loading.value = true; error.value = ''; headings.value = []
  try {
    const value = await api.publicArticle(route.params.id)
    if (current === serial) { article.value = value; document.title = `${value.title} · ${site.site.title}` }
  } catch (e) { if (current === serial) error.value = e.message }
  finally { if (current === serial) { loading.value = false; await nextTick(); update() } }
}
function update() {
  cancelAnimationFrame(frame)
  frame = requestAnimationFrame(() => {
    const length = document.documentElement.scrollHeight - window.innerHeight
    progress.value = length > 0 ? Math.min(100, Math.max(0, window.scrollY / length * 100)) : 100
    const visible = headings.value.filter((heading) => document.getElementById(heading.id)?.getBoundingClientRect().top <= 150)
    activeHeading.value = visible.at(-1)?.id || headings.value[0]?.id || ''
  })
}
watch(() => route.params.id, load, { immediate: true })
watch(headings, () => nextTick(update))
onMounted(() => { window.addEventListener('scroll', update, { passive: true }); window.addEventListener('resize', update); update() })
onBeforeUnmount(() => { serial++; cancelAnimationFrame(frame); window.removeEventListener('scroll', update); window.removeEventListener('resize', update) })
</script>
<template>
  <div class="reading-page" :class="{ 'reading-with-toc': headings.length }">
    <progress v-if="article && !loading && !error" class="reading-meter" :value="progress" max="100" aria-label="页面阅读进度" />
    <ArticleActions />
    <LoadingState v-if="loading" />
    <EmptyState v-else-if="error" title="这篇文章暂时无法打开" :text="error" error><button class="button secondary" @click="load">重新加载</button></EmptyState>
    <template v-else-if="article">
      <header class="article-header">
        <div class="article-kicker"><span class="section-index">NOTE / {{ String(article.id).padStart(3, '0') }}</span>
          <router-link v-if="article.category" :to="{ name: 'home', query: { categoryId: article.category.id } }">{{ article.category.name }}</router-link><span v-if="article.isTop">置顶</span></div>
        <h1>{{ article.title }}</h1>
        <p v-if="article.summary && article.summary.trim() !== article.content.trim()" class="article-lead">{{ article.summary }}</p>
        <div class="article-meta"><span class="article-author">Airuike</span><time :datetime="article.publishedAt">{{ date(article.publishedAt) }}</time>
          <span>{{ article.wordCount }} 字</span><span>{{ article.readingMinutes }} 分钟阅读</span><span><AppIcon name="eye" :size="14" />{{ article.viewCount }}</span></div>
      </header>
      <img v-if="article.cover" class="article-hero-image" :src="article.cover" :alt="`${article.title}的封面`" />
      <div class="reading-layout">
        <article class="reading-body">
          <MarkdownContent :content="article.content" @toc="headings = $event" />
          <footer class="article-end"><span class="article-end-mark" aria-hidden="true">[ END ]</span>
            <div v-if="article.tags?.length" class="tag-cloud"><router-link v-for="tag in article.tags" :key="tag.id" :to="{ name: 'home', query: { tagId: tag.id } }">{{ tag.name }}</router-link></div>
          </footer>
          <ArticleComments :key="article.id" :article-id="article.id" />
        </article>
        <aside v-if="headings.length" class="article-toc"><h2>本篇目录<span>{{ String(headings.length).padStart(2, '0') }}</span></h2>
          <a v-for="heading in headings" :key="heading.id" :class="{ indented: heading.level === 3, active: activeHeading === heading.id }"
            :aria-current="activeHeading === heading.id ? 'location' : undefined" :href="`#${heading.id}`">{{ heading.text }}</a>
          <a class="toc-top" href="#main-content">回到顶部<AppIcon name="arrow-up" :size="14" /></a>
        </aside>
      </div>
    </template>
  </div>
</template>

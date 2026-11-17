<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/api'
import { useSite } from '@/stores/site'
import ArticleCard from '@/components/ArticleCard.vue'
import BlogSidebar from '@/components/BlogSidebar.vue'
import AppIcon from '@/components/AppIcon.vue'
import LoadingState from '@/components/LoadingState.vue'
import EmptyState from '@/components/EmptyState.vue'
const route = useRoute(),
  router = useRouter(),
  site = useSite(),
  items = ref([]),
  total = ref(0),
  loading = ref(true),
  error = ref('')
const page = computed(() => Math.max(1, Number(route.query.page) || 1)),
  filtered = computed(() =>
    Boolean(route.query.q || route.query.categoryId || route.query.tagId),
  )
const heading = computed(() =>
  route.query.q
    ? `搜索「${route.query.q}」`
    : route.query.categoryId
      ? site.categories.find((c) => String(c.id) === String(route.query.categoryId))?.name ||
        '分类文章'
      : route.query.tagId
        ? `# ${site.tags.find((t) => String(t.id) === String(route.query.tagId))?.name || '标签文章'}`
        : '技术笔记',
)
let serial = 0
async function load() {
  const current = ++serial
  loading.value = true
  error.value = ''
  try {
    const result = await api.publicArticles({
      page: page.value,
      pageSize: 8,
      keyword: route.query.q,
      categoryId: route.query.categoryId,
      tagId: route.query.tagId,
    })
    if (current === serial) {
      items.value = result.items
      total.value = result.total
    }
  } catch (e) {
    if (current === serial) error.value = e.message
  } finally {
    if (current === serial) loading.value = false
  }
}
function turn(value) {
  router.push({ name: 'home', query: { ...route.query, page: value } })
}
watch(() => route.fullPath, load, { immediate: true })
</script>
<template>
  <div class="home-page">
    <header class="home-masthead">
      <div><p class="eyebrow">AIRUIKE / ENGINEERING NOTES</p><h1>{{ heading }}<span class="heading-period">.</span></h1>
        <p v-if="!filtered" class="masthead-subtitle">计算机 · ROS 2 · 算法与控制</p>
        <p v-else class="masthead-subtitle">{{ !loading && !error ? `${total} 篇匹配文章` : '文章索引' }}</p>
      </div>
      <div class="masthead-stamp" aria-hidden="true"><span>[ A / N ]</span><span>PERSONAL<br />TECH JOURNAL</span></div>
    </header>
    <div class="public-content-layout">
    <header class="home-heading">
      <div>
        <span class="section-index">01 /</span><h2>{{ filtered ? '筛选结果' : '最新发布' }}</h2>
        <span v-if="!loading && !error" class="article-total" aria-live="polite">
          {{ total }} 篇
        </span>
      </div>
      <router-link v-if="filtered" class="filter-reset" to="/">
        全部文章<AppIcon name="arrow" :size="15" />
      </router-link>
      <router-link v-else class="filter-reset" to="/archives">
        <AppIcon name="archive" :size="16" />归档
      </router-link>
    </header>
    <section id="latest" class="story-section">
      <div class="article-list">
        <LoadingState v-if="loading" /><EmptyState
          v-else-if="error"
          title="文章加载失败"
          :text="error"
          error
        >
          <button class="button secondary" @click="load">重试</button>
        </EmptyState>
        <EmptyState
          v-else-if="!items.length"
          :title="filtered ? '没有匹配的文章' : '暂无公开文章'"
          :text="filtered ? '换个关键词试试。' : ''"
        >
          <router-link v-if="filtered" class="button secondary" to="/">
            查看全部文章
          </router-link>
        </EmptyState>
        <template v-else>
          <ArticleCard v-for="(article, index) in items" :key="article.id" :article="article" :featured="!filtered && page === 1 && index === 0" :index="(page - 1) * 8 + index + 1" />
        </template>
      </div>
      <nav v-if="!loading && !error && Number(total) > 8" class="pagination" aria-label="文章分页">
        <button :disabled="page === 1" @click="turn(page - 1)">上一页</button
        ><span>{{ page }} / {{ Math.ceil(total / 8) }}</span
        ><button :disabled="page * 8 >= total" @click="turn(page + 1)">
          下一页
        </button>
      </nav>
    </section>
    <BlogSidebar />
    </div>
  </div>
</template>

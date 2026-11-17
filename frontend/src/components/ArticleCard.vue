<script setup>
import { date } from '@/utils/format'
import AppIcon from './AppIcon.vue'
defineProps({ article: { type: Object, required: true }, featured: Boolean, index: { type: Number, default: 1 } })
</script>
<template>
  <article class="story-row" :class="{ 'story-featured': featured, 'story-with-cover': article.cover }">
    <div class="story-topline"><span class="story-index">{{ String(index).padStart(2, '0') }}<span v-if="featured"> / LATEST</span></span>
      <time :datetime="article.publishedAt">{{ date(article.publishedAt) }}</time><span v-if="article.isTop" class="story-pinned"><AppIcon name="star" :size="12" />置顶</span></div>
    <router-link v-if="article.cover" class="story-cover" :to="{ name: 'article', params: { id: article.id } }" tabindex="-1" aria-hidden="true"><img :src="article.cover" alt="" loading="lazy" /></router-link>
    <div class="story-content">
      <router-link v-if="article.category" class="story-category" :to="{ name: 'home', query: { categoryId: article.category.id } }">{{ article.category.name }}</router-link>
      <h2><router-link :to="{ name: 'article', params: { id: article.id } }">{{ article.title }}</router-link></h2>
      <p v-if="article.summary" class="story-summary">{{ article.summary }}</p>
      <div v-if="article.tags?.length" class="story-tags"><router-link v-for="tag in article.tags" :key="tag.id" :to="{ name: 'home', query: { tagId: tag.id } }"># {{ tag.name }}</router-link></div>
    </div>
    <footer class="story-bottom"><div class="story-meta"><span><AppIcon name="clock" :size="14" />{{ article.readingMinutes }} 分钟阅读</span><span v-if="article.viewCount != null"><AppIcon name="eye" :size="14" />{{ article.viewCount }} 次浏览</span></div>
      <router-link class="story-read" :to="{ name: 'article', params: { id: article.id } }" :aria-label="`阅读全文：${article.title}`"><span>阅读文章</span><AppIcon name="arrow-up-right" :size="18" /></router-link>
    </footer>
  </article>
</template>

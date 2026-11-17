<script setup>
import { useRoute } from 'vue-router'
import { useSite } from '@/stores/site'
import AppIcon from './AppIcon.vue'
import SignalTrace from './SignalTrace.vue'
const route = useRoute(), site = useSite()
</script>
<template>
  <aside class="public-aside" aria-label="作者与文章索引">
    <section class="blog-profile">
      <div class="profile-index"><span>AUTHOR / 01</span><span class="profile-signal" /></div>
      <header class="profile-heading"><h2>Airuike</h2><p>计算机科学与技术</p></header>
      <SignalTrace />
      <div class="profile-skills" aria-label="关注的技术"><span>C++</span><span>Python</span><span>ROS 2</span></div>
      <router-link class="profile-about" to="/about">关于作者<AppIcon name="arrow-up-right" :size="18" /></router-link>
    </section>
    <section v-if="site.categories.length" class="aside-taxonomy">
      <header><h2>文章分类</h2><router-link to="/categories" aria-label="查看全部分类"><AppIcon name="arrow-up-right" :size="16" /></router-link></header>
      <router-link v-for="category in site.categories.slice(0, 8)" :key="category.id" class="category-line"
        :aria-current="String(route.query.categoryId) === String(category.id) ? 'page' : undefined"
        :to="{ name: 'home', query: { categoryId: category.id } }"><span>{{ category.name }}</span><span>{{ String(category.articleCount).padStart(2, '0') }}</span></router-link>
    </section>
    <section v-if="site.tags.length" class="aside-taxonomy">
      <header><h2>技术标签</h2><router-link to="/tags" aria-label="查看全部标签"><AppIcon name="arrow-up-right" :size="16" /></router-link></header>
      <div class="tag-cloud"><router-link v-for="tag in site.tags.slice(0, 16)" :key="tag.id"
        :aria-current="String(route.query.tagId) === String(tag.id) ? 'page' : undefined"
        :to="{ name: 'home', query: { tagId: tag.id } }">{{ tag.name }}</router-link></div>
    </section>
    <router-link class="aside-archive" to="/archives"><AppIcon name="archive" :size="17" /><span>按时间浏览</span><AppIcon name="arrow" :size="17" /></router-link>
  </aside>
</template>

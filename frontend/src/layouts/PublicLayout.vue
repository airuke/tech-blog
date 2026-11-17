<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSite } from '@/stores/site'
import BrandMark from '@/components/BrandMark.vue'
import AppIcon from '@/components/AppIcon.vue'
const router = useRouter(), route = useRoute(), site = useSite()
const query = ref(''), menu = ref(false), searchOpen = ref(false), error = ref('')
const searchInput = ref(null), menuButton = ref(null)
const links = [
  { name: 'home', text: '文章' }, { name: 'categories', text: '分类' },
  { name: 'tags', text: '标签' }, { name: 'archives', text: '归档' }, { name: 'about', text: '关于' },
]
async function load() {
  try { await site.refresh(); error.value = '' }
  catch (e) { error.value = e.message }
}
function search() {
  router.push({ name: 'home', query: query.value.trim() ? { q: query.value.trim() } : {} })
  menu.value = false
}
async function openSearch() {
  searchOpen.value = !searchOpen.value
  if (searchOpen.value) { await nextTick(); searchInput.value?.focus() }
}
async function toggleMenu() {
  menu.value = !menu.value
  if (menu.value) { await nextTick(); document.querySelector('#public-navigation a')?.focus() }
}
function keyboard(event) {
  const editing = event.target instanceof HTMLElement &&
    (event.target.closest('input,textarea,select') || event.target.isContentEditable)
  if (event.key === '/' && !editing && !event.ctrlKey && !event.metaKey && !event.altKey) {
    event.preventDefault(); menu.value = false; searchOpen.value = true
    nextTick(() => searchInput.value?.focus())
  }
  if (event.key === 'Escape') {
    if (menu.value) menuButton.value?.focus()
    menu.value = false; searchOpen.value = false
  }
}
watch(() => route.query.q, (value) => { query.value = typeof value === 'string' ? value : '' }, { immediate: true })
watch(() => route.fullPath, () => { menu.value = false; searchOpen.value = false })
onMounted(() => { load(); window.addEventListener('keydown', keyboard) })
onBeforeUnmount(() => window.removeEventListener('keydown', keyboard))
</script>
<template>
  <div class="public-app">
    <a class="skip-link" href="#main-content">跳到正文</a>
    <header class="public-header">
      <div class="public-header-inner">
        <BrandMark />
        <nav id="public-navigation" :class="{ opened: menu }" aria-label="博客导航">
          <router-link v-for="link in links" :key="link.name" :to="{ name: link.name }"
            :class="{ 'is-article-route': link.name === 'home' && route.name === 'article' }">{{ link.text }}</router-link>
        </nav>
        <div class="header-actions">
          <button class="icon-button search-toggle" :aria-expanded="searchOpen" aria-controls="blog-search"
            aria-label="搜索文章" @click="openSearch"><AppIcon name="search" :size="19" /></button>
          <router-link class="header-admin" to="/admin" aria-label="进入博客管理后台"><AppIcon name="lock" :size="15" /><span>管理</span></router-link>
          <button ref="menuButton" class="icon-button mobile-menu" :aria-expanded="menu"
            aria-controls="public-navigation" :aria-label="menu ? '收起导航' : '打开导航'" @click="toggleMenu"><AppIcon :name="menu ? 'close' : 'menu'" /></button>
        </div>
        <form id="blog-search" class="header-search" :class="{ 'search-open': searchOpen }" role="search" @submit.prevent="search">
          <AppIcon name="search" :size="16" /><input ref="searchInput" v-model="query" type="search" aria-label="搜索文章" placeholder="搜索文章" maxlength="100" />
          <button v-if="query" type="submit" aria-label="提交搜索"><AppIcon name="arrow" :size="16" /></button><kbd v-else aria-hidden="true">/</kbd>
        </form>
      </div>
    </header>
    <div v-if="error" class="site-error" role="alert">{{ error }} <button @click="load">重新连接</button></div>
    <main id="main-content" class="public-main" tabindex="-1">
      <router-view v-slot="{ Component, route: pageRoute }"><transition name="page" mode="out-in"><component :is="Component" :key="pageRoute.path" /></transition></router-view>
    </main>
    <footer class="public-footer">
      <div><span class="footer-signature">Airuike<span class="signal-dot" /></span><small>© {{ new Date().getFullYear() }}</small></div>
      <div><router-link to="/archives">文章归档</router-link><a href="#main-content">回到顶部<AppIcon name="arrow-up" :size="14" /></a></div>
    </footer>
  </div>
</template>

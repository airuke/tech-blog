<script setup>
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'
import { useSite } from '@/stores/site'
import BrandMark from '@/components/BrandMark.vue'
import AppIcon from '@/components/AppIcon.vue'
const route = useRoute(),
  router = useRouter(),
  auth = useAuth(),
  menu = ref(false), panel = ref(null), menuButton = ref(null)
const media = window.matchMedia('(max-width: 840px)'), compact = ref(media.matches)
let originalOverflow = ''
function resize(event) { compact.value = event.matches; if (!event.matches) menu.value = false }
function keyboard(event) {
  if (!menu.value || !compact.value) return
  if (event.key === 'Escape') { menu.value = false; return }
  if (event.key !== 'Tab') return
  const targets = [...panel.value.querySelectorAll('a,button:not(:disabled)')]
  const first = targets[0], last = targets.at(-1)
  if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus() }
  else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus() }
}
watch(menu, async (open) => {
  if (open && compact.value) {
    originalOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    await nextTick(); panel.value?.querySelector('a')?.focus()
  } else {
    document.body.style.overflow = originalOverflow
    if (compact.value) menuButton.value?.focus()
  }
})
const groups = [
  {
    title: '内容管理',
    items: [
      { name: 'dashboard', label: '概览', icon: 'grid' },
      { name: 'admin-articles', label: '全部文章', icon: 'book' },
      { name: 'admin-drafts', label: '草稿箱', icon: 'edit' },
      { name: 'admin-private', label: '私人知识库', icon: 'lock' },
      { name: 'admin-favorites', label: '收藏文章', icon: 'star' },
      { name: 'admin-comments', label: '评论管理', icon: 'comment' },
    ],
  },
  {
    title: '分类与配置',
    items: [
      { name: 'admin-categories', label: '分类管理', icon: 'folder' },
      { name: 'admin-tags', label: '标签管理', icon: 'tag' },
      { name: 'admin-files', label: '图片与附件', icon: 'image' },
      { name: 'settings', label: '站点设置', icon: 'settings' },
    ],
  },
]
async function logout() {
  await router.push('/login')
  if (router.currentRoute.value.name === 'login') auth.logout()
}
watch(
  () => route.fullPath,
  () => {
    menu.value = false
  },
)
onMounted(() => {
  media.addEventListener('change', resize); window.addEventListener('keydown', keyboard)
  useSite()
    .refresh()
    .catch(() => {})
})
onBeforeUnmount(() => { media.removeEventListener('change', resize); window.removeEventListener('keydown', keyboard); if (menu.value) document.body.style.overflow = originalOverflow })
</script>
<template>
  <div class="admin-app">
    <a class="skip-link" href="#admin-content">跳到管理内容</a
    ><button
      v-if="menu"
      class="admin-overlay"
      aria-label="收起管理导航"
      @click="menu = false"
    />
    <aside ref="panel" id="admin-navigation" class="admin-sidebar" :class="{ opened: menu }" :inert="compact && !menu">
      <BrandMark small />
      <div class="admin-space-caption">CONTENT STUDIO</div>
      <router-link class="button primary new-article" to="/admin/editor">
        <AppIcon name="plus" :size="18" />新建文章
      </router-link>
      <nav aria-label="管理导航">
        <section v-for="group in groups" :key="group.title">
          <p>{{ group.title }}</p>
          <router-link
            v-for="item in group.items"
            :key="item.name"
            :to="{ name: item.name }"
          >
            <AppIcon :name="item.icon" :size="19" />{{ item.label }}
          </router-link>
        </section>
      </nav>
      <router-link class="sidebar-public" to="/" target="_blank">
        查看公开博客<AppIcon name="arrow" :size="17" />
      </router-link>
    </aside>
    <div class="admin-workspace">
      <header class="admin-topbar">
        <div>
          <button
            ref="menuButton"
            class="icon-button mobile-menu"
            :aria-expanded="menu"
            aria-controls="admin-navigation"
            aria-label="打开管理导航"
            @click="menu = !menu"
          >
            <AppIcon name="menu" /></button
          ><span>管理台</span><AppIcon name="chevron" :size="14" /><strong>{{
            route.meta.title
          }}</strong>
        </div>
        <div class="admin-user">
          <span class="user-avatar">{{
            auth.user?.nickname?.slice(0, 1) || 'A'
          }}</span
          ><span>{{ auth.user?.nickname || auth.user?.username }}</span
          ><button class="icon-button" aria-label="退出登录" @click="logout">
            <AppIcon name="logout" :size="18" />
          </button>
        </div>
      </header>
      <main id="admin-content" class="admin-content">
        <router-view
          :key="route.name === 'editor' ? 'editor' : route.fullPath"
        />
      </main>
    </div>
  </div>
</template>

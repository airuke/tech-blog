import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '@/stores/auth'
const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/PublicLayout.vue'),
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/public/HomeView.vue'),
          meta: { title: '技术博客' },
        },
        {
          path: 'articles/:id',
          name: 'article',
          component: () => import('@/views/public/ArticleView.vue'),
          meta: { title: '技术博客' },
        },
        {
          path: 'categories',
          name: 'categories',
          component: () => import('@/views/public/CollectionsView.vue'),
          props: { kind: 'categories' },
          meta: { title: '分类' },
        },
        {
          path: 'tags',
          name: 'tags',
          component: () => import('@/views/public/CollectionsView.vue'),
          props: { kind: 'tags' },
          meta: { title: '标签' },
        },
        {
          path: 'archives',
          name: 'archives',
          component: () => import('@/views/public/ArchivesView.vue'),
          meta: { title: '归档' },
        },
        {
          path: 'about',
          name: 'about',
          component: () => import('@/views/public/AboutView.vue'),
          meta: { title: '关于' },
        },
      ],
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { title: '博主管理' },
    },
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { admin: true },
      children: [
        {
          path: '',
          name: 'dashboard',
          component: () => import('@/views/admin/DashboardView.vue'),
          meta: { title: '概览' },
        },
        {
          path: 'comments',
          name: 'admin-comments',
          component: () => import('@/views/admin/CommentsView.vue'),
          meta: { title: '评论管理' },
        },
        ...['articles', 'drafts', 'private', 'favorites'].map((scope) => ({
          path: scope,
          name: `admin-${scope}`,
          component: () => import('@/views/admin/ArticlesView.vue'),
          props: { scope },
          meta: {
            title: {
              articles: '文章管理',
              drafts: '草稿箱',
              private: '私人知识库',
              favorites: '收藏',
            }[scope],
          },
        })),
        {
          path: 'editor/:id?',
          name: 'editor',
          component: () => import('@/views/admin/EditorView.vue'),
          meta: { title: '写文章' },
        },
        ...['categories', 'tags'].map((kind) => ({
          path: kind,
          name: `admin-${kind}`,
          component: () => import('@/views/admin/TaxonomyView.vue'),
          props: { kind },
          meta: { title: kind === 'categories' ? '分类管理' : '标签管理' },
        })),
        {
          path: 'files',
          name: 'admin-files',
          component: () => import('@/views/admin/ResourcesView.vue'),
          meta: { title: '资源库' },
        },
        {
          path: 'settings',
          name: 'settings',
          component: () => import('@/views/admin/SettingsView.vue'),
          meta: { title: '站点设置' },
        },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})
router.beforeEach(async (to) => {
  if (to.meta.admin) {
    try {
      if (!(await useAuth().initialize()))
        return { name: 'login', query: { redirect: to.fullPath } }
    } catch {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
  }
})
router.afterEach((to) => {
  document.title = `${to.meta.title || '技术博客'} · Airuike`
})
export default router

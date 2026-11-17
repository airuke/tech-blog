<script setup>
import { ref, computed, onMounted } from 'vue'
import { api } from '@/api'
import { useAuth } from '@/stores/auth'
import { date } from '@/utils/format'
import AppIcon from '@/components/AppIcon.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'
const auth = useAuth(),
  stats = ref(null),
  recent = ref([]),
  loading = ref(true),
  error = ref('')
const cards = computed(() =>
  stats.value
    ? [
        {
          name: '全部文章',
          value: stats.value.counts.total,
          note: '文章与技术笔记',
          icon: 'book',
          to: '/admin/articles',
        },
        {
          name: '已公开',
          value: stats.value.counts.published,
          note: '发布后访客可读',
          icon: 'globe',
          to: '/admin/articles?status=PUBLISHED&visibility=PUBLIC',
        },
        {
          name: '草稿',
          value: stats.value.counts.drafts,
          note: '待完成的内容',
          icon: 'edit',
          to: '/admin/drafts',
        },
        {
          name: '私人知识',
          value:
            stats.value.counts.privateArticles ??
            stats.value.counts.privatearticles,
          note: '仅博主可见',
          icon: 'lock',
          to: '/admin/private',
        },
      ]
    : [],
)
async function load() {
  loading.value = true
  error.value = ''
  try {
    const [s, articles] = await Promise.all([
      api.stats(),
      api.articles({ pageSize: 5 }),
    ])
    stats.value = s
    recent.value = articles.items
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>
<template>
  <div class="dashboard">
    <header class="admin-page-heading">
      <div>
        <p class="eyebrow">WORKSPACE / OVERVIEW</p>
        <h1>工作台概览</h1>
        <p>{{ auth.user?.nickname || auth.user?.username }} 的内容工作台</p>
      </div>
      <router-link class="button primary" to="/admin/editor">
        <AppIcon name="plus" :size="18" />新建文章
      </router-link>
    </header>
    <LoadingState v-if="loading" /><EmptyState
      v-else-if="error"
      title="概览暂时无法读取"
      :text="error"
      error
    >
      <button class="button secondary" @click="load">
        重新读取
      </button> </EmptyState
    ><template v-else>
      <div class="stat-grid">
        <router-link
          v-for="card in cards"
          :key="card.name"
          :to="card.to"
          class="stat-tile"
        >
          <div>
            <span>{{ card.name }}</span
            ><AppIcon :name="card.icon" />
          </div>
          <strong>{{ card.value }}</strong>
          <p>{{ card.note }}</p>
        </router-link>
      </div>
      <div class="dashboard-columns">
        <section class="surface">
          <header class="surface-header">
            <div>
              <h2>最近修改</h2>
              <p>最近编辑的文章、草稿与私人笔记。</p>
            </div>
            <router-link class="text-link" to="/admin/articles">
              全部文章<AppIcon name="arrow" :size="17" />
            </router-link>
          </header>
          <div v-if="recent.length" class="recent-list">
            <router-link
              v-for="article in recent"
              :key="article.id"
              :to="{ name: 'editor', params: { id: article.id } }"
            >
              <span class="recent-icon"
                ><AppIcon
                  :name="article.visibility === 'PRIVATE' ? 'lock' : 'file'"
              /></span>
              <div>
                <strong>{{ article.title }}</strong
                ><span
                  >{{ article.category?.name || '未分类' }} ·
                  {{ date(article.updatedAt) }}</span
                >
              </div>
              <span class="badge" :class="article.status.toLowerCase()">{{
                article.status === 'DRAFT'
                  ? '草稿'
                  : article.visibility === 'PRIVATE'
                    ? '私人'
                    : '已公开'
              }}</span
              ><AppIcon name="chevron" :size="17" />
            </router-link>
          </div>
          <EmptyState
            v-else
            title="还没有创建文章"
            text="先创建技术分类，再记录代码实现、问题分析或实验结果。"
          >
            <router-link class="button primary" to="/admin/editor">
              新建文章<AppIcon name="arrow" :size="17" />
            </router-link>
          </EmptyState>
        </section>
        <aside class="dashboard-side">
          <section class="writing-tip dashboard-shortcuts">
            <p class="eyebrow">QUICK ACCESS</p><h2>继续工作</h2>
            <router-link to="/admin/drafts"><AppIcon name="edit" :size="17" />编辑草稿<AppIcon name="arrow-up-right" :size="16" /></router-link>
            <router-link to="/admin/comments"><AppIcon name="comment" :size="17" />审核评论<AppIcon name="arrow-up-right" :size="16" /></router-link>
            <router-link to="/admin/private"><AppIcon name="lock" :size="17" />私人知识库<AppIcon name="arrow-up-right" :size="16" /></router-link>
          </section>
          <section class="surface cache-status">
            <h2>运行状态</h2>
            <div>
              <span>Redis 缓存</span
              ><span
                class="badge"
                :class="stats.cache.available ? 'published' : 'draft'"
                >{{
                  stats.cache.available
                    ? '已连接'
                    : stats.cache.enabled
                      ? '回源数据库'
                      : '未启用'
                }}</span
              >
            </div>
            <div>
              <span>本次运行命中 / 回源</span
              ><strong
                >{{ stats.cache.hits }} / {{ stats.cache.misses }}</strong
              >
            </div>
            <div>
              <span>公开文章阅读次数</span
              ><strong>{{ stats.counts.views }}</strong>
            </div>
            <div>
              <span>资源文件</span><strong>{{ stats.resources }}</strong>
            </div>
            <p>内容修改后同步失效公开缓存；缓存暂不可用时仍可正常阅读。</p>
          </section>
        </aside>
      </div>
    </template>
  </div>
</template>

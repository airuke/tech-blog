<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api'
import { date } from '@/utils/format'
import AppIcon from '@/components/AppIcon.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'
const props = defineProps({ scope: { type: String, required: true } }),
  route = useRoute(),
  router = useRouter()
const items = ref([]),
  categories = ref([]),
  total = ref(0),
  loading = ref(true),
  error = ref(''),
  keyword = ref(''),
  categoryId = ref(''),
  busy = ref('')
const page = computed(() => Math.max(1, Number(route.query.page) || 1)),
  title = computed(
    () =>
      ({
        articles: '文章管理',
        drafts: '草稿箱',
        private: '私人知识库',
        favorites: '收藏文章',
      })[props.scope],
  ),
  subtitle = computed(
    () =>
      ({
        articles: '统一整理文章、草稿与私人笔记，管理每一份记录的可见范围。',
        drafts: '保存未完成的内容；草稿不会出现在公开博客中。',
        private: '私有文章仅博主登录后可见，发布状态也不会将它公开。',
        favorites: '收藏经常查阅的文章与技术笔记。',
      })[props.scope],
  )
async function load() {
  loading.value = true
  error.value = ''
  try {
    const [result, c] = await Promise.all([
      api.articles({
        page: page.value,
        pageSize: 12,
        keyword: route.query.q,
        categoryId: route.query.categoryId,
        status: props.scope === 'drafts' ? 'DRAFT' : route.query.status,
        visibility:
          props.scope === 'private' ? 'PRIVATE' : route.query.visibility,
        favorite: props.scope === 'favorites' ? true : undefined,
      }),
      api.taxonomy('categories', true),
    ])
    items.value = result.items
    total.value = result.total
    categories.value = c
    keyword.value = route.query.q || ''
    categoryId.value = route.query.categoryId || ''
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
function search() {
  router.push({
    path: route.path,
    query: {
      ...route.query,
      q: keyword.value.trim() || undefined,
      categoryId: categoryId.value || undefined,
      page: 1,
    },
  })
}
async function favorite(article) {
  busy.value = article.id
  try {
    await api.favorite(article.id, {
      isFavorite: !article.isFavorite,
      version: article.version,
    })
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    busy.value = ''
  }
}
async function publish(article) {
  busy.value = article.id
  try {
    await api.publication(article.id, {
      status: article.status === 'DRAFT' ? 'PUBLISHED' : 'DRAFT',
      version: article.version,
    })
    ElMessage.success(
      article.status === 'DRAFT'
        ? article.visibility === 'PRIVATE'
          ? '已保存到私人知识库'
          : '文章已公开'
        : '已撤回为草稿',
    )
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    busy.value = ''
  }
}
async function remove(article) {
  try {
    await ElMessageBox.confirm(
      `删除「${article.title}」后无法恢复，图片和附件仍保留在资源库。`,
      '删除文章',
      {
        confirmButtonText: '删除文章',
        cancelButtonText: '保留文章',
        type: 'warning',
      },
    )
    await api.deleteArticle(article.id, article.version)
    ElMessage.success('文章已删除')
    await load()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(e.message)
  }
}
watch(() => [props.scope, route.fullPath], load, { immediate: true })
</script>
<template>
  <section>
    <header class="admin-page-heading">
      <div>
        <p class="eyebrow">
          {{
            scope === 'private'
              ? 'YOUR PRIVATE KNOWLEDGE'
              : 'YOUR COLLECTION OF WORDS'
          }}
        </p>
        <h1>{{ title }}</h1>
        <p>{{ subtitle }}</p>
      </div>
      <router-link
        class="button primary"
        :to="{
          name: 'editor',
          query: scope === 'private' ? { visibility: 'PRIVATE' } : {},
        }"
      >
        <AppIcon name="plus" :size="18" />{{
          scope === 'private' ? '新建私人笔记' : '写一篇文章'
        }}
      </router-link>
    </header>
    <section class="surface article-manager">
      <form class="manager-toolbar" @submit.prevent="search">
        <div class="search-control">
          <AppIcon name="search" :size="18" /><input
            v-model="keyword"
            placeholder="搜索标题或摘要"
            aria-label="搜索管理文章"
            maxlength="100"
          />
        </div>
        <el-select
          v-model="categoryId"
          placeholder="全部分类"
          clearable
          aria-label="按分类筛选"
          @change="search"
        >
          <el-option
            v-for="category in categories"
            :key="category.id"
            :label="category.name"
            :value="category.id"
          /> </el-select
        ><button class="button secondary" type="submit">搜索</button
        ><span>{{ total }} 篇记录</span
        ><button
          class="icon-button"
          type="button"
          aria-label="刷新文章"
          @click="load"
        >
          <AppIcon name="refresh" />
        </button>
      </form>
      <LoadingState v-if="loading" /><EmptyState
        v-else-if="error"
        title="暂时无法读取文章"
        :text="error"
        error
      >
        <button class="button secondary" @click="load">
          重新读取
        </button> </EmptyState
      ><EmptyState
        v-else-if="!items.length"
        :icon="
          scope === 'private' ? 'lock' : scope === 'favorites' ? 'star' : 'edit'
        "
        :title="
          route.query.q
            ? '没有找到匹配的记录'
            : scope === 'favorites'
              ? '暂无收藏文章'
              : '暂无文章'
        "
        text=""
      >
        <router-link
          class="button primary"
          :to="{
            name: 'editor',
            query: scope === 'private' ? { visibility: 'PRIVATE' } : {},
          }"
        >
          新建文章<AppIcon name="arrow" :size="17" />
        </router-link>
      </EmptyState>
      <div v-else class="admin-article-list">
        <article
          v-for="article in items"
          :key="article.id"
          class="admin-article-row"
        >
          <div class="admin-article-main">
            <div class="article-row-tags">
              <span class="badge" :class="article.status.toLowerCase()">{{
                article.status === 'DRAFT'
                  ? '草稿'
                  : article.visibility === 'PRIVATE'
                    ? '私人笔记'
                    : '已公开'
              }}</span
              ><span
                v-if="article.visibility === 'PRIVATE'"
                class="private-indicator"
                ><AppIcon name="lock" :size="13" />仅自己可见</span
              ><span v-if="article.isTop" class="badge top">置顶</span>
            </div>
            <h2>
              <router-link :to="{ name: 'editor', params: { id: article.id } }">
                {{ article.title }}
              </router-link>
            </h2>
            <p>{{ article.summary || '这篇文章还没有摘要。' }}</p>
            <div class="article-row-meta">
              <span>{{ article.category?.name || '未分类' }}</span
              ><span>{{ article.wordCount }} 字</span
              ><span>修改于 {{ date(article.updatedAt) }}</span>
            </div>
          </div>
          <div class="article-row-actions">
            <button
              class="icon-button"
              :class="{ starred: article.isFavorite }"
              :aria-label="article.isFavorite ? '取消收藏' : '收藏文章'"
              :aria-pressed="article.isFavorite"
              :disabled="busy === article.id"
              @click="favorite(article)"
            >
              <AppIcon name="star" :size="19" /></button
            ><router-link
              class="button secondary small"
              :to="{ name: 'editor', params: { id: article.id } }"
            >
              编辑 </router-link
            ><el-dropdown trigger="click">
              <button
                class="icon-button"
                :aria-label="`${article.title}更多操作`"
              >
                •••</button
              ><template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    :disabled="busy === article.id"
                    @click="publish(article)"
                  >
                    {{
                      article.status === 'PUBLISHED'
                        ? '撤回为草稿'
                        : article.visibility === 'PRIVATE'
                          ? '保存到知识库'
                          : '发布文章'
                    }} </el-dropdown-item
                  ><el-dropdown-item
                    v-if="
                      article.visibility === 'PUBLIC' &&
                      article.status === 'PUBLISHED'
                    "
                  >
                    <router-link
                      :to="{ name: 'article', params: { id: article.id } }"
                      target="_blank"
                    >
                      查看公开文章
                    </router-link> </el-dropdown-item
                  ><el-dropdown-item divided @click="remove(article)">
                    删除文章
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </article>
      </div>
      <nav
        v-if="Number(total) > 12"
        class="pagination"
        aria-label="管理文章分页"
      >
        <button
          :disabled="page === 1"
          @click="router.push({ query: { ...route.query, page: page - 1 } })"
        >
          上一页</button
        ><span>{{ page }} / {{ Math.ceil(total / 12) }}</span
        ><button
          :disabled="page * 12 >= total"
          @click="router.push({ query: { ...route.query, page: page + 1 } })"
        >
          下一页
        </button>
      </nav>
    </section>
  </section>
</template>

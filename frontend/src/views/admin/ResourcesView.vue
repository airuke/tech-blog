<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api'
import { date, bytes } from '@/utils/format'
import AppIcon from '@/components/AppIcon.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'
const items = ref([]),
  loading = ref(true),
  error = ref(''),
  total = ref(0),
  page = ref(1),
  keyword = ref(''),
  input = ref(null),
  uploading = ref(false)
async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await api.files({
      page: page.value,
      pageSize: 24,
      keyword: keyword.value,
    })
    items.value = result.items
    total.value = result.total
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
async function upload(event) {
  const files = Array.from(event.target.files || [])
  event.target.value = ''
  if (!files.length) return
  uploading.value = true
  error.value = ''
  let count = 0
  try {
    for (const file of files) {
      await api.upload(file)
      count++
    }
    ElMessage.success(`已保存 ${count} 个资源`)
    page.value = 1
    await load()
  } catch (e) {
    error.value = `${count ? `已保存 ${count} 个文件。` : ''}${e.message}`
    if (count) {
      const message = error.value
      await load()
      error.value = message
    }
  } finally {
    uploading.value = false
  }
}
function turn(value) {
  page.value = value
  load()
}
async function copy(resource) {
  try {
    await navigator.clipboard.writeText(
      resource.contentType.startsWith('image/')
        ? `![${resource.name}](${resource.url})`
        : `[${resource.name}](${resource.url})`,
    )
    ElMessage.success('Markdown 引用已复制')
  } catch {
    ElMessage.error('浏览器无法访问剪贴板，请从文件地址手动复制')
  }
}
async function visibility(resource) {
  try {
    if (!resource.isPublic)
      await ElMessageBox.confirm(
        '设为公开后，任何持有资源地址的人均可访问这个文件。',
        '公开资源',
        {
          confirmButtonText: '设为公开',
          cancelButtonText: '保留文章权限',
          type: 'warning',
        },
      )
    await api.fileVisibility(resource.id, !resource.isPublic)
    await load()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(e.message)
  }
}
async function remove(resource) {
  try {
    await ElMessageBox.confirm(
      `删除「${resource.name}」？文章仍在引用时将阻止删除。`,
      '删除资源',
      {
        confirmButtonText: '删除资源',
        cancelButtonText: '保留',
        type: 'warning',
      },
    )
    await api.deleteFile(resource.id)
    ElMessage.success('资源已删除')
    await load()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(e.message)
  }
}
function search() {
  page.value = 1
  load()
}
onMounted(load)
</script>
<template>
  <section>
    <header class="admin-page-heading">
      <div>
        <p class="eyebrow">WORKSPACE / ASSETS</p>
        <h1>资源库</h1>
        <p>管理文章使用的图片与附件。</p>
      </div>
      <button
        class="button primary"
        :disabled="uploading"
        @click="input.click()"
      >
        <AppIcon name="upload" :size="18" />{{
          uploading ? '正在上传…' : '上传文件'
        }}</button
      ><input
        ref="input"
        class="sr-only"
        type="file"
        multiple
        accept=".png,.jpg,.jpeg,.gif,.webp,.pdf,.txt,.md,.zip"
        aria-label="选择资源文件"
        @change="upload"
      />
    </header>
    <div class="resource-notice">
      <AppIcon name="lock" :size="18" />
      <p>
        资源默认跟随文章权限：公开文章引用的文件可以访客访问，其余仅博主可见。也可单独设为公开资源。
      </p>
    </div>
    <section class="surface resources-surface">
      <form class="manager-toolbar" @submit.prevent="search">
        <div class="search-control">
          <AppIcon name="search" :size="18" /><input
            v-model="keyword"
            aria-label="搜索资源"
            placeholder="搜索文件名称"
            maxlength="100"
          />
        </div>
        <button class="button secondary">搜索</button
        ><span>{{ total }} 个资源</span
        ><button
          type="button"
          class="icon-button"
          aria-label="刷新资源"
          @click="load"
        >
          <AppIcon name="refresh" />
        </button>
      </form>
      <p v-if="error && items.length" class="inline-error" role="alert">
        {{ error }}
      </p>
      <LoadingState v-if="loading" /><EmptyState
        v-else-if="error && !items.length"
        title="资源暂时无法读取"
        :text="error"
        error
      >
        <button class="button secondary" @click="load">
          重新读取
        </button> </EmptyState
      ><EmptyState
        v-else-if="!items.length"
        icon="image"
        title="暂无资源文件"
        text="支持图片、PDF、UTF-8 文本与 ZIP，单个文件最多 20 MiB。"
      >
        <button class="button primary" @click="input.click()">
          上传第一个文件
        </button>
      </EmptyState>
      <div v-else class="resource-grid">
        <article
          v-for="resource in items"
          :key="resource.id"
          class="resource-item"
        >
          <a
            class="resource-preview"
            :href="resource.previewUrl"
            target="_blank"
            rel="noopener noreferrer"
            :aria-label="`打开${resource.name}`"
            ><img
              v-if="resource.contentType.startsWith('image/')"
              :src="resource.previewUrl"
              :alt="resource.name"
              loading="lazy"
            /><AppIcon v-else name="file" :size="48" /><span
              v-if="!resource.contentType.startsWith('image/')"
              >{{ resource.name.split('.').at(-1).toUpperCase() }}</span
            ></a
          >
          <div class="resource-details">
            <h2 :title="resource.name">{{ resource.name }}</h2>
            <p>{{ bytes(resource.size) }} · {{ date(resource.createdAt) }}</p>
            <span
              class="badge"
              :class="resource.isPublic ? 'published' : 'draft'"
              >{{ resource.isPublic ? '公开资源' : '随文章权限' }}</span
            >
            <div class="resource-actions">
              <button @click="copy(resource)">
                <AppIcon name="copy" :size="15" />复制引用</button
              ><el-dropdown trigger="click">
                <button
                  class="icon-button"
                  :aria-label="`${resource.name}更多操作`"
                >
                  •••</button
                ><template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="visibility(resource)">
                      {{
                        resource.isPublic ? '跟随文章权限' : '设为公开资源'
                      }} </el-dropdown-item
                    ><el-dropdown-item divided @click="remove(resource)">
                      删除资源
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </article>
      </div>
      <nav v-if="total > 24" class="pagination" aria-label="资源分页">
        <button :disabled="page === 1" @click="turn(page - 1)">上一页</button
        ><span>{{ page }} / {{ Math.ceil(total / 24) }}</span
        ><button :disabled="page * 24 >= total" @click="turn(page + 1)">
          下一页
        </button>
      </nav>
    </section>
  </section>
</template>

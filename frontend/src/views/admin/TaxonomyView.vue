<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api'
import AppIcon from '@/components/AppIcon.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'
const props = defineProps({ kind: { type: String, required: true } }),
  items = ref([]),
  loading = ref(true),
  error = ref(''),
  dialog = ref(false),
  saving = ref(false),
  editingId = ref(null),
  formError = ref(''),
  form = reactive({ name: '', description: '', sortOrder: 0 }),
  name = computed(() => (props.kind === 'categories' ? '分类' : '标签'))
async function load() {
  loading.value = true
  error.value = ''
  try {
    items.value = await api.taxonomy(props.kind, true)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
function edit(item = null) {
  editingId.value = item?.id || null
  Object.assign(form, {
    name: item?.name || '',
    description: item?.description || '',
    sortOrder: item?.sortOrder || 0,
  })
  formError.value = ''
  dialog.value = true
}
async function save() {
  saving.value = true
  formError.value = ''
  try {
    await api.saveTaxonomy(props.kind, editingId.value, form)
    dialog.value = false
    ElMessage.success(`${name.value}已保存`)
    await load()
  } catch (e) {
    formError.value = e.message
  } finally {
    saving.value = false
  }
}
async function remove(item) {
  try {
    await ElMessageBox.confirm(
      `删除「${item.name}」？有关联文章的${name.value}需要先解除关联。`,
      `删除${name.value}`,
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '保留',
        type: 'warning',
      },
    )
    await api.deleteTaxonomy(props.kind, item.id)
    ElMessage.success('已删除')
    await load()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(e.message)
  }
}
watch(() => props.kind, load, { immediate: true })
</script>
<template>
  <section>
    <header class="admin-page-heading">
      <div>
        <p class="eyebrow">TAXONOMY MANAGEMENT</p>
        <h1>{{ name }}管理</h1>
        <p>
          {{
            kind === 'categories'
              ? '按技术领域或项目方向组织文章。'
              : '用技术栈、算法名称或实验主题关联文章。'
          }}
        </p>
      </div>
      <button class="button primary" @click="edit()">
        <AppIcon name="plus" :size="18" />新建{{ name }}
      </button>
    </header>
    <section class="surface">
      <header class="surface-header">
        <h2>
          全部{{ name }} <span class="muted">{{ items.length }}</span>
        </h2>
        <button class="icon-button" aria-label="刷新分类标签" @click="load">
          <AppIcon name="refresh" />
        </button>
      </header>
      <LoadingState v-if="loading" /><EmptyState
        v-else-if="error"
        title="暂时无法读取"
        :text="error"
        error
      >
        <button class="button secondary" @click="load">
          重新读取
        </button> </EmptyState
      ><EmptyState
        v-else-if="!items.length"
        :icon="kind === 'categories' ? 'folder' : 'tag'"
        :title="`从第一个${name}开始整理`"
        text="例如计算机基础、ROS 2、算法与控制；关联公开文章后会显示在博客中。"
      >
        <button class="button primary" @click="edit()">新建{{ name }}</button>
      </EmptyState>
      <div v-else class="taxonomy-admin-list">
        <article v-for="item in items" :key="item.id">
          <span class="taxonomy-symbol"
            ><AppIcon :name="kind === 'categories' ? 'folder' : 'tag'"
          /></span>
          <div>
            <h2>{{ item.name }}</h2>
            <p>{{ item.description || '暂无说明' }}</p>
          </div>
          <span class="taxonomy-count">{{ item.articleCount }} 篇文章</span
          ><span class="muted taxonomy-sort">排序 {{ item.sortOrder }}</span
          ><button
            class="icon-button"
            :aria-label="`编辑${item.name}`"
            @click="edit(item)"
          >
            <AppIcon name="edit" :size="18" /></button
          ><button
            class="icon-button danger"
            :aria-label="`删除${item.name}`"
            @click="remove(item)"
          >
            <AppIcon name="trash" :size="18" />
          </button>
        </article>
      </div>
    </section>
    <el-dialog
      v-model="dialog"
      :title="`${editingId ? '编辑' : '新建'}${name}`"
      width="480px"
      :close-on-click-modal="false"
    >
      <form class="dialog-form" @submit.prevent="save">
        <label for="taxonomy-name">{{ name }}名称</label
        ><el-input
          id="taxonomy-name"
          v-model="form.name"
          :placeholder="
            kind === 'categories' ? '例如：技术实践' : '例如：Spring Boot'
          "
          maxlength="40"
          required
        /><label for="taxonomy-description">说明</label
        ><el-input
          id="taxonomy-description"
          v-model="form.description"
          type="textarea"
          :rows="3"
          maxlength="240"
          placeholder="一句话描述这个主题"
        /><label for="taxonomy-sort">排列顺序</label
        ><el-input-number
          id="taxonomy-sort"
          v-model="form.sortOrder"
          :min="0"
          :max="9999"
        />
        <p class="muted">
          数字越小越靠前。未关联公开文章时，此{{ name }}仅在管理台显示。
        </p>
        <p v-if="formError" class="inline-error" role="alert">
          {{ formError }}
        </p>
        <div class="dialog-actions">
          <el-button @click="dialog = false">取消</el-button
          ><el-button type="primary" native-type="submit" :loading="saving">
            保存{{ name }}
          </el-button>
        </div>
      </form>
    </el-dialog>
  </section>
</template>

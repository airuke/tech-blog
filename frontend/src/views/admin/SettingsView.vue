<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '@/api'
import { useAuth } from '@/stores/auth'
import { useSite } from '@/stores/site'
import AppIcon from '@/components/AppIcon.vue'
import LoadingState from '@/components/LoadingState.vue'
const site = useSite(),
  auth = useAuth(),
  router = useRouter(),
  loading = ref(true),
  saving = ref(false),
  changing = ref(false),
  error = ref(''),
  passwordError = ref('')
const form = reactive({ title: '', description: '', aboutMarkdown: '' }),
  password = reactive({
    currentPassword: '',
    newPassword: '',
    confirmation: '',
  })
async function load() {
  try {
    Object.assign(form, await api.site())
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
async function save() {
  saving.value = true
  error.value = ''
  try {
    const result = await api.saveSite(form)
    site.site = result
    ElMessage.success('站点设置已保存')
  } catch (e) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}
async function changePassword() {
  passwordError.value = ''
  if (password.newPassword !== password.confirmation) {
    passwordError.value = '两次输入的新密码不一致'
    return
  }
  changing.value = true
  try {
    await api.password({
      currentPassword: password.currentPassword,
      newPassword: password.newPassword,
    })
    auth.logout()
    ElMessage.success('密码已更新，请重新登录')
    await router.replace('/login')
  } catch (e) {
    passwordError.value = e.message
  } finally {
    changing.value = false
  }
}
onMounted(load)
</script>
<template>
  <section>
    <header class="admin-page-heading">
      <div>
        <p class="eyebrow">SITE CONFIGURATION</p>
        <h1>站点设置</h1>
        <p>调整站点介绍、关于页面与登录密码。</p>
      </div>
    </header>
    <LoadingState v-if="loading" />
    <div v-else class="settings-layout">
      <section class="surface">
        <header class="surface-header">
          <div>
            <h2>站点信息</h2>
            <p>这些内容会显示在公开博客中。</p>
          </div>
          <AppIcon name="globe" />
        </header>
        <form class="settings-form" @submit.prevent="save">
          <label for="site-title">站点名称</label
          ><el-input
            id="site-title"
            v-model="form.title"
            maxlength="50"
            required
          /><label for="site-description">一句话介绍</label
          ><el-input
            id="site-description"
            v-model="form.description"
            maxlength="180"
            required
          /><label for="site-about">关于页面 · Markdown</label
          ><el-input
            id="site-about"
            v-model="form.aboutMarkdown"
            type="textarea"
            :rows="12"
            maxlength="20000"
            placeholder="介绍学习方向、技术项目，以及这个博客记录的内容。"
          />
          <p class="muted">支持 Markdown；公开页面会安全渲染，脚本不会执行。</p>
          <p v-if="error" class="inline-error" role="alert">{{ error }}</p>
          <el-button type="primary" native-type="submit" :loading="saving">
            保存站点信息
          </el-button>
        </form>
      </section>
      <section class="surface password-settings">
        <header class="surface-header">
          <div>
            <h2>登录与安全</h2>
            <p>更新自己的博主管理密码。</p>
          </div>
          <AppIcon name="lock" />
        </header>
        <form class="settings-form" @submit.prevent="changePassword">
          <label for="current-password">当前密码</label
          ><el-input
            id="current-password"
            v-model="password.currentPassword"
            type="password"
            show-password
            autocomplete="current-password"
            required
          /><label for="new-password">新密码</label
          ><el-input
            id="new-password"
            v-model="password.newPassword"
            type="password"
            show-password
            autocomplete="new-password"
            minlength="10"
            maxlength="72"
            placeholder="至少 10 个字符"
            required
          /><label for="confirm-new-password">确认新密码</label
          ><el-input
            id="confirm-new-password"
            v-model="password.confirmation"
            type="password"
            show-password
            autocomplete="new-password"
            required
          />
          <p class="security-note">
            <AppIcon
              name="lock"
              :size="16"
            />更新密码后，所有旧登录令牌将失效，需要重新登录。
          </p>
          <p v-if="passwordError" class="inline-error" role="alert">
            {{ passwordError }}
          </p>
          <el-button native-type="submit" :loading="changing">
            更新密码
          </el-button>
        </form>
      </section>
    </div>
  </section>
</template>

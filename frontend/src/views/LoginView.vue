<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/api'
import { useAuth } from '@/stores/auth'
import { useSite } from '@/stores/site'
import BrandMark from '@/components/BrandMark.vue'
import AppIcon from '@/components/AppIcon.vue'
import SignalTrace from '@/components/SignalTrace.vue'
const router = useRouter(),
  route = useRoute(),
  auth = useAuth(),
  loading = ref(true),
  saving = ref(false),
  error = ref(''),
  setup = ref(false),
  allowed = ref(false),
  username = ref(''),
  password = ref(''),
  nickname = ref(''),
  confirmation = ref('')
async function load() {
  loading.value = true
  try {
    const status = await api.status()
    setup.value = status.setupNeeded
    allowed.value = status.setupAllowed
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
async function submit() {
  error.value = ''
  if (setup.value && password.value !== confirmation.value) {
    error.value = '两次输入的密码不一致'
    return
  }
  saving.value = true
  try {
    if (setup.value)
      await api.setup({
        username: username.value.trim(),
        password: password.value,
        nickname: nickname.value.trim(),
      })
    await auth.login({
      username: username.value.trim(),
      password: password.value,
    })
    const redirect =
      typeof route.query.redirect === 'string' &&
      route.query.redirect.startsWith('/admin')
        ? route.query.redirect
        : '/admin'
    await router.replace(redirect)
  } catch (e) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}
onMounted(() => {
  load()
  useSite()
    .refresh()
    .catch(() => {})
})
</script>
<template>
  <div class="auth-page">
    <header>
      <BrandMark /><router-link class="text-link" to="/">
        回到公开博客<AppIcon name="arrow" :size="17" />
      </router-link>
    </header>
    <main class="auth-layout">
      <section class="auth-story">
        <p class="eyebrow">AIRUIKE / WORKSPACE</p>
        <h1>内容工作台<span class="heading-period">.</span></h1>
        <p>
          文章、草稿与私人笔记。
        </p>
        <div class="auth-identity" aria-hidden="true">A<span>.</span></div><SignalTrace />
        <div class="auth-features">
          <span><AppIcon name="code" :size="16" />Markdown 编辑</span
          ><span><AppIcon name="lock" :size="16" />私人知识库</span
          ><span><AppIcon name="tag" :size="16" />分类与标签</span>
        </div>
      </section>
      <section class="auth-form-panel">
        <p class="eyebrow">{{ setup ? 'INITIAL SETUP' : 'OWNER ACCESS' }}</p>
        <h2>{{ setup ? '创建你的博主账号' : '博主登录' }}</h2>
        <p>
          {{
            setup
              ? '首次设置一次，以后使用这个账号管理博客。'
              : '使用你的博主账号继续。'
          }}
        </p>
        <p v-if="error" class="inline-error" role="alert">{{ error }}</p>
        <p v-if="loading" role="status">正在连接博客…</p>
        <div v-else-if="setup && !allowed" class="inline-error">
          首次设置仅在本机开放，请在本机开发环境创建账号。
        </div>
        <form v-else @submit.prevent="submit">
          <label for="blog-username">用户名</label
          ><el-input
            id="blog-username"
            v-model="username"
            :placeholder="setup ? '3–32 位字母、数字或下划线' : '输入用户名'"
            autocomplete="username"
            maxlength="32"
            required
            pattern="[A-Za-z0-9_]{3,32}"
          />
          <template v-if="setup">
            <label for="blog-nickname">昵称（选填）</label
            ><el-input
              id="blog-nickname"
              v-model="nickname"
              placeholder="不填时使用用户名"
              maxlength="40"
            />
          </template>
          <label for="blog-password">{{ setup ? '设置密码' : '密码' }}</label
          ><el-input
            id="blog-password"
            v-model="password"
            :placeholder="setup ? '至少 10 个字符' : '请输入密码'"
            type="password"
            show-password
            :autocomplete="setup ? 'new-password' : 'current-password'"
            :minlength="setup ? 10 : undefined"
            maxlength="72"
            required
          />
          <template v-if="setup">
            <label for="blog-confirmation">确认密码</label
            ><el-input
              id="blog-confirmation"
              v-model="confirmation"
              placeholder="再输入一次密码"
              type="password"
              show-password
              autocomplete="new-password"
              required
            />
          </template>
          <el-button type="primary" native-type="submit" :loading="saving">
            {{ setup ? '创建账号，进入管理台' : '登录管理台'
            }}<AppIcon name="arrow" :size="18" />
          </el-button>
        </form>
        <p class="auth-footnote">
          <AppIcon
            name="lock"
            :size="14"
          />此入口仅供博主使用。
        </p>
      </section>
    </main>
    <footer>Airuike / 博主管理</footer>
  </div>
</template>

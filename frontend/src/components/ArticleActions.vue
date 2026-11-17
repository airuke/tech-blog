<script setup>
import { ref, onBeforeUnmount } from 'vue'
import AppIcon from './AppIcon.vue'
const copied = ref(false), message = ref('')
let timer
async function copy() {
  try {
    await navigator.clipboard.writeText(window.location.href)
    copied.value = true; message.value = '文章链接已复制'
    clearTimeout(timer); timer = setTimeout(() => { copied.value = false; message.value = '' }, 2200)
  } catch { message.value = '未能复制，请从地址栏复制链接' }
}
onBeforeUnmount(() => clearTimeout(timer))
</script>
<template>
  <div class="article-actions">
    <router-link class="back-link" to="/"><AppIcon name="arrow" :size="16" />全部文章</router-link>
    <div><button class="article-share" @click="copy"><AppIcon :name="copied ? 'check' : 'copy'" :size="15" />{{ copied ? '已复制' : '复制链接' }}</button><span class="sr-only" role="status">{{ message }}</span></div>
  </div>
</template>

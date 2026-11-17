<script setup>
import { useSite } from '@/stores/site'
const site = useSite()
defineProps({ small: Boolean })
</script>
<template>
  <router-link class="brand" :class="{ 'brand-small': small }" to="/" aria-label="返回博客首页">
    <span class="brand-symbol"><svg viewBox="0 0 32 32" fill="none" aria-hidden="true">
      <path d="M7 24 15.5 7h3L25 24M11 17h11M7 24h6m9 0h4" stroke="currentColor" stroke-width="2" stroke-linecap="square" />
      <path d="M4 4h5M4 4v5m24 14v5h-5" stroke="currentColor" stroke-width="1" opacity=".5" />
    </svg></span>
    <span class="brand-type"><strong>{{ site.site.title }}</strong><small>ENGINEERING NOTES</small></span>
  </router-link>
</template>

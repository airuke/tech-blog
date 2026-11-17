<script setup>
import { useSite } from '@/stores/site'
import MarkdownContent from '@/components/MarkdownContent.vue'
const site = useSite()
</script>
<template>
  <section class="about-page">
    <header class="page-intro">
      <p class="eyebrow">ABOUT / AIRUIKE</p>
      <h1>Airuike<span class="heading-period">.</span></h1>
      <p>{{ site.site.description }}</p>
    </header>
    <div class="about-content">
      <span class="author-monogram large" aria-hidden="true">A.</span
      ><MarkdownContent :content="site.site.aboutMarkdown" />
    </div>
  </section>
</template>

<script setup>
import AppIcon from './AppIcon.vue'
defineProps({
  title: { type: String, default: '' },
  text: { type: String, default: '' },
  icon: { type: String, default: 'book' },
  error: Boolean,
})
</script>
<template>
  <div class="empty-state" :role="error ? 'alert' : 'status'">
    <span class="empty-symbol"><AppIcon :name="icon" :size="32" /></span>
    <h2>{{ title }}</h2>
    <p v-if="text">{{ text }}</p>
    <slot />
  </div>
</template>

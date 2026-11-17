<script setup>
const trace = Array.from({ length: 110 }, (_, index) => {
  const t = index / 14
  return `${index ? 'L' : 'M'}${(20 + index * 2.3).toFixed(2)},${(103 - 53 * (1 - Math.exp(-t * .7) * Math.cos(t * 1.8))).toFixed(2)}`
}).join(' ')
</script>
<template>
  <svg class="signal-trace" viewBox="0 0 300 132" fill="none" aria-hidden="true">
    <path d="M20 14v100h260 M20 50h260 M82 14v100 M145 14v100 M208 14v100 M271 14v100" class="trace-grid" />
    <path d="M20 50h260" class="trace-target" /><path :d="trace" class="trace-path" pathLength="1" />
    <circle cx="270.7" cy="49.8" r="3.5" class="trace-end" /><path d="M12 114h16m-8-8v16 M273 6h14m-7-7v14" class="trace-cross" />
  </svg>
</template>

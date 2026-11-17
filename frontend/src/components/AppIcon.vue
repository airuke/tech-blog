<script setup>
defineProps({
  name: { type: String, default: 'book' },
  size: { type: Number, default: 20 },
})
const paths = {
  comment: 'M4 4h16v12H9l-5 4z M8 8h8 M8 12h5',
  code: 'm8 6-6 6 6 6m8-12 6 6-6 6 M14 3l-4 18',
  robot:
    'M5 7h14v12H5z M12 3v4 M3 10v6m18-6v6 M8 11h.01M16 11h.01 M9 15h6 M8 19v2m8-2v2',
  control: 'M3 6h18M3 12h18M3 18h18 M8 3v6m8 0v6m-6 0v6',
  nodes: 'M4 4h5v5H4z M15 4h5v5h-5z M10 16h5v5h-5z M6.5 9v4h11V9 M12.5 13v3',
  book: 'M4 3h7c2 0 3 1 3 3v15c0-2-1-3-3-3H4z M14 6c0-2 1-3 3-3h3v15h-3c-2 0-3 1-3 3',
  arrow: 'M5 12h14m-6-6 6 6-6 6',
  'arrow-up-right': 'M5 19 19 5M5 5h14v14',
  'arrow-up': 'M12 20V4m-6 6 6-6 6 6',
  search: 'M10.5 18a7.5 7.5 0 1 0 0-15 7.5 7.5 0 0 0 0 15m5.5-2 5 5',
  plus: 'M12 5v14M5 12h14',
  edit: 'm16 3 5 5-12 12-6 1 1-6z M13 6l5 5',
  grid: 'M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z',
  tag: 'M3 3h8l10 10-8 8L3 11z M7 7h.01',
  lock: 'M5 10h14v11H5z M8 10V6a4 4 0 0 1 8 0v4 M12 14v3',
  star: 'm12 3 3 6 7 1-5 5 1 7-6-3-6 3 1-7-5-5 7-1z',
  archive: 'M3 3h18v5H3z M5 8v13h14V8 M9 12h6',
  image: 'M3 3h18v18H3z m0 14 6-6 4 4 3-3 5 5 M16 7h.01',
  file: 'M5 3h9l5 5v13H5z M14 3v6h5 M8 13h8 M8 17h6',
  upload: 'M12 15V3m-5 5 5-5 5 5 M4 15v6h16v-6',
  settings:
    'M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8 M12 3v2m0 14v2M3 12h2m14 0h2M5.6 5.6 7 7m10 10 1.4 1.4M5.6 18.4 7 17M17 7l1.4-1.4',
  logout: 'M10 4H4v16h6 M9 12h12m-4-4 4 4-4 4',
  close: 'm5 5 14 14 M19 5 5 19',
  check: 'm4 12 5 5L20 6',
  trash: 'M3 6h18 M9 6V3h6v3 M5 6l1 15h12l1-15 M10 10v7m4-7v7',
  clock: 'M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18 M12 7v5l4 2',
  globe:
    'M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18 M3 12h18 M12 3c5 5 5 13 0 18-5-5-5-13 0-18',
  menu: 'M4 6h16M4 12h16M4 18h16',
  chevron: 'm9 5 7 7-7 7',
  refresh: 'M20 7a9 9 0 1 0 1 8 M20 3v5h-5',
  folder: 'M3 5h6l2 3h10v12H3z',
  eye: 'M2 12s4-7 10-7 10 7 10 7-4 7-10 7-10-7-10-7 M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6',
  copy: 'M8 8h13v13H8z M16 8V3H3v13h5',
}
</script>
<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.65"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <path :d="paths[name] || paths.book" />
  </svg>
</template>

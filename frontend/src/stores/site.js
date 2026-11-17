import { ref } from 'vue'
import { defineStore } from 'pinia'
import { api } from '@/api'
export const useSite = defineStore('blog-site', () => {
  const site = ref({
      title: 'Airuike 技术博客',
      description:
        '计算机科学与技术 · ROS 2 · 算法与控制。记录实现过程、实验结果与技术复盘。',
      aboutMarkdown: '',
    }),
    categories = ref([]),
    tags = ref([])
  async function refresh() {
    const [info, c, t] = await Promise.all([
      api.site(),
      api.taxonomy('categories'),
      api.taxonomy('tags'),
    ])
    site.value = info
    categories.value = c
    tags.value = t
  }
  return { site, categories, tags, refresh }
})

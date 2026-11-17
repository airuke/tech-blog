import { ref } from 'vue'
import { defineStore } from 'pinia'
import { api, getToken, tokenKey } from '@/api'
export const useAuth = defineStore('blog-auth', () => {
  const user = ref(null),
    checked = ref(false)
  function logout() {
    localStorage.removeItem(tokenKey)
    user.value = null
    checked.value = true
  }
  async function initialize() {
    if (checked.value && user.value) return true
    if (!getToken()) {
      checked.value = true
      return false
    }
    try {
      user.value = await api.me()
      checked.value = true
      return true
    } catch (e) {
      if (e.status === 401 || e.status === 403) logout()
      throw e
    }
  }
  async function login(body) {
    const data = await api.login(body)
    localStorage.setItem(tokenKey, data.accessToken)
    user.value = data.user
    checked.value = true
  }
  return { user, initialize, login, logout }
})

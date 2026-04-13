import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { loginUser, registerUser } from '@/api/modules/users'
import type { UserSessionView } from '@/types/user'

const STORAGE_KEY = 'drug-research-user-session'

export const useUserStore = defineStore('user', () => {
  const session = ref<UserSessionView | null>(loadSession())

  const isAuthenticated = computed(() => Boolean(session.value?.token))
  const username = computed(() => session.value?.username ?? 'Guest')
  const role = computed(() => session.value?.role ?? '')
  const isAdmin = computed(() => session.value?.role === 'ADMIN')

  async function login(email: string, password: string) {
    const result = await loginUser({ email, password })
    session.value = result
    localStorage.setItem(STORAGE_KEY, JSON.stringify(result))
    return result
  }

  async function register(username: string, email: string, password: string) {
    const result = await registerUser({ username, email, password })
    session.value = result
    localStorage.setItem(STORAGE_KEY, JSON.stringify(result))
    return result
  }

  function logout() {
    session.value = null
    localStorage.removeItem(STORAGE_KEY)
  }

  function loadSession(): UserSessionView | null {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) {
      return null
    }
    try {
      return JSON.parse(raw) as UserSessionView
    } catch {
      localStorage.removeItem(STORAGE_KEY)
      return null
    }
  }

  return {
    session,
    isAuthenticated,
    username,
    role,
    isAdmin,
    login,
    register,
    logout
  }
})

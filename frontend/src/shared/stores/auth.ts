import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin } from '@shared/api/auth'
import { UserRole } from '@shared/types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  const role = ref(localStorage.getItem('role') || '')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === UserRole.ADMIN)
  const isDispatcher = computed(() => role.value === UserRole.DISPATCHER)
  const isFieldworker = computed(() => role.value === UserRole.FIELDWORKER)

  async function login(user: string, password: string) {
    const { data } = await apiLogin(user, password)
    token.value = data.token
    username.value = data.username
    role.value = data.role
    localStorage.setItem('token', data.token)
    localStorage.setItem('username', data.username)
    localStorage.setItem('role', data.role)
  }

  function logout() {
    token.value = ''
    username.value = ''
    role.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
  }

  return { token, username, role, isLoggedIn, isAdmin, isDispatcher, isFieldworker, login, logout }
})

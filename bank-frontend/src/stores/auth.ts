import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User, Staff } from '@/types'

function readJson<T>(key: string): T | null {
  try {
    const raw = localStorage.getItem(key)
    return raw ? (JSON.parse(raw) as T) : null
  } catch {
    localStorage.removeItem(key)
    return null
  }
}

function writeJson(key: string, value: unknown) {
  if (value) localStorage.setItem(key, JSON.stringify(value))
  else localStorage.removeItem(key)
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const staffToken = ref<string | null>(localStorage.getItem('staff_token'))
  const loginType = ref<'user' | 'staff' | null>(localStorage.getItem('loginType') as any)
  const user = ref<User | null>(readJson<User>('user_info'))
  const staff = ref<Staff | null>(readJson<Staff>('staff_info'))

  const isAuthenticated = computed(() => !!token.value)
  const isStaffAuthenticated = computed(() => !!staffToken.value)
  const currentUser = computed(() => user.value)
  const currentStaff = computed(() => staff.value)

  function setAuth(authToken: string, userData: User) {
    token.value = authToken
    user.value = userData
    loginType.value = 'user'
    localStorage.setItem('token', authToken)
    localStorage.setItem('loginType', 'user')
    writeJson('user_info', userData)
  }

  function setStaffAuth(authToken: string, staffData: Staff) {
    staffToken.value = authToken
    staff.value = staffData
    loginType.value = 'staff'
    localStorage.setItem('staff_token', authToken)
    localStorage.setItem('loginType', 'staff')
    writeJson('staff_info', staffData)
  }

  function logout() {
    token.value = null
    staffToken.value = null
    loginType.value = null
    user.value = null
    staff.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('staff_token')
    localStorage.removeItem('loginType')
    localStorage.removeItem('user_info')
    localStorage.removeItem('staff_info')
  }

  function updateUser(data: Partial<User>) {
    if (user.value) {
      user.value = { ...user.value, ...data }
      writeJson('user_info', user.value)
    }
  }

  /**
   * Rehydrate refs from localStorage. The router guard calls this on every
   * navigation, so it must stay cheap and must not clobber fresh state.
   */
  function initializeAuth() {
    const savedToken = localStorage.getItem('token')
    const savedStaffToken = localStorage.getItem('staff_token')
    const savedLoginType = localStorage.getItem('loginType')

    token.value = savedToken
    staffToken.value = savedStaffToken
    loginType.value = (savedLoginType as 'user' | 'staff' | null) ?? null

    if (!user.value) user.value = readJson<User>('user_info')
    if (!staff.value) staff.value = readJson<Staff>('staff_info')
  }

  return {
    token,
    staffToken,
    loginType,
    user,
    staff,
    isAuthenticated,
    isStaffAuthenticated,
    currentUser,
    currentStaff,
    setAuth,
    setStaffAuth,
    logout,
    updateUser,
    initializeAuth,
  }
})
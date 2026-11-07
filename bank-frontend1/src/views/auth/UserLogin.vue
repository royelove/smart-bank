<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Headset } from 'lucide-vue-next'
import { Alert, Button, Input } from '@/components/ui'
import { useAuthStore } from '@/stores/auth'
import { getUserInfo, userLogin } from '@/api/user'
import type { User } from '@/types'
import AuthLayout from '@/layout/AuthLayout.vue'
import {
  errorMessage,
  fallbackUser,
  normalizePhone,
  safeRedirect,
  validatePassword,
  validatePhone,
} from './authForms'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

interface LoginForm {
  userPhone: string
  password: string
}

const form = reactive<LoginForm>({ userPhone: '', password: '' })
const errors = reactive<Record<string, string>>({})
const loading = ref(false)

const redirect = safeRedirect(route.query.redirect, '/dashboard')

/** Hint banners handed over by the register / forgot-password flows. */
const notice = computed(() => {
  if (route.query.registered === '1') return { variant: 'success' as const, text: '注册成功，请使用新账户登录' }
  if (route.query.reset === '1') return { variant: 'success' as const, text: '密码重置成功，请使用新密码登录' }
  return null
})

function validate(): boolean {
  errors.form = ''
  // Safety net for autofill that never fired `input`/`blur` (see `normalizePhone`).
  form.userPhone = normalizePhone(form.userPhone)
  errors.userPhone = validatePhone(form.userPhone)
  errors.password = validatePassword(form.password, '登录密码')
  return !errors.userPhone && !errors.password
}

/**
 * `/user/info` is a second round trip: if it fails the login itself already
 * succeeded, so fall back to a minimal profile built from the phone number
 * instead of dropping the freshly issued token.
 */
async function loadProfile(phone: string): Promise<User> {
  try {
    return (await getUserInfo()) as User
  } catch {
    return fallbackUser(phone)
  }
}

async function handleLogin(): Promise<void> {
  if (!validate()) return

  loading.value = true
  try {
    const userPhone = form.userPhone.trim()
    const token = await userLogin({ userPhone, password: form.password })
    if (!token) throw new Error('登录响应异常，请稍后重试')

    // Persist the token first so the `/user/info` round trip below carries it.
    authStore.setAuth(token as string, fallbackUser(userPhone))
    authStore.updateUser(await loadProfile(userPhone))
    await router.push(redirect)
  } catch (error) {
    errors.form = errorMessage(error, '登录失败，请检查手机号和密码')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthLayout
    title="手机号登录"
    subtitle="登录个人账户，随时查看资产与交易"
  >
    <Alert
      v-if="notice"
      :key="notice.text"
      :variant="notice.variant"
      class="mb-6"
      dismissible
    >
      {{ notice.text }}
    </Alert>

    <Alert
      v-if="errors.form"
      :key="errors.form"
      variant="danger"
      class="mb-6"
      dismissible
    >
      {{ errors.form }}
    </Alert>

    <form
      class="space-y-5"
      novalidate
      @submit.prevent="handleLogin"
    >
      <Input
        v-model="form.userPhone"
        label="手机号"
        type="tel"
        name="userPhone"
        placeholder="请输入 11 位手机号"
        autocomplete="tel-national"
        :transform="normalizePhone"
        required
        :error="errors.userPhone"
      />

      <Input
        v-model="form.password"
        label="登录密码"
        type="password"
        name="password"
        placeholder="请输入 6 位数字密码"
        autocomplete="current-password"
        required
        :show-password-toggle="true"
        :error="errors.password"
      />

      <div class="flex items-center justify-end">
        <router-link
          to="/forgot-password"
          class="text-body-sm font-medium text-primary-600 hover:text-primary-700 dark:text-primary-400 dark:hover:text-primary-300"
        >
          忘记密码？
        </router-link>
      </div>

      <Button
        type="submit"
        size="lg"
        class="w-full"
        :loading="loading"
      >
        登录
      </Button>
    </form>

    <template #footer>
      <div class="space-y-3 text-center">
        <p class="text-body-sm text-navy-500 dark:text-navy-400">
          还没有账户？
          <router-link
            to="/user/register"
            class="ml-1 font-medium text-primary-600 hover:text-primary-700 dark:text-primary-400 dark:hover:text-primary-300"
          >
            立即注册
          </router-link>
        </p>
        <p class="text-body-sm text-navy-500 dark:text-navy-400">
          <router-link
            to="/support/login"
            class="inline-flex items-center gap-1.5 font-medium text-navy-600 hover:text-navy-800 dark:text-navy-300 dark:hover:text-navy-100"
          >
            <Headset class="h-4 w-4" />
            客服人员登录
          </router-link>
        </p>
      </div>
    </template>
  </AuthLayout>
</template>

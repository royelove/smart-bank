<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ShieldAlert, UserRound } from 'lucide-vue-next'
import { Alert, Button, Input } from '@/components/ui'
import { useAuthStore } from '@/stores/auth'
import { staffLogin } from '@/api/support'
import type { Staff } from '@/types'
import AuthLayout from '@/layout/AuthLayout.vue'
import { errorMessage, safeRedirect, validatePassword } from './authForms'

/** `POST /support/login` resolves to `{ token, staffName }` (not a bare token). */
interface StaffLoginResult {
  token: string
  staffName?: string
}

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

interface StaffLoginForm {
  username: string
  password: string
}

const form = reactive<StaffLoginForm>({ username: '', password: '' })
const errors = reactive<Record<string, string>>({})
const loading = ref(false)

const redirect = safeRedirect(route.query.redirect, '/support')

const registered = computed(() => route.query.registered === '1')

function validate(): boolean {
  errors.form = ''
  errors.username = form.username.trim() ? '' : '请输入客服账号'
  errors.password = validatePassword(form.password, '登录密码')
  return !errors.username && !errors.password
}

async function handleLogin(): Promise<void> {
  if (!validate()) return

  loading.value = true
  try {
    const username = form.username.trim()
    const data = (await staffLogin({ username, password: form.password })) as StaffLoginResult
    if (!data?.token) throw new Error('登录响应异常，请稍后重试')

    // The VO only carries the display name, so the rest of the profile is empty.
    const staff: Staff = {
      id: '',
      name: data.staffName || username || '客服',
      email: '',
      role: 'staff',
    }
    authStore.setStaffAuth(data.token, staff)
    await router.push(redirect)
  } catch (error) {
    errors.form = errorMessage(error, '登录失败，请检查账号和密码')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthLayout
    title="客服后台登录"
    subtitle="仅限授权客服人员访问，操作全程留痕"
    panel-title="客服工作台，一站式处理客户诉求"
    panel-description="工单受理、智能分诊与实名信息核验集中在一个后台，让每一次客户沟通都有迹可循。"
  >
    <Alert
      v-if="registered"
      variant="success"
      class="mb-6"
      dismissible
    >
      客服账号已开通，请使用新账号登录
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
        v-model="form.username"
        label="客服账号"
        name="username"
        placeholder="请输入工号或账号名"
        autocomplete="username"
        required
        :error="errors.username"
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

      <Button
        type="submit"
        size="lg"
        class="w-full"
        :loading="loading"
      >
        登录工作台
      </Button>
    </form>

    <template #footer>
      <div class="space-y-4 text-center">
        <p class="text-body-sm text-navy-500 dark:text-navy-400">
          还没有客服账号？
          <router-link
            to="/register"
            class="ml-1 font-medium text-primary-600 hover:text-primary-700 dark:text-primary-400 dark:hover:text-primary-300"
          >
            开通客服账号
          </router-link>
        </p>
        <p class="text-body-sm text-navy-500 dark:text-navy-400">
          <router-link
            to="/user/login"
            class="inline-flex items-center gap-1.5 font-medium text-navy-600 hover:text-navy-800 dark:text-navy-300 dark:hover:text-navy-100"
          >
            <UserRound class="h-4 w-4" />
            我是个人用户，前往用户登录
          </router-link>
        </p>
        <p class="flex items-center justify-center gap-2 text-body-xs text-navy-500 dark:text-navy-400">
          <ShieldAlert class="h-4 w-4 text-primary-500" />
          操作记录将被审计，请遵守安全规范
        </p>
      </div>
    </template>
  </AuthLayout>
</template>

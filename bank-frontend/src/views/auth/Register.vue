<script setup lang="ts">
/**
 * `/register` — self-service onboarding for the customer-service console.
 *
 * Personal (retail) onboarding lives on `/user/register` and is linked below;
 * this page covers the other registration endpoint the backend exposes,
 * `POST /support/register`.
 */
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ShieldAlert } from 'lucide-vue-next'
import { Alert, Button, Input } from '@/components/ui'
import { staffRegister } from '@/api/support'
import AuthLayout from '@/layout/AuthLayout.vue'
import { errorMessage, validateConfirmPassword, validatePassword } from './authForms'

const router = useRouter()

interface StaffRegisterForm {
  username: string
  staffName: string
  password: string
  checkPassword: string
}

const form = reactive<StaffRegisterForm>({
  username: '',
  staffName: '',
  password: '',
  checkPassword: '',
})
const errors = reactive<Record<string, string>>({})
const loading = ref(false)

function validate(): boolean {
  errors.form = ''
  errors.username = form.username.trim() ? '' : '请输入客服账号'
  errors.staffName = ''
  errors.password = validatePassword(form.password, '登录密码')
  errors.checkPassword = validateConfirmPassword(form.password, form.checkPassword)
  return Object.values(errors).every((message) => !message)
}

async function handleRegister(): Promise<void> {
  if (!validate()) return

  loading.value = true
  try {
    // POST /support/register -> { username, password, checkPassword, staffName }
    await staffRegister({
      username: form.username.trim(),
      password: form.password,
      checkPassword: form.checkPassword,
      staffName: form.staffName.trim(),
    })
    await router.push({ path: '/support/login', query: { registered: '1' } })
  } catch (error) {
    errors.form = errorMessage(error, '注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthLayout
    title="开通客服账号"
    subtitle="仅限银行内部客服人员，注册后请使用该账号登录工作台"
    panel-title="客服工作台，一站式处理客户诉求"
    panel-description="工单受理、智能分诊与实名信息核验集中在一个后台，让每一次客户沟通都有迹可循。"
  >
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
      @submit.prevent="handleRegister"
    >
      <Input
        v-model="form.username"
        label="客服账号"
        name="username"
        placeholder="请输入工号或账号名"
        autocomplete="username"
        required
        :error="errors.username"
        hint="登录客服后台时使用"
      />

      <Input
        v-model="form.staffName"
        label="客服姓名"
        name="staffName"
        placeholder="请输入对外显示的姓名"
        autocomplete="name"
        :error="errors.staffName"
        hint="选填，未填写时使用客服账号"
      />

      <Input
        v-model="form.password"
        label="登录密码"
        type="password"
        name="password"
        placeholder="请输入 6 位数字密码"
        autocomplete="new-password"
        required
        :show-password-toggle="true"
        :error="errors.password"
        hint="密码为 6 位数字"
      />

      <Input
        v-model="form.checkPassword"
        label="确认密码"
        type="password"
        name="checkPassword"
        placeholder="请再次输入密码"
        autocomplete="new-password"
        required
        :show-password-toggle="true"
        :error="errors.checkPassword"
      />

      <Button
        type="submit"
        size="lg"
        class="w-full"
        :loading="loading"
      >
        开通客服账号
      </Button>
    </form>

    <template #footer>
      <div class="space-y-4 text-center">
        <p class="text-body-sm text-navy-500 dark:text-navy-400">
          已有客服账号？
          <router-link
            to="/support/login"
            class="ml-1 font-medium text-primary-600 hover:text-primary-700 dark:text-primary-400 dark:hover:text-primary-300"
          >
            前往客服登录
          </router-link>
        </p>
        <p class="text-body-sm text-navy-500 dark:text-navy-400">
          需要办理银行卡？
          <router-link
            to="/user/register"
            class="ml-1 font-medium text-primary-600 hover:text-primary-700 dark:text-primary-400 dark:hover:text-primary-300"
          >
            注册个人用户
          </router-link>
        </p>
        <p class="flex items-center justify-center gap-2 text-body-xs text-navy-500 dark:text-navy-400">
          <ShieldAlert class="h-4 w-4 text-primary-500" />
          客服操作将被审计，请勿共享账号
        </p>
      </div>
    </template>
  </AuthLayout>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ShieldCheck } from 'lucide-vue-next'
import { Alert, Button, Input } from '@/components/ui'
import { userRegister } from '@/api/user'
import AuthLayout from '@/layout/AuthLayout.vue'
import {
  errorMessage,
  validateConfirmPassword,
  validateIdCard,
  validatePassword,
  validatePhone,
  validateUserName,
  type UserRegisterForm,
} from './authForms'

const router = useRouter()

const form = reactive<UserRegisterForm>({
  userName: '',
  userPhone: '',
  userIdCard: '',
  password: '',
  checkPassword: '',
  agreeTerms: false,
})
const errors = reactive<Record<string, string>>({})
const loading = ref(false)

function validate(): boolean {
  errors.form = ''
  errors.userName = validateUserName(form.userName)
  errors.userPhone = validatePhone(form.userPhone)
  errors.userIdCard = validateIdCard(form.userIdCard)
  errors.password = validatePassword(form.password, '登录密码')
  errors.checkPassword = validateConfirmPassword(form.password, form.checkPassword)
  errors.agreeTerms = form.agreeTerms ? '' : '请阅读并同意相关协议'
  return Object.values(errors).every((message) => !message)
}

async function handleRegister(): Promise<void> {
  if (!validate()) return

  loading.value = true
  try {
    // POST /user/register -> { userPhone, userName, password, checkPassword, userIdCard }
    await userRegister({
      userPhone: form.userPhone.trim(),
      userName: form.userName.trim(),
      password: form.password,
      checkPassword: form.checkPassword,
      userIdCard: form.userIdCard.trim().toUpperCase(),
    })
    // The endpoint returns no token: send the user to the login form instead.
    await router.push({ path: '/user/login', query: { registered: '1' } })
  } catch (error) {
    errors.form = errorMessage(error, '注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthLayout
    title="注册个人账户"
    subtitle="填写基本信息，开启智慧理财之旅"
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
        v-model="form.userName"
        label="姓名"
        name="userName"
        placeholder="请输入您的姓名"
        autocomplete="name"
        required
        :error="errors.userName"
        hint="用于账户显示名称"
      />

      <Input
        v-model="form.userPhone"
        label="手机号"
        type="tel"
        name="userPhone"
        placeholder="请输入 11 位手机号"
        autocomplete="tel"
        required
        :error="errors.userPhone"
        hint="用于登录、接收验证码与重要通知"
      />

      <Input
        v-model="form.userIdCard"
        label="身份证号"
        name="userIdCard"
        placeholder="请输入 18 位身份证号"
        autocomplete="off"
        required
        :error="errors.userIdCard"
        hint="用于实名认证与账户关联"
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

      <div>
        <label class="flex items-start gap-3 cursor-pointer">
          <input
            v-model="form.agreeTerms"
            type="checkbox"
            name="agreeTerms"
            class="mt-1 h-4 w-4 rounded border-navy-300 accent-primary-600 dark:border-navy-600"
            :aria-invalid="!!errors.agreeTerms"
          >
          <span class="text-body-sm text-navy-600 dark:text-navy-300">
            我已阅读并同意
            <span class="font-medium text-navy-800 dark:text-navy-100">《用户服务协议》</span>
            、
            <span class="font-medium text-navy-800 dark:text-navy-100">《隐私政策》</span>
            与
            <span class="font-medium text-navy-800 dark:text-navy-100">《个人信息保护指引》</span>
          </span>
        </label>
        <p
          v-if="errors.agreeTerms"
          class="form-error"
          role="alert"
        >
          {{ errors.agreeTerms }}
        </p>
      </div>

      <Button
        type="submit"
        size="lg"
        class="w-full"
        :loading="loading"
      >
        立即注册
      </Button>
    </form>

    <template #footer>
      <div class="space-y-4 text-center">
        <p class="text-body-sm text-navy-500 dark:text-navy-400">
          已有账户？
          <router-link
            to="/user/login"
            class="ml-1 font-medium text-primary-600 hover:text-primary-700 dark:text-primary-400 dark:hover:text-primary-300"
          >
            立即登录
          </router-link>
        </p>
        <p class="flex items-center justify-center gap-2 text-body-xs text-navy-500 dark:text-navy-400">
          <ShieldCheck class="h-4 w-4 text-primary-500" />
          您的资金安全受存款保险制度保护
        </p>
      </div>
    </template>
  </AuthLayout>
</template>

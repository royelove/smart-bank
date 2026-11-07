<script setup lang="ts">
/**
 * `/forgot-password` — account-based password reset.
 *
 * Step 1 `POST /account/sendCode`        { accountNumber, userPhoneNumber }
 * Step 2 `POST /account/resetPassword`   { accountNumber, code, newPassword, confirmPassword }
 *
 * There is no staff/phone-only reset endpoint, so the flow is keyed on the
 * bank account number and the phone number registered on that account.
 */
import { computed, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { CircleCheck, RefreshCw, RotateCcw, ShieldCheck } from 'lucide-vue-next'
import { Alert, Button, Input } from '@/components/ui'
import { resetPassword, sendCode } from '@/api/account'
import AuthLayout from '@/layout/AuthLayout.vue'
import {
  errorMessage,
  maskPhone,
  validateConfirmPassword,
  validatePassword,
  validatePhone,
  validateSmsCode,
} from './authForms'

type Step = 1 | 2 | 3

interface ResetForm {
  accountNumber: string
  userPhoneNumber: string
  code: string
  newPassword: string
  confirmPassword: string
}

const RESEND_SECONDS = 60

const router = useRouter()

const step = ref<Step>(1)
const form = reactive<ResetForm>({
  accountNumber: '',
  userPhoneNumber: '',
  code: '',
  newPassword: '',
  confirmPassword: '',
})
const errors = reactive<Record<string, string>>({})
const loading = ref(false)
const countdown = ref(0)

let timer: ReturnType<typeof setInterval> | null = null

const titles: Record<Step, string> = {
  1: '找回密码',
  2: '重置密码',
  3: '密码重置成功',
}

const title = computed(() => titles[step.value])
const subtitle = computed(() => {
  if (step.value === 1) return '输入银行卡号与预留手机号，我们会向该手机号发送验证码'
  if (step.value === 2) return `验证码已发送至 ${maskPhone(form.userPhoneNumber)}，有效期 5 分钟`
  return '请使用新密码登录您的账户'
})

function stopCountdown(): void {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  countdown.value = 0
}

function startCountdown(): void {
  stopCountdown()
  countdown.value = RESEND_SECONDS
  timer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) stopCountdown()
  }, 1000)
}

onUnmounted(stopCountdown)

function validateAccount(): boolean {
  errors.form = ''
  const accountNumber = form.accountNumber.trim()
  if (!accountNumber) {
    errors.accountNumber = '请输入银行卡号/账号'
  } else if (!/^[0-9A-Za-z]{6,32}$/.test(accountNumber)) {
    errors.accountNumber = '请输入 6-32 位有效账号'
  } else {
    errors.accountNumber = ''
  }
  errors.userPhoneNumber = validatePhone(form.userPhoneNumber)
  return !errors.accountNumber && !errors.userPhoneNumber
}

function validateReset(): boolean {
  errors.form = ''
  errors.code = validateSmsCode(form.code)
  errors.newPassword = validatePassword(form.newPassword, '新密码')
  errors.confirmPassword = validateConfirmPassword(form.newPassword, form.confirmPassword)
  return !errors.code && !errors.newPassword && !errors.confirmPassword
}

async function requestCode(): Promise<void> {
  await sendCode({
    accountNumber: form.accountNumber.trim(),
    userPhoneNumber: form.userPhoneNumber.trim(),
  })
}

async function handleSendCode(): Promise<void> {
  if (!validateAccount()) return

  loading.value = true
  try {
    await requestCode()
    step.value = 2
    startCountdown()
  } catch (error) {
    errors.form = errorMessage(error, '验证码发送失败，请核对账号与手机号')
  } finally {
    loading.value = false
  }
}

async function handleResend(): Promise<void> {
  if (countdown.value > 0 || loading.value) return

  loading.value = true
  try {
    await requestCode()
    startCountdown()
  } catch (error) {
    errors.form = errorMessage(error, '验证码发送失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function handleReset(): Promise<void> {
  if (!validateReset()) return

  loading.value = true
  try {
    await resetPassword({
      accountNumber: form.accountNumber.trim(),
      code: form.code.trim(),
      newPassword: form.newPassword,
      confirmPassword: form.confirmPassword,
    })
    stopCountdown()
    step.value = 3
  } catch (error) {
    errors.form = errorMessage(error, '密码重置失败，请检查验证码')
  } finally {
    loading.value = false
  }
}

function restart(): void {
  stopCountdown()
  form.accountNumber = ''
  form.userPhoneNumber = ''
  form.code = ''
  form.newPassword = ''
  form.confirmPassword = ''
  errors.accountNumber = ''
  errors.userPhoneNumber = ''
  errors.code = ''
  errors.newPassword = ''
  errors.confirmPassword = ''
  errors.form = ''
  step.value = 1
}

function goToLogin(): void {
  router.push({ path: '/user/login', query: { reset: '1' } })
}
</script>

<template>
  <AuthLayout
    :title="title"
    :subtitle="subtitle"
  >
    <!-- Step indicator -->
    <div
      v-if="step < 3"
      class="mb-8"
    >
      <div class="flex items-center">
        <div
          v-for="s in 2"
          :key="s"
          class="flex flex-1 items-center last:flex-none"
        >
          <div
            class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-label-md transition-colors"
            :class="step >= s
              ? 'bg-primary-600 text-white'
              : 'bg-navy-200 text-navy-500 dark:bg-navy-800 dark:text-navy-400'"
          >
            <CircleCheck
              v-if="step > s"
              class="h-5 w-5"
            />
            <span v-else>{{ s }}</span>
          </div>
          <div
            v-if="s < 2"
            class="mx-3 h-1 flex-1 rounded-full"
            :class="step > s ? 'bg-primary-600' : 'bg-navy-200 dark:bg-navy-800'"
          />
        </div>
      </div>
      <div class="mt-3 flex justify-between text-body-xs text-navy-500 dark:text-navy-400">
        <span>验证身份</span>
        <span>重置密码</span>
      </div>
    </div>

    <Alert
      v-if="errors.form"
      :key="errors.form"
      variant="danger"
      class="mb-6"
      dismissible
    >
      {{ errors.form }}
    </Alert>

    <!-- Step 1: account + phone -->
    <form
      v-if="step === 1"
      class="animate-slide-up space-y-5"
      novalidate
      @submit.prevent="handleSendCode"
    >
      <Input
        v-model="form.accountNumber"
        label="银行卡号/账号"
        name="accountNumber"
        placeholder="请输入需要重置密码的账号"
        autocomplete="off"
        required
        :error="errors.accountNumber"
        hint="账号需已完成注册并可正常登录"
      />

      <Input
        v-model="form.userPhoneNumber"
        label="预留手机号"
        type="tel"
        name="userPhoneNumber"
        placeholder="请输入开户时预留的手机号"
        autocomplete="tel"
        required
        :error="errors.userPhoneNumber"
        hint="手机号需与该账号绑定的一致"
      />

      <Button
        type="submit"
        size="lg"
        class="w-full"
        :loading="loading"
      >
        获取验证码
      </Button>
    </form>

    <!-- Step 2: code + new password -->
    <form
      v-else-if="step === 2"
      class="animate-slide-up space-y-5"
      novalidate
      @submit.prevent="handleReset"
    >
      <div>
        <Input
          v-model="form.code"
          label="短信验证码"
          name="code"
          placeholder="请输入 6 位验证码"
          autocomplete="one-time-code"
          required
          :error="errors.code"
        />
        <div class="mt-2 flex items-center justify-between text-body-sm">
          <span class="text-navy-500 dark:text-navy-400">
            {{ countdown > 0 ? `${countdown}s 后可重新发送` : '未收到验证码？' }}
          </span>
          <button
            type="button"
            class="inline-flex items-center gap-1.5 font-medium text-primary-600 hover:text-primary-700 disabled:cursor-not-allowed disabled:text-navy-400 dark:text-primary-400 dark:hover:text-primary-300 dark:disabled:text-navy-500"
            :disabled="countdown > 0 || loading"
            @click="handleResend"
          >
            <RefreshCw class="h-4 w-4" />
            重新发送
          </button>
        </div>
      </div>

      <Input
        v-model="form.newPassword"
        label="新密码"
        type="password"
        name="newPassword"
        placeholder="请输入 6 位数字密码"
        autocomplete="new-password"
        required
        :show-password-toggle="true"
        :error="errors.newPassword"
        hint="密码为 6 位数字"
      />

      <Input
        v-model="form.confirmPassword"
        label="确认新密码"
        type="password"
        name="confirmPassword"
        placeholder="请再次输入新密码"
        autocomplete="new-password"
        required
        :show-password-toggle="true"
        :error="errors.confirmPassword"
      />

      <Button
        type="submit"
        size="lg"
        class="w-full"
        :loading="loading"
      >
        确认重置
      </Button>

      <Button
        type="button"
        variant="ghost"
        class="w-full"
        @click="restart"
      >
        <RotateCcw class="h-4 w-4" />
        换一个账号
      </Button>
    </form>

    <!-- Step 3: done -->
    <div
      v-else
      class="animate-fade-in text-center"
    >
      <div class="mx-auto mb-6 flex h-16 w-16 items-center justify-center rounded-full bg-success-100 dark:bg-success-500/10">
        <CircleCheck class="h-8 w-8 text-success-600 dark:text-success-400" />
      </div>
      <p class="text-body-md text-navy-600 dark:text-navy-300">
        账号 {{ form.accountNumber }} 的登录密码已更新。
      </p>

      <div class="mt-8 space-y-3">
        <Button
          size="lg"
          class="w-full"
          @click="goToLogin"
        >
          去登录
        </Button>
        <Button
          variant="ghost"
          class="w-full"
          @click="restart"
        >
          <RotateCcw class="h-4 w-4" />
          重置其他账号
        </Button>
      </div>
    </div>

    <template #footer>
      <p class="flex items-center justify-center gap-2 text-body-xs text-navy-500 dark:text-navy-400">
        <ShieldCheck class="h-4 w-4 text-primary-500" />
        我们不会通过任何渠道向您索要验证码
      </p>
    </template>
  </AuthLayout>
</template>

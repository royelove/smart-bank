<script setup lang="ts">
/**
 * Settings — profile, security and appearance.
 *
 * Only real backend capabilities are exposed here:
 *   GET  /user/info                     profile (read-only: see the notice)
 *   POST /user/updatePsw                login password (6 digits)
 *   POST /user/face/bind                enrol a face
 *   POST /user/face/verify              face check against the stored feature
 *   POST /user/face/realname/verify     face check against the ID-card library
 *   POST /account/sendCode              SMS code for one of the owned accounts
 *   POST /account/resetPassword         reset that account's payment password
 *
 * Everything that has no endpoint (name / phone / ID card editing, notification
 * preferences, device management) is deliberately absent instead of faked.
 */
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import {
  BadgeCheck,
  Camera,
  Check,
  CreditCard,
  Fingerprint,
  KeyRound,
  Lock,
  Moon,
  RotateCcw,
  ScanFace,
  ShieldCheck,
  Smartphone,
  Sun,
  UserRound,
} from 'lucide-vue-next'
import { Alert, Badge, Button, Card, Input, Modal } from '@/components/ui'
import { PageHeader, SectionNav } from '@/components/layout'
import { usePageMeta } from '@/composables/usePageMeta'
import { useTheme } from '@/composables/useTheme'
import { useAuthStore } from '@/stores/auth'
import {
  bindFace,
  getUserAccounts,
  getUserInfo,
  updateUserPsw,
  verifyFace,
  verifyRealName,
} from '@/api/user'
import { resetPassword, sendCode } from '@/api/account'
import { accountStatusLabel, accountStatusTone, isAccountActive } from '@/utils/account'
import { maskAccountNumber } from '@/utils/format'
import {
  PHONE_PATTERN,
  errorMessage,
  maskPhone,
  readImageAsBase64,
  validateConfirmPassword,
  validatePassword,
} from '@/utils/validation'
import type { Account, User } from '@/types'

const { title, subtitle } = usePageMeta()
const authStore = useAuthStore()
const { theme, setTheme } = useTheme()

const tabs = [
  { id: 'profile', label: '个人信息', icon: UserRound },
  { id: 'security', label: '账户安全', icon: ShieldCheck },
  { id: 'appearance', label: '界面偏好', icon: Sun },
]
const activeTab = ref('profile')

/* ------------------------------------------------------------------ profile */

const loading = ref(true)
const pageError = ref('')
const userInfo = ref<User | null>(null)
const accounts = ref<Account[]>([])

const displayName = computed(() => userInfo.value?.userName || authStore.user?.userName || '未设置')
const maskedPhone = computed(() => maskPhone(userInfo.value?.userPhone ?? '') || '--')
const idCardDisplay = computed(() => userInfo.value?.idCardMask || userInfo.value?.userIdCard || '--')
const realnameState = computed(() =>
  userInfo.value?.realnameBound === 1
    ? { label: '已实名认证', variant: 'success' as const }
    : { label: '未实名认证', variant: 'warning' as const },
)
const faceState = computed(() =>
  userInfo.value?.faceBound === 1
    ? { label: '已录入人脸', variant: 'success' as const }
    : { label: '未录入人脸', variant: 'neutral' as const },
)

async function load() {
  loading.value = true
  pageError.value = ''
  try {
    // The card list is what the payment-password form needs, so fetch it first
    // and let a profile failure degrade to the banner instead of an empty page.
    accounts.value = (await getUserAccounts().catch(() => [] as Account[])) ?? []
    if (!pwdAccount.value && accounts.value.length) pwdAccount.value = accounts.value[0].accountNumber

    const info = await getUserInfo()
    userInfo.value = info
    if (info) authStore.updateUser(info)
  } catch (e) {
    pageError.value = errorMessage(e, '个人信息加载失败')
  } finally {
    loading.value = false
  }
}

/* --------------------------------------------------------- login password */

const loginForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const loginErrors = reactive<Record<string, string>>({})
const loginSaving = ref(false)
const loginNotice = ref('')
const loginError = ref('')

function resetLoginForm() {
  loginForm.oldPassword = ''
  loginForm.newPassword = ''
  loginForm.confirmPassword = ''
  Object.keys(loginErrors).forEach((key) => delete loginErrors[key])
}

async function submitLoginPassword() {
  loginNotice.value = ''
  loginError.value = ''
  loginErrors.oldPassword = loginForm.oldPassword ? '' : '请输入当前登录密码'
  loginErrors.newPassword = validatePassword(loginForm.newPassword, '新密码')
  loginErrors.confirmPassword = validateConfirmPassword(loginForm.newPassword, loginForm.confirmPassword)
  if (loginErrors.oldPassword || loginErrors.newPassword || loginErrors.confirmPassword) return

  loginSaving.value = true
  try {
    await updateUserPsw({
      oldPassword: loginForm.oldPassword,
      newPassword: loginForm.newPassword,
      confirmPassword: loginForm.confirmPassword,
    })
    resetLoginForm()
    loginNotice.value = '登录密码已更新，当前登录状态保持有效'
  } catch (e) {
    loginError.value = errorMessage(e, '密码修改失败')
  } finally {
    loginSaving.value = false
  }
}

/* ------------------------------------------------------------------- face */

type FaceAction = 'bind' | 'verify' | 'realname'

interface FaceVerifyResult {
  pass: boolean
  score?: number
}

const FACE_ACTIONS: Record<FaceAction, { title: string; submit: string; hint: string }> = {
  bind: {
    title: '录入人脸',
    submit: '录入人脸',
    hint: '照片会转换为 512 维特征向量后加密保存，原图不会留存。',
  },
  verify: {
    title: '人脸核身',
    submit: '开始核验',
    hint: '与您已录入的人脸特征比对，相似度达到阈值即通过。',
  },
  realname: {
    title: '实名认证',
    submit: '开始认证',
    hint: '与身份证库中登记的证件照比对，通过后同时完成实名认证并更新人脸特征。',
  },
}

const faceOpen = ref(false)
const faceAction = ref<FaceAction>('bind')
const faceSaving = ref(false)
const faceError = ref('')
const faceResult = ref<FaceVerifyResult | null>(null)
const facePreview = ref('')
const facePayload = ref('')
const faceFileName = ref('')

const faceMeta = computed(() => FACE_ACTIONS[faceAction.value])

/** Shared success banner for the 账户安全 tab. */
const securityNotice = ref('')

function openFace(action: FaceAction) {
  faceAction.value = action
  faceError.value = ''
  faceResult.value = null
  facePreview.value = ''
  facePayload.value = ''
  faceFileName.value = ''
  faceOpen.value = true
}

async function onFaceFile(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  faceError.value = ''
  faceResult.value = null
  if (!file) return
  if (!file.type.startsWith('image/')) {
    faceError.value = '请选择 JPG / PNG 格式的图片'
    input.value = ''
    return
  }
  if (file.size > 4 * 1024 * 1024) {
    faceError.value = '图片大小不能超过 4MB'
    input.value = ''
    return
  }
  try {
    facePreview.value = await readImageAsBase64(file, true)
    facePayload.value = await readImageAsBase64(file, false)
    faceFileName.value = file.name
  } catch (e) {
    faceError.value = errorMessage(e, '图片读取失败')
  }
}

async function submitFace() {
  faceError.value = ''
  faceResult.value = null
  if (!facePayload.value) {
    faceError.value = '请先选择一张正面清晰的人脸照片'
    return
  }
  faceSaving.value = true
  try {
    const image = facePayload.value
    if (faceAction.value === 'bind') {
      await bindFace({ image })
      authStore.updateUser({ faceBound: 1 })
      if (userInfo.value) userInfo.value = { ...userInfo.value, faceBound: 1 }
      faceOpen.value = false
      securityNotice.value = '人脸录入成功，现在可以使用人脸核身验证身份'
    } else if (faceAction.value === 'verify') {
      faceResult.value = await verifyFace({ image })
    } else {
      const result = await verifyRealName(image)
      faceResult.value = result
      if (result?.pass) {
        authStore.updateUser({ faceBound: 1, realnameBound: 1 })
        if (userInfo.value) userInfo.value = { ...userInfo.value, faceBound: 1, realnameBound: 1 }
      }
    }
  } catch (e) {
    faceError.value = errorMessage(e, '人脸服务暂时不可用，请稍后再试')
  } finally {
    faceSaving.value = false
  }
}

/* ------------------------------------------------------- payment password */

const pwdAccount = ref('')
const pwdPhone = ref('')
const pwdCode = ref('')
const pwdNew = ref('')
const pwdConfirm = ref('')
const pwdErrors = reactive<Record<string, string>>({})
const pwdSending = ref(false)
const pwdSaving = ref(false)
const pwdNotice = ref('')
const pwdError = ref('')
const countdown = ref(0)
let countdownTimer: number | undefined

const selectedAccount = computed(
  () => accounts.value.find((account) => account.accountNumber === pwdAccount.value) ?? null,
)

watch(pwdAccount, () => {
  pwdPhone.value = selectedAccount.value?.userPhoneNumber ?? ''
  pwdErrors.phone = ''
  pwdNotice.value = ''
})

function startCountdown() {
  countdown.value = 60
  if (countdownTimer) window.clearInterval(countdownTimer)
  countdownTimer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      window.clearInterval(countdownTimer)
      countdownTimer = undefined
      countdown.value = 0
    }
  }, 1000)
}

onBeforeUnmount(() => {
  if (countdownTimer) window.clearInterval(countdownTimer)
})

async function sendPwdCode() {
  pwdError.value = ''
  pwdNotice.value = ''
  pwdErrors.phone = ''
  if (!pwdAccount.value) {
    pwdError.value = '请先选择要重置密码的银行卡'
    return
  }
  const phone = pwdPhone.value.trim()
  if (!PHONE_PATTERN.test(phone)) {
    pwdErrors.phone = '请输入 11 位有效手机号'
    return
  }
  pwdSending.value = true
  try {
    await sendCode({ accountNumber: pwdAccount.value, userPhoneNumber: phone })
    startCountdown()
    pwdNotice.value = `验证码已发送至 ${maskPhone(phone)}，5 分钟内有效`
  } catch (e) {
    pwdError.value = errorMessage(e, '验证码发送失败')
  } finally {
    pwdSending.value = false
  }
}

async function submitPaymentPassword() {
  pwdNotice.value = ''
  pwdError.value = ''
  pwdErrors.code = pwdCode.value.trim() ? '' : '请输入验证码'
  pwdErrors.newPassword = validatePassword(pwdNew.value, '新密码')
  pwdErrors.confirmPassword = validateConfirmPassword(pwdNew.value, pwdConfirm.value)
  if (!pwdAccount.value) pwdError.value = '请先选择要重置密码的银行卡'
  if (pwdError.value || pwdErrors.code || pwdErrors.newPassword || pwdErrors.confirmPassword) return

  pwdSaving.value = true
  try {
    await resetPassword({
      accountNumber: pwdAccount.value,
      code: pwdCode.value.trim(),
      newPassword: pwdNew.value,
      confirmPassword: pwdConfirm.value,
    })
    pwdCode.value = ''
    pwdNew.value = ''
    pwdConfirm.value = ''
    Object.keys(pwdErrors).forEach((key) => delete pwdErrors[key])
    pwdNotice.value = '支付密码已重置，请使用新密码进行存取款与转账'
  } catch (e) {
    pwdError.value = errorMessage(e, '支付密码重置失败')
  } finally {
    pwdSaving.value = false
  }
}

/* -------------------------------------------------------------- appearance */

const themeOptions = [
  { value: 'light' as const, label: '浅色', description: '始终使用浅色主题', icon: Sun },
  { value: 'dark' as const, label: '深色', description: '始终使用深色主题', icon: Moon },
]

const resetNotice = ref('')

function resetLocalPreferences() {
  try {
    window.localStorage.removeItem('smartbank-theme')
    window.localStorage.removeItem('smartbank-sidebar-collapsed')
  } catch {
    /* storage disabled — nothing to clear */
  }
  const prefersDark = window.matchMedia?.('(prefers-color-scheme: dark)').matches
  setTheme(prefersDark ? 'dark' : 'light')
  resetNotice.value = '已清除本地偏好，主题将跟随系统设置'
}

onMounted(load)
</script>

<template>
  <div class="app-container">
    <PageHeader
      :title="title"
      :subtitle="subtitle"
    >
      <template #actions>
        <Button
          variant="secondary"
          size="sm"
          :loading="loading"
          @click="load"
        >
          刷新
        </Button>
      </template>
    </PageHeader>

    <Alert
      v-if="pageError"
      variant="danger"
      title="无法加载账户信息"
      :description="pageError"
      class="mb-6"
    />

    <div class="layout-split">
      <SectionNav
        v-model="activeTab"
        title="设置"
        :items="tabs"
      />

      <!-- ============================ 个人信息 ============================ -->
      <template v-if="activeTab === 'profile'">
        <div class="space-y-6">
          <Card>
            <div class="card-body flex flex-col gap-6 sm:flex-row sm:items-center">
              <div class="flex items-center gap-4">
                <div class="flex h-16 w-16 shrink-0 items-center justify-center rounded-full bg-primary-100 text-heading-md text-primary-700 dark:bg-primary-500/10 dark:text-primary-300">
                  {{ displayName.slice(0, 1) }}
                </div>
                <div class="min-w-0">
                  <p class="text-heading-md text-navy-900 dark:text-navy-50">
                    {{ displayName }}
                  </p>
                  <p class="font-mono text-body-sm text-navy-500 dark:text-navy-400">
                    {{ maskedPhone }}
                  </p>
                </div>
              </div>

              <div class="flex flex-wrap gap-2 sm:ml-auto">
                <Badge
                  :variant="realnameState.variant"
                  dot
                >
                  {{ realnameState.label }}
                </Badge>
                <Badge
                  :variant="faceState.variant"
                  dot
                >
                  {{ faceState.label }}
                </Badge>
              </div>
            </div>
          </Card>

          <Card>
            <div class="card-header">
              <div>
                <h2 class="section-title">
                  <UserRound class="h-4 w-4 text-navy-400" />
                  基本信息
                </h2>
                <p class="mt-1 text-body-sm text-navy-500 dark:text-navy-400">
                  注册时提交的身份信息，用于账户验证与监管报送
                </p>
              </div>
            </div>
            <div class="card-body">
              <dl class="grid grid-cols-1 gap-x-8 gap-y-5 sm:grid-cols-2">
                <div>
                  <dt class="text-body-sm text-navy-500 dark:text-navy-400">
                    姓名
                  </dt>
                  <dd class="mt-1 text-navy-900 dark:text-navy-50">
                    {{ userInfo?.userName || '--' }}
                  </dd>
                </div>
                <div>
                  <dt class="text-body-sm text-navy-500 dark:text-navy-400">
                    手机号
                  </dt>
                  <dd class="mt-1 font-mono text-navy-900 dark:text-navy-50">
                    {{ maskedPhone }}
                  </dd>
                </div>
                <div>
                  <dt class="text-body-sm text-navy-500 dark:text-navy-400">
                    证件号码
                  </dt>
                  <dd class="mt-1 font-mono text-navy-900 dark:text-navy-50">
                    {{ idCardDisplay }}
                  </dd>
                </div>
                <div>
                  <dt class="text-body-sm text-navy-500 dark:text-navy-400">
                    已绑定银行卡
                  </dt>
                  <dd class="mt-1 text-navy-900 dark:text-navy-50">
                    {{ accounts.length }} 张
                  </dd>
                </div>
              </dl>

              <Alert
                variant="warning"
                class="mt-6"
                title="信息修改说明"
                description="根据监管要求，姓名、手机号与证件号码在注册后不可自行修改。如需变更，请携带有效身份证件前往网点办理，或通过在线客服提交申请。"
              />
            </div>
          </Card>
        </div>
      </template>

      <!-- ============================ 账户安全 ============================ -->
      <template v-else-if="activeTab === 'security'">
        <div class="space-y-6">
          <Alert
            v-if="securityNotice"
            variant="success"
            :description="securityNotice"
            dismissible
            @dismiss="securityNotice = ''"
          />

          <!-- 登录密码 -->
          <Card>
            <div class="card-header">
              <div>
                <h2 class="section-title">
                  <Lock class="h-4 w-4 text-navy-400" />
                  登录密码
                </h2>
                <p class="mt-1 text-body-sm text-navy-500 dark:text-navy-400">
                  用于登录手机银行，为 6 位数字
                </p>
              </div>
            </div>
            <form
              class="card-body max-w-xl space-y-5"
              @submit.prevent="submitLoginPassword"
            >
              <Alert
                v-if="loginNotice"
                variant="success"
                :description="loginNotice"
              />
              <Alert
                v-if="loginError"
                variant="danger"
                :description="loginError"
              />

              <Input
                v-model="loginForm.oldPassword"
                type="password"
                label="当前登录密码"
                placeholder="请输入当前密码"
                :error="loginErrors.oldPassword"
                :show-password-toggle="true"
                autocomplete="current-password"
                required
              />
              <Input
                v-model="loginForm.newPassword"
                type="password"
                label="新登录密码"
                placeholder="6 位数字"
                :error="loginErrors.newPassword"
                :show-password-toggle="true"
                autocomplete="new-password"
                required
              />
              <Input
                v-model="loginForm.confirmPassword"
                type="password"
                label="确认新密码"
                placeholder="请再次输入新密码"
                :error="loginErrors.confirmPassword"
                :show-password-toggle="true"
                autocomplete="new-password"
                required
              />

              <div class="flex items-center gap-3 border-t border-navy-200 pt-5 dark:border-navy-700">
                <Button
                  type="submit"
                  :loading="loginSaving"
                >
                  修改登录密码
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  @click="resetLoginForm"
                >
                  清空
                </Button>
              </div>
            </form>
          </Card>

          <!-- 人脸识别 -->
          <Card>
            <div class="card-header">
              <div>
                <h2 class="section-title">
                  <ScanFace class="h-4 w-4 text-navy-400" />
                  人脸识别与实名认证
                </h2>
                <p class="mt-1 text-body-sm text-navy-500 dark:text-navy-400">
                  人脸特征由独立的人脸服务提取，后端仅保存 512 维特征向量
                </p>
              </div>
              <div class="flex flex-wrap gap-2">
                <Badge :variant="realnameState.variant">
                  {{ realnameState.label }}
                </Badge>
                <Badge :variant="faceState.variant">
                  {{ faceState.label }}
                </Badge>
              </div>
            </div>
            <div class="card-body space-y-5">
              <div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
                <button
                  type="button"
                  class="flex items-start gap-3 rounded-xl border border-navy-200 p-4 text-left transition-colors hover:border-primary-400 hover:bg-primary-50/50 dark:border-navy-700 dark:hover:border-primary-600 dark:hover:bg-primary-900/10"
                  @click="openFace('bind')"
                >
                  <Camera class="mt-0.5 h-5 w-5 shrink-0 text-primary-600 dark:text-primary-400" />
                  <span>
                    <span class="block font-medium text-navy-900 dark:text-navy-50">
                      {{ userInfo?.faceBound === 1 ? '更新人脸' : '录入人脸' }}
                    </span>
                    <span class="mt-0.5 block text-body-sm text-navy-500 dark:text-navy-400">
                      登记一张人脸照片作为核身依据
                    </span>
                  </span>
                </button>

                <button
                  type="button"
                  class="flex items-start gap-3 rounded-xl border border-navy-200 p-4 text-left transition-colors hover:border-primary-400 hover:bg-primary-50/50 disabled:cursor-not-allowed disabled:opacity-60 dark:border-navy-700 dark:hover:border-primary-600 dark:hover:bg-primary-900/10"
                  :disabled="userInfo?.faceBound !== 1"
                  @click="openFace('verify')"
                >
                  <Fingerprint class="mt-0.5 h-5 w-5 shrink-0 text-primary-600 dark:text-primary-400" />
                  <span>
                    <span class="block font-medium text-navy-900 dark:text-navy-50">
                      人脸核身
                    </span>
                    <span class="mt-0.5 block text-body-sm text-navy-500 dark:text-navy-400">
                      {{ userInfo?.faceBound === 1 ? '与已录入人脸比对' : '请先录入人脸' }}
                    </span>
                  </span>
                </button>

                <button
                  type="button"
                  class="flex items-start gap-3 rounded-xl border border-navy-200 p-4 text-left transition-colors hover:border-primary-400 hover:bg-primary-50/50 dark:border-navy-700 dark:hover:border-primary-600 dark:hover:bg-primary-900/10"
                  @click="openFace('realname')"
                >
                  <BadgeCheck class="mt-0.5 h-5 w-5 shrink-0 text-primary-600 dark:text-primary-400" />
                  <span>
                    <span class="block font-medium text-navy-900 dark:text-navy-50">
                      {{ userInfo?.realnameBound === 1 ? '重新实名认证' : '实名认证' }}
                    </span>
                    <span class="mt-0.5 block text-body-sm text-navy-500 dark:text-navy-400">
                      与身份证库证件照比对
                    </span>
                  </span>
                </button>
              </div>

              <Alert
                variant="info"
                description="人脸相关能力依赖独立部署的人脸识别微服务，若该服务未启动，提交时会返回 910004 服务不可用。"
              />
            </div>
          </Card>

          <!-- 支付密码 -->
          <Card>
            <div class="card-header">
              <div>
                <h2 class="section-title">
                  <KeyRound class="h-4 w-4 text-navy-400" />
                  银行卡支付密码
                </h2>
                <p class="mt-1 text-body-sm text-navy-500 dark:text-navy-400">
                  通过预留手机号接收验证码后重置，存取款与转账均使用该密码
                </p>
              </div>
            </div>
            <div class="card-body">
              <Alert
                v-if="!accounts.length && !loading"
                variant="warning"
                description="您还没有绑定任何银行卡，请先在「我的银行卡」中绑定后再设置支付密码。"
              />

              <form
                v-else
                class="max-w-xl space-y-5"
                @submit.prevent="submitPaymentPassword"
              >
                <Alert
                  v-if="pwdNotice"
                  variant="success"
                  :description="pwdNotice"
                />
                <Alert
                  v-if="pwdError"
                  variant="danger"
                  :description="pwdError"
                />

                <div>
                  <label
                    class="label"
                    for="pwd-account"
                  >选择银行卡</label>
                  <select
                    id="pwd-account"
                    v-model="pwdAccount"
                    class="input"
                  >
                    <option
                      v-for="account in accounts"
                      :key="account.accountNumber"
                      :value="account.accountNumber"
                    >
                      {{ account.accountHolderName }} · {{ maskAccountNumber(account.accountNumber) }}{{ isAccountActive(account) ? '' : '（冻结）' }}
                    </option>
                  </select>
                </div>

                <div
                  v-if="selectedAccount"
                  class="flex flex-wrap items-center gap-2 rounded-lg bg-navy-50 px-4 py-3 dark:bg-navy-800/50"
                >
                  <CreditCard class="h-4 w-4 text-navy-400" />
                  <span class="font-mono text-body-sm text-navy-600 dark:text-navy-300">
                    {{ selectedAccount.accountNumber }}
                  </span>
                  <Badge
                    :variant="accountStatusTone(selectedAccount.accountStatus)"
                    class="ml-auto"
                  >
                    {{ accountStatusLabel(selectedAccount.accountStatus) }}
                  </Badge>
                </div>

                <div class="grid grid-cols-1 gap-4 sm:grid-cols-[1fr_auto] sm:items-end">
                  <Input
                    v-model="pwdPhone"
                    label="预留手机号"
                    placeholder="11 位手机号"
                    :error="pwdErrors.phone"
                    autocomplete="tel"
                  />
                  <Button
                    type="button"
                    variant="secondary"
                    class="sm:mb-0.5"
                    :loading="pwdSending"
                    :disabled="countdown > 0"
                    @click="sendPwdCode"
                  >
                    <Smartphone class="h-4 w-4" />
                    {{ countdown > 0 ? `${countdown}s 后重发` : '发送验证码' }}
                  </Button>
                </div>

                <Input
                  v-model="pwdCode"
                  label="短信验证码"
                  placeholder="6 位验证码"
                  :error="pwdErrors.code"
                  autocomplete="one-time-code"
                />
                <Input
                  v-model="pwdNew"
                  type="password"
                  label="新支付密码"
                  placeholder="6 位数字"
                  :error="pwdErrors.newPassword"
                  :show-password-toggle="true"
                  autocomplete="new-password"
                />
                <Input
                  v-model="pwdConfirm"
                  type="password"
                  label="确认新支付密码"
                  placeholder="请再次输入"
                  :error="pwdErrors.confirmPassword"
                  :show-password-toggle="true"
                  autocomplete="new-password"
                />

                <div class="border-t border-navy-200 pt-5 dark:border-navy-700">
                  <Button
                    type="submit"
                    :loading="pwdSaving"
                  >
                    重置支付密码
                  </Button>
                </div>
              </form>
            </div>
          </Card>
        </div>
      </template>

      <!-- ============================ 界面偏好 ============================ -->
      <template v-else-if="activeTab === 'appearance'">
        <div class="space-y-6">
          <Card>
            <div class="card-header">
              <div>
                <h2 class="section-title">
                  <Sun class="h-4 w-4 text-navy-400" />
                  主题模式
                </h2>
                <p class="mt-1 text-body-sm text-navy-500 dark:text-navy-400">
                  切换后立即生效，并保存在当前浏览器中
                </p>
              </div>
            </div>
            <div class="card-body">
              <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <button
                  v-for="option in themeOptions"
                  :key="option.value"
                  type="button"
                  class="relative rounded-xl border-2 p-4 text-left transition-colors"
                  :class="
                    theme === option.value
                      ? 'border-primary-500 bg-primary-50 dark:bg-primary-500/10'
                      : 'border-navy-200 hover:border-primary-300 dark:border-navy-700 dark:hover:border-primary-700'
                  "
                  @click="setTheme(option.value)"
                >
                  <div class="flex items-center gap-3">
                    <component
                      :is="option.icon"
                      class="h-6 w-6"
                      :class="theme === option.value ? 'text-primary-600 dark:text-primary-400' : 'text-navy-400'"
                    />
                    <div>
                      <p
                        class="font-medium"
                        :class="theme === option.value ? 'text-primary-700 dark:text-primary-300' : 'text-navy-900 dark:text-navy-50'"
                      >
                        {{ option.label }}
                      </p>
                      <p class="text-body-sm text-navy-500 dark:text-navy-400">
                        {{ option.description }}
                      </p>
                    </div>
                  </div>
                  <span
                    v-if="theme === option.value"
                    class="absolute right-3 top-3 flex h-5 w-5 items-center justify-center rounded-full bg-primary-600"
                  >
                    <Check class="h-3 w-3 text-white" />
                  </span>
                </button>
              </div>
            </div>
          </Card>

          <Card>
            <div class="card-header">
              <div>
                <h2 class="section-title">
                  <RotateCcw class="h-4 w-4 text-navy-400" />
                  本地偏好
                </h2>
                <p class="mt-1 text-body-sm text-navy-500 dark:text-navy-400">
                  界面偏好仅保存在本机浏览器，不会上传到服务端
                </p>
              </div>
            </div>
            <div class="card-body space-y-5">
              <Alert
                v-if="resetNotice"
                variant="success"
                :description="resetNotice"
              />

              <dl class="grid grid-cols-1 gap-x-8 gap-y-5 sm:grid-cols-3">
                <div>
                  <dt class="text-body-sm text-navy-500 dark:text-navy-400">
                    界面语言
                  </dt>
                  <dd class="mt-1 text-navy-900 dark:text-navy-50">
                    简体中文
                  </dd>
                </div>
                <div>
                  <dt class="text-body-sm text-navy-500 dark:text-navy-400">
                    默认货币
                  </dt>
                  <dd class="mt-1 text-navy-900 dark:text-navy-50">
                    人民币 CNY
                  </dd>
                </div>
                <div>
                  <dt class="text-body-sm text-navy-500 dark:text-navy-400">
                    当前主题
                  </dt>
                  <dd class="mt-1 text-navy-900 dark:text-navy-50">
                    {{ theme === 'dark' ? '深色' : '浅色' }}
                  </dd>
                </div>
              </dl>

              <div class="border-t border-navy-200 pt-5 dark:border-navy-700">
                <Button
                  variant="secondary"
                  @click="resetLocalPreferences"
                >
                  <RotateCcw class="h-4 w-4" />
                  重置本地偏好
                </Button>
              </div>
            </div>
          </Card>
        </div>
      </template>
    </div>

    <!-- 人脸操作弹窗 -->
    <Modal
      v-model="faceOpen"
      :title="faceMeta.title"
    >
      <div class="space-y-5">
        <Alert
          v-if="faceError"
          variant="danger"
          :description="faceError"
        />
        <Alert
          v-if="faceResult"
          :variant="faceResult.pass ? 'success' : 'warning'"
          :title="faceResult.pass ? '核验通过' : '核验未通过'"
          :description="`相似度得分 ${(faceResult.score ?? 0).toFixed(3)}，阈值由人脸服务统一设定。`"
        />

        <p class="text-body-sm text-navy-500 dark:text-navy-400">
          {{ faceMeta.hint }}
        </p>

        <div class="rounded-xl border border-dashed border-navy-300 p-4 dark:border-navy-600">
          <div
            v-if="facePreview"
            class="flex items-center gap-4"
          >
            <img
              :src="facePreview"
              alt="人脸照片预览"
              class="h-24 w-24 rounded-lg object-cover"
            >
            <div class="min-w-0">
              <p class="truncate text-body-sm text-navy-900 dark:text-navy-50">
                {{ faceFileName }}
              </p>
              <p class="mt-1 text-body-xs text-navy-500 dark:text-navy-400">
                已就绪，可重新选择其他照片
              </p>
            </div>
          </div>
          <p
            v-else
            class="text-body-sm text-navy-500 dark:text-navy-400"
          >
            请选择一张正面、光线充足的 JPG / PNG 照片（不超过 4MB）
          </p>

          <input
            type="file"
            accept="image/png,image/jpeg"
            class="mt-4 block w-full text-body-sm text-navy-600 file:mr-3 file:rounded-lg file:border-0 file:bg-primary-600 file:px-4 file:py-2 file:text-body-sm file:font-medium file:text-white hover:file:bg-primary-700 dark:text-navy-300"
            @change="onFaceFile"
          >
        </div>
      </div>

      <template #footer>
        <Button
          variant="secondary"
          @click="faceOpen = false"
        >
          取消
        </Button>
        <Button
          :loading="faceSaving"
          @click="submitFace"
        >
          {{ faceMeta.submit }}
        </Button>
      </template>
    </Modal>
  </div>
</template>

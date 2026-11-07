<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ArrowRight,
  Check,
  Copy,
  CreditCard,
  Eye,
  EyeOff,
  Link2,
  Lock,
  Plus,
  RefreshCw,
  Wallet,
} from 'lucide-vue-next'
import { Alert, Badge, Button, Card, EmptyState, Input, Modal, StatCard } from '@/components/ui'
import { PageHeader } from '@/components/layout'
import { usePageMeta } from '@/composables/usePageMeta'
import { addAccount, getUserAccounts, getUserInfo } from '@/api/user'
import {
  accountStatusLabel,
  accountStatusTone,
  accountTypeLabel,
  isAccountActive,
  sumBalance,
} from '@/utils/account'
import { formatMoney, maskAccountNumber } from '@/utils/format'
import type { Account, User as BankUser } from '@/types'

const { title, subtitle } = usePageMeta()

const loading = ref(true)
const error = ref('')
const accounts = ref<Account[]>([])
const userInfo = ref<BankUser | null>(null)

/** Account numbers the customer chose to reveal in this session. */
const revealed = ref<string[]>([])
const copied = ref('')

const bindOpen = ref(false)
const bindSaving = ref(false)
const bindError = ref('')
const bindForm = reactive({ userIdCard: '', accountNumber: '' })

/**
 * Tailwind needs complete class strings, so the palette is a literal list.
 * Only a thin top rule is tinted — five saturated card faces fought the rest of
 * the page for attention and made balances harder to read, not easier.
 */
const CARD_ACCENTS = [
  'bg-primary-500',
  'bg-info-500',
  'bg-success-500',
  'bg-warning-500',
  'bg-navy-400',
]

function accentFor(index: number): string {
  return CARD_ACCENTS[index % CARD_ACCENTS.length]
}

const totalBalance = computed(() => sumBalance(accounts.value))
const activeAccounts = computed(() => accounts.value.filter(isAccountActive))
const frozenAccounts = computed(() => accounts.value.filter((account) => !isAccountActive(account)))
const frozenBalance = computed(() => sumBalance(frozenAccounts.value))

function isRevealed(accountNumber: string): boolean {
  return revealed.value.includes(accountNumber)
}

function toggleReveal(accountNumber: string) {
  revealed.value = isRevealed(accountNumber)
    ? revealed.value.filter((item) => item !== accountNumber)
    : [...revealed.value, accountNumber]
}

function displayNumber(account: Account): string {
  return isRevealed(account.accountNumber) ? account.accountNumber : maskAccountNumber(account.accountNumber)
}

async function copyNumber(accountNumber: string) {
  try {
    await navigator.clipboard.writeText(accountNumber)
    copied.value = accountNumber
    window.setTimeout(() => {
      if (copied.value === accountNumber) copied.value = ''
    }, 1600)
  } catch {
    // Clipboard access can be denied outside a secure context; the number stays visible.
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [list, info] = await Promise.all([
      getUserAccounts().catch(() => [] as Account[]),
      getUserInfo().catch(() => null),
    ])
    accounts.value = list ?? []
    userInfo.value = info
    bindForm.userIdCard = info?.userIdCard ?? ''
  } catch (e) {
    error.value = (e as Error).message || '数据加载失败'
  } finally {
    loading.value = false
  }
}

function openBind() {
  bindError.value = ''
  bindForm.userIdCard = userInfo.value?.userIdCard ?? ''
  bindForm.accountNumber = ''
  bindOpen.value = true
}

async function submitBind() {
  bindError.value = ''
  if (!bindForm.userIdCard.trim() || !bindForm.accountNumber.trim()) {
    bindError.value = '请填写身份证号与银行卡号'
    return
  }
  bindSaving.value = true
  try {
    await addAccount({
      userIdCard: bindForm.userIdCard.trim(),
      accountNumber: bindForm.accountNumber.replace(/\s/g, ''),
    })
    bindOpen.value = false
    await load()
  } catch (e) {
    bindError.value = (e as Error).message || '绑定失败'
  } finally {
    bindSaving.value = false
  }
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
          variant="ghost"
          size="icon"
          :disabled="loading"
          title="刷新"
          @click="load"
        >
          <RefreshCw
            class="w-5 h-5"
            :class="{ 'animate-spin': loading }"
          />
        </Button>
        <Button @click="openBind">
          <Plus class="w-4 h-4" />
          绑定银行卡
        </Button>
      </template>
    </PageHeader>

    <Alert
      v-if="error"
      variant="danger"
      class="mb-6"
    >
      {{ error }}
    </Alert>

    <section class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4 mb-6">
      <StatCard
        label="总资产"
        :value="formatMoney(totalBalance)"
        :hint="`身份证号 ${userInfo?.idCardMask || '—'}`"
        :icon="Wallet"
        :loading="loading"
      />
      <StatCard
        label="银行卡数量"
        :value="accounts.length"
        :hint="`${activeAccounts.length} 张正常使用`"
        :icon="CreditCard"
        :loading="loading"
      />
      <StatCard
        label="正常账户余额"
        :value="formatMoney(sumBalance(activeAccounts))"
        hint="可正常收付的账户"
        :icon="Check"
        :loading="loading"
      />
      <StatCard
        label="冻结账户余额"
        :value="formatMoney(frozenBalance)"
        :hint="frozenAccounts.length ? `${frozenAccounts.length} 张卡被冻结` : '没有冻结账户'"
        :icon="Lock"
        :loading="loading"
      />
    </section>

    <EmptyState
      v-if="!loading && !accounts.length"
      title="还没有绑定任何银行卡"
      description="输入本人身份证号与卡号即可把已有账户绑定到当前用户，绑定后即可查看余额、流水与理财推荐。"
    >
      <template #action>
        <Button @click="openBind">
          <Link2 class="w-4 h-4" />
          立即绑定
        </Button>
      </template>
    </EmptyState>

    <template v-else>
      <div
        v-if="loading"
        class="grid gap-6 sm:grid-cols-2 xl:grid-cols-3"
      >
        <div
          v-for="n in 3"
          :key="n"
          class="skeleton h-64 w-full rounded-xl"
        />
      </div>

      <section
        v-else
        class="grid gap-6 sm:grid-cols-2 xl:grid-cols-3"
      >
        <Card
          v-for="(account, index) in accounts"
          :key="account.accountNumber"
          class="overflow-hidden"
        >
          <!-- Card face -->
          <div class="relative p-6 pt-7">
            <span
              aria-hidden="true"
              class="absolute inset-x-0 top-0 h-1"
              :class="accentFor(index)"
            />

            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <div class="flex items-center gap-2">
                  <CreditCard class="w-5 h-5 text-navy-400 dark:text-navy-500" />
                  <Badge variant="neutral">
                    {{ accountTypeLabel(account.accountType) }}
                  </Badge>
                </div>
                <p class="mt-2 truncate text-sm text-navy-500 dark:text-navy-400">
                  {{ account.accountHolderName || '持卡人' }}
                </p>
              </div>
              <Badge
                :variant="accountStatusTone(account.accountStatus)"
                class="shrink-0"
              >
                {{ accountStatusLabel(account.accountStatus) }}
              </Badge>
            </div>

            <div class="mt-6">
              <p class="text-xs text-navy-400 dark:text-navy-500">
                卡号
              </p>
              <div class="mt-1 flex items-center gap-2">
                <span class="font-mono text-lg tracking-wider text-navy-900 dark:text-navy-50">{{ displayNumber(account) }}</span>
                <button
                  type="button"
                  class="rounded p-1 text-navy-400 transition-colors hover:text-navy-700 dark:hover:text-navy-200"
                  :aria-label="isRevealed(account.accountNumber) ? '隐藏卡号' : '显示卡号'"
                  @click="toggleReveal(account.accountNumber)"
                >
                  <component
                    :is="isRevealed(account.accountNumber) ? EyeOff : Eye"
                    class="w-4 h-4"
                  />
                </button>
                <button
                  type="button"
                  class="rounded p-1 text-navy-400 transition-colors hover:text-navy-700 dark:hover:text-navy-200"
                  aria-label="复制卡号"
                  @click="copyNumber(account.accountNumber)"
                >
                  <component
                    :is="copied === account.accountNumber ? Check : Copy"
                    class="w-4 h-4"
                  />
                </button>
              </div>
            </div>

            <div class="mt-6 flex items-end justify-between gap-4">
              <div>
                <p class="text-xs text-navy-400 dark:text-navy-500">
                  账户余额
                </p>
                <p class="font-mono text-2xl font-bold text-navy-900 dark:text-navy-50">
                  {{ formatMoney(account.accountBalance) }}
                </p>
              </div>
              <p class="text-right text-xs text-navy-400 dark:text-navy-500">
                {{ account.userPhoneNumber || '—' }}
              </p>
            </div>
          </div>

          <!-- Card actions -->
          <div class="flex items-center justify-between gap-3 border-t border-navy-200 px-4 py-3 dark:border-navy-700">
            <span
              v-if="!isAccountActive(account)"
              class="text-xs text-danger-600 dark:text-danger-400"
            >
              该账户已冻结，无法收付款
            </span>
            <span
              v-else
              class="text-xs text-navy-500 dark:text-navy-400"
            >
              可用余额 {{ formatMoney(account.accountBalance) }}
            </span>
            <RouterLink :to="`/trade/${account.accountNumber}`">
              <Button
                variant="ghost"
                size="sm"
              >
                交易明细
                <ArrowRight class="w-4 h-4" />
              </Button>
            </RouterLink>
          </div>
        </Card>

        <!-- Bind another card -->
        <button
          type="button"
          class="flex min-h-[16rem] flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed
                 border-navy-300 p-8 text-center transition-colors hover:border-primary-500
                 hover:bg-primary-50/50 dark:border-navy-600 dark:hover:border-primary-500 dark:hover:bg-primary-900/10"
          @click="openBind"
        >
          <span
            class="grid h-14 w-14 place-items-center rounded-full
                   bg-primary-50 text-primary-600 dark:bg-primary-500/10 dark:text-primary-400"
          >
            <Plus class="h-7 w-7" />
          </span>
          <span class="text-heading-sm text-navy-900 dark:text-navy-50">绑定其他银行卡</span>
          <span class="text-sm text-navy-500 dark:text-navy-400">
            需与本人身份证号一致
          </span>
        </button>
      </section>
    </template>

    <Modal
      v-model="bindOpen"
      title="绑定银行卡"
      size="md"
    >
      <div class="space-y-4">
        <p class="text-sm text-navy-500 dark:text-navy-400">
          仅可绑定与本人身份证号一致的账户，绑定后该卡会出现在当前用户名下。
        </p>
        <Input
          v-model="bindForm.userIdCard"
          label="身份证号"
          placeholder="请输入 18 位身份证号"
          required
        />
        <Input
          v-model="bindForm.accountNumber"
          label="银行卡号"
          placeholder="请输入要绑定的银行卡号"
          required
        />
        <p
          v-if="bindError"
          class="text-sm text-danger-600 dark:text-danger-400"
          role="alert"
        >
          {{ bindError }}
        </p>
      </div>

      <template #footer>
        <Button
          variant="ghost"
          :disabled="bindSaving"
          @click="bindOpen = false"
        >
          取消
        </Button>
        <Button
          :loading="bindSaving"
          @click="submitBind"
        >
          确认绑定
        </Button>
      </template>
    </Modal>
  </div>
</template>

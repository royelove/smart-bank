<script setup lang="ts">
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  ArrowDownToLine,
  ArrowRightLeft,
  ArrowUpFromLine,
  ChevronLeft,
  ChevronRight,
  RefreshCw,
  Settings2,
  TrendingUp,
} from 'lucide-vue-next'
import { Alert, Badge, BalanceHero, Button, Card, EmptyState, Input, Table } from '@/components/ui'
import { PageHeader, SectionNav } from '@/components/layout'
import { usePageMeta } from '@/composables/usePageMeta'
import { deposit, getTrades, transfer, withdraw } from '@/api/account'
import { getUserAccounts } from '@/api/user'
import {
  accountStatusLabel,
  accountStatusTone,
  accountTypeLabel,
  isAccountActive,
} from '@/utils/account'
import { formatDateTime, formatMoney, maskAccountNumber } from '@/utils/format'
import { TRADE_TYPES, tradeCounterparty, tradeDirection, tradeTone } from '@/utils/trade'
import type { Account, Transaction } from '@/types'

const route = useRoute()
const { title, subtitle } = usePageMeta()

const accountNumber = computed(() => String(route.params.accountNumber ?? ''))

const account = ref<Account | null>(null)
const accountLoading = ref(true)
const notFound = ref(false)

/* ---------------------------------- actions --------------------------------- */

type ActionId = 'transfer' | 'deposit' | 'withdraw'

const actionTabs = [
  { id: 'transfer', label: '转账汇款', icon: ArrowRightLeft },
  { id: 'deposit', label: '存款', icon: ArrowDownToLine },
  { id: 'withdraw', label: '取款', icon: ArrowUpFromLine },
]

const action = ref<ActionId>('transfer')
const actionLoading = ref(false)
const actionError = ref('')
const actionSuccess = ref('')

const transferForm = reactive({ inNumber: '', amount: '', password: '' })
const depositForm = reactive({ amount: '', password: '' })
const withdrawForm = reactive({ amount: '', password: '' })

function newRequestId(): string {
  const cryptoRef = globalThis.crypto
  if (cryptoRef && typeof cryptoRef.randomUUID === 'function') {
    return cryptoRef.randomUUID()
  }
  return `req-${Date.now()}-${Math.random().toString(16).slice(2, 10)}`
}

function validateAmount(raw: string): string {
  const amount = Number(raw)
  if (!raw.trim() || !Number.isFinite(amount) || amount <= 0) return '请输入大于 0 的金额'
  return ''
}

function validatePassword(password: string): string {
  return /^\d{6}$/.test(password) ? '' : '交易密码为 6 位数字'
}

function resetForms() {
  transferForm.inNumber = ''
  transferForm.amount = ''
  transferForm.password = ''
  depositForm.amount = ''
  depositForm.password = ''
  withdrawForm.amount = ''
  withdrawForm.password = ''
}

function setAction(next: string) {
  action.value = next as ActionId
  actionError.value = ''
  actionSuccess.value = ''
}

/** Shared guard: the backend rejects every money movement on a frozen account. */
function guardActive(): string {
  if (!account.value) return '账户信息未加载'
  if (!isAccountActive(account.value)) return '该账户已冻结，无法进行交易'
  return ''
}

async function submitTransfer() {
  actionError.value = ''
  actionSuccess.value = ''
  const problem =
    guardActive() ||
    (!transferForm.inNumber.replace(/\s/g, '') ? '请输入收款账号' : '') ||
    validateAmount(transferForm.amount) ||
    validatePassword(transferForm.password)
  if (problem) {
    actionError.value = problem
    return
  }
  actionLoading.value = true
  try {
    await transfer({
      outNumber: accountNumber.value,
      inNumber: transferForm.inNumber.replace(/\s/g, ''),
      amount: Number(transferForm.amount),
      password: transferForm.password,
      requestId: newRequestId(),
    })
    actionSuccess.value = '转账已提交，账户余额已更新'
    resetForms()
    await Promise.all([loadAccount(), loadTrades()])
  } catch (e) {
    actionError.value = (e as Error).message || '转账失败'
  } finally {
    actionLoading.value = false
  }
}

async function submitDeposit() {
  actionError.value = ''
  actionSuccess.value = ''
  const problem = guardActive() || validateAmount(depositForm.amount) || validatePassword(depositForm.password)
  if (problem) {
    actionError.value = problem
    return
  }
  actionLoading.value = true
  try {
    await deposit({
      accountNo: accountNumber.value,
      amount: Number(depositForm.amount),
      password: depositForm.password,
      requestId: newRequestId(),
    })
    actionSuccess.value = '存款成功，账户余额已更新'
    resetForms()
    await Promise.all([loadAccount(), loadTrades()])
  } catch (e) {
    actionError.value = (e as Error).message || '存款失败'
  } finally {
    actionLoading.value = false
  }
}

async function submitWithdraw() {
  actionError.value = ''
  actionSuccess.value = ''
  const problem = guardActive() || validateAmount(withdrawForm.amount) || validatePassword(withdrawForm.password)
  if (problem) {
    actionError.value = problem
    return
  }
  actionLoading.value = true
  try {
    await withdraw({
      accountNumber: accountNumber.value,
      amount: Number(withdrawForm.amount),
      password: withdrawForm.password,
    })
    actionSuccess.value = '取款成功，账户余额已更新'
    resetForms()
    await Promise.all([loadAccount(), loadTrades()])
  } catch (e) {
    actionError.value = (e as Error).message || '取款失败'
  } finally {
    actionLoading.value = false
  }
}

/* ---------------------------------- trades ---------------------------------- */

const filters = reactive({ type: '', startDate: '', endDate: '' })
const page = ref(1)
const size = ref(10)
const total = ref(0)
const pages = ref(0)
const trades = ref<Transaction[]>([])
const tradesLoading = ref(false)
const tradesError = ref('')

const typeFilters = [
  { id: '', label: '全部' },
  ...TRADE_TYPES.map((type) => ({ id: type, label: type })),
]

const columns = [
  {
    key: 'tradeDate',
    header: '交易时间',
    width: '170px',
    render: (row: Transaction) => formatDateTime(row.tradeDate),
  },
  {
    key: 'tradeType',
    header: '类型',
    width: '130px',
    render: (row: Transaction) => h(Badge, { variant: tradeTone(row.tradeType) }, () => row.tradeType || '交易'),
  },
  {
    key: 'counterparty',
    header: '对方账户',
    render: (row: Transaction) => {
      const counterparty = tradeCounterparty(row, accountNumber.value)
      return counterparty ? maskAccountNumber(counterparty) : '—'
    },
  },
  {
    key: 'amount',
    header: '金额',
    width: '170px',
    align: 'right' as const,
    render: (row: Transaction) => {
      const isOut = tradeDirection(row, accountNumber.value) === 'out'
      return h(
        'span',
        {
          class: isOut
            ? 'font-mono font-semibold text-danger-600 dark:text-danger-400'
            : 'font-mono font-semibold text-success-600 dark:text-success-400',
        },
        `${isOut ? '-' : '+'}${formatMoney(row.amount)}`,
      )
    },
  },
  {
    key: 'tradeNo',
    header: '流水号',
    width: '210px',
    render: (row: Transaction) =>
      h('span', { class: 'font-mono text-xs text-navy-500 dark:text-navy-400' }, row.tradeNo || '—'),
  },
]

async function loadAccount() {
  accountLoading.value = true
  try {
    const list: Account[] = (await getUserAccounts()) ?? []
    const found = list.find((item) => item.accountNumber === accountNumber.value)
    account.value = found ?? null
    notFound.value = !found
  } catch (e) {
    notFound.value = true
    tradesError.value = (e as Error).message || '账户信息加载失败'
  } finally {
    accountLoading.value = false
  }
}

async function loadTrades() {
  if (!accountNumber.value) return
  tradesLoading.value = true
  tradesError.value = ''
  try {
    const result = await getTrades({
      accountNumber: accountNumber.value,
      page: page.value,
      size: size.value,
      type: filters.type || undefined,
      startDate: filters.startDate || undefined,
      endDate: filters.endDate || undefined,
    })
    trades.value = result?.records ?? []
    total.value = Number(result?.total ?? 0)
    pages.value = Number(result?.pages ?? 0)
  } catch (e) {
    tradesError.value = (e as Error).message || '交易明细加载失败'
    trades.value = []
    total.value = 0
    pages.value = 0
  } finally {
    tradesLoading.value = false
  }
}

function applyFilters() {
  page.value = 1
  loadTrades()
}

function resetFilters() {
  filters.type = ''
  filters.startDate = ''
  filters.endDate = ''
  applyFilters()
}

function goToPage(next: number) {
  if (next < 1 || (pages.value && next > pages.value)) return
  page.value = next
  loadTrades()
}

function refreshAll() {
  loadAccount()
  loadTrades()
}

watch(accountNumber, () => {
  page.value = 1
  filters.type = ''
  filters.startDate = ''
  filters.endDate = ''
  resetForms()
  refreshAll()
})

onMounted(() => {
  loadAccount()
  loadTrades()
})
</script>

<template>
  <div class="app-container">
    <PageHeader
      :title="title"
      :subtitle="account
        ? `${accountTypeLabel(account.accountType)} · ${maskAccountNumber(account.accountNumber)} · ${account.accountHolderName || '持卡人'}`
        : subtitle"
    >
      <template #actions>
        <RouterLink to="/user/dashboard">
          <Button variant="ghost">
            <ChevronLeft class="w-4 h-4" />
            我的银行卡
          </Button>
        </RouterLink>
        <Button
          variant="ghost"
          size="icon"
          :disabled="tradesLoading"
          title="刷新"
          @click="refreshAll"
        >
          <RefreshCw
            class="w-5 h-5"
            :class="{ 'animate-spin': tradesLoading || accountLoading }"
          />
        </Button>
      </template>
    </PageHeader>

    <EmptyState
      v-if="notFound && !accountLoading"
      title="未找到该账户"
      description="该账户不在当前用户名下，或已被解绑。请从我的银行卡列表重新进入。"
    >
      <template #action>
        <RouterLink to="/user/dashboard">
          <Button>返回我的银行卡</Button>
        </RouterLink>
      </template>
    </EmptyState>

    <template v-else>
      <!-- Account summary -->
      <BalanceHero
        label="账户余额"
        :value="accountLoading ? '——' : formatMoney(account?.accountBalance)"
        class="mb-6"
      >
        <template #meta>
          <p class="hero-meta mt-2 font-mono">
            {{ account ? maskAccountNumber(account.accountNumber) : '——' }}
          </p>
        </template>
        <template #eyebrow>
          <div
            v-if="account"
            class="flex flex-wrap items-center gap-2"
          >
            <Badge variant="neutral">
              {{ accountTypeLabel(account.accountType) }}
            </Badge>
            <Badge :variant="accountStatusTone(account.accountStatus)">
              {{ accountStatusLabel(account.accountStatus) }}
            </Badge>
          </div>
        </template>

        <template #actions>
          <RouterLink to="/market">
            <Button>
              <TrendingUp class="w-4 h-4" />
              购买理财
            </Button>
          </RouterLink>
          <RouterLink to="/settings">
            <Button variant="secondary">
              <Settings2 class="w-4 h-4" />
              修改交易密码
            </Button>
          </RouterLink>
        </template>

        <Alert
          v-if="account && !isAccountActive(account)"
          variant="warning"
          class="mt-5"
        >
          该账户当前为冻结状态，存款、取款、转账均会被拒绝。
        </Alert>
      </BalanceHero>

      <div class="layout-split">
        <SectionNav
          :model-value="action"
          title="账户操作"
          :items="actionTabs"
          @update:model-value="setAction"
        />

        <div class="min-w-0 space-y-6">
          <!-- Money movement -->
          <Card>
            <div class="card-body">
              <form
                v-if="action === 'transfer'"
                class="grid max-w-3xl gap-4 sm:grid-cols-2"
                @submit.prevent="submitTransfer"
              >
                <Input
                  v-model="transferForm.inNumber"
                  label="收款账号"
                  placeholder="请输入收款方银行卡号"
                  required
                />
                <Input
                  v-model="transferForm.amount"
                  label="转账金额"
                  type="number"
                  placeholder="0.00"
                  required
                />
                <Input
                  v-model="transferForm.password"
                  label="交易密码"
                  type="password"
                  placeholder="6 位数字密码"
                  hint="密码为 6 位数字"
                  required
                />
                <div class="flex items-end">
                  <Button
                    type="submit"
                    size="lg"
                    :loading="actionLoading"
                  >
                    确认转账
                  </Button>
                </div>
              </form>

              <form
                v-else-if="action === 'deposit'"
                class="grid max-w-3xl gap-4 sm:grid-cols-2"
                @submit.prevent="submitDeposit"
              >
                <Input
                  v-model="depositForm.amount"
                  label="存款金额"
                  type="number"
                  placeholder="0.00"
                  required
                />
                <Input
                  v-model="depositForm.password"
                  label="交易密码"
                  type="password"
                  placeholder="6 位数字密码"
                  hint="密码为 6 位数字"
                  required
                />
                <div class="flex items-end sm:col-span-2">
                  <Button
                    type="submit"
                    size="lg"
                    :loading="actionLoading"
                  >
                    确认存款
                  </Button>
                </div>
              </form>

              <form
                v-else
                class="grid max-w-3xl gap-4 sm:grid-cols-2"
                @submit.prevent="submitWithdraw"
              >
                <Input
                  v-model="withdrawForm.amount"
                  label="取款金额"
                  type="number"
                  placeholder="0.00"
                  required
                />
                <Input
                  v-model="withdrawForm.password"
                  label="交易密码"
                  type="password"
                  placeholder="6 位数字密码"
                  hint="密码为 6 位数字"
                  required
                />
                <div class="flex items-end sm:col-span-2">
                  <Button
                    type="submit"
                    size="lg"
                    :loading="actionLoading"
                  >
                    确认取款
                  </Button>
                </div>
              </form>

              <Alert
                v-if="actionError"
                variant="danger"
                class="mt-4"
              >
                {{ actionError }}
              </Alert>
              <Alert
                v-if="actionSuccess"
                variant="success"
                class="mt-4"
              >
                {{ actionSuccess }}
              </Alert>
            </div>
          </Card>

          <!-- Trade history -->
          <Card>
            <div class="card-header flex-col items-stretch gap-4 sm:flex-row sm:items-center">
              <h2 class="section-title">
                交易明细
              </h2>
              <span class="text-sm text-navy-500 dark:text-navy-400">
                共 {{ total }} 条记录
              </span>
            </div>

            <div class="card-body space-y-4">
              <!-- Filters -->
              <div class="flex flex-wrap items-end gap-3">
                <div class="flex flex-wrap items-center gap-2">
                  <button
                    v-for="filter in typeFilters"
                    :key="filter.id || 'all'"
                    type="button"
                    class="rounded-lg px-3 py-1.5 text-sm transition-colors"
                    :class="filters.type === filter.id
                      ? 'bg-primary-100 text-primary-700 dark:bg-primary-500/10 dark:text-primary-300'
                      : 'text-navy-500 hover:text-navy-800 dark:text-navy-400 dark:hover:text-navy-100'"
                    @click="filters.type = filter.id; applyFilters()"
                  >
                    {{ filter.label }}
                  </button>
                </div>

                <div class="flex flex-wrap items-end gap-3 sm:ml-auto">
                  <Input
                    v-model="filters.startDate"
                    type="date"
                    label="开始日期"
                    class="w-40"
                  />
                  <Input
                    v-model="filters.endDate"
                    type="date"
                    label="结束日期"
                    class="w-40"
                  />
                  <Button
                    variant="secondary"
                    @click="applyFilters"
                  >
                    查询
                  </Button>
                  <Button
                    variant="ghost"
                    @click="resetFilters"
                  >
                    重置
                  </Button>
                </div>
              </div>

              <Alert
                v-if="tradesError"
                variant="danger"
              >
                {{ tradesError }}
              </Alert>

              <Table
                :columns="columns"
                :data="trades"
                key-field="id"
                :loading="tradesLoading"
                :hoverable="true"
                :striped="true"
                empty-message="暂无交易记录"
              />

              <div
                v-if="pages > 1"
                class="flex items-center justify-between gap-3"
              >
                <span class="text-sm text-navy-500 dark:text-navy-400">
                  第 {{ page }} / {{ pages }} 页
                </span>
                <div class="flex items-center gap-2">
                  <Button
                    variant="secondary"
                    size="sm"
                    :disabled="page <= 1 || tradesLoading"
                    @click="goToPage(page - 1)"
                  >
                    <ChevronLeft class="w-4 h-4" />
                    上一页
                  </Button>
                  <Button
                    variant="secondary"
                    size="sm"
                    :disabled="page >= pages || tradesLoading"
                    @click="goToPage(page + 1)"
                  >
                    下一页
                    <ChevronRight class="w-4 h-4" />
                  </Button>
                </div>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  ArrowDownRight,
  ArrowRight,
  ArrowUpRight,
  CreditCard,
  LineChart as LineChartIcon,
  PieChart,
  Receipt,
  RefreshCw,
  Settings,
  TrendingUp,
} from 'lucide-vue-next'
import { Alert, Badge, BalanceHero, Button, Card, EmptyState, StatCard } from '@/components/ui'
import { PageHeader } from '@/components/layout'
import { usePageMeta } from '@/composables/usePageMeta'
import { useAuthStore } from '@/stores/auth'
import { getForecast, getTrades } from '@/api/account'
import { getUserAccounts } from '@/api/user'
import {
  accountStatusLabel,
  accountStatusTone,
  accountTypeLabel,
  isAccountActive,
  sumBalance,
} from '@/utils/account'
import { formatAmount, formatDateTime, formatMoney, maskAccountNumber, toNumber } from '@/utils/format'
import { signedAmount, tradeCounterparty, tradeDirection, tradeTone } from '@/utils/trade'
import { CHART_COLORS, CHART_PRIMARY_FILL, DoughnutChart, LineChart } from '@/utils/charts'
import type { Account, BalanceForecast, Transaction } from '@/types'

interface TradeRow extends Transaction {
  /** The account this row was loaded for, used to derive direction and labels. */
  accountNumber: string
}

const { title, subtitle } = usePageMeta()
const authStore = useAuthStore()

const loading = ref(true)
const error = ref('')
const accounts = ref<Account[]>([])
const trades = ref<TradeRow[]>([])
const forecast = ref<BalanceForecast | null>(null)
const forecastLoading = ref(false)

/** The account used for drill-down links and the balance forecast. */
const primaryAccount = computed<Account | undefined>(
  () => accounts.value.find(isAccountActive) ?? accounts.value[0],
)

const totalBalance = computed(() => sumBalance(accounts.value))
const activeCount = computed(() => accounts.value.filter(isAccountActive).length)

function timeOf(trade: TradeRow): number {
  const raw = trade.tradeDate
  if (!raw) return 0
  const date = new Date(String(raw).replace(' ', 'T'))
  return Number.isNaN(date.getTime()) ? 0 : date.getTime()
}

/** Trades loaded for the current calendar month only. */
const monthTrades = computed(() => {
  const now = new Date()
  const from = new Date(now.getFullYear(), now.getMonth(), 1).getTime()
  return trades.value.filter((trade) => timeOf(trade) >= from)
})

const monthIncome = computed(() =>
  monthTrades.value.reduce((sum, trade) => {
    const signed = signedAmount(trade, trade.accountNumber)
    return signed > 0 ? sum + signed : sum
  }, 0),
)

const monthExpense = computed(() =>
  monthTrades.value.reduce((sum, trade) => {
    const signed = signedAmount(trade, trade.accountNumber)
    return signed < 0 ? sum + Math.abs(signed) : sum
  }, 0),
)

const recentTrades = computed(() =>
  [...trades.value].sort((a, b) => timeOf(b) - timeOf(a)).slice(0, 6),
)

const monthLabel = computed(() => `${new Date().getMonth() + 1} 月`)

const distributionData = computed(() => ({
  labels: accounts.value.map(
    (account) => `${accountTypeLabel(account.accountType)} ${maskAccountNumber(account.accountNumber).slice(-4)}`,
  ),
  datasets: [
    {
      data: accounts.value.map((account) => toNumber(account.accountBalance)),
      backgroundColor: accounts.value.map((_, index) => CHART_COLORS[index % CHART_COLORS.length]),
      borderWidth: 0,
      hoverOffset: 4,
    },
  ],
}))

const forecastData = computed(() => {
  const points = forecast.value?.points ?? []
  return {
    labels: points.map((point) => String(point.date ?? '').slice(5)),
    datasets: [
      {
        label: '预测余额',
        data: points.map((point) => toNumber(point.balance)),
        borderColor: CHART_COLORS[0],
        backgroundColor: CHART_PRIMARY_FILL,
        fill: true,
        tension: 0.35,
        pointRadius: 0,
        pointHoverRadius: 4,
        borderWidth: 2,
      },
    ],
  }
})

const forecastOptions = {
  responsive: true,
  maintainAspectRatio: false,
  interaction: { mode: 'index' as const, intersect: false },
  plugins: {
    legend: { display: false },
    tooltip: {
      callbacks: {
        label: (ctx: { parsed: { y: number } }) => formatMoney(ctx.parsed.y),
      },
    },
  },
  scales: {
    x: { grid: { display: false }, ticks: { maxTicksLimit: 6 } },
    y: {
      grid: { color: 'rgba(123, 135, 148, 0.15)' },
      ticks: { maxTicksLimit: 4, callback: (value: string | number) => formatAmount(value, 0) },
    },
  },
}

const doughnutOptions = {
  responsive: true,
  maintainAspectRatio: false,
  cutout: '62%',
  plugins: {
    legend: { position: 'bottom' as const, labels: { boxWidth: 10, boxHeight: 10, usePointStyle: true } },
    tooltip: {
      callbacks: {
        label: (ctx: { label: string; parsed: number }) => `${ctx.label}: ${formatMoney(ctx.parsed)}`,
      },
    },
  },
}

const quickActions = computed(() => [
  {
    label: '转账汇款',
    hint: '行内实时到账',
    icon: CreditCard,
    to: primaryAccount.value ? `/trade/${primaryAccount.value.accountNumber}` : '/user/dashboard',
  },
  { label: '投资理财', hint: '理财与股票行情', icon: TrendingUp, to: '/market' },
  { label: '交易明细', hint: '按条件筛选流水', icon: Receipt, to: primaryAccount.value ? `/trade/${primaryAccount.value.accountNumber}` : '/user/dashboard' },
  { label: '账户设置', hint: '资料与安全', icon: Settings, to: '/settings' },
])

async function loadForecast(accountNumber?: string) {
  if (!accountNumber) {
    forecast.value = null
    return
  }
  forecastLoading.value = true
  try {
    forecast.value = await getForecast(accountNumber)
  } catch {
    // The forecast is a nice-to-have: a failure must not blank the dashboard.
    forecast.value = null
  } finally {
    forecastLoading.value = false
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const list: Account[] = (await getUserAccounts()) ?? []
    accounts.value = list

    // One recent-trades request per account, then merge. 200 rows comfortably
    // covers the current month for the month-income/expense totals as well.
    const pages = await Promise.all(
      list.map((account) =>
        getTrades({ accountNumber: account.accountNumber, page: 1, size: 200 })
          .then((page: { records?: Transaction[] }) =>
            (page?.records ?? []).map((trade) => ({ ...trade, accountNumber: account.accountNumber })),
          )
          .catch(() => [] as TradeRow[]),
      ),
    )
    trades.value = pages.flat()

    await loadForecast(primaryAccount.value?.accountNumber)
  } catch (e) {
    error.value = (e as Error).message || '数据加载失败'
  } finally {
    loading.value = false
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
        <RouterLink to="/market">
          <Button>
            <TrendingUp class="w-4 h-4" />
            去投资
          </Button>
        </RouterLink>
      </template>
    </PageHeader>

    <Alert
      v-if="error"
      variant="danger"
      class="mb-6"
    >
      {{ error }}
    </Alert>

    <!-- Hero: the one number a customer opens the app for. -->
    <BalanceHero
      label="总资产（人民币）"
      :value="formatMoney(totalBalance)"
      class="mb-6"
    >
      <template #meta>
        <p class="hero-meta mt-2">
          <span>{{ accounts.length }} 个账户 · {{ activeCount }} 个状态正常</span>
          <span>欢迎回来，{{ authStore.user?.userName || '客户' }}</span>
        </p>
      </template>

      <template #actions>
        <RouterLink
          v-if="primaryAccount"
          :to="`/trade/${primaryAccount.accountNumber}`"
        >
          <Button>
            交易中心
            <ArrowRight class="w-4 h-4" />
          </Button>
        </RouterLink>
        <RouterLink to="/user/dashboard">
          <Button variant="secondary">
            我的银行卡
          </Button>
        </RouterLink>
      </template>
    </BalanceHero>

    <!-- Key figures -->
    <section class="grid gap-4 sm:grid-cols-3 mb-6">
      <StatCard
        label="可用账户"
        :value="`${activeCount} / ${accounts.length}`"
        hint="状态正常的账户数"
        :icon="CreditCard"
        :loading="loading"
      />
      <StatCard
        :label="`${monthLabel}收入`"
        :value="formatMoney(monthIncome)"
        :trend="`${monthTrades.filter((t) => signedAmount(t, t.accountNumber) > 0).length} 笔入账`"
        trend-tone="up"
        :icon="ArrowDownRight"
        :loading="loading"
      />
      <StatCard
        :label="`${monthLabel}支出`"
        :value="formatMoney(monthExpense)"
        :trend="`${monthTrades.filter((t) => signedAmount(t, t.accountNumber) < 0).length} 笔出账`"
        trend-tone="down"
        :icon="ArrowUpRight"
        :loading="loading"
      />
    </section>

    <EmptyState
      v-if="!loading && !accounts.length"
      title="还没有绑定任何银行卡"
      description="绑定本人名下的银行卡后即可查看余额、交易流水与理财推荐。"
      class="mb-6"
    >
      <template #action>
        <RouterLink to="/user/dashboard">
          <Button>去绑定银行卡</Button>
        </RouterLink>
      </template>
    </EmptyState>

    <template v-else>
      <div class="layout-main-aside">
        <div class="min-w-0 space-y-6">
          <!-- Charts -->
          <section class="grid gap-6 lg:grid-cols-2">
            <Card>
              <div class="card-header">
                <h2 class="section-title">
                  <PieChart class="w-4 h-4 text-primary-600 dark:text-primary-400" />
                  资产分布
                </h2>
              </div>
              <div class="card-body">
                <div class="h-56">
                  <DoughnutChart
                    v-if="accounts.length"
                    :data="distributionData"
                    :options="doughnutOptions"
                  />
                  <div
                    v-else
                    class="h-full grid place-items-center text-sm text-navy-500 dark:text-navy-400"
                  >
                    暂无账户数据
                  </div>
                </div>
              </div>
            </Card>

            <Card>
              <div class="card-header">
                <h2 class="section-title">
                  <LineChartIcon class="w-4 h-4 text-primary-600 dark:text-primary-400" />
                  余额预测
                </h2>
                <RouterLink
                  v-if="primaryAccount"
                  :to="`/trade/${primaryAccount.accountNumber}`"
                  class="link-muted"
                >
                  {{ maskAccountNumber(primaryAccount.accountNumber) }}
                </RouterLink>
              </div>
              <div class="card-body">
                <div class="h-56">
                  <LineChart
                    v-if="forecast?.points?.length"
                    :data="forecastData"
                    :options="forecastOptions"
                  />
                  <div
                    v-else
                    class="h-full grid place-items-center text-sm text-navy-500 dark:text-navy-400"
                  >
                    {{ forecastLoading ? '正在计算未来余额…' : '暂无预测数据' }}
                  </div>
                </div>
                <p
                  v-if="forecast"
                  class="mt-3 flex flex-wrap items-center gap-2 text-sm"
                >
                  <Badge :variant="forecast.lowRiskWarning ? 'danger' : 'success'">
                    {{ forecast.lowRiskWarning ? '存在低余额风险' : '余额充足' }}
                  </Badge>
                  <span class="text-navy-500 dark:text-navy-400">
                    预测最低余额 {{ formatMoney(forecast.minForecastBalance) }}
                  </span>
                </p>
              </div>
            </Card>
          </section>

          <!-- Accounts + recent trades -->
          <section class="space-y-6">
            <Card>
              <div class="card-header">
                <h2 class="section-title">
                  我的账户
                </h2>
                <RouterLink
                  to="/user/dashboard"
                  class="link-muted"
                >
                  查看全部
                </RouterLink>
              </div>
              <div class="card-body divide-y divide-navy-100 dark:divide-navy-800">
                <RouterLink
                  v-for="account in accounts.slice(0, 4)"
                  :key="account.accountNumber"
                  :to="`/trade/${account.accountNumber}`"
                  class="flex items-center justify-between gap-3 py-3 first:pt-0 last:pb-0
                         transition-colors hover:bg-navy-50 dark:hover:bg-navy-800/50"
                >
                  <div class="flex min-w-0 items-center gap-3">
                    <div
                      class="grid h-10 w-10 shrink-0 place-items-center rounded-lg
                             bg-primary-50 text-primary-600 dark:bg-primary-500/10 dark:text-primary-400"
                    >
                      <CreditCard class="h-5 w-5" />
                    </div>
                    <div class="min-w-0">
                      <p class="truncate font-medium text-navy-900 dark:text-navy-50">
                        {{ account.accountHolderName || '持卡人' }} · {{ accountTypeLabel(account.accountType) }}
                      </p>
                      <p class="truncate font-mono text-xs text-navy-500 dark:text-navy-400">
                        {{ maskAccountNumber(account.accountNumber) }}
                      </p>
                    </div>
                  </div>
                  <div class="shrink-0 text-right">
                    <p class="font-mono font-semibold text-navy-900 dark:text-navy-50">
                      {{ formatMoney(account.accountBalance) }}
                    </p>
                    <Badge
                      :variant="accountStatusTone(account.accountStatus)"
                      class="mt-1 text-xs"
                    >
                      {{ accountStatusLabel(account.accountStatus) }}
                    </Badge>
                  </div>
                </RouterLink>
              </div>
            </Card>

            <Card>
              <div class="card-header">
                <h2 class="section-title">
                  最近交易
                </h2>
                <RouterLink
                  v-if="primaryAccount"
                  :to="`/trade/${primaryAccount.accountNumber}`"
                  class="link-muted"
                >
                  查看全部
                </RouterLink>
              </div>
              <div class="card-body">
                <div
                  v-if="loading"
                  class="space-y-3"
                >
                  <div
                    v-for="n in 4"
                    :key="n"
                    class="skeleton h-12 w-full"
                  />
                </div>
                <div
                  v-else-if="!recentTrades.length"
                  class="py-8 text-center text-sm text-navy-500 dark:text-navy-400"
                >
                  暂无交易记录
                </div>
                <div
                  v-else
                  class="divide-y divide-navy-100 dark:divide-navy-800"
                >
                  <div
                    v-for="trade in recentTrades"
                    :key="trade.id ?? `${trade.tradeNo}-${trade.accountNumber}`"
                    class="flex items-center justify-between gap-3 py-3 first:pt-0 last:pb-0"
                  >
                    <div class="flex min-w-0 items-center gap-3">
                      <div
                        class="grid h-9 w-9 shrink-0 place-items-center rounded-lg"
                        :class="tradeDirection(trade, trade.accountNumber) === 'out'
                          ? 'bg-danger-100 text-danger-600 dark:bg-danger-500/10 dark:text-danger-400'
                          : 'bg-success-100 text-success-600 dark:bg-success-500/10 dark:text-success-400'"
                      >
                        <component
                          :is="tradeDirection(trade, trade.accountNumber) === 'out' ? ArrowUpRight : ArrowDownRight"
                          class="h-4 w-4"
                        />
                      </div>
                      <div class="min-w-0">
                        <p class="truncate font-medium text-navy-900 dark:text-navy-50">
                          {{ trade.tradeType || '交易' }}
                          <Badge
                            :variant="tradeTone(trade.tradeType)"
                            class="ml-1 align-middle text-[11px]"
                          >
                            {{ tradeDirection(trade, trade.accountNumber) === 'out' ? '支出' : '收入' }}
                          </Badge>
                        </p>
                        <p class="truncate text-xs text-navy-500 dark:text-navy-400">
                          {{ formatDateTime(trade.tradeDate) }}
                          <template v-if="tradeCounterparty(trade, trade.accountNumber)">
                            · {{ maskAccountNumber(tradeCounterparty(trade, trade.accountNumber)) }}
                          </template>
                        </p>
                      </div>
                    </div>
                    <p
                      class="shrink-0 font-mono font-semibold"
                      :class="tradeDirection(trade, trade.accountNumber) === 'out'
                        ? 'text-danger-600 dark:text-danger-400'
                        : 'text-success-600 dark:text-success-400'"
                    >
                      {{ tradeDirection(trade, trade.accountNumber) === 'out' ? '-' : '+'
                      }}{{ formatMoney(trade.amount) }}
                    </p>
                  </div>
                </div>
              </div>
            </Card>
          </section>
        </div>

        <!-- Quick actions: a task menu in the aside -->
        <aside>
          <Card>
            <div class="card-header">
              <h2 class="section-title">
                常用功能
              </h2>
            </div>
            <div class="card-body space-y-2">
              <RouterLink
                v-for="action in quickActions"
                :key="action.label"
                :to="action.to"
                class="flex items-center gap-3 rounded-lg border border-navy-100 p-3
                       transition-colors hover:border-primary-300 hover:bg-primary-50
                       dark:border-navy-800 dark:hover:border-primary-800 dark:hover:bg-primary-900/20"
              >
                <span
                  class="grid h-9 w-9 shrink-0 place-items-center rounded-lg
                         bg-primary-50 text-primary-600 dark:bg-primary-500/10 dark:text-primary-400"
                >
                  <component
                    :is="action.icon"
                    class="h-5 w-5"
                  />
                </span>
                <span class="min-w-0">
                  <span class="block truncate text-sm font-medium text-navy-800 dark:text-navy-100">
                    {{ action.label }}
                  </span>
                  <span class="block truncate text-xs text-navy-500 dark:text-navy-400">
                    {{ action.hint }}
                  </span>
                </span>
              </RouterLink>
            </div>
          </Card>
        </aside>
      </div>
    </template>
  </div>
</template>

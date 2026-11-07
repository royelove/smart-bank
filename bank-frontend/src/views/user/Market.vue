<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
import type { Component } from 'vue'
import {
  BadgeDollarSign,
  Briefcase,
  Check,
  LineChart,
  PiggyBank,
  RefreshCw,
  Sparkles,
  TrendingUp,
  Wallet,
} from 'lucide-vue-next'
import { Alert, Badge, Button, Card, EmptyState, Input, Modal, StatCard, Table } from '@/components/ui'
import { PageHeader, SectionNav } from '@/components/layout'
import { usePageMeta } from '@/composables/usePageMeta'
import {
  getRecommendations,
  listHoldings,
  listProducts,
  purchaseProduct,
  redeemProduct,
} from '@/api/financial'
import { cancelOrder, listQuotes, myOrders, myPositions, placeOrder } from '@/api/stock'
import { getUserAccounts } from '@/api/user'
import { accountTypeLabel, isAccountActive } from '@/utils/account'
import { formatDate, formatDateTime, formatMoney, formatPercent, toNumber } from '@/utils/format'
import {
  changeClass,
  holdingStatusLabel,
  holdingStatusTone,
  orderDirectionLabel,
  orderDirectionTone,
  orderPriceTypeLabel,
  orderStatusLabel,
  orderStatusTone,
  productStatusLabel,
  productStatusTone,
} from '@/utils/market'
import type {
  Account,
  FinancialHolding,
  FinancialProduct,
  FinancialRecommend,
  Position,
  StockOrder,
  StockQuote,
} from '@/types'

const { title, subtitle } = usePageMeta()

/* ------------------------------- account scope ------------------------------ */

const accounts = ref<Account[]>([])
const activeAccountNumber = ref('')
const accountsLoading = ref(true)

const activeAccount = computed(() =>
  accounts.value.find((account) => account.accountNumber === activeAccountNumber.value),
)

const canTrade = computed(() => !!activeAccount.value && isAccountActive(activeAccount.value))

const accountHint = computed(() => {
  if (!activeAccountNumber.value) return '请选择交易账户'
  return canTrade.value ? '' : '该账户已冻结，无法购买或赎回'
})

/* ---------------------------------- shared ---------------------------------- */

function newRequestId(): string {
  const cryptoRef = globalThis.crypto
  if (cryptoRef && typeof cryptoRef.randomUUID === 'function') return cryptoRef.randomUUID()
  return `req-${Date.now()}-${Math.random().toString(16).slice(2, 10)}`
}

function validateAmount(raw: string, min = 0): string {
  const amount = Number(raw)
  if (!raw.trim() || !Number.isFinite(amount) || amount <= 0) return '请输入大于 0 的金额'
  if (min && amount < min) return `金额不得低于 ${formatMoney(min)}`
  return ''
}

function validatePassword(password: string): string {
  return /^\d{6}$/.test(password) ? '' : '交易密码为 6 位数字'
}

/* ----------------------------------- tabs ----------------------------------- */

type TabId = 'products' | 'holdings' | 'stocks'

const tab = ref<TabId>('products')
const tabs: { id: TabId; label: string; icon: Component }[] = [
  { id: 'products', label: '理财产品', icon: PiggyBank },
  { id: 'holdings', label: '我的理财', icon: Wallet },
  { id: 'stocks', label: '股票交易', icon: LineChart },
]

const activeTabLabel = computed(() => tabs.find((item) => item.id === tab.value)?.label ?? '')

/* --------------------------------- products --------------------------------- */

const products = ref<FinancialProduct[]>([])
const recommendations = ref<FinancialRecommend[]>([])
const productsLoading = ref(false)
const productsError = ref('')

const productColumns = [
  {
    key: 'productName',
    header: '产品名称',
    render: (row: FinancialProduct) => h('div', {}, [
      h('p', { class: 'font-medium text-navy-900 dark:text-navy-50' }, row.productName || '—'),
      h('p', { class: 'font-mono text-xs text-navy-500 dark:text-navy-400' }, row.productCode || ''),
    ]),
  },
  {
    key: 'annualReturnRate',
    header: '年化收益率',
    width: '130px',
    align: 'right' as const,
    render: (row: FinancialProduct) =>
      h('span', { class: 'font-mono font-semibold text-danger-600 dark:text-danger-400' }, formatPercent(row.annualReturnRate)),
  },
  {
    key: 'termDays',
    header: '期限',
    width: '100px',
    align: 'right' as const,
    render: (row: FinancialProduct) => (row.termDays ? `${row.termDays} 天` : '—'),
  },
  {
    key: 'minAmount',
    header: '起投金额',
    width: '140px',
    align: 'right' as const,
    render: (row: FinancialProduct) => h('span', { class: 'font-mono' }, formatMoney(row.minAmount)),
  },
  {
    key: 'remainingAmount',
    header: '剩余额度',
    width: '150px',
    align: 'right' as const,
    render: (row: FinancialProduct) =>
      h('span', { class: 'font-mono text-navy-600 dark:text-navy-300' }, formatMoney(row.remainingAmount)),
  },
  {
    key: 'status',
    header: '状态',
    width: '90px',
    align: 'center' as const,
    render: (row: FinancialProduct) =>
      h(Badge, { variant: productStatusTone(row.status) }, () => productStatusLabel(row.status)),
  },
  {
    key: 'actions',
    header: '操作',
    width: '110px',
    align: 'right' as const,
    render: (row: FinancialProduct) =>
      h(
        Button,
        {
          size: 'sm',
          variant: 'secondary',
          disabled: row.status !== 1 || !canTrade.value,
          onClick: () => openPurchase(row),
        },
        () => '购买',
      ),
  },
]

/* -------------------------------- my holdings ------------------------------- */

const holdings = ref<FinancialHolding[]>([])
const holdingsLoading = ref(false)
const holdingsError = ref('')

const holdingsColumns = [
  {
    key: 'productName',
    header: '产品名称',
    render: (row: FinancialHolding) =>
      h('span', { class: 'font-medium text-navy-900 dark:text-navy-50' }, row.productName || '—'),
  },
  {
    key: 'annualReturnRate',
    header: '年化收益率',
    width: '130px',
    align: 'right' as const,
    render: (row: FinancialHolding) =>
      h('span', { class: 'font-mono text-danger-600 dark:text-danger-400' }, formatPercent(row.annualReturnRate)),
  },
  { key: 'termDays', header: '期限', width: '90px', align: 'right' as const, render: (row: FinancialHolding) => (row.termDays ? `${row.termDays} 天` : '—') },
  {
    key: 'investAmount',
    header: '投资金额',
    width: '150px',
    align: 'right' as const,
    render: (row: FinancialHolding) => h('span', { class: 'font-mono' }, formatMoney(row.investAmount)),
  },
  {
    key: 'expectedReturn',
    header: '预期收益',
    width: '140px',
    align: 'right' as const,
    render: (row: FinancialHolding) =>
      h('span', { class: 'font-mono font-semibold text-success-600 dark:text-success-400' }, formatMoney(row.expectedReturn)),
  },
  { key: 'purchaseDate', header: '购买日期', width: '150px', render: (row: FinancialHolding) => formatDateTime(row.purchaseDate) },
  {
    key: 'status',
    header: '状态',
    width: '90px',
    align: 'center' as const,
    render: (row: FinancialHolding) =>
      h(Badge, { variant: holdingStatusTone(row.status) }, () => holdingStatusLabel(row.status)),
  },
  {
    key: 'actions',
    header: '操作',
    width: '110px',
    align: 'right' as const,
    render: (row: FinancialHolding) =>
      h(
        Button,
        {
          size: 'sm',
          variant: 'secondary',
          disabled: row.status !== 0 || !canTrade.value,
          onClick: () => openRedeem(row),
        },
        () => (row.status === 0 ? '赎回' : '已赎回'),
      ),
  },
]

const holdingInvested = computed(() =>
  holdings.value.filter((item) => item.status === 0).reduce((sum, item) => sum + toNumber(item.investAmount), 0),
)
const holdingExpected = computed(() =>
  holdings.value.filter((item) => item.status === 0).reduce((sum, item) => sum + toNumber(item.expectedReturn), 0),
)

/* ----------------------------------- stocks --------------------------------- */

const quotes = ref<StockQuote[]>([])
const quotesLoading = ref(false)
const positions = ref<Position[]>([])
const orders = ref<StockOrder[]>([])
const stockError = ref('')

const marketValue = computed(() => positions.value.reduce((sum, item) => sum + toNumber(item.marketValue), 0))
const floatingProfit = computed(() => positions.value.reduce((sum, item) => sum + toNumber(item.profitLoss), 0))

const quoteColumns = [
  {
    key: 'stockName',
    header: '股票',
    render: (row: StockQuote) => h('div', {}, [
      h('p', { class: 'font-medium text-navy-900 dark:text-navy-50' }, row.stockName || '—'),
      h('p', { class: 'font-mono text-xs text-navy-500 dark:text-navy-400' }, row.stockCode || ''),
    ]),
  },
  {
    key: 'currentPrice',
    header: '现价',
    width: '110px',
    align: 'right' as const,
    render: (row: StockQuote) => h('span', { class: 'font-mono font-semibold' }, toNumber(row.currentPrice).toFixed(2)),
  },
  {
    key: 'prevClose',
    header: '昨收',
    width: '110px',
    align: 'right' as const,
    render: (row: StockQuote) => h('span', { class: 'font-mono text-navy-500 dark:text-navy-400' }, toNumber(row.prevClose).toFixed(2)),
  },
  {
    key: 'changePercent',
    header: '涨跌幅',
    width: '110px',
    align: 'right' as const,
    render: (row: StockQuote) =>
      h('span', { class: `font-mono font-semibold ${changeClass(row.changePercent)}` }, formatPercent(row.changePercent, 2, true)),
  },
  { key: 'quoteTime', header: '行情时间', width: '160px', render: (row: StockQuote) => formatDateTime(row.quoteTime) },
  {
    key: 'actions',
    header: '操作',
    width: '160px',
    align: 'right' as const,
    render: (row: StockQuote) =>
      h('div', { class: 'flex justify-end gap-2' }, [
        h(
          Button,
          { size: 'sm', variant: 'secondary', disabled: !canTrade.value, onClick: () => openStockOrder(row, 'BUY') },
          () => '买入',
        ),
        h(
          Button,
          { size: 'sm', variant: 'ghost', disabled: !canTrade.value, onClick: () => openStockOrder(row, 'SELL') },
          () => '卖出',
        ),
      ]),
  },
]

const positionColumns = [
  {
    key: 'stockName',
    header: '股票',
    render: (row: Position) => h('div', {}, [
      h('p', { class: 'font-medium text-navy-900 dark:text-navy-50' }, row.stockName || '—'),
      h('p', { class: 'font-mono text-xs text-navy-500 dark:text-navy-400' }, row.stockCode || ''),
    ]),
  },
  { key: 'quantity', header: '持仓数量', width: '120px', align: 'right' as const, render: (row: Position) => h('span', { class: 'font-mono' }, String(row.quantity ?? 0)) },
  {
    key: 'avgCost',
    header: '成本价',
    width: '110px',
    align: 'right' as const,
    render: (row: Position) => h('span', { class: 'font-mono' }, toNumber(row.avgCost).toFixed(2)),
  },
  {
    key: 'currentPrice',
    header: '现价',
    width: '110px',
    align: 'right' as const,
    render: (row: Position) => h('span', { class: 'font-mono' }, toNumber(row.currentPrice).toFixed(2)),
  },
  {
    key: 'marketValue',
    header: '市值',
    width: '140px',
    align: 'right' as const,
    render: (row: Position) => h('span', { class: 'font-mono' }, formatMoney(row.marketValue)),
  },
  {
    key: 'profitLoss',
    header: '浮动盈亏',
    width: '160px',
    align: 'right' as const,
    render: (row: Position) =>
      h('span', { class: `font-mono font-semibold ${changeClass(row.profitLoss)}` }, formatMoney(row.profitLoss)),
  },
  {
    key: 'profitLossPercent',
    header: '盈亏比例',
    width: '110px',
    align: 'right' as const,
    render: (row: Position) =>
      h('span', { class: `font-mono ${changeClass(row.profitLossPercent)}` }, formatPercent(row.profitLossPercent, 2, true)),
  },
]

const orderColumns = [
  { key: 'orderNo', header: '委托编号', width: '190px', render: (row: StockOrder) => h('span', { class: 'font-mono text-xs text-navy-500 dark:text-navy-400' }, row.orderNo || '—') },
  {
    key: 'stockName',
    header: '股票',
    render: (row: StockOrder) => h('div', {}, [
      h('p', { class: 'font-medium text-navy-900 dark:text-navy-50' }, row.stockName || '—'),
      h('p', { class: 'font-mono text-xs text-navy-500 dark:text-navy-400' }, row.stockCode || ''),
    ]),
  },
  {
    key: 'direction',
    header: '方向',
    width: '90px',
    align: 'center' as const,
    render: (row: StockOrder) =>
      h(Badge, { variant: orderDirectionTone(row.direction) }, () => orderDirectionLabel(row.direction)),
  },
  {
    key: 'priceType',
    header: '价格类型',
    width: '100px',
    align: 'center' as const,
    render: (row: StockOrder) => orderPriceTypeLabel(row.priceType),
  },
  {
    key: 'orderPrice',
    header: '委托价',
    width: '110px',
    align: 'right' as const,
    render: (row: StockOrder) => h('span', { class: 'font-mono' }, toNumber(row.orderPrice).toFixed(2)),
  },
  { key: 'orderQuantity', header: '数量', width: '100px', align: 'right' as const, render: (row: StockOrder) => h('span', { class: 'font-mono' }, String(row.orderQuantity ?? 0)) },
  {
    key: 'filledAmount',
    header: '成交金额',
    width: '140px',
    align: 'right' as const,
    render: (row: StockOrder) => h('span', { class: 'font-mono' }, formatMoney(row.filledAmount)),
  },
  { key: 'creationDate', header: '委托时间', width: '160px', render: (row: StockOrder) => formatDateTime(row.creationDate) },
  {
    key: 'status',
    header: '状态',
    width: '100px',
    align: 'center' as const,
    render: (row: StockOrder) =>
      h(Badge, { variant: orderStatusTone(row.status) }, () => orderStatusLabel(row.status)),
  },
  {
    key: 'actions',
    header: '操作',
    width: '100px',
    align: 'right' as const,
    render: (row: StockOrder) =>
      row.status === 0
        ? h(Button, { size: 'sm', variant: 'ghost', onClick: () => handleCancelOrder(row) }, () => '撤单')
        : h('span', { class: 'text-xs text-navy-400' }, '—'),
  },
]

/* --------------------------------- purchase --------------------------------- */

const purchaseOpen = ref(false)
const purchaseSaving = ref(false)
const purchaseError = ref('')
const purchaseTarget = ref<FinancialProduct | null>(null)
const purchaseForm = reactive({ amount: '', password: '' })

const redeemOpen = ref(false)
const redeemSaving = ref(false)
const redeemError = ref('')
const redeemTarget = ref<FinancialHolding | null>(null)
const redeemForm = reactive({ password: '' })

const orderOpen = ref(false)
const orderSaving = ref(false)
const orderError = ref('')
const orderTarget = ref<StockQuote | null>(null)
const orderForm = reactive({ direction: 'BUY', priceType: 'MARKET', price: '', quantity: '', password: '' })

const orderAmount = computed(() => {
  const price = orderForm.priceType === 'MARKET'
    ? toNumber(orderTarget.value?.currentPrice)
    : toNumber(orderForm.price)
  return price * toNumber(orderForm.quantity)
})

function openPurchase(product: FinancialProduct) {
  purchaseTarget.value = product
  purchaseForm.amount = ''
  purchaseForm.password = ''
  purchaseError.value = ''
  purchaseOpen.value = true
}

async function submitPurchase() {
  purchaseError.value = ''
  const product = purchaseTarget.value
  if (!product) return
  const problem =
    (!canTrade.value ? '该账户已冻结，无法购买' : '') ||
    validateAmount(purchaseForm.amount, toNumber(product.minAmount)) ||
    validatePassword(purchaseForm.password)
  if (problem) {
    purchaseError.value = problem
    return
  }
  purchaseSaving.value = true
  try {
    await purchaseProduct({
      accountNumber: activeAccountNumber.value,
      productId: product.id,
      amount: Number(purchaseForm.amount),
      password: purchaseForm.password,
      requestId: newRequestId(),
    })
    purchaseOpen.value = false
    await Promise.all([loadProducts(), loadAccountData()])
    tab.value = 'holdings'
  } catch (e) {
    purchaseError.value = (e as Error).message || '购买失败'
  } finally {
    purchaseSaving.value = false
  }
}

function openRedeem(holding: FinancialHolding) {
  redeemTarget.value = holding
  redeemForm.password = ''
  redeemError.value = ''
  redeemOpen.value = true
}

async function submitRedeem() {
  redeemError.value = ''
  const holding = redeemTarget.value
  if (!holding) return
  const problem = (!canTrade.value ? '该账户已冻结，无法赎回' : '') || validatePassword(redeemForm.password)
  if (problem) {
    redeemError.value = problem
    return
  }
  redeemSaving.value = true
  try {
    await redeemProduct({
      accountNumber: activeAccountNumber.value,
      holdingId: holding.id,
      password: redeemForm.password,
    })
    redeemOpen.value = false
    await Promise.all([loadProducts(), loadAccountData()])
  } catch (e) {
    redeemError.value = (e as Error).message || '赎回失败'
  } finally {
    redeemSaving.value = false
  }
}

function openStockOrder(quote: StockQuote, direction: 'BUY' | 'SELL') {
  orderTarget.value = quote
  orderForm.direction = direction
  orderForm.priceType = 'MARKET'
  orderForm.price = toNumber(quote.currentPrice).toFixed(2)
  orderForm.quantity = '100'
  orderForm.password = ''
  orderError.value = ''
  orderOpen.value = true
}

async function submitStockOrder() {
  orderError.value = ''
  const quote = orderTarget.value
  if (!quote) return
  const quantity = Number(orderForm.quantity)
  const problem =
    (!canTrade.value ? '该账户已冻结，无法下单' : '') ||
    (!Number.isInteger(quantity) || quantity <= 0 ? '委托数量必须为正整数' : '') ||
    (Number.isInteger(quantity) && quantity % 100 !== 0 ? '委托数量必须为 100 的整数倍' : '') ||
    (orderForm.priceType === 'LIMIT' && toNumber(orderForm.price) <= 0 ? '请输入有效的委托价格' : '') ||
    validatePassword(orderForm.password)
  if (problem) {
    orderError.value = problem
    return
  }
  orderSaving.value = true
  try {
    await placeOrder({
      accountNumber: activeAccountNumber.value,
      stockCode: quote.stockCode,
      direction: orderForm.direction,
      priceType: orderForm.priceType,
      price: orderForm.priceType === 'LIMIT' ? Number(orderForm.price) : null,
      quantity,
      password: orderForm.password,
    })
    orderOpen.value = false
    await loadAccountData()
    await loadQuotes()
  } catch (e) {
    orderError.value = (e as Error).message || '委托失败'
  } finally {
    orderSaving.value = false
  }
}

const cancelError = ref('')

async function handleCancelOrder(order: StockOrder) {
  cancelError.value = ''
  try {
    await cancelOrder(order.id, activeAccountNumber.value)
    await loadAccountData()
  } catch (e) {
    cancelError.value = (e as Error).message || '撤单失败'
  }
}

/* ---------------------------------- loading --------------------------------- */

async function loadProducts() {
  productsLoading.value = true
  productsError.value = ''
  try {
    products.value = (await listProducts()) ?? []
  } catch (e) {
    productsError.value = (e as Error).message || '理财产品加载失败'
    products.value = []
  } finally {
    productsLoading.value = false
  }
}

async function loadQuotes() {
  quotesLoading.value = true
  try {
    quotes.value = (await listQuotes()) ?? []
  } catch (e) {
    stockError.value = (e as Error).message || '行情加载失败'
    quotes.value = []
  } finally {
    quotesLoading.value = false
  }
}

/** Everything that is scoped to the selected account. */
async function loadAccountData() {
  const accountNumber = activeAccountNumber.value
  if (!accountNumber) {
    holdings.value = []
    positions.value = []
    orders.value = []
    recommendations.value = []
    return
  }

  holdingsLoading.value = true
  holdingsError.value = ''
  stockError.value = ''

  const [holdingsResult, recommendResult, positionsResult, ordersResult] = await Promise.all([
    listHoldings(accountNumber).catch((e: Error) => {
      holdingsError.value = e.message || '理财持仓加载失败'
      return [] as FinancialHolding[]
    }),
    getRecommendations(accountNumber, 3).catch(() => [] as FinancialRecommend[]),
    myPositions(accountNumber).catch((e: Error) => {
      stockError.value = e.message || '持仓加载失败'
      return [] as Position[]
    }),
    myOrders(accountNumber).catch((e: Error) => {
      stockError.value = e.message || '委托记录加载失败'
      return [] as StockOrder[]
    }),
  ])

  holdings.value = holdingsResult ?? []
  recommendations.value = recommendResult ?? []
  positions.value = positionsResult ?? []
  orders.value = ordersResult ?? []
  holdingsLoading.value = false
}

async function loadAccounts() {
  accountsLoading.value = true
  try {
    accounts.value = (await getUserAccounts()) ?? []
    if (!accounts.value.some((account) => account.accountNumber === activeAccountNumber.value)) {
      activeAccountNumber.value =
        accounts.value.find(isAccountActive)?.accountNumber ?? accounts.value[0]?.accountNumber ?? ''
    }
  } catch {
    accounts.value = []
  } finally {
    accountsLoading.value = false
  }
}

function refreshAll() {
  loadAccounts()
  loadProducts()
  loadQuotes()
  loadAccountData()
}

onMounted(async () => {
  loadProducts()
  loadQuotes()
  await loadAccounts()
  await loadAccountData()
})
</script>

<template>
  <div class="app-container">
    <PageHeader
      :title="title"
      :subtitle="subtitle"
    >
      <template #actions>
        <div class="flex items-center gap-2">
          <label
            class="text-sm text-navy-500 dark:text-navy-400"
            for="market-account"
          >交易账户</label>
          <select
            id="market-account"
            v-model="activeAccountNumber"
            class="input w-64 font-mono text-sm"
            :disabled="accountsLoading || !accounts.length"
            @change="loadAccountData"
          >
            <option
              v-if="!accounts.length"
              value=""
            >
              {{ accountsLoading ? '加载中…' : '暂无可用账户' }}
            </option>
            <option
              v-for="account in accounts"
              :key="account.accountNumber"
              :value="account.accountNumber"
            >
              {{ accountTypeLabel(account.accountType) }} · {{ account.accountNumber }}
              {{ isAccountActive(account) ? '' : '（冻结）' }}
            </option>
          </select>
        </div>
        <Button
          variant="ghost"
          size="icon"
          title="刷新"
          @click="refreshAll"
        >
          <RefreshCw class="w-5 h-5" />
        </Button>
      </template>
    </PageHeader>

    <EmptyState
      v-if="!accountsLoading && !accounts.length"
      title="需要先绑定银行卡"
      description="理财产品与股票交易都需要一个可用账户，请先在“我的银行卡”完成绑定。"
    >
      <template #action>
        <RouterLink to="/user/dashboard">
          <Button>去绑定银行卡</Button>
        </RouterLink>
      </template>
    </EmptyState>

    <template v-else>
      <Alert
        v-if="accountHint"
        variant="warning"
        class="mb-6"
      >
        {{ accountHint }}
      </Alert>

      <div class="layout-split">
        <SectionNav
          v-model="tab"
          title="交易"
          :items="tabs"
        />

        <div class="min-w-0 space-y-6">
          <section class="grid gap-4 sm:grid-cols-2">
            <StatCard
              label="理财持仓金额"
              :value="formatMoney(holdingInvested)"
              :hint="`${holdings.filter((item) => item.status === 0).length} 笔持有中`"
              :icon="PiggyBank"
              :loading="holdingsLoading"
            />
            <StatCard
              label="理财预期收益"
              :value="formatMoney(holdingExpected)"
              hint="按当前年化收益率估算"
              :icon="BadgeDollarSign"
              :loading="holdingsLoading"
            />
            <StatCard
              label="股票市值"
              :value="formatMoney(marketValue)"
              :hint="`${positions.length} 只持仓`"
              :icon="Briefcase"
              :loading="holdingsLoading"
            />
            <StatCard
              label="浮动盈亏"
              :value="formatMoney(floatingProfit)"
              hint="按最新价计算"
              :icon="TrendingUp"
              :loading="holdingsLoading"
            />
          </section>

          <Card>
            <div class="card-header">
              <h2 class="section-title">
                {{ activeTabLabel }}
              </h2>
              <span class="font-mono text-sm text-navy-500 dark:text-navy-400">
                {{ activeAccountNumber || '—' }}
              </span>
            </div>

            <div class="card-body">
              <!-- 理财产品 -->
              <div
                v-if="tab === 'products'"
                class="space-y-5"
              >
                <div v-if="recommendations.length">
                  <h3 class="section-title mb-3">
                    <Sparkles class="w-4 h-4 text-primary-600 dark:text-primary-400" />
                    智能推荐
                  </h3>
                  <div class="grid gap-4 lg:grid-cols-3">
                    <div
                      v-for="item in recommendations"
                      :key="item.product?.id ?? item.product?.productCode"
                      class="rounded-xl border border-primary-100 bg-primary-50/40 p-4
                             dark:border-primary-500/20 dark:bg-primary-500/5"
                    >
                      <div class="flex items-start justify-between gap-3">
                        <div class="min-w-0">
                          <p class="truncate font-medium text-navy-900 dark:text-navy-50">
                            {{ item.product?.productName || '—' }}
                          </p>
                          <p class="mt-1 text-xs text-navy-500 dark:text-navy-400">
                            风险等级 {{ item.userRiskLevel || '—' }}
                          </p>
                        </div>
                        <Badge variant="info">
                          匹配 {{ Math.round(toNumber(item.matchScore)) }} 分
                        </Badge>
                      </div>
                      <p class="mt-3 text-sm text-navy-600 dark:text-navy-300">
                        {{ item.reason || '综合您的风险偏好与资金情况推荐' }}
                      </p>
                      <div class="mt-3 flex items-center justify-between">
                        <span class="font-mono text-lg font-semibold text-danger-600 dark:text-danger-400">
                          {{ formatPercent(item.product?.annualReturnRate) }}
                        </span>
                        <Button
                          size="sm"
                          :disabled="!item.product || item.product.status !== 1 || !canTrade"
                          @click="item.product && openPurchase(item.product)"
                        >
                          购买
                        </Button>
                      </div>
                    </div>
                  </div>
                </div>

                <Alert
                  v-if="productsError"
                  variant="danger"
                >
                  {{ productsError }}
                </Alert>

                <Table
                  :columns="productColumns"
                  :data="products"
                  key-field="id"
                  :loading="productsLoading"
                  :striped="true"
                  empty-message="暂无在售理财产品"
                />
              </div>

              <!-- 我的理财 -->
              <div
                v-else-if="tab === 'holdings'"
                class="space-y-4"
              >
                <Alert
                  v-if="holdingsError"
                  variant="danger"
                >
                  {{ holdingsError }}
                </Alert>
                <Table
                  :columns="holdingsColumns"
                  :data="holdings"
                  key-field="id"
                  :loading="holdingsLoading"
                  :striped="true"
                  empty-message="暂无理财持仓"
                />
              </div>

              <!-- 股票交易 -->
              <div
                v-else
                class="space-y-8"
              >
                <Alert
                  v-if="stockError"
                  variant="danger"
                >
                  {{ stockError }}
                </Alert>

                <section>
                  <h3 class="section-title mb-3">
                    <LineChart class="w-4 h-4 text-primary-600 dark:text-primary-400" />
                    实时行情
                  </h3>
                  <Table
                    :columns="quoteColumns"
                    :data="quotes"
                    key-field="stockCode"
                    :loading="quotesLoading"
                    :striped="true"
                    empty-message="暂无行情数据"
                  />
                </section>

                <section>
                  <h3 class="section-title mb-3">
                    <Wallet class="w-4 h-4 text-primary-600 dark:text-primary-400" />
                    我的持仓
                  </h3>
                  <Table
                    :columns="positionColumns"
                    :data="positions"
                    key-field="id"
                    :loading="holdingsLoading"
                    :striped="true"
                    empty-message="暂无股票持仓"
                  />
                </section>

                <section>
                  <h3 class="section-title mb-3">
                    <Check class="w-4 h-4 text-primary-600 dark:text-primary-400" />
                    我的委托
                  </h3>
                  <Alert
                    v-if="cancelError"
                    variant="danger"
                    class="mb-3"
                  >
                    {{ cancelError }}
                  </Alert>
                  <Table
                    :columns="orderColumns"
                    :data="orders"
                    key-field="id"
                    :loading="holdingsLoading"
                    :striped="true"
                    empty-message="暂无委托记录"
                  />
                  <p class="mt-3 text-xs text-navy-500 dark:text-navy-400">
                    当前委托为即时成交模式，下单后状态直接变为“已成交”，因此通常不会出现可撤销的委托。
                  </p>
                </section>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </template>

    <!-- 购买理财 -->
    <Modal
      v-model="purchaseOpen"
      title="购买理财产品"
      size="md"
    >
      <div class="space-y-4">
        <div
          v-if="purchaseTarget"
          class="rounded-xl border border-navy-200 p-4 dark:border-navy-700"
        >
          <p class="font-medium text-navy-900 dark:text-navy-50">
            {{ purchaseTarget.productName }}
          </p>
          <div class="mt-2 grid grid-cols-3 gap-3 text-center">
            <div>
              <p class="font-mono font-semibold text-danger-600 dark:text-danger-400">
                {{ formatPercent(purchaseTarget.annualReturnRate) }}
              </p>
              <p class="text-xs text-navy-500 dark:text-navy-400">年化收益率</p>
            </div>
            <div>
              <p class="font-mono font-semibold text-navy-900 dark:text-navy-50">
                {{ purchaseTarget.termDays }} 天
              </p>
              <p class="text-xs text-navy-500 dark:text-navy-400">产品期限</p>
            </div>
            <div>
              <p class="font-mono font-semibold text-navy-900 dark:text-navy-50">
                {{ formatMoney(purchaseTarget.minAmount) }}
              </p>
              <p class="text-xs text-navy-500 dark:text-navy-400">起投金额</p>
            </div>
          </div>
        </div>

        <Input
          v-model="purchaseForm.amount"
          label="购买金额"
          type="number"
          placeholder="0.00"
          required
          :hint="purchaseTarget ? `不得低于 ${formatMoney(purchaseTarget.minAmount)}，剩余额度 ${formatMoney(purchaseTarget.remainingAmount)}` : ''"
        />
        <Input
          v-model="purchaseForm.password"
          label="交易密码"
          type="password"
          placeholder="6 位数字密码"
          required
        />
        <p
          v-if="purchaseError"
          class="text-sm text-danger-600 dark:text-danger-400"
          role="alert"
        >
          {{ purchaseError }}
        </p>
      </div>

      <template #footer>
        <Button
          variant="ghost"
          :disabled="purchaseSaving"
          @click="purchaseOpen = false"
        >
          取消
        </Button>
        <Button
          :loading="purchaseSaving"
          @click="submitPurchase"
        >
          确认购买
        </Button>
      </template>
    </Modal>

    <!-- 赎回 -->
    <Modal
      v-model="redeemOpen"
      title="赎回理财持仓"
      size="md"
    >
      <div class="space-y-4">
        <div
          v-if="redeemTarget"
          class="rounded-xl border border-navy-200 p-4 dark:border-navy-700"
        >
          <p class="font-medium text-navy-900 dark:text-navy-50">
            {{ redeemTarget.productName }}
          </p>
          <p class="mt-2 text-sm text-navy-500 dark:text-navy-400">
            投资金额 {{ formatMoney(redeemTarget.investAmount) }} · 预期收益
            {{ formatMoney(redeemTarget.expectedReturn) }} · 购买日期
            {{ formatDate(redeemTarget.purchaseDate) }}
          </p>
          <p class="mt-2 text-xs text-warning-600 dark:text-warning-400">
            未到期赎回将按提前赎回规则结算，收益可能与预期不同。
          </p>
        </div>

        <Input
          v-model="redeemForm.password"
          label="交易密码"
          type="password"
          placeholder="6 位数字密码"
          required
        />
        <p
          v-if="redeemError"
          class="text-sm text-danger-600 dark:text-danger-400"
          role="alert"
        >
          {{ redeemError }}
        </p>
      </div>

      <template #footer>
        <Button
          variant="ghost"
          :disabled="redeemSaving"
          @click="redeemOpen = false"
        >
          取消
        </Button>
        <Button
          :loading="redeemSaving"
          @click="submitRedeem"
        >
          确认赎回
        </Button>
      </template>
    </Modal>

    <!-- 股票下单 -->
    <Modal
      v-model="orderOpen"
      :title="orderForm.direction === 'BUY' ? '买入股票' : '卖出股票'"
      size="md"
    >
      <div class="space-y-4">
        <div
          v-if="orderTarget"
          class="flex items-center justify-between rounded-xl border border-navy-200 p-4 dark:border-navy-700"
        >
          <div>
            <p class="font-medium text-navy-900 dark:text-navy-50">
              {{ orderTarget.stockName }}
            </p>
            <p class="font-mono text-xs text-navy-500 dark:text-navy-400">
              {{ orderTarget.stockCode }}
            </p>
          </div>
          <div class="text-right">
            <p class="font-mono font-semibold" :class="changeClass(orderTarget.changePercent)">
              {{ toNumber(orderTarget.currentPrice).toFixed(2) }}
            </p>
            <p class="text-xs text-navy-500 dark:text-navy-400">现价</p>
          </div>
        </div>

        <div>
          <p class="label">
            委托方向
          </p>
          <div class="flex gap-2">
            <button
              v-for="direction in ['BUY', 'SELL']"
              :key="direction"
              type="button"
              class="flex-1 rounded-lg border px-4 py-2 text-sm font-medium transition-colors"
              :class="orderForm.direction === direction
                ? 'border-primary-500 bg-primary-50 text-primary-700 dark:bg-primary-500/10 dark:text-primary-300'
                : 'border-navy-200 text-navy-500 dark:border-navy-700 dark:text-navy-400'"
              @click="orderForm.direction = direction"
            >
              {{ orderDirectionLabel(direction) }}
            </button>
          </div>
        </div>

        <div>
          <p class="label">
            价格类型
          </p>
          <div class="flex gap-2">
            <button
              v-for="type in ['MARKET', 'LIMIT']"
              :key="type"
              type="button"
              class="flex-1 rounded-lg border px-4 py-2 text-sm font-medium transition-colors"
              :class="orderForm.priceType === type
                ? 'border-primary-500 bg-primary-50 text-primary-700 dark:bg-primary-500/10 dark:text-primary-300'
                : 'border-navy-200 text-navy-500 dark:border-navy-700 dark:text-navy-400'"
              @click="orderForm.priceType = type"
            >
              {{ orderPriceTypeLabel(type) }}
            </button>
          </div>
        </div>

        <Input
          v-model="orderForm.price"
          label="委托价格"
          type="number"
          :disabled="orderForm.priceType === 'MARKET'"
          :hint="orderForm.priceType === 'MARKET' ? '市价委托按最新价成交' : ''"
        />
        <Input
          v-model="orderForm.quantity"
          label="委托数量（股）"
          type="number"
          placeholder="100"
          hint="必须为 100 的整数倍"
          required
        />
        <Input
          v-model="orderForm.password"
          label="交易密码"
          type="password"
          placeholder="6 位数字密码"
          required
        />

        <div class="flex items-center justify-between rounded-lg bg-navy-50 px-4 py-3 dark:bg-navy-800/50">
          <span class="text-sm text-navy-600 dark:text-navy-300">预计成交金额</span>
          <span class="font-mono font-semibold text-navy-900 dark:text-navy-50">
            {{ formatMoney(orderAmount) }}
          </span>
        </div>

        <p
          v-if="orderError"
          class="text-sm text-danger-600 dark:text-danger-400"
          role="alert"
        >
          {{ orderError }}
        </p>
      </div>

      <template #footer>
        <Button
          variant="ghost"
          :disabled="orderSaving"
          @click="orderOpen = false"
        >
          取消
        </Button>
        <Button
          :loading="orderSaving"
          @click="submitStockOrder"
        >
          确认委托
        </Button>
      </template>
    </Modal>
  </div>
</template>

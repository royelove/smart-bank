<template>
  <div class="stock-trading">
    <div v-if="!props.accountNumber" class="no-card-wrap">
      <el-empty description="请先选择银行卡再交易" />
    </div>

    <el-tabs v-else v-model="activeTab">
      <el-tab-pane label="交易" name="trade">
        <el-form label-width="0" class="trade-form">
          <div class="direction-toggle">
            <el-radio-group v-model="direction" class="dir-group">
              <el-radio-button :value="'BUY'">买入</el-radio-button>
              <el-radio-button :value="'SELL'">卖出</el-radio-button>
            </el-radio-group>
          </div>

          <div class="form-row">
            <label class="atm-label">股票</label>
            <el-select v-model="stockCode" placeholder="请选择股票" filterable size="large" style="width: 100%">
              <el-option
                v-for="s in stocks"
                :key="s.stockCode"
                :label="`${s.stockName} (${s.stockCode})`"
                :value="s.stockCode"
              />
            </el-select>
          </div>

          <div class="form-row">
            <label class="atm-label">委托类型</label>
            <el-radio-group v-model="priceType" class="pt-group">
              <el-radio-button :value="'MARKET'">市价</el-radio-button>
              <el-radio-button :value="'LIMIT'">限价</el-radio-button>
            </el-radio-group>
          </div>

          <div v-if="priceType === 'LIMIT'" class="form-row">
            <label class="atm-label">委托价格</label>
            <el-input-number
              v-model="price"
              :precision="2"
              :step="0.01"
              :min="0.01"
              size="large"
              style="width: 100%"
            />
          </div>

          <div class="form-row">
            <label class="atm-label">委托数量（100股的整数倍）</label>
            <el-input-number
              v-model="quantity"
              :step="100"
              :min="100"
              size="large"
              style="width: 100%"
            />
          </div>

          <div class="estimate">
            <div class="estimate-item">
              <span class="e-label">参考价</span>
              <span class="e-value vault-num">¥{{ (currentStock?.currentPrice ?? 0).toFixed(2) }}</span>
            </div>
            <div class="estimate-item">
              <span class="e-label">预计金额</span>
              <span class="e-value highlight vault-num">¥{{ estimateAmount.toFixed(2) }}</span>
            </div>
          </div>

          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :class="direction === 'BUY' ? 'buy-btn' : 'sell-btn'"
            @click="handleSubmit"
          >
            {{ direction === 'BUY' ? '确认买入' : '确认卖出' }}
          </el-button>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="持仓" name="positions">
        <el-table :data="positions" class="trade-table">
          <el-table-column label="股票" min-width="140">
            <template #default="{ row }">
              <div class="pos-stock">
                <span class="pos-name">{{ row.stockName }}</span>
                <span class="pos-code">{{ row.stockCode }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="持仓量" width="110" align="right">
            <template #default="{ row }">
              <span class="vault-num">{{ row.quantity }}</span>
            </template>
          </el-table-column>
          <el-table-column label="成本价" width="110" align="right">
            <template #default="{ row }">
              <span class="vault-num">¥{{ Number(row.avgCost).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="现价" width="110" align="right">
            <template #default="{ row }">
              <span class="vault-num">¥{{ Number(row.currentPrice).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="市值" width="130" align="right">
            <template #default="{ row }">
              <span class="vault-num">¥{{ Number(row.marketValue).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="浮动盈亏" width="150" align="right">
            <template #default="{ row }">
              <span class="vault-num" :class="Number(row.profitLoss) >= 0 ? 'up' : 'down'">
                {{ Number(row.profitLoss) >= 0 ? '+' : '' }}{{ Number(row.profitLoss).toFixed(2) }}
              </span>
              <div class="pct" :class="Number(row.profitLossPercent) >= 0 ? 'up' : 'down'">
                {{ Number(row.profitLossPercent) >= 0 ? '+' : '' }}{{ Number(row.profitLossPercent).toFixed(2) }}%
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="委托" name="orders">
        <el-table :data="orders" class="trade-table">
          <el-table-column label="委托号" width="200">
            <template #default="{ row }">
              <span class="order-no">{{ row.orderNo }}</span>
            </template>
          </el-table-column>
          <el-table-column label="股票" min-width="130">
            <template #default="{ row }">
              <span class="pos-name">{{ row.stockName }}</span>
            </template>
          </el-table-column>
          <el-table-column label="方向" width="80">
            <template #default="{ row }">
              <el-tag :type="row.direction === 'BUY' ? 'danger' : 'success'" size="small">
                {{ row.direction === 'BUY' ? '买入' : '卖出' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="委托价" width="110" align="right">
            <template #default="{ row }">
              <span class="vault-num">¥{{ Number(row.orderPrice).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="数量" width="90" align="right">
            <template #default="{ row }">
              <span class="vault-num">{{ row.orderQuantity }}</span>
            </template>
          </el-table-column>
          <el-table-column label="成交金额" width="130" align="right">
            <template #default="{ row }">
              <span class="vault-num">¥{{ Number(row.filledAmount).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <PasswordDialog
      :visible="passwordVisible"
      :title="passwordTitle"
      @update:visible="passwordVisible = $event"
      @confirm="handlePasswordConfirm"
      @cancel="passwordVisible = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { listStocks, listQuotes, placeOrder, myOrders, myPositions } from '@/api/stock'
import PasswordDialog from './PasswordDialog.vue'

const props = defineProps({
  accountNumber: { type: String, required: true }
})
const emit = defineEmits(['refreshBalance'])

const activeTab = ref('trade')
const direction = ref('BUY')
const priceType = ref('MARKET')
const stockCode = ref('')
const price = ref(null)
const quantity = ref(100)
const stocks = ref([])
const quotes = ref([])
const positions = ref([])
const orders = ref([])
const passwordVisible = ref(false)
const password = ref('')
const submitting = ref(false)

const currentStock = computed(() => quotes.value.find(q => q.stockCode === stockCode.value))
const estimateAmount = computed(() => {
  const p = priceType.value === 'LIMIT' ? price.value : currentStock.value?.currentPrice
  return (Number(p || 0) * Number(quantity.value || 0))
})
const passwordTitle = computed(() => `请输入交易密码以确认${direction.value === 'BUY' ? '买入' : '卖出'}`)

const loadStocks = async () => {
  try {
    const res = await listStocks()
    stocks.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

const loadQuotes = async () => {
  try {
    const res = await listQuotes()
    quotes.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

const loadPositions = async () => {
  if (!props.accountNumber) return
  try {
    const res = await myPositions(props.accountNumber)
    positions.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

const loadOrders = async () => {
  if (!props.accountNumber) return
  try {
    const res = await myOrders(props.accountNumber)
    orders.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

const onTabChange = (tab) => {
  if (tab === 'positions') loadPositions()
  if (tab === 'orders') loadOrders()
}

watch(activeTab, onTabChange)
watch(stockCode, () => {
  if (priceType.value === 'MARKET') price.value = null
})

const handleSubmit = () => {
  if (!stockCode.value) {
    ElMessage.warning('请先选择股票')
    return
  }
  if (priceType.value === 'LIMIT' && (!price.value || price.value <= 0)) {
    ElMessage.warning('请输入有效的委托价格')
    return
  }
  if (!quantity.value || quantity.value <= 0 || quantity.value % 100 !== 0) {
    ElMessage.warning('委托数量须为100股的整数倍')
    return
  }
  passwordVisible.value = true
}

const handlePasswordConfirm = async (pwd) => {
  password.value = pwd
  passwordVisible.value = false
  submitting.value = true
  try {
    await placeOrder({
      accountNumber: props.accountNumber,
      stockCode: stockCode.value,
      direction: direction.value,
      priceType: priceType.value,
      price: priceType.value === 'LIMIT' ? price.value : null,
      quantity: quantity.value,
      password: password.value,
      requestId: crypto.randomUUID()
    })
    ElMessage.success(`${direction.value === 'BUY' ? '买入' : '卖出'}成功`)
    emit('refreshBalance')
    loadQuotes()
    loadPositions()
    loadOrders()
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

const statusText = (s) => (s === 0 ? '待成交' : s === 1 ? '已成交' : '已撤单')
const statusType = (s) => (s === 0 ? 'warning' : s === 1 ? 'success' : 'info')

onMounted(() => {
  loadStocks()
  loadQuotes()
})
</script>

<style scoped>
.stock-trading {
  padding: var(--space-1) 0;
}

.no-card-wrap {
  padding: var(--space-16) 0;
}

.trade-form {
  max-width: 520px;
  margin: 0 auto;
}

.direction-toggle {
  text-align: center;
  margin-bottom: var(--space-6);
}

.dir-group .el-radio-button {
  width: 120px;
}

.form-row {
  margin-bottom: var(--space-4);
}

.atm-label {
  display: block;
  font-size: var(--font-sm);
  font-weight: 600;
  color: var(--color-muted-foreground);
  margin-bottom: var(--space-2);
  letter-spacing: 0.05em;
  font-family: var(--font-sans);
}

.pt-group .el-radio-button {
  width: 100px;
}

.estimate {
  display: flex;
  gap: var(--space-3);
  margin-bottom: var(--space-5);
  background: var(--color-card);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  padding: var(--space-4);
}

.estimate-item {
  flex: 1;
}

.e-label {
  display: block;
  font-size: var(--font-sm);
  color: var(--color-muted-foreground);
  margin-bottom: var(--space-1);
  font-family: var(--font-sans);
}

.e-value {
  font-size: var(--font-h3);
  font-weight: 700;
  color: var(--color-foreground);
  font-family: var(--font-mono);
}

.e-value.highlight {
  color: var(--color-primary);
}

.submit-btn {
  width: 100%;
  font-size: var(--font-base);
  letter-spacing: 0.1em;
  height: var(--space-12);
  font-family: var(--font-sans);
}

.buy-btn {
  background: var(--color-destructive);
  border: none;
  color: #fff;
}

.buy-btn:hover {
  background: color-mix(in srgb, var(--color-destructive) 85%, #000);
}

.sell-btn {
  background: var(--color-success);
  border: none;
  color: #fff;
}

.sell-btn:hover {
  background: color-mix(in srgb, var(--color-success) 85%, #000);
}

.trade-table {
  --el-table-header-bg-color: var(--color-card);
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-border-color: var(--glass-border);
  --el-table-text-color: var(--color-foreground);
  --el-table-header-text-color: var(--color-muted-foreground);
}

:deep(.el-table th.el-table__cell) {
  font-weight: 600;
  font-size: var(--font-sm);
  letter-spacing: 0.05em;
}

:deep(.el-table .el-table__row:hover > td) {
  background: var(--color-muted) !important;
}

.pos-stock {
  display: flex;
  flex-direction: column;
}

.pos-name {
  color: var(--color-foreground);
  font-weight: 600;
  font-family: var(--font-sans);
}

.pos-code {
  font-size: var(--font-sm);
  color: var(--color-muted-foreground);
  font-family: var(--font-mono);
}

.order-no {
  font-family: var(--font-mono);
  font-size: var(--font-sm);
  color: var(--color-muted-foreground);
}

.vault-num {
  font-family: var(--font-mono);
}

.pct {
  font-size: var(--font-sm);
  font-family: var(--font-mono);
}

.up {
  color: var(--color-success);
}

.down {
  color: var(--color-destructive);
}

:deep(.el-tabs__item) {
  color: var(--color-muted-foreground);
}

:deep(.el-tabs__item.is-active) {
  color: var(--color-primary);
}

:deep(.el-tabs__active-bar) {
  background-color: var(--color-primary);
}

:deep(.el-input__wrapper) {
  background: var(--color-card);
  border: 1px solid var(--glass-border);
  box-shadow: none;
}

:deep(.el-input__inner) {
  color: var(--color-foreground);
}

:deep(.el-select .el-input__wrapper) {
  background: var(--color-card);
}

:deep(.el-radio-button__inner) {
  background: var(--color-card);
  border-color: var(--glass-border);
  color: var(--color-muted-foreground);
}

:deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: var(--color-on-primary);
}

:deep(.el-tag--danger) {
  --el-tag-bg-color: color-mix(in srgb, var(--color-destructive) 15%, transparent);
  --el-tag-text-color: var(--color-destructive);
}

:deep(.el-tag--success) {
  --el-tag-bg-color: color-mix(in srgb, var(--color-success) 15%, transparent);
  --el-tag-text-color: var(--color-success);
}

:deep(.el-tag--warning) {
  --el-tag-bg-color: color-mix(in srgb, var(--color-primary) 15%, transparent);
  --el-tag-text-color: var(--color-primary);
}

:deep(.el-tag--info) {
  --el-tag-bg-color: var(--color-muted);
  --el-tag-text-color: var(--color-muted-foreground);
}
</style>
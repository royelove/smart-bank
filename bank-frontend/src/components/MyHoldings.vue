<template>
  <div class="portfolio">
    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="4" animated />
    </div>

    <template v-else-if="holdings.length === 0">
      <el-empty description="暂无持仓，快去选购理财产品吧" />
    </template>

    <template v-else>
      <!-- Portfolio Summary -->
      <div class="portfolio-summary">
        <div class="vault-contour"></div>
        <div class="summary-item">
          <span class="summary-label">持有产品</span>
          <span class="summary-value vault-num">{{ holdings.length }}</span>
        </div>
        <div class="summary-divider"></div>
        <div class="summary-item">
          <span class="summary-label">总投资金额</span>
          <span class="summary-value vault-num">¥{{ totalInvested }}</span>
        </div>
        <div class="summary-divider"></div>
        <div class="summary-item">
          <span class="summary-label">预期总收益</span>
          <span class="summary-value profit vault-num">+¥{{ totalReturn }}</span>
        </div>
        <div class="summary-divider"></div>
        <div class="summary-item">
          <span class="summary-label">预期总资产</span>
          <span class="summary-value vault-num">¥{{ totalValue }}</span>
        </div>
      </div>

      <!-- Holdings List -->
      <div class="portfolio-list">
        <div
          v-for="item in holdings"
          :key="item.id"
          class="portfolio-card"
          :class="{ redeemed: item.status === 1 }"
        >
          <div class="pc-header">
            <div class="pc-header-left">
              <div class="pc-icon" :class="item.status === 0 ? 'active' : ''">
                <span>{{ item.productName.charAt(0) }}</span>
              </div>
              <div>
                <h4 class="pc-name">{{ item.productName }}</h4>
                <span class="pc-status" :class="item.status === 0 ? 'holding' : 'done'">
                  {{ item.status === 0 ? '持有中' : '已赎回' }}
                </span>
              </div>
            </div>
            <div class="pc-header-right">
              <div class="pc-return">
                <span class="return-label">预期收益</span>
                <span class="return-value">+¥{{ item.expectedReturn }}</span>
              </div>
            </div>
          </div>

          <div class="pc-body">
            <div class="pc-metrics">
              <div class="metric">
                <span class="metric-label">投资金额</span>
                <span class="metric-value">¥{{ item.investAmount }}</span>
              </div>
              <div class="metric">
                <span class="metric-label">年化收益率</span>
                <span class="metric-value rate">{{ item.annualReturnRate }}%</span>
              </div>
              <div class="metric">
                <span class="metric-label">期限</span>
                <span class="metric-value">{{ item.termDays }}天</span>
              </div>
              <div class="metric">
                <span class="metric-label">购买日期</span>
                <span class="metric-value date">{{ formatDate(item.purchaseDate) }}</span>
              </div>
            </div>

            <!-- Progress Bar -->
            <div v-if="item.status === 0" class="pc-progress">
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: progressPercent(item) + '%' }"></div>
              </div>
              <div class="progress-info">
                <span>持有中</span>
                <span>{{ progressPercent(item) }}%</span>
              </div>
            </div>
          </div>

          <div class="pc-footer">
            <div v-if="item.status === 0" class="pc-footer-left">
            </div>
            <div v-if="item.status === 0" class="pc-footer-right">
              <el-button
                type="danger"
                plain
                size="small"
                :loading="redeemingId === item.id"
                @click="openRedeem(item)"
              >
                提前赎回
              </el-button>
            </div>
            <div v-else class="pc-footer-left">
              <span class="footer-hint redeemed-text">已赎回 · {{ formatDate(item.redeemDate) }}</span>
            </div>
          </div>
        </div>
      </div>
    </template>

    <PasswordDialog
      :visible="passwordVisible"
      title="请输入交易密码以确认赎回"
      @update:visible="passwordVisible = $event"
      @confirm="handleRedeemConfirm"
      @cancel="passwordVisible = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listHoldings, redeemProduct } from '@/api/financial'
import PasswordDialog from './PasswordDialog.vue'

const emit = defineEmits(['refreshBalance'])
const props = defineProps({
  accountNumber: { type: String, required: true }
})
const loading = ref(false)
const holdings = ref([])
const redeemingId = ref(null)
const passwordVisible = ref(false)
const password = ref('')
const currentHolding = ref(null)

const totalInvested = computed(() => {
  return holdings.value.reduce((s, h) => s + Number(h.investAmount || 0), 0).toFixed(2)
})
const totalReturn = computed(() => {
  return holdings.value.reduce((s, h) => h.status === 0 ? s + Number(h.expectedReturn || 0) : s, 0).toFixed(2)
})
const totalValue = computed(() => {
  return holdings.value.reduce((s, h) => h.status === 0 ? s + Number(h.investAmount || 0) + Number(h.expectedReturn || 0) : s, 0).toFixed(2)
})

const progressPercent = (item) => {
  if (!item.termDays || !item.purchaseDate) return 0
  const start = new Date(item.purchaseDate)
  const now = new Date()
  const elapsed = Math.floor((now - start) / (1000 * 60 * 60 * 24))
  return Math.min(100, Math.max(0, Math.round((elapsed / item.termDays) * 100)))
}

const loadHoldings = async () => {
  loading.value = true
  try {
    const res = await listHoldings(props.accountNumber)
    holdings.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const openRedeem = (item) => {
  currentHolding.value = item
  passwordVisible.value = true
}

const handleRedeemConfirm = async (pwd) => {
  password.value = pwd
  passwordVisible.value = false
  const item = currentHolding.value
  redeemingId.value = item.id
  try {
    await redeemProduct({ accountNumber: props.accountNumber, holdingId: item.id, password: password.value })
    ElMessage.success('赎回成功，资金已到账')
    emit('refreshBalance')
    loadHoldings()
  } catch (e) {
    console.error(e)
  } finally {
    redeemingId.value = null
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '--'
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
}

onMounted(loadHoldings)
</script>

<style scoped>
.portfolio {
  padding: 0;
}

.loading-wrap {
  padding: var(--space-10);
}

.portfolio-summary {
  position: relative;
  display: flex;
  align-items: center;
  background: var(--color-card);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
  padding: var(--space-5) var(--space-6);
  margin-bottom: var(--space-5);
  color: var(--color-foreground);
  overflow: hidden;
  box-shadow: var(--shadow-md);
}

.summary-item {
  flex: 1;
  text-align: center;
  position: relative;
  z-index: 2;
}

.summary-label {
  display: block;
  font-size: var(--font-sm);
  color: var(--color-muted-foreground);
  margin-bottom: var(--space-1);
  letter-spacing: 0.05em;
  font-family: var(--font-sans);
}

.summary-value {
  display: block;
  font-size: var(--font-h3);
  font-weight: 700;
  letter-spacing: 0.5px;
  color: var(--color-foreground);
  font-family: var(--font-mono);
}

.summary-value.profit {
  color: var(--color-success);
}

.summary-divider {
  width: 1px;
  height: var(--space-9);
  background: var(--glass-border);
  flex-shrink: 0;
}

.portfolio-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.portfolio-card {
  background: var(--color-card);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
  overflow: hidden;
  transition: all var(--transition-normal);
  box-shadow: var(--shadow-sm);
}

.portfolio-card:hover {
  border-color: var(--color-border-hover);
  box-shadow: var(--shadow-md);
}

.portfolio-card.redeemed {
  opacity: 0.65;
}

.pc-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-4) var(--space-5) var(--space-3);
}

.pc-header-left {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.pc-icon {
  width: var(--space-10);
  height: var(--space-10);
  border-radius: var(--radius-md);
  background: var(--color-muted);
  border: 1px solid var(--glass-border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-muted-foreground);
  font-family: var(--font-sans);
  transition: all var(--transition-normal);
}

.pc-icon.active {
  background: color-mix(in srgb, var(--color-primary) 20%, var(--color-muted));
  color: var(--color-primary);
  box-shadow: 0 0 14px color-mix(in srgb, var(--color-primary) 20%, transparent);
}

.pc-name {
  font-size: var(--font-base);
  font-weight: 600;
  color: var(--color-foreground);
  margin: 0 0 var(--space-1);
  font-family: var(--font-sans);
}

.pc-status {
  font-size: var(--font-sm);
  font-weight: 500;
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-md);
  font-family: var(--font-sans);
}

.pc-status.holding {
  background: color-mix(in srgb, var(--color-success) 12%, transparent);
  color: var(--color-success);
}

.pc-status.done {
  background: var(--color-muted);
  color: var(--color-muted-foreground);
}

.pc-header-right {
  text-align: right;
}

.return-label {
  display: block;
  font-size: var(--font-sm);
  color: var(--color-muted-foreground);
  margin-bottom: 2px;
  font-family: var(--font-sans);
}

.return-value {
  font-size: var(--font-h3);
  font-weight: 700;
  color: var(--color-success);
  font-family: var(--font-mono);
}

.pc-body {
  padding: 0 var(--space-5) var(--space-3);
}

.pc-metrics {
  display: flex;
  gap: var(--space-6);
  padding: var(--space-3) 0;
  border-top: 1px solid var(--glass-border);
}

.metric {
  flex: 1;
}

.metric-label {
  display: block;
  font-size: var(--font-sm);
  color: var(--color-muted-foreground);
  margin-bottom: 2px;
  font-family: var(--font-sans);
}

.metric-value {
  font-size: var(--font-base);
  font-weight: 600;
  color: var(--color-foreground);
  font-family: var(--font-mono);
}

.metric-value.rate {
  color: var(--color-primary);
}

.metric-value.date {
  font-size: var(--font-sm);
  color: var(--color-muted-foreground);
}

.pc-progress {
  margin-top: var(--space-1);
}

.progress-bar {
  height: 4px;
  background: var(--color-muted);
  border-radius: var(--radius-full);
  overflow: hidden;
  margin-bottom: var(--space-1);
}

.progress-fill {
  height: 100%;
  background: var(--color-primary);
  border-radius: var(--radius-full);
  transition: width var(--transition-slow);
}

.progress-info {
  display: flex;
  justify-content: space-between;
  font-size: var(--font-sm);
  color: var(--color-muted-foreground);
  font-family: var(--font-sans);
}

.pc-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-3) var(--space-5);
  background: var(--color-muted);
  border-top: 1px solid var(--glass-border);
}

.footer-hint {
  font-size: var(--font-sm);
  color: var(--color-muted-foreground);
  font-family: var(--font-sans);
}

.redeemed-text {
  color: var(--color-muted-foreground);
}

:deep(.el-button--danger.is-plain) {
  --el-button-bg-color: color-mix(in srgb, var(--color-destructive) 10%, transparent);
  --el-button-border-color: var(--color-destructive);
  --el-button-text-color: var(--color-destructive);
  --el-button-hover-bg-color: var(--color-destructive);
  --el-button-hover-border-color: var(--color-destructive);
  --el-button-hover-text-color: #fff;
}
</style>
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
              <span class="footer-hint">到期自动赎回本息 · 提前赎回仅退本金</span>
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
.portfolio { padding: 0; }
.loading-wrap { padding: 40px; }

/* ===== SUMMARY ===== */
.portfolio-summary {
  position: relative;
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, #0e1522, #131f33 45%, #182741);
  border: 1px solid var(--vault-line-strong);
  border-radius: 14px;
  padding: 20px 24px;
  margin-bottom: 20px;
  color: var(--vault-text);
  overflow: hidden;
}
.summary-item { flex: 1; text-align: center; position: relative; z-index: 2; }
.summary-label { display: block; font-size: 12px; color: var(--vault-text-3); margin-bottom: 4px; letter-spacing: 0.05em; }
.summary-value { display: block; font-size: 20px; font-weight: 700; letter-spacing: 0.5px; color: #f5ecd8; }
.summary-value.profit { color: var(--vault-up); }
.summary-divider { width: 1px; height: 36px; background: var(--vault-line-strong); flex-shrink: 0; }

/* ===== LIST ===== */
.portfolio-list { display: flex; flex-direction: column; gap: 12px; }
.portfolio-card {
  background: linear-gradient(180deg, var(--vault-panel-2), var(--vault-panel));
  border: 1px solid var(--vault-line);
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.2s;
}
.portfolio-card:hover {
  border-color: var(--vault-line-hover);
  box-shadow: 0 2px 12px rgba(212,176,106,0.1);
}
.portfolio-card.redeemed { opacity: 0.65; }

/* Header */
.pc-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px 12px; }
.pc-header-left { display: flex; align-items: center; gap: 12px; }
.pc-icon {
  width: 40px; height: 40px;
  border-radius: 10px;
  background: var(--vault-graphite);
  border: 1px solid var(--vault-line);
  display: flex; align-items: center; justify-content: center;
  font-size: 16px; font-weight: 700;
  color: var(--vault-text-3);
}
.pc-icon.active {
  background: linear-gradient(135deg, #3a2c14, #5a4420);
  color: #e6c98a;
  box-shadow: 0 0 14px rgba(212,176,106,0.2);
}
.pc-name { font-size: 15px; font-weight: 600; color: var(--vault-text); margin: 0 0 2px; }
.pc-status { font-size: 11px; font-weight: 500; padding: 1px 8px; border-radius: 8px; }
.pc-status.holding { background: rgba(47,168,160,0.12); color: var(--vault-up); }
.pc-status.done { background: var(--vault-graphite); color: var(--vault-text-3); }

.pc-header-right { text-align: right; }
.return-label { display: block; font-size: 11px; color: var(--vault-text-3); margin-bottom: 2px; }
.return-value { font-size: 18px; font-weight: 700; color: var(--vault-up); }

/* Body */
.pc-body { padding: 0 20px 12px; }
.pc-metrics { display: flex; gap: 24px; padding: 10px 0; border-top: 1px solid var(--vault-line); }
.metric { flex: 1; }
.metric-label { display: block; font-size: 11px; color: var(--vault-text-3); margin-bottom: 2px; }
.metric-value { font-size: 14px; font-weight: 600; color: var(--vault-text); }
.metric-value.rate { color: var(--vault-gold); }
.metric-value.date { font-size: 12px; color: var(--vault-text-3); }

/* Progress */
.pc-progress { margin-top: 4px; }
.progress-bar { height: 4px; background: var(--vault-graphite); border-radius: 2px; overflow: hidden; margin-bottom: 4px; }
.progress-fill { height: 100%; background: linear-gradient(90deg, #b08d4a, #d4b06a); border-radius: 2px; transition: width 0.5s; }
.progress-info { display: flex; justify-content: space-between; font-size: 11px; color: var(--vault-text-3); }

/* Footer */
.pc-footer { display: flex; justify-content: space-between; align-items: center; padding: 10px 20px; background: rgba(8,12,20,0.4); border-top: 1px solid var(--vault-line); }
.footer-hint { font-size: 12px; color: var(--vault-text-3); }
.redeemed-text { color: var(--vault-text-3); }
</style>

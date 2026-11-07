<template>
  <div class="balance-query">
    <div class="section-top">
      <div class="section-top-left">
        <el-icon :size="20" color="var(--color-primary)"><Wallet /></el-icon>
        <h3>余额查询</h3>
      </div>
    </div>

    <!-- Loading skeleton -->
    <div v-if="loading" class="loading-skeleton">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- Result Card -->
    <div v-else-if="resultVisible" class="result-card">
      <div class="result-card-header">
        <div class="header-badge">
          <el-icon :size="32" color="var(--color-success)"><CircleCheck /></el-icon>
          <span>查询成功</span>
        </div>
      </div>
      <div class="account-info">
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">账户号</span>
            <span class="info-value">{{ resultData.accountNumber }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">户名</span>
            <span class="info-value">{{ resultData.accountHolderName }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">账户类型</span>
            <el-tag :type="resultData.accountType === '储蓄' ? 'primary' : resultData.accountType === '活期' ? 'success' : 'warning'" size="small">
              {{ resultData.accountType }}
            </el-tag>
          </div>
          <div class="info-item">
            <span class="info-label">账户状态</span>
            <el-tag :type="resultData.accountStatus === 1 ? 'success' : 'danger'" size="small">
              {{ resultData.accountStatus === 1 ? '正常' : '冻结' }}
            </el-tag>
          </div>
          <div class="info-item">
            <span class="info-label">身份证号</span>
            <span class="info-value">{{ maskIdCard(resultData.userIdCard) }}</span>
          </div>
        </div>

        <div class="balance-display">
          <span class="balance-label">当前余额</span>
          <div class="balance-row">
            <span class="balance-symbol">¥</span>
            <span class="balance-number">{{ resultData.accountBalance }}</span>
          </div>
        </div>
      </div>
    </div>

    <div v-if="resultVisible && forecastPoints.length > 0" class="forecast-card">
      <div class="forecast-header">
        <div>
          <h4 class="forecast-title">未来 7 天余额趋势预测</h4>
        </div>
        <el-tag v-if="forecast.lowRiskWarning" type="danger" size="small">资金不足风险预警</el-tag>
        <el-tag v-else type="success" size="small">预测安全</el-tag>
      </div>
      <div class="forecast-bars">
        <div v-for="(p, i) in forecastPoints" :key="i" class="forecast-bar-item">
          <div class="bar-col" :class="{ danger: Number(p.predictedBalance) < 0 }">
            <div class="bar-value">{{ Number(p.predictedBalance).toFixed(0) }}</div>
            <div class="bar-fill" :style="{ height: barHeight(Number(p.predictedBalance)) }"></div>
          </div>
          <div class="bar-date">{{ p.date }}</div>
        </div>
      </div>
    </div>

    <!-- Error -->
    <el-result v-else-if="errorMsg" icon="error" title="查询失败" :sub-title="errorMsg" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck } from '@element-plus/icons-vue'
import request from '@/utils/request'

const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
const loading = ref(false)
const resultVisible = ref(false)
const errorMsg = ref('')
const maskIdCard = (id) => {
  if (!id || id.length < 10) return id || '--'
  return id.substring(0, 6) + '********' + id.substring(14)
}

const resultData = reactive({
  accountNumber: '', accountHolderName: '', accountType: '',
  accountStatus: 1, accountBalance: '0.00', userIdCard: ''
})

const forecast = ref({ lowRiskWarning: false })
const forecastPoints = ref([])

const maxAbs = () => {
  const vals = forecastPoints.value.map(p => Math.abs(Number(p.predictedBalance)))
  return Math.max(...vals, 1)
}

const barHeight = (v) => {
  const h = Math.max(10, (Math.abs(v) / maxAbs()) * 70)
  return h + 'px'
}

const loadForecast = async () => {
  try {
    const res = await request({
      url: '/account/forecast',
      method: 'get',
      params: { accountNumber: userInfo?.accountNumber || '' }
    })
    forecast.value = res.data || { lowRiskWarning: false }
    forecastPoints.value = forecast.value.points || []
  } catch (e) {
    forecastPoints.value = []
  }
}

const handleQuery = async () => {
  loading.value = true; errorMsg.value = ''; resultVisible.value = false
  try {
    const accountRes = await request({
      url: '/account/getAccountEntity',
      method: 'post',
      params: { accountNumber: userInfo?.accountNumber || '' }
    })

    const d = accountRes.data
    resultData.accountNumber = d.accountNumber
    resultData.accountHolderName = d.accountHolderName
    resultData.accountType = d.accountType
    resultData.accountStatus = d.accountStatus
    resultData.accountBalance = d.accountBalance?.toFixed(2) || '0.00'
    resultData.userIdCard = d.userIdCard
    resultVisible.value = true
    loadForecast()
  } catch (error) {
    errorMsg.value = '查询失败，请检查账号是否正确'
  } finally { loading.value = false }
}

onMounted(() => { handleQuery() })
</script>

<style scoped>
.balance-query {
  width: 100%;
}

.section-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-6);
  padding: var(--space-3) var(--space-5);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.section-top-left {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.section-top-left h3 {
  margin: 0;
  font-size: var(--font-h3);
  font-weight: 600;
  color: var(--color-foreground);
  font-family: var(--font-sans);
}

.loading-skeleton {
  padding: var(--space-6) var(--space-5);
}

.result-card {
  background: var(--color-card);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
  overflow: hidden;
  max-width: 560px;
  margin: 0 auto;
  box-shadow: var(--shadow-md);
}

.result-card-header {
  background: var(--color-muted);
  padding: var(--space-5) var(--space-6);
  border-bottom: 1px solid var(--color-border);
}

.header-badge {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-base);
  font-weight: 600;
  color: var(--color-success);
  font-family: var(--font-sans);
}

.account-info {
  padding: var(--space-6) var(--space-6) var(--space-4);
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
  margin-bottom: var(--space-5);
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.info-label {
  font-size: var(--font-sm);
  color: var(--color-muted-foreground);
  font-family: var(--font-sans);
  letter-spacing: 0.02em;
}

.info-value {
  font-size: var(--font-base);
  font-weight: 500;
  color: var(--color-foreground);
  font-family: var(--font-mono);
}

.balance-display {
  background: var(--color-muted);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
  padding: var(--space-5) var(--space-6);
  text-align: center;
  margin-top: var(--space-4);
}

.balance-label {
  font-size: var(--font-sm);
  color: var(--color-muted-foreground);
  display: block;
  margin-bottom: var(--space-3);
  font-family: var(--font-sans);
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.balance-row {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: var(--space-1);
}

.balance-symbol {
  font-size: 28px;
  color: var(--color-primary);
  font-weight: 600;
  font-family: var(--font-mono);
}

.balance-number {
  font-family: var(--font-mono);
  font-size: 42px;
  font-weight: 700;
  line-height: 1;
  color: var(--color-foreground);
  letter-spacing: 0.02em;
}

.forecast-card {
  background: var(--color-card);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
  padding: var(--space-5) var(--space-6);
  max-width: 560px;
  margin: var(--space-5) auto;
  box-shadow: var(--shadow-md);
}

.forecast-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-5);
}

.forecast-title {
  font-size: var(--font-base);
  font-weight: 600;
  color: var(--color-foreground);
  margin: 0 0 var(--space-1);
  font-family: var(--font-sans);
}

.forecast-note {
  font-size: var(--font-sm);
  color: var(--color-muted-foreground);
  font-family: var(--font-sans);
}

.forecast-bars {
  display: flex;
  align-items: flex-end;
  gap: var(--space-3);
  min-height: 80px;
}

.forecast-bar-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-1);
}

.bar-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  height: 60px;
  width: 100%;
}

.bar-value {
  font-size: var(--font-sm);
  color: var(--color-muted-foreground);
  margin-bottom: var(--space-1);
  font-weight: 550;
  font-family: var(--font-mono);
}

.bar-fill {
  width: 55%;
  height: 100%;
  border-radius: var(--radius-md) var(--radius-md) 0 0;
  background: var(--color-primary);
  transition: height var(--transition-slow);
}

.bar-col.danger .bar-fill {
  background: linear-gradient(180deg, var(--color-destructive), var(--color-primary));
}

.bar-col.danger .bar-value {
  color: var(--color-destructive);
}

.bar-date {
  font-size: var(--font-sm);
  color: var(--color-muted-foreground);
  font-family: var(--font-sans);
}

:deep(.el-tag--success) {
  --el-tag-bg-color: color-mix(in srgb, var(--color-success) 15%, transparent);
  --el-tag-text-color: var(--color-success);
}

:deep(.el-tag--danger) {
  --el-tag-bg-color: color-mix(in srgb, var(--color-destructive) 15%, transparent);
  --el-tag-text-color: var(--color-destructive);
}

:deep(.el-tag--primary) {
  --el-tag-bg-color: color-mix(in srgb, var(--color-primary) 15%, transparent);
  --el-tag-text-color: var(--color-primary);
}

:deep(.el-tag--warning) {
  --el-tag-bg-color: color-mix(in srgb, var(--color-primary) 15%, transparent);
  --el-tag-text-color: var(--color-primary);
}

@media (max-width: 480px) {
  .info-grid { grid-template-columns: 1fr; }
}
</style>
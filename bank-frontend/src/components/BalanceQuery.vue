<template>
  <div class="balance-query">
    <div class="section-top">
      <div class="section-top-left">
        <el-icon :size="20" color="#1a3a6b"><Wallet /></el-icon>
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
          <el-icon :size="32" color="#52c41a"><CircleCheck /></el-icon>
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
          <span class="forecast-note">基于近 30 天收支线性回归预测，仅供参考</span>
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
.balance-query { width: 100%; }

.section-top {
  display: flex; justify-content: space-between;
  align-items: center; margin-bottom: 20px;
}
.section-top-left {
  display: flex; align-items: center; gap: 8px;
}
.section-top-left h3 {
  margin: 0; font-size: 16px; font-weight: 600; color: #333;
}

.loading-skeleton { padding: 20px; }

.result-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 12px;
  overflow: hidden;
  max-width: 520px;
}

.result-card-header {
  background: linear-gradient(135deg, #f0f9f0, #e8f5e8);
  padding: 16px 20px;
  border-bottom: 1px solid #d0e8d0;
}

.header-badge {
  display: flex; align-items: center; gap: 8px;
  font-size: 15px; font-weight: 600; color: #52c41a;
}

.account-info { padding: 20px; }

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 20px;
}

.info-item {
  display: flex; flex-direction: column; gap: 4px;
}
.info-label { font-size: 12px; color: #999; }
.info-value { font-size: 14px; font-weight: 600; color: #333; }

.balance-display {
  background: linear-gradient(135deg, #fff8e6, #fff3d6);
  border: 1px solid #f5e6c0;
  border-radius: 12px;
  padding: 16px 20px;
  text-align: center;
}

.balance-label { font-size: 13px; color: #b8860b; display: block; margin-bottom: 8px; }
.balance-row { display: flex; align-items: baseline; justify-content: center; gap: 4px; }
.balance-symbol { font-size: 20px; color: #b8860b; font-weight: 600; }
.balance-number {
  font-size: 32px; font-weight: 700;
  color: #b8860b; font-family: 'Noto Sans SC', monospace;
}

.forecast-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 12px;
  padding: 18px 20px;
  max-width: 520px;
  margin-top: 16px;
}
.forecast-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px;
}
.forecast-title { font-size: 15px; font-weight: 600; color: #333; margin: 0 0 2px; }
.forecast-note { font-size: 12px; color: #999; }
.forecast-bars {
  display: flex; align-items: flex-end; gap: 12px;
  min-height: 120px;
}
.forecast-bar-item {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; gap: 6px;
}
.bar-col {
  display: flex; flex-direction: column;
  align-items: center; justify-content: flex-end;
  height: 90px; width: 100%;
}
.bar-value { font-size: 11px; color: #333; margin-bottom: 4px; font-weight: 600; }
.bar-fill {
  width: 60%;
  background: linear-gradient(180deg, #1a3a6b, #2d5a8c);
  border-radius: 6px 6px 0 0;
  transition: height 0.4s;
}
.bar-col.danger .bar-fill { background: linear-gradient(180deg, #fa541c, #e6a23c); }
.bar-col.danger .bar-value { color: #fa541c; }
.bar-date { font-size: 11px; color: #999; }

@media (max-width: 480px) {
  .info-grid { grid-template-columns: 1fr; }
}
</style>

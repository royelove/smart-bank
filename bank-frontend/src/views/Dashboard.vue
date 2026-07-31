<template>
  <div class="dashboard">
    <!-- ===== TOP NAV ===== -->
    <header class="dashboard-header">
      <div class="header-left">
        <div class="brand-logo-mini">
          <el-icon :size="24" color="#f5af19"><CreditCard /></el-icon>
          <span class="brand-text">智慧银行</span>
        </div>
        <span class="header-divider"></span>
        <span class="header-welcome">欢迎回来，{{ userInfo?.accountHolderName || '用户' }}</span>
      </div>
      <div class="header-right">
        <div class="header-time">
          <el-icon><Clock /></el-icon>
          <span>{{ currentTime }}</span>
        </div>
        <div class="user-dropdown">
          <el-dropdown trigger="click">
            <span class="user-trigger">
              <div class="user-avatar">
                {{ (userInfo?.accountHolderName || '用')[0] }}
              </div>
              <span class="user-name">{{ userInfo?.accountHolderName || '用户' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="showProfile = true">
                  <el-icon><User /></el-icon> 个人信息
                </el-dropdown-item>
                <el-dropdown-item @click="showChangePwd = true">
                  <el-icon><Key /></el-icon> 修改密码
                </el-dropdown-item>
                <el-dropdown-item divided @click="logout">
                  <el-icon><SwitchButton /></el-icon> 安全退出
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <div class="dashboard-body">
      <!-- ===== SIDEBAR ===== -->
      <aside class="dashboard-sidebar">
        <div class="sidebar-user-card">
          <div class="sidebar-avatar">{{ (userInfo?.accountHolderName || '用')[0] }}</div>
          <div class="sidebar-user-info">
            <span class="sidebar-username">{{ userInfo?.accountHolderName || '用户' }}</span>
            <span class="sidebar-account">{{ userInfo?.accountNumber || '****' }}</span>
          </div>
        </div>

        <el-menu
          @select="handleMenuSelect"
          class="sidebar-menu"
        >
          <div class="menu-group-label">交易服务</div>
          <el-menu-item index="records">
            <el-icon><Document /></el-icon>
            <span>交易记录</span>
          </el-menu-item>

          <div class="menu-group-label">理财服务</div>
          <el-menu-item index="products">
            <el-icon><Coin /></el-icon>
            <span>理财产品</span>
          </el-menu-item>
          <el-menu-item index="holdings">
            <el-icon><DataAnalysis /></el-icon>
            <span>我的持仓</span>
          </el-menu-item>
        </el-menu>
      </aside>

      <!-- ===== MAIN ===== -->
      <main class="dashboard-main">
        <!-- Balance Card -->
        <div class="balance-card">
          <div class="balance-card-bg"></div>
          <div class="balance-content">
            <div class="balance-top">
              <div class="balance-type">
                <el-icon><Wallet /></el-icon>
                <span>账户总资产</span>
              </div>
              <div class="balance-status">
                <span class="status-dot"></span>
                账户正常
              </div>
            </div>
            <div class="balance-amount">
              <span class="currency">¥</span>
              <span class="amount-number">{{ totalAssets }}</span>
            </div>
            <div class="balance-bottom">
              <div class="balance-user">
                户名：{{ userInfo?.accountHolderName || '--' }}
              </div>
              <div class="balance-account">
                账号：{{ userInfo?.accountNumber || '--' }}
              </div>
              <div class="balance-idcard">
                身份证号：{{ maskIdCard(userInfo?.userIdCard) }}
              </div>
            </div>
          </div>
          <div class="balance-card-chip"></div>
        </div>

        <!-- Quick Actions -->
        <div class="quick-actions">
          <div class="section-header">
            <h3>快捷交易</h3>
            <span class="section-sub">快速发起常用交易</span>
          </div>
          <div class="actions-grid">
            <div class="action-card" @click="showDeposit = true">
              <div class="action-icon deposit-icon">
                <el-icon :size="24" color="#fff"><Coin /></el-icon>
              </div>
              <span class="action-label">存款</span>
            </div>
            <div class="action-card" @click="showWithdraw = true">
              <div class="action-icon withdraw-icon">
                <el-icon :size="24" color="#fff"><Wallet /></el-icon>
              </div>
              <span class="action-label">取款</span>
            </div>
            <div class="action-card" @click="showTransfer = true">
              <div class="action-icon transfer-icon">
                <el-icon :size="24" color="#fff"><Right /></el-icon>
              </div>
              <span class="action-label">转账</span>
            </div>
            <div class="action-card" @click="showRecords = true">
              <div class="action-icon records-icon">
                <el-icon :size="24" color="#fff"><Document /></el-icon>
              </div>
              <span class="action-label">明细</span>
            </div>
            <div class="action-card" @click="showProducts = true">
              <div class="action-icon deposit-icon">
                <el-icon :size="24" color="#fff"><Coin /></el-icon>
              </div>
              <span class="action-label">理财</span>
            </div>
            <div class="action-card" @click="showHoldings = true">
              <div class="action-icon records-icon">
                <el-icon :size="24" color="#fff"><DataAnalysis /></el-icon>
              </div>
              <span class="action-label">持仓</span>
            </div>
          </div>
        </div>

      </main>
    </div>

    <!-- Dialogs -->
    <el-dialog v-model="showDeposit" title="存款" width="500px" destroy-on-close>
      <Deposit @close="showDeposit = false" />
    </el-dialog>

    <el-dialog v-model="showWithdraw" title="取款" width="500px" destroy-on-close>
      <Withdraw @close="showWithdraw = false" />
    </el-dialog>

    <el-dialog v-model="showTransfer" title="转账" width="500px" destroy-on-close>
      <Transfer @close="showTransfer = false" />
    </el-dialog>

    <el-dialog v-model="showRecords" title="交易明细" width="900px" destroy-on-close>
      <TradeRecords />
    </el-dialog>

    <el-dialog v-model="showProducts" title="理财产品" width="800px" destroy-on-close>
      <ProductList @refresh-balance="loadBalance" />
    </el-dialog>

    <el-dialog v-model="showHoldings" title="我的持仓" width="800px" destroy-on-close>
      <MyHoldings @refresh-balance="loadBalance" />
    </el-dialog>

    <el-dialog v-model="showProfile" title="个人信息" width="480px" destroy-on-close>
      <ProfileSettings @close="showProfile = false" />
    </el-dialog>

    <el-dialog v-model="showChangePwd" title="修改密码" width="480px" destroy-on-close>
      <ChangePassword @close="showChangePwd = false" />
    </el-dialog>

    <ChatBot />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, provide } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import Deposit from '@/components/Deposit.vue'
import Withdraw from '@/components/Withdraw.vue'
import Transfer from '@/components/Transfer.vue'
import TradeRecords from '@/components/TradeRecords.vue'
import ProductList from '@/components/ProductList.vue'
import MyHoldings from '@/components/MyHoldings.vue'
import ChangePassword from '@/components/ChangePassword.vue'
import ProfileSettings from '@/components/ProfileSettings.vue'
import ChatBot from '@/components/ChatBot.vue'

import { ArrowDown, SwitchButton, Clock, Key, User } from '@element-plus/icons-vue'

const maskIdCard = (id) => {
  if (!id || id.length < 10) return id || '--'
  return id.substring(0, 6) + '********' + id.substring(14)
}

const router = useRouter()
const userInfo = ref(null)
const totalAssets = ref('0.00')
const currentTime = ref('')
const showDeposit = ref(false)
const showWithdraw = ref(false)
const showTransfer = ref(false)
const showRecords = ref(false)
const showProducts = ref(false)
const showHoldings = ref(false)
const showChangePwd = ref(false)
const showProfile = ref(false)
let timer = null

try {
  const info = localStorage.getItem('userInfo')
  if (info) {
    userInfo.value = JSON.parse(info)
  }
} catch (e) { console.warn(e) }

const loadBalance = async () => {
  try {
    const res = await request({ url: '/account/getAccountBalance', method: 'get' })
    const val = res.data
    totalAssets.value = typeof val === 'number' ? val.toFixed(2) : '0.00'
  } catch (e) { console.error(e) }
}

provide('refreshBalance', loadBalance)

const handleMenuSelect = (index) => {
  if (index === 'records') showRecords.value = true
  if (index === 'products') showProducts.value = true
  if (index === 'holdings') showHoldings.value = true
}

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', { hour12: false })
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  loadBalance()
})
onUnmounted(() => { clearInterval(timer) })

const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  ElMessage.success('已安全退出')
  router.push('/login')
}
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f0f2f5;
}

/* ===== HEADER ===== */
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 60px;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.brand-logo-mini {
  display: flex;
  align-items: center;
  gap: 8px;
}

.brand-text {
  font-size: 18px;
  font-weight: 700;
  color: #1a3a6b;
  letter-spacing: 2px;
}

.header-divider {
  width: 1px;
  height: 24px;
  background: #e0e4ea;
}

.header-welcome {
  font-size: 14px;
  color: #666;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.header-time {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #999;
  font-weight: 500;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s;
}
.user-trigger:hover { background: #f5f7fa; }

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1a3a6b, #f5af19);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

/* ===== BODY ===== */
.dashboard-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* ===== SIDEBAR ===== */
.dashboard-sidebar {
  width: 220px;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  flex-shrink: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.sidebar-user-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 16px 16px;
  border-bottom: 1px solid #f0f2f5;
}

.sidebar-avatar {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #1a3a6b, #2d5a8c);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
}

.sidebar-user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.sidebar-username { font-size: 15px; font-weight: 600; color: #333; }
.sidebar-account { font-size: 12px; color: #999; font-family: monospace; }

.sidebar-menu { border: none; flex: 1; }
.sidebar-menu :deep(.el-menu-item) {
  height: 44px;
  line-height: 44px;
  margin: 2px 8px;
  border-radius: 8px;
  font-size: 14px;
}
.sidebar-menu :deep(.el-menu-item.is-active) {
  background: #e8f0fe;
  color: #1a3a6b;
  font-weight: 600;
}
.sidebar-menu :deep(.el-menu-item .el-icon) { margin-right: 10px; font-size: 18px; }

.menu-group-label {
  font-size: 11px;
  font-weight: 600;
  color: #999;
  text-transform: uppercase;
  padding: 16px 16px 4px;
  letter-spacing: 1px;
}

/* ===== MAIN ===== */
.dashboard-main {
  flex: 1;
  padding: 20px 24px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ===== BALANCE CARD ===== */
.balance-card {
  position: relative;
  background: linear-gradient(135deg, #0c1a3b 0%, #1a3a6b 40%, #2d5a8c 100%);
  border-radius: 16px;
  padding: 28px 32px;
  color: #fff;
  overflow: hidden;
  min-height: 160px;
}

.balance-card-bg {
  position: absolute;
  top: -50%;
  right: -20%;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(245,175,25,0.08) 0%, transparent 70%);
  pointer-events: none;
}

.balance-content { position: relative; z-index: 2; }

.balance-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.balance-type {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  opacity: 0.8;
}

.balance-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #52c41a;
  background: rgba(82, 196, 26, 0.15);
  padding: 4px 12px;
  border-radius: 20px;
}

.status-dot {
  width: 6px;
  height: 6px;
  background: #52c41a;
  border-radius: 50%;
  animation: blink 2s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.balance-amount {
  margin-bottom: 16px;
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.currency { font-size: 20px; opacity: 0.8; font-weight: 500; }
.amount-number {
  font-size: 36px;
  font-weight: 700;
  letter-spacing: 2px;
  font-family: 'Noto Sans SC', monospace;
}

.balance-bottom {
  display: flex;
  gap: 32px;
  font-size: 12px;
  opacity: 0.7;
}

.balance-card-chip {
  position: absolute;
  top: 20px;
  right: 28px;
  width: 50px;
  height: 36px;
  background: linear-gradient(135deg, #f5af19, #e6a23c);
  border-radius: 6px;
  opacity: 0.6;
  z-index: 1;
}

/* ===== QUICK ACTIONS ===== */
.quick-actions {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}

.section-header {
  margin-bottom: 16px;
}
.section-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0;
}
.section-sub { font-size: 12px; color: #999; }

.actions-grid {
  display: flex;
  gap: 12px;
}

.action-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 24px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #f0f2f5;
  background: #fafafa;
  min-width: 100px;
}
.action-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
  border-color: #d0d5de;
}

.action-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.deposit-icon { background: linear-gradient(135deg, #1a3a6b, #2d5a8c); }
.withdraw-icon { background: linear-gradient(135deg, #e6a23c, #f5af19); }
.transfer-icon { background: linear-gradient(135deg, #67c23a, #85ce61); }
.records-icon { background: linear-gradient(135deg, #409eff, #79bbff); }

.action-label {
  font-size: 13px;
  font-weight: 600;
  color: #333;
}

/* ===== CONTENT CARD ===== */
.content-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  min-height: 300px;
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.content-card > * {
  flex: 1;
  overflow: hidden;
}

.trade-tabs :deep(.el-tabs__item) {
  font-size: 14px;
  font-weight: 500;
  padding: 0 20px;
}
.trade-tabs :deep(.el-tabs__header) { margin-bottom: 20px; }

/* ===== RESPONSIVE ===== */
@media (max-width: 768px) {
  .dashboard-sidebar { width: 60px; }
  .sidebar-user-card, .menu-group-label, .sidebar-user-info,
  .sidebar-menu :deep(.el-menu-item span) { display: none; }
  .sidebar-menu :deep(.el-menu-item) { justify-content: center; padding: 0 !important; }
  .sidebar-menu :deep(.el-menu-item .el-icon) { margin-right: 0; }
  .actions-grid { flex-wrap: wrap; }
  .action-card { min-width: 80px; flex: 1; }
  .balance-bottom { flex-direction: column; gap: 4px; }
  .header-welcome { display: none; }
  .header-time { display: none; }
}

@media (max-width: 480px) {
  .dashboard-main { padding: 12px; }
  .balance-card { padding: 20px; }
  .amount-number { font-size: 28px; }
  .content-card { padding: 12px; }
}
</style>

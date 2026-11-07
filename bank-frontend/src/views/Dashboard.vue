<template>
  <div class="dashboard">
    <!-- ============ 顶栏 · 全局导航 ============ -->
    <header class="top-nav">
      <div class="nav-left">
        <div class="brand">
          <div class="brand-emblem">
            <svg width="24" height="24" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M24 4L4 14V34L24 44L44 34V14L24 4Z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
              <path d="M24 4V44" stroke="currentColor" stroke-width="2"/>
              <path d="M4 14L24 24L44 14" stroke="currentColor" stroke-width="2"/>
            </svg>
          </div>
          <div class="brand-text">
            <span class="brand-name">SMART BANK</span>
            <span class="brand-en">PRIVATE BANKING</span>
          </div>
        </div>
        <span class="header-divider"></span>
        <span class="header-welcome">欢迎回来，{{ userInfo?.accountHolderName || '用户' }}</span>
      </div>

      <nav class="nav-center">
        <div class="nav-item" @click="showRecords = true">
          <el-icon :size="16"><Document /></el-icon><span>交易明细</span>
        </div>
        <div class="nav-item" @click="showProducts = true">
          <el-icon :size="16"><Coin /></el-icon><span>理财产品</span>
        </div>
        <div class="nav-item" @click="showHoldings = true">
          <el-icon :size="16"><DataAnalysis /></el-icon><span>我的持仓</span>
        </div>
<div class="nav-item" @click="showRank = true">
            <el-icon :size="16"><TrendCharts /></el-icon><span>股票排行</span>
          </div>
          <div class="nav-item" @click="showMarket = true">
            <el-icon :size="16"><TrendCharts /></el-icon><span>股票行情</span>
          </div>
        <div class="nav-item" @click="showTrading = true">
          <el-icon :size="16"><Odometer /></el-icon><span>股票交易</span>
        </div>
      </nav>

      <div class="nav-right">
        <div class="server-status">
          <span class="vault-status-dot"></span>
          <span class="server-text">服务在线</span>
        </div>
        <div class="header-time">
          <el-icon :size="15"><Clock /></el-icon>
          <span>{{ currentTime }}</span>
        </div>
        <el-dropdown trigger="click">
          <span class="user-trigger">
            <div class="user-avatar">{{ (userInfo?.accountHolderName || '用')[0] }}</div>
            <span class="user-name">{{ userInfo?.accountHolderName || '用户' }}</span>
            <el-icon :size="12"><ArrowDown /></el-icon>
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
    </header>

    <!-- ============ 巨幅资产面板 ============ -->
    <section class="hero">
      <div class="hero-glow"></div>
      <div class="hero-inner">
        <div class="hero-top">
          <div class="hero-welcome">
            <span class="hero-greet">私人银行账户</span>
            <span class="hero-acct vault-num">{{ userInfo?.accountNumber || '****' }}</span>
          </div>
          <div class="hero-quick">
            <button class="q-btn q-primary" @click="showDeposit = true">
              <el-icon :size="16"><Coin /></el-icon><span>存款</span>
            </button>
            <button class="q-btn" @click="showWithdraw = true">
              <el-icon :size="16"><Wallet /></el-icon><span>取款</span>
            </button>
            <button class="q-btn" @click="showTransfer = true">
              <el-icon :size="16"><Right /></el-icon><span>转账</span>
            </button>
          </div>
        </div>

        <div class="hero-balance">
          <div class="balance-caption">账户总资产 · 人民币</div>
          <div class="balance-main">
            <span class="cur">¥</span>
            <span class="num vault-num">{{ totalAssets }}</span>
          </div>
          <div class="balance-rule"></div>
        </div>

        <div class="hero-meta">
          <div class="meta-item">
            <span class="meta-label">户主姓名</span>
            <span class="meta-value">{{ userInfo?.accountHolderName || '--' }}</span>
          </div>
          <div class="meta-divider"></div>
          <div class="meta-item">
            <span class="meta-label">账户状态</span>
            <span class="meta-value ok"><span class="status-dot"></span>正常</span>
          </div>
          <div class="meta-divider"></div>
          <div class="meta-item">
            <span class="meta-label">实名证件</span>
            <span class="meta-value vault-num">{{ maskIdCard(userInfo?.userIdCard) }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- ============ 快捷服务 · 大功能卡区 ============ -->
    <main class="content">
      <section class="services">
        <div class="section-head">
          <div class="section-title">
            <h2>快捷服务</h2>
            <span class="section-en">QUICK SERVICES</span>
          </div>
          <span class="section-sub">一键触达核心业务，资金管理从容有序</span>
        </div>

        <div class="service-grid">
          <div class="service-card" @click="showDeposit = true">
            <div class="s-icon gold"><el-icon :size="24"><Coin /></el-icon></div>
            <div class="s-name">存款</div>
            <div class="s-desc">灵活存入，财富沉淀</div>
            <div class="s-go"><el-icon :size="14"><Right /></el-icon></div>
          </div>
          <div class="service-card" @click="showWithdraw = true">
            <div class="s-icon blue"><el-icon :size="24"><Wallet /></el-icon></div>
            <div class="s-name">取款</div>
            <div class="s-desc">安全支取，随需而动</div>
            <div class="s-go"><el-icon :size="14"><Right /></el-icon></div>
          </div>
          <div class="service-card" @click="showTransfer = true">
            <div class="s-icon cyan"><el-icon :size="24"><Right /></el-icon></div>
            <div class="s-name">转账</div>
            <div class="s-desc">实时划转，直达至亲</div>
            <div class="s-go"><el-icon :size="14"><Right /></el-icon></div>
          </div>
          <div class="service-card" @click="showRecords = true">
            <div class="s-icon violet"><el-icon :size="24"><Document /></el-icon></div>
            <div class="s-name">交易明细</div>
            <div class="s-desc">收支流水，一目了然</div>
            <div class="s-go"><el-icon :size="14"><Right /></el-icon></div>
          </div>
          <div class="service-card" @click="showProducts = true">
            <div class="s-icon blue"><el-icon :size="24"><Coin /></el-icon></div>
            <div class="s-name">理财产品</div>
            <div class="s-desc">多元配置，稳健增值</div>
            <div class="s-go"><el-icon :size="14"><Right /></el-icon></div>
          </div>
          <div class="service-card" @click="showHoldings = true">
            <div class="s-icon gold"><el-icon :size="24"><DataAnalysis /></el-icon></div>
            <div class="s-name">我的持仓</div>
            <div class="s-desc">资产全景，了如指掌</div>
            <div class="s-go"><el-icon :size="14"><Right /></el-icon></div>
          </div>
          <div class="service-card" @click="showSupport = true">
            <div class="s-icon violet"><el-icon :size="24"><Headset /></el-icon></div>
            <div class="s-name">客服中心</div>
            <div class="s-desc">专属服务，排忧解难</div>
            <div class="s-go"><el-icon :size="14"><Right /></el-icon></div>
          </div>
          <div class="service-card" @click="showRank = true">
            <div class="s-icon rose"><el-icon :size="24"><TrendCharts /></el-icon></div>
            <div class="s-name">股票排行</div>
            <div class="s-desc">涨幅跌幅，实时榜单</div>
            <div class="s-go"><el-icon :size="14"><Right /></el-icon></div>
          </div>
          <div class="service-card" @click="showTrading = true">
            <div class="s-icon amber"><el-icon :size="24"><Odometer /></el-icon></div>
            <div class="s-name">股票交易</div>
            <div class="s-desc">委托下单，一键成交</div>
            <div class="s-go"><el-icon :size="14"><Right /></el-icon></div>
          </div>
        </div>
      </section>

      <footer class="page-foot">
        <span class="foot-brand">SMART BANK · 智能银行</span>
        <span class="foot-slogan">稳健笃行 · 财富永续</span>
      </footer>
    </main>

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
      <ProductList :account-number="userInfo?.accountNumber" @refresh-balance="loadBalance" />
    </el-dialog>

    <el-dialog v-model="showHoldings" title="我的持仓" width="800px" destroy-on-close>
      <MyHoldings :account-number="userInfo?.accountNumber" @refresh-balance="loadBalance" />
    </el-dialog>

    <el-dialog v-model="showProfile" title="个人信息" width="480px" destroy-on-close>
      <ProfileSettings @close="showProfile = false" />
    </el-dialog>

    <el-dialog v-model="showChangePwd" title="修改密码" width="480px" destroy-on-close>
      <ChangePassword @close="showChangePwd = false" />
    </el-dialog>

    <el-dialog v-model="showSupport" title="客服中心" width="640px" destroy-on-close>
      <SupportCenter :account-number="userInfo?.accountNumber" />
    </el-dialog>

    <el-dialog v-model="showMarket" title="股票行情" width="980px" destroy-on-close>
      <StockMarket @open-kline="(code, name) => { selectedStockCode = code; selectedStockName = name; showKline = true }" />
    </el-dialog>

    <el-dialog v-model="showTrading" title="股票交易" width="980px" destroy-on-close @closed="loadBalance">
      <StockTrading :account-number="userInfo?.accountNumber" @refresh-balance="loadBalance" />
    </el-dialog>

    <el-dialog v-model="showRank" title="股票排行" width="980px" destroy-on-close>
      <RankBoard @open-kline="(code, name) => { selectedStockCode = code; selectedStockName = name; showKline = true }" />
    </el-dialog>

    <el-dialog v-model="showKline" :title="selectedStockName + ' K线图'" width="980px" destroy-on-close>
      <KlineChart :stock-code="selectedStockCode" :stock-name="selectedStockName" />
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
import SupportCenter from '@/components/SupportCenter.vue'
import StockMarket from '@/components/StockMarket.vue'
import StockTrading from '@/components/StockTrading.vue'
import RankBoard from '@/components/RankBoard.vue'
import KlineChart from '@/components/KlineChart.vue'
import ChatBot from '@/components/ChatBot.vue'

import { ArrowDown, SwitchButton, Clock, Key, User, Coin, Right, Document, DataAnalysis, TrendCharts, Odometer, Headset } from '@element-plus/icons-vue'

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
const showSupport = ref(false)
const showMarket = ref(false)
    const showTrading = ref(false)
    const showRank = ref(false)
    const showKline = ref(false)
    const selectedStockCode = ref('')
    const selectedStockName = ref('')
let timer = null

try {
  const info = localStorage.getItem('userInfo')
  if (info) {
    userInfo.value = JSON.parse(info)
  }
} catch (e) { console.warn(e) }

const loadBalance = async () => {
  try {
    const res = await request({ url: '/account/getAccountBalance', method: 'get', params: { accountNumber: userInfo.value?.accountNumber || '' } })
    const val = res.data
    totalAssets.value = typeof val === 'number' ? val.toFixed(2) : '0.00'
  } catch (e) { console.error(e) }
}

provide('refreshBalance', loadBalance)

const handleMenuSelect = (index) => {
  if (index === 'records') showRecords.value = true
  if (index === 'products') showProducts.value = true
  if (index === 'holdings') showHoldings.value = true
  if (index === 'market') showMarket.value = true
  if (index === 'trading') showTrading.value = true
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
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--color-background);
  font-family: var(--font-sans);
}

/* ============ 顶栏 ============ */
.top-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 var(--space-6);
  background: rgba(13, 15, 26, 0.8);
  backdrop-filter: var(--glass-blur);
  border-bottom: 1px solid var(--color-border);
  position: sticky;
  top: 0;
  z-index: 100;
  flex-shrink: 0;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.brand {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.brand-emblem {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-primary);
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-secondary) 100%);
  box-shadow: var(--shadow-glow-primary);
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.brand-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-foreground);
  letter-spacing: 0.1em;
}

.brand-en {
  font-size: 8px;
  color: var(--color-muted-foreground);
  letter-spacing: 0.35em;
  font-weight: 500;
}

.header-divider {
  width: 1px;
  height: 20px;
  background: var(--color-border);
}

.header-welcome {
  font-size: 12px;
  color: var(--color-muted-foreground);
  letter-spacing: 0.04em;
}

.nav-center {
  display: flex;
  align-items: center;
  gap: calc(var(--space-1) - 2px);
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  font-size: 12px;
  color: var(--color-muted-foreground);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
  letter-spacing: 0.04em;
  font-weight: 500;
}

.nav-item:hover {
  color: var(--color-primary);
  background: rgba(192, 132, 216, 0.08);
}

.nav-right {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.server-status {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.server-text {
  font-size: 11px;
  color: var(--color-muted-foreground);
  letter-spacing: 0.04em;
  font-weight: 500;
}

.header-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-muted-foreground);
  font-weight: 500;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.user-trigger:hover {
  background: rgba(192, 132, 216, 0.06);
}

.user-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-primary) 100%);
  color: var(--color-foreground);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  box-shadow: var(--shadow-glow-accent);
}

.user-name {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-foreground);
}

/* ============ 巨幅资产面板 ============ */
.hero {
  position: relative;
  padding: var(--space-8) var(--space-10) var(--space-6);
  margin: var(--space-5) var(--space-6) 0;
  border-radius: var(--radius-2xl);
  overflow: hidden;
  background: linear-gradient(135deg, var(--color-card) 0%, rgba(30, 41, 59, 0.8) 100%);
  border: 1px solid var(--glass-border);
  box-shadow: var(--shadow-xl);
}

.hero::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 80% 20%, rgba(192, 132, 216, 0.04) 0%, transparent 50%);
  pointer-events: none;
}

.hero-glow {
  top: -50%;
  right: -6%;
  width: 400px;
  height: 400px;
}

.hero-inner {
  position: relative;
  z-index: 2;
}

.hero-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-5);
}

.hero-welcome {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.hero-greet {
  font-size: 12px;
  color: var(--color-muted-foreground);
  letter-spacing: 0.22em;
  text-transform: uppercase;
  font-weight: 600;
}

.hero-acct {
  font-size: 14px;
  color: var(--color-muted-foreground);
  letter-spacing: 0.08em;
  font-family: var(--font-mono);
}

.hero-quick {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.q-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: var(--space-2) var(--space-4);
  font-size: 12px;
  font-weight: 600;
  color: var(--color-muted-foreground);
  background: var(--color-muted);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  letter-spacing: 0.05em;
  transition: all var(--transition-fast);
  font-family: var(--font-sans);
}

.q-btn:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.q-btn.q-primary {
  color: var(--color-on-primary);
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-secondary) 100%);
  border: none;
  box-shadow: var(--shadow-glow-primary);
}

.q-btn.q-primary:hover {
  box-shadow: 0 0 30px rgba(192, 132, 216, 0.3);
}

.hero-balance {
  margin: var(--space-8) 0 var(--space-6);
}

.balance-caption {
  font-size: 12px;
  color: var(--color-muted-foreground);
  letter-spacing: 0.22em;
  margin-bottom: var(--space-2);
  text-transform: uppercase;
  font-weight: 600;
}

.balance-main {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
}

.balance-main .cur {
  font-size: 32px;
  color: var(--color-primary);
  font-weight: 600;
}

.balance-main .num {
  font-size: 56px;
  font-weight: 700;
  line-height: 1;
  color: var(--color-foreground);
  letter-spacing: 0.02em;
  font-family: var(--font-mono);
}

.balance-rule {
  margin-top: var(--space-4);
  width: 100%;
  height: 1px;
  background: linear-gradient(90deg, var(--color-border) 0%, transparent 100%);
}

.hero-meta {
  gap: 0;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 0 var(--space-4);
}

.meta-item:first-child {
  padding-left: 0;
}

.meta-label {
  font-size: 11px;
  color: var(--color-muted-foreground);
  letter-spacing: 0.14em;
  text-transform: uppercase;
  font-weight: 600;
}

.meta-value {
  font-size: 14px;
  color: var(--color-foreground);
  font-weight: 500;
  letter-spacing: 0.04em;
}

.meta-value.ok {
  color: var(--color-success);
  display: flex;
  align-items: center;
  gap: 4px;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  box-shadow: 0 0 4px currentColor;
}

.meta-divider {
  width: 1px;
  height: 28px;
  background: var(--color-border);
}

/* ============ 内容区 ============ */
.content {
  flex: 1;
  padding: var(--space-9) var(--space-8) var(--space-6);
}

.services {
  max-width: 1240px;
  margin: 0 auto;
  background: linear-gradient(135deg, var(--color-card) 0%, rgba(20, 27, 45, 0.6) 100%);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-2xl);
  padding: var(--space-9) var(--space-9);
  box-shadow: var(--shadow-lg);
  position: relative;
  overflow: hidden;
}

.services::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--color-primary), var(--color-accent), transparent);
  opacity: 0.5;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: var(--space-7);
}

.section-title {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.section-title h2 {
  font-size: 26px;
  font-weight: 700;
  color: var(--color-foreground);
  letter-spacing: 0.1em;
  margin: 0;
}

.section-title h2::after {
  content: '';
  display: block;
  margin-top: 10px;
  width: 46px;
  height: 3px;
  border-radius: 3px;
  background: linear-gradient(90deg, var(--color-primary), var(--color-secondary));
}

.section-en {
  font-size: 10px;
  color: var(--color-accent);
  letter-spacing: 0.4em;
  font-weight: 600;
  margin-top: 6px;
}

.section-sub {
  font-size: 13px;
  color: var(--color-muted-foreground);
  letter-spacing: 0.06em;
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-6);
}

.service-card {
  position: relative;
  padding: var(--space-5) var(--space-4) var(--space-4);
  border-radius: var(--radius-xl);
  border: 1px solid var(--glass-border);
  background: var(--color-muted);
  cursor: pointer;
  transition: all var(--transition-normal);
  overflow: hidden;
}

.service-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--color-primary), transparent);
  opacity: 0;
  transition: opacity var(--transition-normal);
}

.service-card:hover {
  transform: translateY(-4px);
  border-color: var(--color-primary);
  box-shadow: var(--shadow-lg), var(--shadow-glow-primary);
  background: var(--color-card);
}

.service-card:hover::before {
  opacity: 1;
}

.s-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--space-3);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.s-icon.gold {
  color: var(--color-primary);
  background: rgba(245, 158, 11, 0.1);
}

.s-icon.blue {
  color: #60A5FA;
  background: rgba(96, 165, 250, 0.1);
}

.s-icon.cyan {
  color: #2DD4BF;
  background: rgba(45, 212, 191, 0.1);
}

.s-icon.violet {
  color: var(--color-accent);
  background: rgba(139, 92, 246, 0.1);
}

.s-icon.rose {
  color: #FB7185;
  background: rgba(251, 113, 133, 0.1);
}

.s-icon.amber {
  color: var(--color-secondary);
  background: rgba(251, 191, 36, 0.1);
}

.s-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-foreground);
  letter-spacing: 0.08em;
  margin-bottom: 4px;
}

.s-desc {
  font-size: 12px;
  color: var(--color-muted-foreground);
  letter-spacing: 0.04em;
}

.s-go {
  position: absolute;
  top: var(--space-5);
  right: var(--space-5);
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 1px solid var(--color-border);
  color: var(--color-muted-foreground);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-normal);
}

.service-card:hover .s-go {
  color: var(--color-primary);
  border-color: var(--color-primary);
  transform: translateX(1px);
}

.page-foot {
  max-width: 1240px;
  margin: var(--space-8) auto 0;
  padding: 0 var(--space-2);
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: var(--color-muted-foreground);
  font-size: 12px;
  letter-spacing: 0.14em;
}

.foot-slogan {
  color: var(--color-primary);
  opacity: 0.8;
  letter-spacing: 0.3em;
}

/* ===== RESPONSIVE ===== */
@media (max-width: 1100px) {
  .service-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .nav-center {
    display: none;
  }
}

@media (max-width: 820px) {
  .service-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .hero {
    padding: var(--space-7) var(--space-6) var(--space-6);
    margin: var(--space-4) var(--space-4) 0;
  }
  .hero-top {
    flex-direction: column;
    align-items: flex-start;
  }
  .balance-main .num {
    font-size: 46px;
  }
  .meta-item {
    padding: 0 var(--space-4);
  }
  .server-text,
  .header-time,
  .header-welcome {
    display: none;
  }
}

@media (max-width: 520px) {
  .service-grid {
    grid-template-columns: 1fr;
  }
  .hero-quick {
    width: 100%;
  }
  .q-btn {
    flex: 1;
    justify-content: center;
  }
  .brand-en {
    display: none;
  }
  .content {
    padding: var(--space-4);
  }
}
</style>
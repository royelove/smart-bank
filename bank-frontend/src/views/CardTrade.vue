<template>
  <div class="trade-page">
    <!-- ============ 顶栏 · 全局导航 ============ -->
    <header class="top-nav">
      <div class="nav-left">
        <button class="back-btn" @click="goBack" title="返回">
          <el-icon :size="18"><ArrowLeft /></el-icon>
        </button>
        <div class="brand">
          <div class="brand-emblem">钱</div>
          <div class="brand-text">
            <span class="brand-name">钱钱银行</span>
            <span class="brand-en">PRIVATE BANKING</span>
          </div>
        </div>
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
            <div class="user-avatar">{{ (accountInfo?.accountHolderName || '用')[0] }}</div>
            <span class="user-name">{{ accountInfo?.accountHolderName || '用户' }}</span>
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
              <el-dropdown-item divided @click="showCardList = true">
                <el-icon><CreditCard /></el-icon> 切换银行卡
              </el-dropdown-item>
              <el-dropdown-item @click="logout">
                <el-icon><SwitchButton /></el-icon> 退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- ============ 巨幅资产面板 ============ -->
    <section class="hero">
      <div class="vault-contour"></div>
      <div class="hero-glow"></div>

      <div class="hero-inner">
        <div class="hero-top">
          <div class="account-chips">
            <div
              v-for="card in myCards"
              :key="card.accountNumber"
              class="acct-chip"
              :class="{ active: card.accountNumber === accountNumber }"
              @click="switchCard(card.accountNumber)"
            >
              <el-icon :size="13"><CreditCard /></el-icon>
              <span class="chip-num vault-num">{{ card.accountNumber }}</span>
            </div>
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
            <span class="num vault-num">{{ balance }}</span>
          </div>
          <div class="balance-rule"></div>
        </div>

        <div class="hero-meta">
          <div class="meta-item">
            <span class="meta-label">户主姓名</span>
            <span class="meta-value">{{ accountInfo?.accountHolderName || '--' }}</span>
          </div>
          <div class="meta-divider"></div>
          <div class="meta-item">
            <span class="meta-label">账户类型</span>
            <span class="meta-value">{{ accountInfo?.accountType || '--' }}</span>
          </div>
          <div class="meta-divider"></div>
          <div class="meta-item">
            <span class="meta-label">账户状态</span>
            <span class="meta-value" :class="accountInfo?.accountStatus === 1 ? 'ok' : 'bad'">
              <span class="status-dot"></span>
              {{ accountInfo?.accountStatus === 1 ? '正常' : '冻结' }}
            </span>
          </div>
          <div class="meta-divider"></div>
          <div class="meta-item">
            <span class="meta-label">实名证件</span>
            <span class="meta-value vault-num">{{ maskIdCard(accountInfo?.userIdCard) }}</span>
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
          <div class="service-card" @click="showMarket = true">
            <div class="s-icon rose"><el-icon :size="24"><TrendCharts /></el-icon></div>
            <div class="s-name">股票行情</div>
            <div class="s-desc">实时盯盘，洞察先机</div>
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
        <span class="foot-brand">钱钱银行 · SMART BANKING</span>
        <span class="foot-slogan">稳健笃行 · 财富永续</span>
      </footer>
    </main>

    <el-dialog v-model="showDeposit" title="存款" width="500px" destroy-on-close>
      <Deposit :account-number="accountNumber" @close="showDeposit = false" @refresh="loadBalance" />
    </el-dialog>
    <el-dialog v-model="showWithdraw" title="取款" width="500px" destroy-on-close>
      <Withdraw :account-number="accountNumber" @close="showWithdraw = false" @refresh="loadBalance" />
    </el-dialog>
    <el-dialog v-model="showTransfer" title="转账" width="500px" destroy-on-close>
      <Transfer :account-number="accountNumber" @close="showTransfer = false" @refresh="loadBalance" />
    </el-dialog>
    <el-dialog v-model="showRecords" title="交易明细" width="900px" destroy-on-close>
      <TradeRecords :account-number="accountNumber" />
    </el-dialog>
    <el-dialog v-model="showProducts" title="理财产品" width="800px" destroy-on-close>
      <ProductList :account-number="accountNumber" @refresh-balance="loadBalance" />
    </el-dialog>
    <el-dialog v-model="showHoldings" title="我的持仓" width="800px" destroy-on-close>
      <MyHoldings :account-number="accountNumber" @refresh-balance="loadBalance" />
    </el-dialog>
    <el-dialog v-model="showMarket" title="股票行情" width="980px" destroy-on-close>
      <StockMarket />
    </el-dialog>
    <el-dialog v-model="showTrading" title="股票交易" width="980px" destroy-on-close @closed="loadBalance">
      <StockTrading :account-number="accountNumber" @refresh-balance="loadBalance" />
    </el-dialog>
    <el-dialog v-model="showProfile" title="个人信息" width="480px" destroy-on-close>
      <ProfileSettings @close="showProfile = false" />
    </el-dialog>
    <el-dialog v-model="showChangePwd" title="修改密码" width="480px" destroy-on-close>
      <ChangePassword @close="showChangePwd = false" />
    </el-dialog>
    <el-dialog v-model="showCardList" title="切换银行卡" width="420px" destroy-on-close>
      <div class="card-list">
        <div
          v-for="card in myCards"
          :key="card.accountNumber"
          class="card-item"
          :class="{ active: card.accountNumber === accountNumber }"
          @click="switchCard(card.accountNumber)"
        >
          <div class="card-item-left">
            <el-icon :size="20" color="#d4b06a"><CreditCard /></el-icon>
            <div>
              <div class="card-item-number vault-num">{{ card.accountNumber }}</div>
              <div class="card-item-name">{{ card.accountHolderName }}</div>
            </div>
          </div>
          <el-tag v-if="card.accountNumber === accountNumber" size="small" type="warning">当前</el-tag>
        </div>
      </div>
    </el-dialog>

    <ChatBot />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, provide } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { getUserAccounts } from '@/api/user'
import Deposit from '@/components/Deposit.vue'
import Withdraw from '@/components/Withdraw.vue'
import Transfer from '@/components/Transfer.vue'
import TradeRecords from '@/components/TradeRecords.vue'
import ProductList from '@/components/ProductList.vue'
import MyHoldings from '@/components/MyHoldings.vue'
import StockMarket from '@/components/StockMarket.vue'
import StockTrading from '@/components/StockTrading.vue'
import ChangePassword from '@/components/ChangePassword.vue'
import ProfileSettings from '@/components/ProfileSettings.vue'
import ChatBot from '@/components/ChatBot.vue'
import { ArrowDown, SwitchButton, Clock, Key, User, CreditCard, ArrowLeft, Wallet, Coin, Right, Document, DataAnalysis, TrendCharts, Odometer } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const accountNumber = route.params.accountNumber

const accountInfo = ref(null)
const balance = ref('0.00')
const currentTime = ref('')
const myCards = ref([])

const showDeposit = ref(false)
const showWithdraw = ref(false)
const showTransfer = ref(false)
const showRecords = ref(false)
const showProducts = ref(false)
const showHoldings = ref(false)
const showMarket = ref(false)
const showTrading = ref(false)
const showProfile = ref(false)
const showChangePwd = ref(false)
const showCardList = ref(false)
let timer = null

const maskIdCard = (id) => {
  if (!id || id.length < 10) return id || '--'
  return id.substring(0, 6) + '********' + id.substring(14)
}

const loadBalance = async () => {
  try {
    const [infoRes, balRes] = await Promise.all([
      request({ url: '/account/getAccountEntity', method: 'post', params: { accountNumber } }),
      request({ url: '/account/getAccountBalance', method: 'get', params: { accountNumber } })
    ])
    accountInfo.value = infoRes.data
    const val = balRes.data
    balance.value = typeof val === 'number' ? val.toFixed(2) : '0.00'
  } catch (e) { console.error(e) }
}

const loadMyCards = async () => {
  try {
    const res = await getUserAccounts()
    myCards.value = res.data || []
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

const switchCard = (cardNumber) => {
  showCardList.value = false
  const card = myCards.value.find(c => c.accountNumber === cardNumber)
  if (card) {
    localStorage.setItem('userInfo', JSON.stringify(card))
  }
  router.push('/dashboard')
}

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', { hour12: false })
}

const goBack = () => {
  router.push('/user/dashboard')
}

const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  ElMessage.success('已安全退出')
  router.push('/user/login')
}

onMounted(async () => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  await Promise.all([loadBalance(), loadMyCards()])
})
onUnmounted(() => { clearInterval(timer) })
</script>

<style scoped>
.trade-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-background);
  font-family: var(--font-sans);
}

/* ============ 顶栏 ============ */
.top-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 var(--space-8);
  background: rgba(20, 27, 45, 0.9);
  backdrop-filter: blur(var(--glass-blur));
  border-bottom: 1px solid var(--glass-border);
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-left { display: flex; align-items: center; gap: var(--space-4); }

.back-btn {
  width: 36px; height: 36px;
  display: flex; align-items: center; justify-content: center;
  border-radius: var(--radius-md);
  color: var(--color-muted-foreground);
  background: var(--color-muted);
  border: 1px solid var(--glass-border);
  cursor: pointer;
  transition: all var(--transition-normal);
}

.back-btn:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
  background: rgba(245, 158, 11, 0.06);
}

.brand { display: flex; align-items: center; gap: var(--space-3); }

.brand-emblem {
  width: 40px; height: 40px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; font-weight: 800;
  color: var(--color-on-primary);
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-secondary) 100%);
  box-shadow: var(--shadow-glow-primary);
}

.brand-text { display: flex; flex-direction: column; line-height: 1.2; }

.brand-name { font-size: 16px; font-weight: 700; color: var(--color-foreground); }

.brand-en { font-size: 9px; color: var(--color-accent); letter-spacing: 0.3em; font-weight: 600; }

.nav-center { display: flex; align-items: center; gap: var(--space-1); }

.nav-item {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 14px;
  font-size: 13px;
  color: var(--color-muted-foreground);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-normal);
  font-weight: 500;
}

.nav-item:hover { color: var(--color-primary); background: rgba(245, 158, 11, 0.06); }

.nav-right { display: flex; align-items: center; gap: var(--space-5); }

.server-status { display: flex; align-items: center; gap: 6px; }

.vault-status-dot {
  width: 8px; height: 8px; border-radius: 50%;
  background: var(--color-success);
  box-shadow: 0 0 6px var(--color-success);
}

.server-text { font-size: 12px; color: var(--color-muted-foreground); font-weight: 500; }

.header-time { display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--color-muted-foreground); font-weight: 500; }

.user-trigger { display: flex; align-items: center; gap: 8px; cursor: pointer; padding: 6px 10px; border-radius: var(--radius-md); transition: all var(--transition-normal); }

.user-trigger:hover { background: var(--color-muted); }

.user-avatar {
  width: 34px; height: 34px; border-radius: 50%;
  background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-primary) 100%);
  color: var(--color-foreground);
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 700;
  box-shadow: var(--shadow-glow-accent);
}

.user-name { font-size: 14px; font-weight: 600; color: var(--color-foreground); }

/* ============ 巨幅资产面板 ============ */
.hero {
  position: relative;
  padding: var(--space-10) var(--space-16) var(--space-8);
  margin: var(--space-7) var(--space-8) 0;
  border-radius: var(--radius-2xl);
  overflow: hidden;
  background: linear-gradient(135deg, var(--color-card) 0%, rgba(20, 27, 45, 0.8) 100%);
  border: 1px solid var(--glass-border);
  box-shadow: var(--shadow-lg);
}

.hero::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 80% 20%, rgba(245, 158, 11, 0.05) 0%, transparent 70%);
  pointer-events: none;
}

.hero-glow {
  position: absolute;
  top: -50%; right: -10%;
  width: 500px; height: 500px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(245,158,11,0.06) 0%, transparent 70%);
  pointer-events: none;
}

.hero-inner { position: relative; z-index: 2; }

.hero-top { display: flex; justify-content: space-between; align-items: center; gap: var(--space-6); }

.account-chips { display: flex; align-items: center; gap: var(--space-3); flex-wrap: wrap; }

.acct-chip {
  display: flex; align-items: center; gap: 6px;
  padding: 7px 14px;
  border-radius: var(--radius-full);
  border: 1px solid var(--glass-border);
  color: var(--color-muted-foreground);
  font-size: 13px;
  cursor: pointer;
  transition: all var(--transition-normal);
  background: var(--color-muted);
}

.acct-chip .el-icon { color: var(--color-accent); }

.acct-chip:hover { color: var(--color-foreground); border-color: var(--color-border-hover); }

.acct-chip.active {
  color: var(--color-primary);
  border-color: var(--color-primary);
  background: rgba(245,158,11,0.08);
  box-shadow: var(--shadow-glow-primary);
}

.chip-num { font-family: var(--font-mono); letter-spacing: 0.06em; }

.hero-quick { display: flex; align-items: center; gap: var(--space-3); }

.q-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 10px 20px;
  font-size: 13px; font-weight: 600;
  color: var(--color-muted-foreground);
  background: var(--color-muted);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-normal);
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
  box-shadow: 0 0 30px rgba(245, 158, 11, 0.3);
  transform: translateY(-1px);
}

.hero-balance { margin: var(--space-10) 0 var(--space-8); }

.balance-caption {
  font-size: 12px;
  color: var(--color-muted-foreground);
  letter-spacing: 0.2em;
  margin-bottom: var(--space-3);
  text-transform: uppercase;
  font-weight: 600;
}

.balance-main { display: flex; align-items: baseline; gap: var(--space-3); }

.balance-main .cur { font-size: 32px; color: var(--color-primary); font-weight: 600; }

.balance-main .num {
  font-family: var(--font-mono);
  font-size: 56px;
  font-weight: 700;
  line-height: 1;
  color: var(--color-foreground);
  letter-spacing: 0.02em;
}

.balance-rule {
  margin-top: var(--space-6);
  width: 100%;
  height: 1px;
  background: linear-gradient(90deg, var(--color-border) 0%, transparent 100%);
}

.hero-meta { display: flex; align-items: center; flex-wrap: wrap; }

.meta-item { display: flex; flex-direction: column; gap: 4px; padding: 0 var(--space-9); }

.meta-item:first-child { padding-left: 0; }

.meta-label { font-size: 11px; color: var(--color-muted-foreground); letter-spacing: 0.1em; text-transform: uppercase; font-weight: 600; }

.meta-value { font-size: 14px; color: var(--color-foreground); font-weight: 500; }

.meta-value.ok { color: var(--color-success); display: flex; align-items: center; gap: 6px; }

.meta-value.bad { color: var(--color-destructive); display: flex; align-items: center; gap: 6px; }

.status-dot { width: 7px; height: 7px; border-radius: 50%; background: currentColor; }

.meta-divider { width: 1px; height: 32px; background: var(--color-border); }

/* ============ 内容区 ============ */
.content { flex: 1; padding: var(--space-9) var(--space-8) var(--space-6); }

.services {
  max-width: 1200px;
  margin: 0 auto;
  background: linear-gradient(135deg, var(--color-card) 0%, rgba(20, 27, 45, 0.6) 100%);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-2xl);
  padding: var(--space-9) var(--space-9);
  box-shadow: var(--shadow-md);
}

.section-head { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: var(--space-7); }

.section-title { display: flex; flex-direction: column; gap: 4px; }

.section-title h2 {
  font-size: 24px; font-weight: 700; color: var(--color-foreground); margin: 0;
}

.section-title h2::after {
  content: '';
  display: block;
  margin-top: var(--space-3);
  width: 40px; height: 3px;
  border-radius: 3px;
  background: linear-gradient(90deg, var(--color-primary), var(--color-secondary));
}

.section-en { font-size: 10px; color: var(--color-accent); letter-spacing: 0.35em; font-weight: 600; margin-top: 4px; }

.section-sub { font-size: 13px; color: var(--color-muted-foreground); }

.service-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-5);
}

.service-card {
  position: relative;
  padding: var(--space-8) var(--space-7) var(--space-6);
  border-radius: var(--radius-lg);
  border: 1px solid var(--glass-border);
  background: var(--color-muted);
  cursor: pointer;
  transition: all var(--transition-slow);
  overflow: hidden;
}

.service-card:hover {
  transform: translateY(-4px);
  border-color: var(--color-primary);
  box-shadow: 0 12px 32px rgba(0,0,0,0.3), 0 0 0 1px rgba(245,158,11,0.15);
  background: var(--color-card);
}

.s-icon {
  width: 48px; height: 48px;
  border-radius: var(--radius-md);
  display: flex; align-items: center; justify-content: center;
  margin-bottom: var(--space-4);
}

.s-icon.gold { color: var(--color-primary); background: rgba(245,158,11,0.1); }
.s-icon.blue { color: #60A5FA; background: rgba(96,165,250,0.1); }
.s-icon.cyan { color: #2DD4BF; background: rgba(45,212,191,0.1); }
.s-icon.violet { color: var(--color-accent); background: rgba(139,92,246,0.1); }
.s-icon.rose { color: #FB7185; background: rgba(251,113,133,0.1); }
.s-icon.amber { color: var(--color-secondary); background: rgba(251,191,36,0.1); }

.s-name { font-size: 16px; font-weight: 600; color: var(--color-foreground); margin-bottom: 4px; }
.s-desc { font-size: 12px; color: var(--color-muted-foreground); }

.s-go {
  position: absolute;
  top: var(--space-6); right: var(--space-6);
  width: 28px; height: 28px;
  border-radius: 50%;
  border: 1px solid var(--color-border);
  color: var(--color-muted-foreground);
  display: flex; align-items: center; justify-content: center;
  transition: all var(--transition-normal);
}

.service-card:hover .s-go {
  color: var(--color-primary);
  border-color: var(--color-primary);
  transform: translateX(2px);
}

.page-foot {
  max-width: 1200px;
  margin: var(--space-8) auto 0;
  padding: 0 var(--space-2);
  display: flex; justify-content: space-between; align-items: center;
  color: var(--color-muted-foreground);
  font-size: 12px;
  opacity: 0.7;
}

.foot-slogan { color: var(--color-primary); letter-spacing: 0.2em; }

/* ============ 切卡列表 ============ */
.card-list { display: flex; flex-direction: column; gap: var(--space-2); }
.card-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-normal);
  border: 1px solid var(--glass-border);
  background: var(--color-muted);
}
.card-item:hover { background: var(--color-card); border-color: var(--color-border-hover); }
.card-item.active { background: rgba(245,158,11,0.08); border-color: var(--color-primary); }
.card-item-left { display: flex; align-items: center; gap: var(--space-3); }
.card-item-number { font-family: var(--font-mono); font-size: 14px; font-weight: 600; color: var(--color-foreground); }
.card-item-name { font-size: 12px; color: var(--color-muted-foreground); }

/* ============ 响应式 ============ */
@media (max-width: 1100px) {
  .service-grid { grid-template-columns: repeat(3, 1fr); }
  .nav-center { display: none; }
}

@media (max-width: 820px) {
  .service-grid { grid-template-columns: repeat(2, 1fr); }
  .hero { padding: var(--space-7) var(--space-6) var(--space-6); margin: var(--space-4) var(--space-4) 0; }
  .hero-top { flex-direction: column; align-items: flex-start; }
  .balance-main .num { font-size: 40px; }
  .meta-item { padding: 0 var(--space-4); }
  .server-text, .header-time { display: none; }
}

@media (max-width: 520px) {
  .service-grid { grid-template-columns: 1fr; }
  .hero-quick { width: 100%; }
  .q-btn { flex: 1; justify-content: center; }
  .brand-en { display: none; }
  .content { padding: var(--space-4); }
}
</style>
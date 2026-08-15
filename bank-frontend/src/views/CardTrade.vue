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
  } catch (e) {
    console.error(e)
  }
}

const loadMyCards = async () => {
  try {
    const res = await getUserAccounts()
    myCards.value = res.data || []
  } catch (e) {
    console.error(e)
  }
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
  router.push(`/trade/${cardNumber}`)
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
/* ============================================================
   深藏蓝 × 香槟金 · 奢华排布
   ============================================================ */
.trade-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(1400px 640px at 85% -12%, rgba(212,176,106,0.06), transparent 60%),
    radial-gradient(1200px 600px at 8% 8%, rgba(47,168,160,0.04), transparent 55%),
    var(--vault-bg);
}

/* ============ 顶栏 ============ */
.top-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 72px;
  padding: 0 32px;
  background: rgba(10,16,28,0.82);
  border-bottom: 1px solid var(--vault-line);
  backdrop-filter: blur(12px);
  position: sticky;
  top: 0;
  z-index: 100;
}
.nav-left { display: flex; align-items: center; gap: 14px; }
.back-btn {
  width: 38px; height: 38px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 10px;
  color: var(--vault-text-2);
  background: transparent;
  border: 1px solid var(--vault-line);
  cursor: pointer;
  transition: all 0.2s;
}
.back-btn:hover { color: var(--vault-gold); border-color: var(--vault-line-hover); }
.brand { display: flex; align-items: center; gap: 12px; }
.brand-emblem {
  width: 40px; height: 40px;
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; font-weight: 800;
  color: #231a08;
  border-radius: 12px;
  background: linear-gradient(135deg, #e6c98a, #b08d4a);
  box-shadow: 0 0 20px var(--vault-accent-glow);
}
.brand-text { display: flex; flex-direction: column; line-height: 1.15; }
.brand-name { font-size: 17px; font-weight: 700; color: var(--vault-text); letter-spacing: 0.18em; }
.brand-en { font-size: 9px; color: var(--vault-accent); letter-spacing: 0.42em; font-weight: 600; }

.nav-center { display: flex; align-items: center; gap: 4px; }
.nav-item {
  display: flex; align-items: center; gap: 6px;
  padding: 9px 16px;
  font-size: 14px;
  color: var(--vault-text-2);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  letter-spacing: 0.05em;
}
.nav-item:hover { color: var(--vault-gold); background: rgba(212,176,106,0.08); }

.nav-right { display: flex; align-items: center; gap: 22px; }
.server-status { display: flex; align-items: center; gap: 8px; }
.server-text { font-size: 12px; color: var(--vault-text-3); letter-spacing: 0.06em; }
.header-time { display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--vault-text-3); font-weight: 500; }
.user-trigger { display: flex; align-items: center; gap: 8px; cursor: pointer; padding: 5px 10px; border-radius: 10px; transition: all 0.2s; }
.user-trigger:hover { background: rgba(212,176,106,0.08); }
.user-avatar {
  width: 34px; height: 34px; border-radius: 50%;
  background: linear-gradient(135deg, #3a2c14, #5a4420);
  border: 1px solid var(--vault-line-strong);
  color: var(--vault-gold);
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 700;
}
.user-name { font-size: 14px; font-weight: 500; color: var(--vault-text); }

/* ============ 巨幅资产面板 ============ */
.hero {
  position: relative;
  padding: 40px 64px 34px;
  margin: 28px 32px 0;
  border-radius: 22px;
  overflow: hidden;
  background: linear-gradient(150deg, #131f38 0%, #0f1830 48%, #141c30 100%);
  border: 1px solid var(--vault-line-strong);
  box-shadow: inset 0 1px 0 rgba(212,176,106,0.10), 0 24px 64px rgba(0,0,0,0.45);
}
.hero-glow {
  position: absolute;
  top: -60%; right: -12%;
  width: 620px; height: 620px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(212,176,106,0.12) 0%, transparent 68%);
  pointer-events: none;
}
.hero-inner { position: relative; z-index: 2; }

.hero-top { display: flex; justify-content: space-between; align-items: center; gap: 24px; }
.account-chips { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.acct-chip {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 16px;
  border-radius: 20px;
  border: 1px solid var(--vault-line);
  color: var(--vault-text-3);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.22s;
  background: rgba(212,176,106,0.02);
}
.acct-chip .el-icon { color: var(--vault-accent); }
.acct-chip:hover { color: var(--vault-text); border-color: var(--vault-line-hover); }
.acct-chip.active {
  color: var(--vault-gold);
  border-color: var(--vault-accent);
  background: rgba(212,176,106,0.12);
  box-shadow: 0 0 16px rgba(212,176,106,0.18);
}
.chip-num { letter-spacing: 0.08em; }

.hero-quick { display: flex; align-items: center; gap: 12px; }
.q-btn {
  display: flex; align-items: center; gap: 7px;
  padding: 11px 22px;
  font-size: 14px; font-weight: 600;
  color: var(--vault-text-2);
  background: rgba(212,176,106,0.04);
  border: 1px solid var(--vault-line-strong);
  border-radius: 12px;
  cursor: pointer;
  letter-spacing: 0.08em;
  transition: all 0.22s;
}
.q-btn:hover { color: var(--vault-gold); border-color: var(--vault-line-hover); transform: translateY(-1px); }
.q-btn.q-primary {
  color: #231a08;
  background: linear-gradient(135deg, #e6c98a, #b08d4a);
  border: none;
  box-shadow: 0 6px 22px rgba(212,176,106,0.30);
}
.q-btn.q-primary:hover { color: #1a1306; box-shadow: 0 8px 28px rgba(212,176,106,0.42); }

.hero-balance { margin: 40px 0 30px; }
.balance-caption {
  font-size: 13px;
  color: var(--vault-text-3);
  letter-spacing: 0.24em;
  margin-bottom: 10px;
}
.balance-main { display: flex; align-items: baseline; gap: 10px; }
.balance-main .cur { font-size: 34px; color: var(--vault-gold); font-weight: 600; }
.balance-main .num {
  font-size: 64px;
  font-weight: 700;
  line-height: 1;
  color: var(--vault-text);
  letter-spacing: 0.04em;
  background: linear-gradient(180deg, #ffffff 0%, var(--vault-gold) 120%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 0 40px rgba(212,176,106,0.18);
}
.balance-rule {
  margin-top: 22px;
  width: 100%;
  height: 1px;
  background: linear-gradient(90deg, rgba(212,176,106,0.34), rgba(212,176,106,0.05) 60%, transparent);
}

.hero-meta { display: flex; align-items: center; gap: 0; flex-wrap: wrap; }
.meta-item { display: flex; flex-direction: column; gap: 5px; padding: 0 36px; }
.meta-item:first-child { padding-left: 0; }
.meta-label { font-size: 11px; color: var(--vault-text-dim); letter-spacing: 0.16em; }
.meta-value { font-size: 15px; color: var(--vault-text-2); font-weight: 500; letter-spacing: 0.06em; }
.meta-value.ok { color: var(--vault-up-soft); display: flex; align-items: center; gap: 6px; }
.meta-value.bad { color: var(--vault-down-soft); display: flex; align-items: center; gap: 6px; }
.status-dot { width: 7px; height: 7px; border-radius: 50%; background: currentColor; }
.meta-divider { width: 1px; height: 32px; background: var(--vault-line); }

/* ============ 内容区 ============ */
.content { flex: 1; padding: 36px 32px 24px; }
.services {
  max-width: 1240px;
  margin: 0 auto;
  background: linear-gradient(180deg, var(--vault-panel-2), var(--vault-panel));
  border: 1px solid var(--vault-line);
  border-radius: 22px;
  padding: 34px 36px;
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.03), 0 16px 44px rgba(0,0,0,0.32);
  position: relative;
}
.services::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(212,176,106,0.45), transparent);
}
.section-head { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 26px; }
.section-title { display: flex; flex-direction: column; gap: 3px; }
.section-title h2 { font-size: 26px; font-weight: 700; color: var(--vault-text); letter-spacing: 0.1em; margin: 0; }
.section-title h2::after {
  content: '';
  display: block;
  margin-top: 10px;
  width: 46px; height: 3px;
  border-radius: 3px;
  background: linear-gradient(90deg, var(--vault-accent), transparent);
}
.section-en { font-size: 10px; color: var(--vault-accent); letter-spacing: 0.4em; font-weight: 600; margin-top: 6px; }
.section-sub { font-size: 13px; color: var(--vault-text-3); letter-spacing: 0.06em; }

.service-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}
.service-card {
  position: relative;
  padding: 30px 26px 24px;
  border-radius: 18px;
  border: 1px solid var(--vault-line);
  background: linear-gradient(165deg, rgba(212,176,106,0.045), rgba(255,255,255,0.012) 55%);
  cursor: pointer;
  transition: all 0.28s;
  overflow: hidden;
}
.service-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(212,176,106,0.6), transparent);
  opacity: 0;
  transition: opacity 0.28s;
}
.service-card:hover {
  transform: translateY(-4px);
  border-color: var(--vault-line-hover);
  box-shadow: 0 18px 40px rgba(0,0,0,0.42), 0 0 24px rgba(212,176,106,0.10);
  background: linear-gradient(165deg, rgba(212,176,106,0.10), rgba(255,255,255,0.02) 55%);
}
.service-card:hover::before { opacity: 1; }
.s-icon {
  width: 54px; height: 54px;
  border-radius: 15px;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 18px;
  border: 1px solid rgba(255,255,255,0.10);
}
.s-icon.gold { color: #e6c98a; background: linear-gradient(135deg, rgba(212,176,106,0.20), rgba(212,176,106,0.04)); }
.s-icon.blue { color: #7ea8f7; background: linear-gradient(135deg, rgba(126,168,247,0.18), rgba(126,168,247,0.03)); }
.s-icon.cyan { color: #3fc9bf; background: linear-gradient(135deg, rgba(63,201,191,0.16), rgba(63,201,191,0.03)); }
.s-icon.violet { color: #a78bfa; background: linear-gradient(135deg, rgba(167,139,250,0.18), rgba(167,139,250,0.03)); }
.s-icon.rose { color: #f48fb1; background: linear-gradient(135deg, rgba(244,143,177,0.18), rgba(244,143,177,0.03)); }
.s-icon.amber { color: #f0b14e; background: linear-gradient(135deg, rgba(240,177,78,0.20), rgba(240,177,78,0.04)); }
.s-name { font-size: 17px; font-weight: 600; color: var(--vault-text); letter-spacing: 0.1em; margin-bottom: 6px; }
.s-desc { font-size: 12px; color: var(--vault-text-3); letter-spacing: 0.05em; }
.s-go {
  position: absolute;
  top: 24px; right: 24px;
  width: 28px; height: 28px;
  border-radius: 50%;
  border: 1px solid var(--vault-line);
  color: var(--vault-text-3);
  display: flex; align-items: center; justify-content: center;
  transition: all 0.25s;
}
.service-card:hover .s-go { color: var(--vault-gold); border-color: var(--vault-accent); transform: translateX(2px); }

.page-foot {
  max-width: 1240px;
  margin: 30px auto 0;
  padding: 0 8px;
  display: flex; justify-content: space-between; align-items: center;
  color: var(--vault-text-dim);
  font-size: 12px;
  letter-spacing: 0.14em;
}
.foot-slogan { color: var(--vault-accent); opacity: 0.8; letter-spacing: 0.3em; }

/* ============ 切卡列表 ============ */
.card-list { display: flex; flex-direction: column; gap: 8px; }
.card-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 16px; border-radius: 10px; cursor: pointer; transition: all 0.2s;
  border: 1px solid var(--vault-line);
}
.card-item:hover { background: rgba(212,176,106,0.05); border-color: var(--vault-line-hover); }
.card-item.active { background: rgba(212,176,106,0.08); border-color: var(--vault-accent); }
.card-item-left { display: flex; align-items: center; gap: 12px; }
.card-item-number { font-size: 14px; font-weight: 600; color: var(--vault-text); letter-spacing: 0.06em; }
.card-item-name { font-size: 12px; color: var(--vault-text-3); }

/* ============ 响应式 ============ */
@media (max-width: 1100px) {
  .service-grid { grid-template-columns: repeat(3, 1fr); }
  .nav-center { display: none; }
}
@media (max-width: 820px) {
  .service-grid { grid-template-columns: repeat(2, 1fr); }
  .hero { padding: 28px 24px 24px; margin: 16px 16px 0; }
  .hero-top { flex-direction: column; align-items: flex-start; }
  .balance-main .num { font-size: 46px; }
  .meta-item { padding: 0 18px; }
  .server-text, .header-time { display: none; }
}
@media (max-width: 520px) {
  .service-grid { grid-template-columns: 1fr; }
  .hero-quick { width: 100%; }
  .q-btn { flex: 1; justify-content: center; }
  .brand-en { display: none; }
}
</style>

<template>
  <div class="product-list">
    <div class="pl-header">
      <div>
        <h3 class="pl-title">理财产品</h3>
        <p class="pl-desc">稳健理财，让财富持续增值</p>
      </div>
    </div>

    <div v-if="recommends.length > 0" class="recommend-section">
      <div class="recommend-header">
        <div>
          <h3 class="recommend-title">为你推荐</h3>
          <p class="recommend-sub">基于风险偏好画像 · {{ recommends[0].userRiskLevel }}</p>
        </div>
        <span class="recommend-badge">智能推荐</span>
      </div>
      <div class="recommend-grid">
        <div v-for="item in recommends" :key="item.product.id" class="recommend-card" @click="openBuy(item.product)">
          <div class="rec-top">
            <span class="rec-name">{{ item.product.productName }}</span>
            <span class="rec-rate">{{ item.product.annualReturnRate }}%</span>
          </div>
          <div class="rec-meta">期限 {{ item.product.termDays }} 天 · 起购 ¥{{ item.product.minAmount }}</div>
          <el-progress :percentage="item.matchScore" :stroke-width="8" :color="matchColor(item.matchScore)" />
          <div class="rec-reason">{{ item.reason }}</div>
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="3" animated />
    </div>

    <div v-else-if="products.length === 0" class="empty-wrap">
      <el-empty description="暂无可售理财产品" />
    </div>

    <div v-else class="product-grid">
      <div
        v-for="item in products"
        :key="item.id"
        class="product-card"
        @click="openBuy(item)"
      >
        <div class="pc-top">
          <span class="pc-badge" :class="{ hot: item.annualReturnRate >= 4 }">
            {{ item.annualReturnRate >= 4 ? '热销' : '稳健' }}
          </span>
          <span class="pc-code">{{ item.productCode }}</span>
        </div>
        <h4 class="pc-name">{{ item.productName }}</h4>
        <div class="pc-rate">
          <span class="rate-num">{{ item.annualReturnRate }}</span>
          <span class="rate-unit">%</span>
        </div>
        <div class="pc-meta">预期年化收益率</div>
        <div class="pc-info">
          <div class="info-item">
            <span class="info-label">期限</span>
            <span class="info-value">{{ item.termDays }}天</span>
          </div>
          <div class="info-item">
            <span class="info-label">起购</span>
            <span class="info-value">¥{{ item.minAmount }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">剩余</span>
            <span class="info-value">¥{{ item.remainingAmount }}</span>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="buyVisible" title="购买理财产品" width="420px" :close-on-click-modal="false">
      <el-form :model="buyForm" label-width="0">
        <div class="buy-product-info">
          <h4>{{ currentProduct?.productName }}</h4>
          <div class="buy-rate">
            预期年化收益率：<span class="highlight">{{ currentProduct?.annualReturnRate }}%</span>
          </div>
          <div class="buy-rate">
            期限：<span class="highlight">{{ currentProduct?.termDays }}天</span>
          </div>
          <div class="buy-rate">
            起购金额：<span class="highlight">¥{{ currentProduct?.minAmount }}</span>
          </div>
        </div>
        <div class="form-row">
          <label class="atm-label">购买金额</label>
          <el-input-number
            v-model="buyForm.amount"
            :precision="2"
            :step="1000"
            :min="Number(currentProduct?.minAmount || 0)"
            :max="Number(currentProduct?.remainingAmount || Infinity)"
            size="large"
            style="width: 100%"
          />
        </div>
      </el-form>
      <template #footer>
        <el-button @click="buyVisible = false">取消</el-button>
        <el-button type="primary" :loading="buying" @click="handleBuy">确认购买</el-button>
      </template>
    </el-dialog>

    <PasswordDialog
      :visible="passwordVisible"
      title="请输入交易密码以确认购买"
      @update:visible="passwordVisible = $event"
      @confirm="handlePasswordConfirm"
      @cancel="passwordVisible = false"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listProducts, purchaseProduct, getRecommendations } from '@/api/financial'
import PasswordDialog from './PasswordDialog.vue'

const emit = defineEmits(['refreshBalance'])
const props = defineProps({
  accountNumber: { type: String, required: true }
})
const loading = ref(false)
const products = ref([])
const recommends = ref([])
const buyVisible = ref(false)
const buying = ref(false)
const currentProduct = ref(null)
const buyForm = ref({ amount: 0 })
const passwordVisible = ref(false)
const password = ref('')

const loadProducts = async () => {
  loading.value = true
  try {
    const res = await listProducts()
    products.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const loadRecommend = async () => {
  try {
    const res = await getRecommendations(props.accountNumber, 3)
    recommends.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

const matchColor = (score) => {
  if (score >= 85) return 'var(--color-success)'
  if (score >= 70) return 'var(--color-accent)'
  return 'var(--color-primary)'
}

const openBuy = (product) => {
  currentProduct.value = product
  buyForm.value.amount = Number(product.minAmount)
  buyVisible.value = true
}

const handleBuy = () => {
  if (!buyForm.value.amount || buyForm.value.amount <= 0) {
    ElMessage.warning('请输入购买金额')
    return
  }
  passwordVisible.value = true
}

const handlePasswordConfirm = async (pwd) => {
  password.value = pwd
  passwordVisible.value = false
  buying.value = true
  try {
    await purchaseProduct({
      accountNumber: props.accountNumber,
      productId: currentProduct.value.id,
      amount: buyForm.value.amount,
      password: password.value,
      requestId: crypto.randomUUID()
    })
    ElMessage.success('购买成功')
    buyVisible.value = false
    emit('refreshBalance')
    loadProducts()
    loadRecommend()
  } catch (e) {
    console.error(e)
  } finally {
    buying.value = false
  }
}

onMounted(() => {
  loadProducts()
  loadRecommend()
})
</script>

<style scoped>
.product-list {
  padding: var(--space-1) 0;
}

.pl-header {
  margin-bottom: var(--space-5);
}

.pl-title {
  font-size: var(--font-h3);
  font-weight: 700;
  color: var(--color-foreground);
  margin: 0 0 var(--space-1);
  letter-spacing: 0.08em;
  font-family: var(--font-sans);
}

.pl-desc {
  font-size: var(--font-base);
  color: var(--color-muted-foreground);
  margin: 0;
  font-family: var(--font-sans);
}

.recommend-section {
  background: var(--color-card);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
  padding: var(--space-5);
  margin-bottom: var(--space-5);
  box-shadow: var(--shadow-md);
}

.recommend-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-4);
}

.recommend-title {
  font-size: var(--font-h3);
  font-weight: 700;
  color: var(--color-foreground);
  margin: 0 0 var(--space-1);
  font-family: var(--font-sans);
}

.recommend-sub {
  font-size: var(--font-base);
  color: var(--color-muted-foreground);
  margin: 0;
  font-family: var(--font-sans);
}

.recommend-badge {
  font-size: var(--font-sm);
  font-weight: 600;
  color: var(--color-on-primary);
  background: var(--color-primary);
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-full);
  box-shadow: var(--shadow-md);
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: var(--space-3);
}

.recommend-card {
  background: var(--color-muted);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
  padding: var(--space-4);
  cursor: pointer;
  transition: all var(--transition-normal);
}

.recommend-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
  border-color: var(--color-border-hover);
}

.rec-top {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: var(--space-1);
}

.rec-name {
  font-size: var(--font-base);
  font-weight: 600;
  color: var(--color-foreground);
  font-family: var(--font-sans);
}

.rec-rate {
  font-size: var(--font-h3);
  font-weight: 800;
  color: var(--color-primary);
  font-family: var(--font-mono);
}

.rec-meta {
  font-size: var(--font-sm);
  color: var(--color-muted-foreground);
  margin-bottom: var(--space-3);
  font-family: var(--font-sans);
}

.rec-reason {
  font-size: var(--font-sm);
  color: var(--color-muted-foreground);
  margin-top: var(--space-2);
  line-height: 1.5;
  font-family: var(--font-sans);
}

.loading-wrap {
  padding: var(--space-10);
}

.empty-wrap {
  padding: var(--space-16) 0;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: var(--space-4);
}

.product-card {
  background: var(--color-card);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
  padding: var(--space-5);
  cursor: pointer;
  transition: all var(--transition-slow);
  box-shadow: var(--shadow-sm);
}

.product-card:hover {
  border-color: var(--color-accent);
  box-shadow: var(--shadow-xl);
  transform: translateY(-2px);
}

.pc-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-3);
}

.pc-badge {
  font-size: var(--font-sm);
  font-weight: 600;
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-full);
  background: color-mix(in srgb, var(--color-primary) 12%, transparent);
  color: var(--color-primary);
  font-family: var(--font-sans);
}

.pc-badge.hot {
  background: color-mix(in srgb, var(--color-primary) 12%, transparent);
  color: var(--color-primary);
}

.pc-code {
  font-size: var(--font-sm);
  color: var(--color-muted-foreground);
  font-family: var(--font-mono);
}

.pc-name {
  font-size: var(--font-base);
  font-weight: 600;
  color: var(--color-foreground);
  margin: 0 0 var(--space-3);
  font-family: var(--font-sans);
}

.pc-rate {
  margin-bottom: 2px;
}

.rate-num {
  font-size: 36px;
  font-weight: 800;
  color: var(--color-primary);
  font-family: var(--font-mono);
}

.rate-unit {
  font-size: var(--font-h3);
  font-weight: 600;
  color: var(--color-primary);
  font-family: var(--font-mono);
}

.pc-meta {
  font-size: var(--font-sm);
  color: var(--color-muted-foreground);
  margin-bottom: var(--space-4);
  font-family: var(--font-sans);
}

.pc-info {
  display: flex;
  gap: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid var(--glass-border);
}

.info-item {
  flex: 1;
}

.info-label {
  display: block;
  font-size: var(--font-sm);
  color: var(--color-muted-foreground);
  margin-bottom: 2px;
  font-family: var(--font-sans);
}

.info-value {
  font-size: var(--font-base);
  font-weight: 600;
  color: var(--color-foreground);
  font-family: var(--font-mono);
}

.buy-product-info {
  background: var(--color-card);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  padding: var(--space-4);
  margin-bottom: var(--space-4);
}

.buy-product-info h4 {
  margin: 0 0 var(--space-3);
  font-size: var(--font-h3);
  color: var(--color-foreground);
  font-family: var(--font-sans);
}

.buy-rate {
  font-size: var(--font-base);
  color: var(--color-muted-foreground);
  margin-bottom: var(--space-1);
  font-family: var(--font-sans);
}

.highlight {
  color: var(--color-primary);
  font-weight: 600;
  font-family: var(--font-mono);
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

:deep(.el-dialog) {
  --el-dialog-bg-color: var(--color-card);
  --el-dialog-border-radius: var(--radius-xl);
  border: 1px solid var(--glass-border);
}

:deep(.el-dialog__header) {
  border-bottom: 1px solid var(--glass-border);
  padding: var(--space-4) var(--space-5);
}

:deep(.el-dialog__title) {
  color: var(--color-foreground);
  font-family: var(--font-sans);
  font-weight: 600;
}

:deep(.el-dialog__body) {
  padding: var(--space-5);
  color: var(--color-foreground);
}

:deep(.el-dialog__footer) {
  border-top: 1px solid var(--glass-border);
  padding: var(--space-3) var(--space-5);
}

:deep(.el-input__wrapper) {
  background: var(--color-muted);
  border: 1px solid var(--glass-border);
  box-shadow: none;
}

:deep(.el-input__inner) {
  color: var(--color-foreground);
}

:deep(.el-button--primary) {
  --el-button-bg-color: var(--color-primary);
  --el-button-border-color: var(--color-primary);
  --el-button-text-color: var(--color-on-primary);
  --el-button-hover-bg-color: var(--color-secondary);
  --el-button-hover-border-color: var(--color-secondary);
}
</style>
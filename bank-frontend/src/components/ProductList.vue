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
  if (score >= 85) return '#52c41a'
  if (score >= 70) return '#409eff'
  return '#fa541c'
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
      password: password.value
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
.product-list { padding: 4px 0; }
.pl-header { margin-bottom: 20px; }
.pl-title { font-size: 18px; font-weight: 700; color: #333; margin: 0 0 4px; }
.pl-desc { font-size: 13px; color: #999; margin: 0; }
.recommend-section {
  background: linear-gradient(135deg, #f6f9ff, #eef2fb);
  border: 1px solid #dfe7f5;
  border-radius: 14px;
  padding: 18px 20px;
  margin-bottom: 20px;
}
.recommend-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 14px;
}
.recommend-title { font-size: 16px; font-weight: 700; color: #1a3a6b; margin: 0 0 2px; }
.recommend-sub { font-size: 12px; color: #8a97ab; margin: 0; }
.recommend-badge {
  font-size: 11px; font-weight: 600; color: #fff;
  background: linear-gradient(135deg, #1a3a6b, #2d5a8c);
  padding: 3px 12px; border-radius: 12px;
}
.recommend-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}
.recommend-card {
  background: #fff;
  border: 1px solid #e8eef9;
  border-radius: 12px;
  padding: 14px 16px;
  cursor: pointer;
  transition: all 0.2s;
}
.recommend-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(26,58,107,0.12);
  border-color: #1a3a6b;
}
.rec-top { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 4px; }
.rec-name { font-size: 14px; font-weight: 600; color: #333; }
.rec-rate { font-size: 18px; font-weight: 800; color: #fa541c; }
.rec-meta { font-size: 12px; color: #8a97ab; margin-bottom: 10px; }
.rec-reason { font-size: 12px; color: #666; margin-top: 8px; line-height: 1.5; }

.loading-wrap { padding: 40px; }
.empty-wrap { padding: 60px 0; }
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}
.product-card {
  background: #fff;
  border: 1px solid #f0f2f5;
  border-radius: 14px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.25s;
}
.product-card:hover {
  border-color: #1a3a6b;
  box-shadow: 0 6px 24px rgba(26,58,107,0.1);
  transform: translateY(-2px);
}
.pc-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.pc-badge {
  font-size: 11px; font-weight: 600;
  padding: 2px 10px; border-radius: 10px;
  background: #e8f0fe; color: #1a3a6b;
}
.pc-badge.hot { background: #fff2e8; color: #fa541c; }
.pc-code { font-size: 11px; color: #bbb; font-family: monospace; }
.pc-name { font-size: 15px; font-weight: 600; color: #333; margin: 0 0 12px; }
.pc-rate { margin-bottom: 2px; }
.rate-num { font-size: 32px; font-weight: 800; color: #fa541c; }
.rate-unit { font-size: 16px; font-weight: 600; color: #fa541c; }
.pc-meta { font-size: 12px; color: #999; margin-bottom: 14px; }
.pc-info { display: flex; gap: 16px; padding-top: 14px; border-top: 1px solid #f5f5f5; }
.info-item { flex: 1; }
.info-label { display: block; font-size: 11px; color: #bbb; margin-bottom: 2px; }
.info-value { font-size: 13px; font-weight: 600; color: #333; }
.buy-product-info {
  background: #f8f9fc; border-radius: 10px;
  padding: 16px; margin-bottom: 16px;
}
.buy-product-info h4 { margin: 0 0 10px; font-size: 16px; color: #333; }
.buy-rate { font-size: 13px; color: #666; margin-bottom: 4px; }
.highlight { color: #fa541c; font-weight: 600; }
.form-row { margin-bottom: 16px; }
.atm-label { display: block; font-size: 13px; font-weight: 600; color: #333; margin-bottom: 6px; }
</style>

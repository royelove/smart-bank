<template>
  <div class="rank-board">
    <div class="rank-toolbar">
      <div class="rank-title">
        股票排行
        <span class="rank-sub">实时 · 自动刷新</span>
      </div>
      <div class="rank-toolbar-right">
        <el-select v-model="topN" size="small" style="width: 80px; margin-right: 8px">
          <el-option label="TOP 10" :value="10" />
          <el-option label="TOP 20" :value="20" />
          <el-option label="TOP 50" :value="50" />
        </el-select>
        <el-button size="small" :loading="loading" @click="loadRank">刷新</el-button>
      </div>
    </div>

    <el-tabs v-model="activeType" class="rank-tabs" @tab-change="onTypeChange">
      <el-tab-pane label="涨幅榜" name="change">
        <span class="tab-icon">🔺</span>
      </el-tab-pane>
      <el-tab-pane label="跌幅榜" name="fall">
        <span class="tab-icon">🔻</span>
      </el-tab-pane>
      <el-tab-pane label="成交量榜" name="volume">
        <span class="tab-icon">📊</span>
      </el-tab-pane>
      <el-tab-pane label="成交额榜" name="amount">
        <span class="tab-icon">💰</span>
      </el-tab-pane>
      <el-tab-pane label="换手率榜" name="turnover">
        <span class="tab-icon">🔄</span>
      </el-tab-pane>
      <el-tab-pane label="振幅榜" name="amplitude">
        <span class="tab-icon">📐</span>
      </el-tab-pane>
    </el-tabs>

    <div v-if="loading && rankList.length === 0" class="loading-wrap">
      <el-skeleton :rows="8" animated />
    </div>

    <el-table v-else :data="rankList" class="rank-table" size="default" @row-click="onRowClick">
      <el-table-column label="排名" width="70" align="center">
        <template #default="{ row }">
          <span class="rank-num" :class="{ 'top1': row.rank === 1, 'top2': row.rank === 2, 'top3': row.rank === 3 }">
            {{ row.rank }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="代码" width="110">
        <template #default="{ row }">
          <span class="stock-code">{{ row.stockCode }}</span>
        </template>
      </el-table-column>
      <el-table-column label="名称" min-width="120">
        <template #default="{ row }">
          <span class="stock-name">{{ row.stockName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="指标值" width="130" align="right">
        <template #default="{ row }">
          <span class="rank-value" :class="{ up: isUp(row), down: !isUp(row) }">
            {{ formatRankValue(row) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="当前价" width="120" align="right">
        <template #default="{ row }">
          <span class="price">¥{{ Number(row.currentPrice).toFixed(2) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="昨收" width="110" align="right">
        <template #default="{ row }">
          <span class="prev-close">¥{{ Number(row.prevClose).toFixed(2) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="最高" width="110" align="right">
        <template #default="{ row }">
          <span class="high-price">¥{{ Number(row.highPrice).toFixed(2) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="最低" width="110" align="right">
        <template #default="{ row }">
          <span class="low-price">¥{{ Number(row.lowPrice).toFixed(2) }}</span>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && rankList.length === 0" description="暂无排行数据" />
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { getRankTopN } from '@/api/stock'

const activeType = ref('change')
const rankList = ref([])
const loading = ref(false)
const topN = ref(10)
let timer = null

const rankTypes = ['change', 'fall', 'volume', 'amount', 'turnover', 'amplitude']
const rankLabels = { change: '涨幅榜', fall: '跌幅榜', volume: '成交量榜', amount: '成交额榜', turnover: '换手率榜', amplitude: '振幅榜' }

const props = defineProps({
  stockCode: { type: String, default: '' }
})

const emit = defineEmits(['open-kline'])

const loadRank = async () => {
  loading.value = true
  try {
    const res = await getRankTopN(activeType.value, topN.value)
    rankList.value = res.data || []
  } catch (e) {
    console.error(e)
    ElMessage.error('加载排行数据失败')
  } finally {
    loading.value = false
  }
}

const onTypeChange = () => {
  loadRank()
}

const isUp = (row) => {
  return row.rankValue != null && Number(row.rankValue) >= 0
}

const formatRankValue = (row) => {
  const val = row.rankValue
  if (val == null) return '-'
  const type = activeType.value
  if (type === 'volume' || type === 'amount') {
    if (type === 'amount') {
      return '¥' + (Number(val) / 10000).toFixed(2) + '万'
    }
    return Number(val).toLocaleString() + '手'
  }
  if (type === 'turnover') {
    return Number(val).toFixed(2) + '%'
  }
  const prefix = Number(val) >= 0 ? '+' : ''
  return prefix + Number(val).toFixed(2) + '%'
}

const onRowClick = (row) => {
  emit('open-kline', row.stockCode, row.stockName)
}

onMounted(() => {
  loadRank()
  timer = setInterval(loadRank, 5000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.rank-board {
  padding: var(--space-1) 0;
}

.rank-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-4);
}

.rank-title {
  font-size: var(--font-h3);
  font-weight: 700;
  color: var(--color-foreground);
  letter-spacing: 0.06em;
  font-family: var(--font-sans);
}

.rank-sub {
  font-size: var(--font-base);
  color: var(--color-muted-foreground);
  margin-left: var(--space-2);
  font-weight: 400;
}

.rank-toolbar-right {
  display: flex;
  align-items: center;
}

.rank-tabs {
  margin-bottom: var(--space-4);
}

.tab-icon {
  font-size: var(--font-base);
}

.loading-wrap {
  padding: var(--space-8);
}

.rank-table {
  --el-table-header-bg-color: var(--color-card);
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-border-color: var(--glass-border);
  --el-table-text-color: var(--color-foreground);
  --el-table-header-text-color: var(--color-muted-foreground);
}

:deep(.el-table th.el-table__cell) {
  font-weight: 600;
  font-size: var(--font-sm);
  letter-spacing: 0.05em;
}

:deep(.el-table .el-table__row:hover > td) {
  background: var(--color-muted) !important;
}

.stock-code {
  font-family: var(--font-mono);
  color: var(--color-muted-foreground);
  font-size: var(--font-base);
}

.stock-name {
  color: var(--color-foreground);
  font-weight: 600;
  font-family: var(--font-sans);
}

.rank-num {
  font-family: var(--font-mono);
  font-weight: 700;
  font-size: var(--font-base);
  color: var(--color-muted-foreground);
}

.rank-num.top1 {
  color: var(--color-destructive);
}

.rank-num.top2 {
  color: #e67e22;
}

.rank-num.top3 {
  color: var(--color-success);
}

.rank-value {
  font-family: var(--font-mono);
  font-weight: 700;
  font-size: var(--font-base);
}

.rank-value.up {
  color: var(--color-success);
}

.rank-value.down {
  color: var(--color-destructive);
}

.price {
  font-family: var(--font-mono);
  font-size: var(--font-base);
  font-weight: 600;
}

.prev-close {
  font-family: var(--font-mono);
  color: var(--color-muted-foreground);
}

.high-price {
  font-family: var(--font-mono);
  color: var(--color-success);
}

.low-price {
  font-family: var(--font-mono);
  color: var(--color-destructive);
}

:deep(.el-button--default) {
  --el-button-bg-color: var(--color-card);
  --el-button-border-color: var(--glass-border);
  --el-button-text-color: var(--color-foreground);
  --el-button-hover-bg-color: var(--color-muted);
  --el-button-hover-border-color: var(--color-border-hover);
  --el-button-hover-text-color: var(--color-foreground);
}
</style>
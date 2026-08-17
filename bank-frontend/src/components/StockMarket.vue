<template>
  <div class="stock-market">
    <div class="market-toolbar">
      <div class="market-title">
        实时行情
        <span class="market-sub">{{ quotes.length }} 只 · 模拟行情</span>
      </div>
      <el-button size="small" :loading="loading" @click="loadQuotes">刷新</el-button>
    </div>

    <div v-if="loading && quotes.length === 0" class="loading-wrap">
      <el-skeleton :rows="5" animated />
    </div>

    <el-table v-else :data="quotes" class="market-table" size="default">
      <el-table-column label="代码" width="100">
        <template #default="{ row }">
          <span class="stock-code">{{ row.stockCode }}</span>
        </template>
      </el-table-column>
      <el-table-column label="名称" min-width="120">
        <template #default="{ row }">
          <span class="stock-name">{{ row.stockName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="现价" width="130" align="right">
        <template #default="{ row }">
          <span class="price" :class="{ up: Number(row.changePercent) >= 0, down: Number(row.changePercent) < 0 }">
            ¥{{ Number(row.currentPrice).toFixed(2) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="涨跌幅" width="130" align="right">
        <template #default="{ row }">
          <span class="change" :class="{ up: Number(row.changePercent) >= 0, down: Number(row.changePercent) < 0 }">
            {{ Number(row.changePercent) >= 0 ? '+' : '' }}{{ Number(row.changePercent).toFixed(2) }}%
          </span>
        </template>
      </el-table-column>
      <el-table-column label="昨收" width="120" align="right">
        <template #default="{ row }">
          <span class="prev-close">¥{{ Number(row.prevClose).toFixed(2) }}</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { listQuotes } from '@/api/stock'

const quotes = ref([])
const loading = ref(false)
let timer = null

const loadQuotes = async () => {
  loading.value = true
  try {
    const res = await listQuotes()
    quotes.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadQuotes()
  timer = setInterval(loadQuotes, 5000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.stock-market { padding: 4px 0; }
.market-toolbar {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 14px;
}
.market-title { font-size: 15px; font-weight: 700; color: var(--vault-text); letter-spacing: 0.06em; }
.market-sub { font-size: 12px; color: var(--vault-text-3); margin-left: 8px; font-weight: 400; }
.loading-wrap { padding: 30px; }
.market-table {
  --el-table-header-bg-color: rgba(8,12,20,0.6);
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-border-color: var(--vault-line);
  --el-table-text-color: var(--vault-text);
  --el-table-header-text-color: var(--vault-text-3);
}
.stock-code { font-family: var(--vault-font-num); color: var(--vault-text-2); font-size: 13px; }
.stock-name { color: var(--vault-text); font-weight: 600; }
.price { font-family: var(--vault-font-num); font-size: 15px; font-weight: 700; }
.change { font-family: var(--vault-font-num); font-weight: 600; }
.prev-close { font-family: var(--vault-font-num); color: var(--vault-text-3); }
.up { color: var(--vault-up-soft); }
.down { color: var(--vault-down-soft); }
</style>
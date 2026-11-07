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

    <el-table v-else :data="quotes" class="market-table" size="default" @row-click="onRowClick">
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

const props = defineProps({
  accountNumber: { type: String, default: '' }
})
const emit = defineEmits(['open-kline'])

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

const onRowClick = (row) => {
  emit('open-kline', row.stockCode, row.stockName)
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
.stock-market {
  padding: var(--space-1) 0;
}

.market-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-4);
}

.market-title {
  font-size: var(--font-h3);
  font-weight: 700;
  color: var(--color-foreground);
  letter-spacing: 0.06em;
  font-family: var(--font-sans);
}

.market-sub {
  font-size: var(--font-base);
  color: var(--color-muted-foreground);
  margin-left: var(--space-2);
  font-weight: 400;
  font-family: var(--font-sans);
}

.loading-wrap {
  padding: var(--space-8);
}

.market-table {
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

.price {
  font-family: var(--font-mono);
  font-size: var(--font-base);
  font-weight: 700;
}

.change {
  font-family: var(--font-mono);
  font-weight: 600;
}

.prev-close {
  font-family: var(--font-mono);
  color: var(--color-muted-foreground);
}

.up {
  color: var(--color-success);
}

.down {
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
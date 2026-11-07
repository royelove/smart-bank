<template>
  <div class="trade-records">
    <div class="trade-header">
      <div class="trade-header-left">
        <el-icon :size="20" color="var(--color-primary)"><Document /></el-icon>
        <h3>交易明细</h3>
      </div>
    </div>
    <div class="trade-filter">
      <el-select v-model="filterType" placeholder="交易类型" clearable style="width: 140px">
        <el-option label="存款" value="存款" />
        <el-option label="取款" value="取款" />
        <el-option label="转账" value="转账" />
        <el-option label="理财购买" value="理财购买" />
        <el-option label="理财赎回" value="理财赎回" />
      </el-select>
      <el-date-picker
        v-model="startDate"
        type="date"
        placeholder="开始日期"
        value-format="YYYY-MM-DD"
        style="width: 150px"
      />
      <span class="filter-sep">至</span>
      <el-date-picker
        v-model="endDate"
        type="date"
        placeholder="结束日期"
        value-format="YYYY-MM-DD"
        style="width: 150px"
      />
      <el-button type="primary" @click="loadTrades">查询</el-button>
      <el-button @click="resetFilter">重置</el-button>
    </div>
    <el-table
      :data="tableData"
      v-loading="loading"
      border
      stripe
      style="width: 100%;"
    >
      <el-table-column prop="tradeNo" label="交易编号" width="200">
        <template #default="{ row }">
          <span class="vault-num">{{ row.tradeNo }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="fromAccount" label="转出账户" width="140">
        <template #default="{ row }">
          <span class="vault-num">{{ row.fromAccount || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="toAccount" label="转入账户" width="140">
        <template #default="{ row }">
          <span class="vault-num">{{ row.toAccount || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="amount" label="金额" width="120" align="right">
        <template #default="{ row }">
          <span class="amount-cell vault-num">¥ {{ Number(row.amount).toFixed(2) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="tradeType" label="类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getTagType(row.tradeType)" size="small" effect="plain">
            {{ row.tradeType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="tradeDate" label="交易时间" width="180">
        <template #default="{ row }">
          <span class="vault-num">{{ formatDate(row.tradeDate) }}</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-box">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadTrades"
        @current-change="loadTrades"
        background
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const props = defineProps({ accountNumber: { type: String, default: '' } })

const tableData = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const size = ref(10)
const searchAccount = ref(props.accountNumber)
const filterType = ref('')
const startDate = ref('')
const endDate = ref('')

const loadTrades = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (searchAccount.value) params.accountNumber = searchAccount.value
    if (filterType.value) params.type = filterType.value
    if (startDate.value) params.startDate = startDate.value
    if (endDate.value) params.endDate = endDate.value
    const res = await request({ url: '/account/trades', method: 'get', params })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    ElMessage.error('加载交易记录失败')
  } finally { loading.value = false }
}

const resetFilter = () => {
  filterType.value = ''
  startDate.value = ''
  endDate.value = ''
  page.value = 1
  loadTrades()
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}:${String(d.getSeconds()).padStart(2,'0')}`
}

const getTagType = (type) => {
  const map = { '存款': 'success', '取款': 'danger', '转账': 'primary' }
  return map[type] || ''
}

onMounted(() => { loadTrades() })
</script>

<style scoped>
.trade-records {
  width: 100%;
}

.trade-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-5);
}

.trade-header-left {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.trade-header-left h3 {
  margin: 0;
  font-size: var(--font-h3);
  font-weight: 600;
  color: var(--color-foreground);
  letter-spacing: 0.08em;
  font-family: var(--font-sans);
}

.amount-cell {
  font-weight: 600;
  color: var(--color-primary);
  font-size: var(--font-base);
  font-family: var(--font-mono);
}

.pagination-box {
  margin-top: var(--space-5);
  display: flex;
  justify-content: flex-end;
}

.trade-filter {
  display: flex;
  gap: var(--space-3);
  align-items: center;
  margin-bottom: var(--space-4);
  flex-wrap: wrap;
}

.filter-sep {
  color: var(--color-muted-foreground);
  font-size: var(--font-base);
  font-family: var(--font-sans);
}

.vault-num {
  font-family: var(--font-mono);
}

:deep(.el-table) {
  --el-table-header-bg-color: var(--color-card);
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-border-color: var(--glass-border);
  --el-table-text-color: var(--color-foreground);
  --el-table-header-text-color: var(--color-muted-foreground);
  font-family: var(--font-sans);
}

:deep(.el-table th.el-table__cell) {
  font-weight: 600;
  font-size: var(--font-sm);
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

:deep(.el-table .el-table__row:hover > td) {
  background: var(--color-muted) !important;
}

:deep(.el-select),
:deep(.el-date-editor) {
  --el-input-bg-color: var(--color-card);
  --el-input-border-color: var(--glass-border);
  --el-input-text-color: var(--color-foreground);
  --el-input-placeholder-color: var(--color-muted-foreground);
  --el-input-hover-border-color: var(--color-border-hover);
  --el-input-focus-border-color: var(--color-primary);
}

:deep(.el-button--primary) {
  --el-button-bg-color: var(--color-primary);
  --el-button-border-color: var(--color-primary);
  --el-button-text-color: var(--color-on-primary);
  --el-button-hover-bg-color: var(--color-secondary);
  --el-button-hover-border-color: var(--color-secondary);
  --el-button-hover-text-color: var(--color-on-primary);
}

:deep(.el-pagination) {
  --el-pagination-bg-color: var(--color-card);
  --el-pagination-text-color: var(--color-muted-foreground);
  --el-pagination-button-color: var(--color-muted-foreground);
  --el-pagination-button-bg-color: var(--color-card);
  --el-pagination-button-disabled-bg-color: var(--color-muted);
  --el-pagination-hover-color: var(--color-primary);
}

:deep(.el-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background: var(--color-primary);
}

:deep(.el-tag--success) {
  --el-tag-bg-color: color-mix(in srgb, var(--color-success) 15%, transparent);
  --el-tag-text-color: var(--color-success);
  --el-tag-border-color: transparent;
}

:deep(.el-tag--danger) {
  --el-tag-bg-color: color-mix(in srgb, var(--color-destructive) 15%, transparent);
  --el-tag-text-color: var(--color-destructive);
  --el-tag-border-color: transparent;
}

:deep(.el-tag--primary) {
  --el-tag-bg-color: color-mix(in srgb, var(--color-primary) 15%, transparent);
  --el-tag-text-color: var(--color-primary);
  --el-tag-border-color: transparent;
}
</style>
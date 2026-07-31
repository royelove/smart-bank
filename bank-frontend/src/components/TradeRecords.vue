<template>
  <div class="trade-records">
    <div class="trade-header">
      <div class="trade-header-left">
        <el-icon :size="20" color="#1a3a6b"><Document /></el-icon>
        <h3>交易明细</h3>
      </div>
    </div>

    <el-table
      :data="tableData"
      v-loading="loading"
      border
      stripe
      style="width: 100%;"
      :header-cell-style="{ background: '#f8f9fc', color: '#333', fontWeight: 600 }"
    >
      <el-table-column prop="tradeNo" label="交易编号" width="200">
        <template #default="{ row }">
          <span class="mono-text">{{ row.tradeNo }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="fromAccount" label="转出账户" width="140">
        <template #default="{ row }">
          <span class="mono-text">{{ row.fromAccount || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="toAccount" label="转入账户" width="140">
        <template #default="{ row }">
          <span class="mono-text">{{ row.toAccount || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="amount" label="金额" width="120" align="right">
        <template #default="{ row }">
          <span class="amount-cell">¥ {{ Number(row.amount).toFixed(2) }}</span>
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
          <span class="mono-text">{{ formatDate(row.tradeDate) }}</span>
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

const loadTrades = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (searchAccount.value) params.accountNumber = searchAccount.value

    const res = await request({ url: '/account/trades', method: 'get', params })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    ElMessage.error('加载交易记录失败')
  } finally { loading.value = false }
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
.trade-records { width: 100%; }

.trade-header {
  display: flex; justify-content: space-between;
  align-items: center; margin-bottom: 20px;
}

.trade-header-left {
  display: flex; align-items: center; gap: 8px;
}
.trade-header-left h3 {
  margin: 0; font-size: 16px; font-weight: 600; color: #333;
}

.mono-text { font-family: 'Noto Sans SC', monospace; font-size: 12px; color: #666; }
.amount-cell { font-weight: 600; color: #f56c6c; font-size: 14px; }

.pagination-box {
  margin-top: 20px; display: flex; justify-content: flex-end;
}

:deep(.el-table th.el-table__cell) { background-color: #f8f9fc !important; }
:deep(.el-table--striped .el-table__body tr.el-table__row--striped td) { background: #fafbfc; }
</style>

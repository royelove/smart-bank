<template>
  <div class="support-center">
    <el-tabs v-model="tab">
      <el-tab-pane label="提交工单/投诉" name="submit">
        <el-form :model="form" label-width="90px">
          <el-form-item label="选择银行卡">
            <el-select v-model="form.accountNumber" placeholder="请选择本人名下的银行卡" style="width:100%" :loading="accountsLoading">
              <el-option
                v-for="acc in accounts"
                :key="acc.accountNumber"
                :label="acc.accountNumber + '（' + acc.accountHolderName + '）'"
                :value="acc.accountNumber"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="问题类型">
            <el-select v-model="form.category" style="width:100%">
              <el-option label="业务办理" value="业务办理" />
              <el-option label="咨询" value="咨询" />
              <el-option label="投诉" value="投诉" />
              <el-option label="其他" value="其他" />
            </el-select>
          </el-form-item>
          <el-form-item label="标题">
            <el-input v-model="form.title" placeholder="一句话描述问题" maxlength="100" show-word-limit />
          </el-form-item>
          <el-form-item label="详细描述">
            <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请描述遇到的问题或投诉内容" maxlength="1000" show-word-limit />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">提交</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="我的工单" name="list">
        <el-table :data="tickets" v-loading="loading" stripe size="small" style="width:100%">
          <el-table-column prop="ticketNo" label="工单号" width="150" />
          <el-table-column prop="categoryText" label="类型" width="80" />
          <el-table-column prop="title" label="标题" min-width="120" show-overflow-tooltip />
          <el-table-column prop="statusText" label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">{{ row.statusText }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="情绪" width="70">
            <template #default="{ row }">
              <el-tag :type="sentimentType(row.sentiment)" size="small" effect="plain">{{ row.sentiment || '--' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="有效性" width="70">
            <template #default="{ row }">
              <el-tag :type="row.aiValid === 0 ? 'info' : 'success'" size="small" effect="plain">
                {{ row.aiValid === 0 ? '无效' : '有效' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="客服回复" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">{{ row.reply || '--' }}</template>
          </el-table-column>
          <el-table-column label="创建时间" width="140">
            <template #default="{ row }">{{ formatTime(row.creationDate) }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && tickets.length === 0" description="暂无工单" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { submitTicket, getMyTickets } from '@/api/support'
import { getUserAccounts } from '@/api/user'

const props = defineProps({
  accountNumber: { type: String, default: '' }
})

const tab = ref('submit')
const accounts = ref([])
const accountsLoading = ref(false)
const form = ref({ accountNumber: '', category: '咨询', title: '', description: '' })
const submitting = ref(false)
const tickets = ref([])
const loading = ref(false)

const statusType = (s) => {
  if (s === 0) return 'danger'
  if (s === 1) return 'warning'
  if (s === 2) return 'success'
  return 'info'
}

const sentimentType = (s) => {
  if (s === '积极') return 'success'
  if (s === '消极') return 'warning'
  if (s === '恶意') return 'danger'
  return 'info'
}

const formatTime = (t) => {
  if (!t) return '--'
  return String(t).replace('T', ' ').substring(0, 16)
}

const currentAccount = () => form.value.accountNumber || props.accountNumber || (accounts.value[0] && accounts.value[0].accountNumber) || ''

const loadAccounts = async () => {
  accountsLoading.value = true
  try {
    const res = await getUserAccounts()
    accounts.value = res.data || []
    if (accounts.value.length > 0 && !currentAccount()) {
      form.value.accountNumber = props.accountNumber || accounts.value[0].accountNumber
    }
  } catch (e) {
    console.error(e)
  } finally {
    accountsLoading.value = false
  }
}

const handleSubmit = async () => {
  const accountNumber = currentAccount()
  if (!accountNumber) {
    ElMessage.warning('请选择银行卡')
    return
  }
  if (!form.value.title.trim() || !form.value.description.trim()) {
    ElMessage.warning('请填写标题和详细描述')
    return
  }
  submitting.value = true
  try {
    await submitTicket({ ...form.value, accountNumber })
    ElMessage.success('提交成功，客服将尽快处理')
    form.value = { accountNumber, category: '咨询', title: '', description: '' }
    tab.value = 'list'
    loadTickets()
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

const loadTickets = async () => {
  loading.value = true
  try {
    const res = await getMyTickets(currentAccount())
    tickets.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAccounts()
  loadTickets()
})
</script>

<style scoped>
.support-center { padding: 4px 0; }
</style>

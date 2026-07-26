<template>
  <div class="atm-container">
    <!-- Step 1: Input -->
    <div v-if="step === 'input'" class="atm-step">
      <div class="atm-step-header">
        <div class="atm-icon">
          <el-icon :size="28" color="#1a3a6b"><Coin /></el-icon>
        </div>
        <div>
          <h3 class="atm-step-title">存款</h3>
          <p class="atm-step-desc">请输入存款信息</p>
        </div>
      </div>

      <el-form :model="form" label-width="0" class="atm-form">
        <div class="form-row">
          <label class="atm-label">账户账号</label>
          <el-input
            v-model="form.accountNo"
            placeholder="请输入存款账户号"
            size="large"
            clearable
          />
        </div>
        <div class="form-row">
          <label class="atm-label">存款金额</label>
          <el-input-number
            v-model="form.amount"
            :precision="2"
            :step="100"
            :min="0.01"
            controls-position="right"
            class="full-width"
            size="large"
          />
        </div>
      </el-form>

      <div class="atm-actions">
        <el-button size="large" class="atm-btn-primary" @click="showPasswordDialog">
          下一步
        </el-button>
      </div>
    </div>

    <!-- Step 2: Confirm -->
    <div v-if="step === 'confirm'" class="atm-step">
      <div class="atm-step-header">
        <div class="atm-icon confirm-icon">
          <el-icon :size="28" color="#e6a23c"><Warning /></el-icon>
        </div>
        <div>
          <h3 class="atm-step-title">确认存款信息</h3>
          <p class="atm-step-desc">请核对以下信息是否正确</p>
        </div>
      </div>

      <div class="confirm-card">
        <div class="confirm-row">
          <span class="confirm-label">收款账户</span>
          <span class="confirm-value">{{ form.accountNo }}</span>
        </div>
        <div class="confirm-row">
          <span class="confirm-label">存款金额</span>
          <span class="confirm-value highlight">¥ {{ form.amount.toFixed(2) }}</span>
        </div>
      </div>

      <div class="atm-actions">
        <el-button size="large" class="atm-btn-secondary" @click="step = 'input'">修改</el-button>
        <el-button size="large" class="atm-btn-primary" @click="showPasswordDialog">确认存款</el-button>
      </div>
    </div>

    <!-- Step 3: Result -->
    <div v-if="step === 'result'" class="atm-step">
      <div class="result-display" :class="resultType">
        <div class="result-icon">
          <el-icon :size="48" v-if="resultType === 'success'" color="#52c41a"><CircleCheck /></el-icon>
          <el-icon :size="48" v-else color="#f56c6c"><CircleClose /></el-icon>
        </div>
        <h3 class="result-title">{{ resultType === 'success' ? '存款成功' : '存款失败' }}</h3>
        <p class="result-msg">{{ resultMsg }}</p>
        <div class="result-details" v-if="resultType === 'success'">
          <div class="detail-row">
            <span>交易账户</span>
            <span>{{ form.accountNo }}</span>
          </div>
          <div class="detail-row">
            <span>交易金额</span>
            <span class="highlight">¥ {{ form.amount.toFixed(2) }}</span>
          </div>
          <div class="detail-row">
            <span>交易时间</span>
            <span>{{ tradeTime }}</span>
          </div>
        </div>
      </div>

      <div class="atm-actions">
        <el-button size="large" class="atm-btn-primary" @click="resetForm">继续存款</el-button>
        <el-button size="large" class="atm-btn-secondary" @click="closePanel">返回</el-button>
      </div>
    </div>

    <!-- Password Dialog -->
    <PasswordDialog
      :visible="passwordVisible"
      title="请输入存款交易密码"
      @update:visible="passwordVisible = $event"
      @confirm="handlePasswordConfirm"
      @cancel="passwordVisible = false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, inject } from 'vue'
import { ElMessage } from 'element-plus'
import { deposit } from '@/api/account'
import PasswordDialog from './PasswordDialog.vue'
import { CircleCheck, CircleClose } from '@element-plus/icons-vue'

const emit = defineEmits(['close'])
const refreshBalance = inject('refreshBalance', null)

const step = ref('input')
const form = reactive({ accountNo: '', amount: 0 })
const passwordVisible = ref(false)
const password = ref('')
const resultMsg = ref('')
const resultType = ref('success')
const tradeTime = ref('')

const showPasswordDialog = () => {
  if (!form.accountNo) { ElMessage.warning('请输入账户号'); return }
  if (form.amount <= 0) { ElMessage.warning('请输入有效的金额'); return }
  passwordVisible.value = true
}

const handlePasswordConfirm = async (pwd) => {
  password.value = pwd
  passwordVisible.value = false
  await doDeposit()
}

const doDeposit = async () => {
  try {
    await deposit({ accountNo: form.accountNo, amount: form.amount, password: password.value })
    if (refreshBalance) refreshBalance()
    tradeTime.value = new Date().toLocaleString('zh-CN', { hour12: false })
    resultMsg.value = `账户 ${form.accountNo} 存款成功`
    resultType.value = 'success'
    step.value = 'result'
  } catch (error) {
    resultMsg.value = error.message || '存款失败，请重试'
    resultType.value = 'error'
    step.value = 'result'
  }
}

const resetForm = () => {
  form.accountNo = ''
  form.amount = 0
  password.value = ''
  step.value = 'input'
  resultMsg.value = ''
}

const closePanel = () => {
  resetForm()
  emit('close')
}
</script>

<style scoped>
.atm-container {
  width: 100%;
  max-width: 480px;
}

.atm-step {
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.atm-step-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f2f5;
}

.atm-icon {
  width: 48px;
  height: 48px;
  background: #e8f0fe;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.confirm-icon { background: #fdf6ec; }

.atm-step-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}
.atm-step-desc {
  margin: 2px 0 0;
  font-size: 12px;
  color: #999;
}

.atm-form { max-width: 380px; }

.form-row { margin-bottom: 20px; }
.atm-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
}

.atm-form :deep(.el-input__wrapper),
.atm-form :deep(.el-input-number__increase),
.atm-form :deep(.el-input-number__decrease) {
  height: 46px;
  border-radius: 10px !important;
}
.atm-form .full-width { width: 100%; }

.atm-form :deep(.el-input-number .el-input__wrapper) {
  padding-right: 50px;
}

/* ===== Confirm Card ===== */
.confirm-card {
  background: #f8f9fc;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 24px;
}

.confirm-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #eee;
}
.confirm-row:last-child { border-bottom: none; }
.confirm-label { font-size: 14px; color: #999; }
.confirm-value { font-size: 15px; font-weight: 600; color: #333; }
.confirm-value.highlight { color: #f56c6c; font-size: 18px; }

/* ===== Result ===== */
.result-display {
  text-align: center;
  padding: 20px 0;
}

.result-icon { margin-bottom: 12px; }
.result-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 8px;
}
.result-display.success .result-title { color: #52c41a; }
.result-display.error .result-title { color: #f56c6c; }

.result-msg {
  font-size: 14px;
  color: #666;
  margin-bottom: 20px;
}

.result-details {
  background: #f8f9fc;
  border-radius: 12px;
  padding: 16px 20px;
  text-align: left;
  max-width: 360px;
  margin: 0 auto;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 14px;
  color: #666;
}
.detail-row .highlight { color: #f56c6c; font-weight: 600; }

/* ===== Actions ===== */
.atm-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.atm-btn-primary {
  flex: 1;
  height: 46px;
  background: linear-gradient(135deg, #1a3a6b, #2d5a8c);
  border: none;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;
}
.atm-btn-primary:hover { background: linear-gradient(135deg, #2d5a8c, #3a6a9c); }

.atm-btn-secondary {
  flex: 1;
  height: 46px;
  border: 1px solid #d0d5de;
  background: #fff;
  color: #666;
  font-size: 15px;
  font-weight: 500;
  border-radius: 10px;
}
.atm-btn-secondary:hover { border-color: #1a3a6b; color: #1a3a6b; }

@media (max-width: 480px) {
  .atm-container { max-width: 100%; }
}
</style>

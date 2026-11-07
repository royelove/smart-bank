<template>
  <div class="atm-container">
    <!-- Step 1: Input -->
    <div v-if="step === 'input'" class="atm-step">
      <div class="atm-step-header">
        <div class="atm-icon">
          <el-icon :size="28" color="var(--color-primary)"><Wallet /></el-icon>
        </div>
        <div>
          <h3 class="atm-step-title">取款</h3>
          <p class="atm-step-desc">请输入取款信息</p>
        </div>
      </div>

      <el-form :model="form" label-width="0" class="atm-form">
        <div class="form-row">
          <label class="atm-label">取款账户</label>
          <el-input
            v-model="form.accountNumber"
            placeholder="请输入取款账户号"
            size="large"
            clearable
          />
        </div>
        <div class="form-row">
          <label class="atm-label">取款金额</label>
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
        <el-button size="large" class="atm-btn-primary" @click="goConfirm">
          下一步
        </el-button>
      </div>
    </div>

    <!-- Step 2: Confirm -->
    <div v-if="step === 'confirm'" class="atm-step">
      <div class="atm-step-header">
        <div class="atm-icon confirm-icon">
          <el-icon :size="28" color="var(--color-primary)"><Warning /></el-icon>
        </div>
        <div>
          <h3 class="atm-step-title">确认取款信息</h3>
          <p class="atm-step-desc">请核对以下信息是否正确</p>
        </div>
      </div>

      <div class="confirm-card">
        <div class="confirm-row">
          <span class="confirm-label">取款账户</span>
          <span class="confirm-value">{{ form.accountNumber }}</span>
        </div>
        <div class="confirm-row">
          <span class="confirm-label">取款金额</span>
          <span class="confirm-value highlight">¥ {{ form.amount.toFixed(2) }}</span>
        </div>
      </div>

      <div class="atm-actions">
        <el-button size="large" class="atm-btn-secondary" @click="step = 'input'">修改</el-button>
        <el-button size="large" class="atm-btn-primary" @click="showPasswordDialog">确认取款</el-button>
      </div>
    </div>

    <!-- Step 3: Result -->
    <div v-if="step === 'result'" class="atm-step">
      <div class="result-display" :class="resultType">
        <div class="result-icon">
          <el-icon :size="48" v-if="resultType === 'success'" color="var(--color-success)"><CircleCheck /></el-icon>
          <el-icon :size="48" v-else color="var(--color-destructive)"><CircleClose /></el-icon>
        </div>
        <h3 class="result-title">{{ resultType === 'success' ? '取款成功' : '取款失败' }}</h3>
        <p class="result-msg">{{ resultMsg }}</p>
        <div class="result-details" v-if="resultType === 'success'">
          <div class="detail-row">
            <span>取款账户</span>
            <span>{{ form.accountNumber }}</span>
          </div>
          <div class="detail-row">
            <span>取款金额</span>
            <span class="highlight">¥ {{ form.amount.toFixed(2) }}</span>
          </div>
          <div class="detail-row">
            <span>交易时间</span>
            <span>{{ tradeTime }}</span>
          </div>
        </div>
      </div>

      <div class="atm-actions">
        <el-button size="large" class="atm-btn-primary" @click="resetForm">继续取款</el-button>
        <el-button size="large" class="atm-btn-secondary" @click="closePanel">返回</el-button>
      </div>
    </div>

    <PasswordDialog
      :visible="passwordVisible"
      title="请输入取款交易密码"
      @update:visible="passwordVisible = $event"
      @confirm="handlePasswordConfirm"
      @cancel="passwordVisible = false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, inject } from 'vue'
import { ElMessage } from 'element-plus'
import { withdraw } from '@/api/account'
import PasswordDialog from './PasswordDialog.vue'
import { CircleCheck, CircleClose } from '@element-plus/icons-vue'

const props = defineProps({ accountNumber: { type: String, default: '' } })
const emit = defineEmits(['close', 'refresh'])
const refreshBalance = inject('refreshBalance', null)

const step = ref('input')
const form = reactive({ accountNumber: props.accountNumber, amount: 0 })
const passwordVisible = ref(false)
const password = ref('')
const resultMsg = ref('')
const resultType = ref('success')
const tradeTime = ref('')

const goConfirm = () => {
  if (!form.accountNumber) { ElMessage.warning('请输入取款账户号'); return }
  if (form.amount <= 0) { ElMessage.warning('请输入有效的金额'); return }
  step.value = 'confirm'
}

const showPasswordDialog = () => {
  passwordVisible.value = true
}

const handlePasswordConfirm = async (pwd) => {
  password.value = pwd
  passwordVisible.value = false
  await doWithdraw()
}

const doWithdraw = async () => {
  try {
    await withdraw({ accountNumber: form.accountNumber, amount: form.amount, password: password.value, requestId: crypto.randomUUID() })
    if (refreshBalance) refreshBalance()
    emit('refresh')
    tradeTime.value = new Date().toLocaleString('zh-CN', { hour12: false })
    resultMsg.value = `账户 ${form.accountNumber} 取款成功`
    resultType.value = 'success'
    step.value = 'result'
  } catch (error) {
    resultMsg.value = error.message || '取款失败，请重试'
    resultType.value = 'error'
    step.value = 'result'
  }
}

const resetForm = () => {
  form.accountNumber = ''
  form.amount = 0
  password.value = ''
  step.value = 'input'
  resultMsg.value = ''
}

const closePanel = () => { resetForm(); emit('close') }
</script>

<style scoped>
.atm-container {
  width: 100%;
  max-width: 480px;
}

.atm-step {
  animation: fadeIn var(--transition-slow) ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(var(--space-2)); }
  to { opacity: 1; transform: translateY(0); }
}

.atm-step-header {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  margin-bottom: var(--space-6);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--glass-border);
}

.atm-icon {
  width: var(--space-12);
  height: var(--space-12);
  background: color-mix(in srgb, var(--color-primary) 10%, transparent);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all var(--transition-normal);
}

.confirm-icon {
  background: color-mix(in srgb, var(--color-primary) 10%, transparent);
}

.atm-step-title {
  margin: 0;
  font-size: var(--font-h3);
  font-weight: 600;
  color: var(--color-foreground);
  font-family: var(--font-sans);
}

.atm-step-desc {
  margin: var(--space-1) 0 0;
  font-size: var(--font-base);
  color: var(--color-muted-foreground);
  font-family: var(--font-sans);
}

.atm-form {
  max-width: 380px;
}

.form-row {
  margin-bottom: var(--space-5);
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

.atm-form :deep(.el-input__wrapper),
.atm-form :deep(.el-input-number__increase),
.atm-form :deep(.el-input-number__decrease) {
  height: var(--space-12);
  border-radius: var(--radius-lg) !important;
  background: var(--color-card);
  border: 1px solid var(--glass-border);
  box-shadow: none;
  transition: border-color var(--transition-normal);
}

.atm-form :deep(.el-input__wrapper:hover),
.atm-form :deep(.el-input-number__increase:hover),
.atm-form :deep(.el-input-number__decrease:hover) {
  border-color: var(--color-border-hover);
}

.atm-form :deep(.el-input__wrapper:focus-within),
.atm-form :deep(.el-input-number__increase:focus-within),
.atm-form :deep(.el-input-number__decrease:focus-within) {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--color-primary) 15%, transparent);
}

.atm-form .full-width {
  width: 100%;
}

.atm-form :deep(.el-input-number .el-input__wrapper) {
  padding-right: 50px;
}

.atm-form :deep(.el-input__inner) {
  color: var(--color-foreground);
  font-family: var(--font-sans);
}

.atm-form :deep(.el-input__inner::placeholder) {
  color: var(--color-muted-foreground);
}

.confirm-card {
  background: var(--color-card);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
  padding: var(--space-5) var(--space-6);
  margin-bottom: var(--space-6);
  box-shadow: var(--shadow-md);
}

.confirm-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-3) 0;
  border-bottom: 1px solid var(--glass-border);
}

.confirm-row:last-child {
  border-bottom: none;
}

.confirm-label {
  font-size: var(--font-base);
  color: var(--color-muted-foreground);
  font-family: var(--font-sans);
}

.confirm-value {
  font-size: var(--font-base);
  font-weight: 600;
  color: var(--color-foreground);
  font-family: var(--font-mono);
}

.confirm-value.highlight {
  color: var(--color-primary);
  font-size: var(--font-h3);
}

.result-display {
  text-align: center;
  padding: var(--space-5) 0;
}

.result-icon {
  margin-bottom: var(--space-3);
}

.result-title {
  font-size: var(--font-title);
  font-weight: 700;
  margin-bottom: var(--space-2);
  font-family: var(--font-sans);
}

.result-display.success .result-title {
  color: var(--color-success);
}

.result-display.error .result-title {
  color: var(--color-destructive);
}

.result-msg {
  font-size: var(--font-base);
  color: var(--color-muted-foreground);
  margin-bottom: var(--space-5);
  font-family: var(--font-sans);
}

.result-details {
  background: var(--color-card);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
  padding: var(--space-4) var(--space-5);
  text-align: left;
  max-width: 360px;
  margin: 0 auto;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  padding: var(--space-2) 0;
  font-size: var(--font-base);
  color: var(--color-muted-foreground);
  font-family: var(--font-sans);
}

.detail-row .highlight {
  color: var(--color-primary);
  font-weight: 600;
  font-family: var(--font-mono);
}

.atm-actions {
  display: flex;
  gap: var(--space-3);
  margin-top: var(--space-6);
}

.atm-btn-primary {
  flex: 1;
  height: var(--space-12);
  background: var(--color-primary);
  border: none;
  color: var(--color-on-primary);
  font-size: var(--font-base);
  font-weight: 600;
  border-radius: var(--radius-lg);
  font-family: var(--font-sans);
  cursor: pointer;
  transition: all var(--transition-normal);
  box-shadow: var(--shadow-md);
}

.atm-btn-primary:hover {
  background: var(--color-secondary);
  transform: translateY(-1px);
  box-shadow: var(--shadow-lg);
}

.atm-btn-primary:active {
  transform: translateY(0);
  box-shadow: var(--shadow-sm);
}

.atm-btn-secondary {
  flex: 1;
  height: var(--space-12);
  border: 1px solid var(--glass-border);
  background: var(--color-card);
  color: var(--color-muted-foreground);
  font-size: var(--font-base);
  font-weight: 500;
  border-radius: var(--radius-lg);
  font-family: var(--font-sans);
  cursor: pointer;
  transition: all var(--transition-normal);
}

.atm-btn-secondary:hover {
  border-color: var(--color-border-hover);
  color: var(--color-foreground);
  background: var(--color-muted);
}

@media (max-width: 480px) {
  .atm-container { max-width: 100%; }
}
</style>
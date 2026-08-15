<template>
  <div class="atm-container">
    <!-- Step 1: Input -->
    <div v-if="step === 'input'" class="atm-step">
      <div class="atm-step-header">
        <div class="atm-icon">
          <el-icon :size="28" color="var(--vault-accent)"><Coin /></el-icon>
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
        <el-button size="large" class="atm-btn-primary"  @click = "showPasswordDialog">
          下一步
        </el-button>
      </div>
    </div>

    <!-- Step 2: Confirm -->
    <div v-if="step === 'confirm'" class="atm-step">
      <div class="atm-step-header">
        <div class="atm-icon confirm-icon">
          <el-icon :size="28" color="var(--vault-gold)"><Warning /></el-icon>
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
          <el-icon :size="48" v-if="resultType === 'success'" color="var(--vault-up)"><CircleCheck /></el-icon>
          <el-icon :size="48" v-else color="var(--vault-down)"><CircleClose /></el-icon>
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

const props = defineProps({ accountNumber: { type: String, default: '' } })
const emit = defineEmits(['close', 'refresh'])
const refreshBalance = inject('refreshBalance', null)

const step = ref('input')
const form = reactive({ accountNo: props.accountNumber, amount: 0 })
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
    await deposit({ accountNo: form.accountNo, amount: form.amount, password: password.value,requestId : crypto.randomUUID()})
    if (refreshBalance) refreshBalance()
    emit('refresh')
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
.atm-container { width: 100%; max-width: 480px; }
.atm-step { animation: fadeIn 0.3s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
.atm-step-header { display: flex; align-items: center; gap: 14px; margin-bottom: 24px; padding-bottom: 16px; border-bottom: 1px solid var(--vault-line); }
.atm-icon {
  width: 48px; height: 48px;
  background: rgba(212,176,106,0.1);
  border: 1px solid var(--vault-line);
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.confirm-icon { background: rgba(212,176,106,0.1); }
.atm-step-title { margin: 0; font-size: 16px; font-weight: 600; color: var(--vault-text); }
.atm-step-desc { margin: 2px 0 0; font-size: 12px; color: var(--vault-text-3); }
.atm-form { max-width: 380px; }
.form-row { margin-bottom: 20px; }
.atm-label { display: block; font-size: 13px; font-weight: 600; color: var(--vault-text-2); margin-bottom: 6px; letter-spacing: 0.05em; }
.atm-form :deep(.el-input__wrapper),
.atm-form :deep(.el-input-number__increase),
.atm-form :deep(.el-input-number__decrease) { height: 46px; border-radius: 10px !important; }
.atm-form .full-width { width: 100%; }
.atm-form :deep(.el-input-number .el-input__wrapper) { padding-right: 50px; }

.confirm-card { background: rgba(8,12,20,0.5); border: 1px solid var(--vault-line); border-radius: 12px; padding: 20px 24px; margin-bottom: 24px; }
.confirm-row { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid var(--vault-line); }
.confirm-row:last-child { border-bottom: none; }
.confirm-label { font-size: 14px; color: var(--vault-text-3); }
.confirm-value { font-size: 15px; font-weight: 600; color: var(--vault-text); }
.confirm-value.highlight { color: var(--vault-down); font-size: 18px; }

.result-display { text-align: center; padding: 20px 0; }
.result-icon { margin-bottom: 12px; }
.result-title { font-size: 20px; font-weight: 700; margin-bottom: 8px; }
.result-display.success .result-title { color: var(--vault-up); }
.result-display.error .result-title { color: var(--vault-down); }
.result-msg { font-size: 14px; color: var(--vault-text-2); margin-bottom: 20px; }
.result-details { background: rgba(8,12,20,0.5); border: 1px solid var(--vault-line); border-radius: 12px; padding: 16px 20px; text-align: left; max-width: 360px; margin: 0 auto; }
.detail-row { display: flex; justify-content: space-between; padding: 8px 0; font-size: 14px; color: var(--vault-text-2); }
.detail-row .highlight { color: var(--vault-down); font-weight: 600; }

.atm-actions { display: flex; gap: 12px; margin-top: 24px; }
.atm-btn-primary {
  flex: 1; height: 46px;
  background: linear-gradient(135deg, #b08d4a, #d4b06a);
  border: none; color: #fff; font-size: 15px;
  font-weight: 600; border-radius: 10px;
  box-shadow: 0 0 18px rgba(212,176,106,0.2), inset 0 0 0 1px rgba(255,255,255,0.08);
}
.atm-btn-primary:hover { background: linear-gradient(135deg, #c9a458, #e6c98a); }
.atm-btn-secondary {
  flex: 1; height: 46px;
  border: 1px solid var(--vault-line); background: rgba(212,176,106,0.04);
  color: var(--vault-text-2); font-size: 15px; font-weight: 500; border-radius: 10px;
}
.atm-btn-secondary:hover { border-color: var(--vault-line-hover); color: var(--vault-text); }

@media (max-width: 480px) { .atm-container { max-width: 100%; } }
</style>

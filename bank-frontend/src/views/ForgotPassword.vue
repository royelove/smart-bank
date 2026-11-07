<template>
  <div class="forgot-page">
    <div class="forgot-container">
      <div class="vault-contour"></div>
      <div class="forgot-header">
        <router-link to="/login" class="back-link">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回登录</span>
        </router-link>
        <div class="header-brand">
          <div class="header-logo">
            <el-icon :size="28" color="var(--vault-accent)"><CreditCard /></el-icon>
            <span>钱钱银行</span>
          </div>
          <h2>忘记密码</h2>
          <p>验证身份后重置您的登录密码</p>
        </div>
      </div>

      <div class="forgot-body">
        <el-form :model="form" label-width="0" class="forgot-form">
          <div class="form-group">
            <label class="input-label">账户账号</label>
            <el-input
              v-model="form.accountNumber"
              placeholder="请输入您的银行账号"
              size="large"
              clearable
              :disabled="codeSent"
            />
          </div>

          <div class="form-group">
            <label class="input-label">手机号码</label>
            <el-input
              v-model="form.userPhoneNumber"
              placeholder="请输入绑定的手机号码"
              size="large"
              clearable
              maxlength="11"
              :disabled="codeSent"
            />
          </div>

          <div class="form-group">
            <label class="input-label">验证码</label>
            <div class="code-row">
              <el-input
                v-model="form.code"
                placeholder="请输入6位验证码"
                size="large"
                maxlength="6"
                @input="form.code = form.code.replace(/\D/g, '')"
              />
              <el-button
                size="large"
                :disabled="sending || countdown > 0 || !form.accountNumber || !form.userPhoneNumber"
                @click="handleSendCode"
                class="code-btn"
              >
                {{ countdown > 0 ? `${countdown}s` : (sending ? '发送中...' : '获取验证码') }}
              </el-button>
            </div>
          </div>

          <div class="form-group">
            <label class="input-label">新密码</label>
            <el-input
              v-model="form.newPassword"
              type="password"
              placeholder="请输入6位数字新密码"
              size="large"
              show-password
              maxlength="6"
              @input="form.newPassword = form.newPassword.replace(/\D/g, '')"
            />
          </div>

          <div class="form-group">
            <label class="input-label">确认新密码</label>
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              size="large"
              show-password
              maxlength="6"
              @input="form.confirmPassword = form.confirmPassword.replace(/\D/g, '')"
            />
          </div>

          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleReset"
            class="reset-btn"
          >
            {{ loading ? '重置中...' : '重置密码' }}
          </el-button>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { sendCode, resetPassword } from '@/api/account'
import { ArrowLeft } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)
const codeSent = ref(false)
let timer = null

const form = ref({
  accountNumber: '',
  userPhoneNumber: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})

const startCountdown = () => {
  countdown.value = 60
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
      timer = null
    }
  }, 1000)
}

const handleSendCode = async () => {
  if (!form.value.accountNumber) { ElMessage.warning('请输入账号'); return }
  if (!form.value.userPhoneNumber) { ElMessage.warning('请输入手机号'); return }
  sending.value = true
  try {
    await sendCode({
      accountNumber: form.value.accountNumber,
      userPhoneNumber: form.value.userPhoneNumber
    })
    codeSent.value = true
    ElMessage.success('验证码已发送，请查收')
    startCountdown()
  } catch (error) {
    console.error('发送验证码失败', error)
  } finally {
    sending.value = false
  }
}

const handleReset = async () => {
  if (!form.value.accountNumber) { ElMessage.warning('请输入账号'); return }
  if (!form.value.userPhoneNumber) { ElMessage.warning('请输入手机号'); return }
  if (!form.value.code) { ElMessage.warning('请输入验证码'); return }
  if (!/^\d{6}$/.test(form.value.newPassword)) { ElMessage.warning('密码必须为6位数字'); return }
  if (form.value.newPassword !== form.value.confirmPassword) { ElMessage.warning('两次密码不一致'); return }
  loading.value = true
  try {
    await resetPassword({
      accountNumber: form.value.accountNumber,
      code: form.value.code,
      newPassword: form.value.newPassword,
      confirmPassword: form.value.confirmPassword
    })
    ElMessage.success('密码重置成功，请重新登录')
    router.push('/login')
  } catch (error) {
    console.error('重置密码失败', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.forgot-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background:
    radial-gradient(1200px 600px at 30% 20%, rgba(139, 92, 246, 0.06) 0%, transparent 60%),
    var(--color-background);
  padding: 40px 20px;
  font-family: var(--font-sans);
}

.forgot-container {
  position: relative;
  width: 480px;
  max-width: 100%;
  background: var(--color-card);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-2xl);
  box-shadow: 0 32px 80px rgba(0, 0, 0, 0.4);
  overflow: hidden;
  animation: fadeInUp 0.5s ease-out;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

.forgot-header {
  padding: var(--space-8) var(--space-9) var(--space-7);
  border-bottom: 1px solid var(--color-border);
  position: relative;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-muted-foreground);
  text-decoration: none;
  margin-bottom: var(--space-5);
  cursor: pointer;
  transition: color var(--transition-fast) ease;
}

.back-link:hover {
  color: var(--color-primary);
}

.header-brand {
  text-align: center;
}

.header-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  margin-bottom: var(--space-5);
}

.header-logo svg {
  width: 28px;
  height: 28px;
  color: var(--color-primary);
}

.header-logo span {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-foreground);
  letter-spacing: 2px;
}

.header-brand h2 {
  font-size: var(--font-title);
  font-weight: 700;
  color: var(--color-foreground);
  letter-spacing: 0.05em;
  margin-bottom: var(--space-2);
}

.header-brand p {
  font-size: var(--font-body);
  color: var(--color-muted-foreground);
}

.forgot-body {
  padding: var(--space-7) var(--space-9) var(--space-9);
}

.forgot-form {
  max-width: 380px;
  margin: 0 auto;
}

.form-group {
  margin-bottom: var(--space-6);
}

.input-label {
  display: block;
  font-size: var(--font-sm);
  font-weight: 600;
  color: var(--color-muted-foreground);
  margin-bottom: var(--space-2);
  letter-spacing: 0.02em;
}

.forgot-form :deep(.el-input__wrapper) {
  height: var(--space-12);
  border-radius: var(--radius-md) !important;
  box-shadow: 0 0 0 1px var(--color-border) !important;
  background: var(--color-muted);
  transition: all var(--transition-fast) ease;
}

.forgot-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px var(--color-primary), 0 0 16px rgba(245, 158, 11, 0.15) !important;
}

.forgot-form :deep(.el-input__inner) {
  color: var(--color-foreground);
}

.forgot-form :deep(.el-input__inner::placeholder) {
  color: var(--color-muted-foreground);
}

.code-row {
  display: flex;
  gap: var(--space-3);
}

.code-row .el-input {
  flex: 1;
}

.code-btn {
  width: 130px;
  flex-shrink: 0;
  height: var(--space-12);
  background: var(--color-primary);
  border: none;
  color: var(--color-on-primary);
  font-size: var(--font-sm);
  font-weight: 600;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast) ease;
}

.code-btn:hover:not(:disabled) {
  background: var(--color-secondary);
}

.code-btn:disabled {
  background: var(--color-muted);
  color: var(--color-muted-foreground);
  cursor: not-allowed;
}

.reset-btn {
  width: 100%;
  height: var(--space-12);
  background: var(--color-primary);
  border: none;
  color: var(--color-on-primary);
  font-size: var(--font-base);
  font-weight: 600;
  border-radius: var(--radius-md);
  margin-top: var(--space-3);
  cursor: pointer;
  transition: all var(--transition-fast) ease;
}

.reset-btn:hover {
  background: var(--color-secondary);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(245, 158, 11, 0.35);
}

.reset-btn:active {
  transform: translateY(0);
}

@media (max-width: 480px) {
  .forgot-header { padding: var(--space-6) var(--space-5) var(--space-5); }
  .forgot-body { padding: var(--space-5); }
  .code-btn { width: 110px; font-size: 13px; }
}
</style>
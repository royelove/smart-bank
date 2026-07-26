<template>
  <div class="forgot-page">
    <div class="forgot-container">
      <div class="forgot-header">
        <router-link to="/login" class="back-link">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回登录</span>
        </router-link>
        <div class="header-brand">
          <div class="header-logo">
            <el-icon :size="28" color="#f5af19"><CreditCard /></el-icon>
            <span>智慧银行</span>
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
    radial-gradient(ellipse at 30% 20%, rgba(26,58,107,0.4) 0%, transparent 60%),
    radial-gradient(ellipse at 70% 80%, rgba(245,175,25,0.05) 0%, transparent 50%),
    #0a1628;
  padding: 40px 20px;
}

.forgot-container {
  width: 480px;
  max-width: 100%;
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 32px 80px rgba(0,0,0,0.4);
  overflow: hidden;
  animation: fadeInUp 0.5s ease-out;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

.forgot-header {
  padding: 28px 32px 20px;
  border-bottom: 1px solid #f0f2f5;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #999;
  text-decoration: none;
  margin-bottom: 16px;
  transition: color 0.2s;
}
.back-link:hover { color: #1a3a6b; }

.header-brand { text-align: center; }
.header-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 16px;
}
.header-logo span {
  font-size: 20px;
  font-weight: 700;
  color: #1a3a6b;
  letter-spacing: 2px;
}
.header-brand h2 {
  font-size: 22px;
  font-weight: 700;
  color: #0c1a3b;
  margin-bottom: 4px;
}
.header-brand p {
  font-size: 13px;
  color: #999;
}

.forgot-body {
  padding: 24px 32px 32px;
}

.forgot-form {
  max-width: 380px;
  margin: 0 auto;
}

.form-group {
  margin-bottom: 20px;
}

.input-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
}

.code-row {
  display: flex;
  gap: 12px;
}
.code-row .el-input {
  flex: 1;
}

.code-btn {
  width: 130px;
  flex-shrink: 0;
  height: 46px;
  background: linear-gradient(135deg, #1a3a6b, #2d5a8c);
  border: none;
  color: #fff;
  font-size: 14px;
  border-radius: 10px;
}
.code-btn:hover {
  background: linear-gradient(135deg, #2d5a8c, #3a6a9c);
  color: #fff;
}
.code-btn:disabled {
  background: #d0d5de;
  color: #999;
}

.forgot-form :deep(.el-input__wrapper) {
  height: 46px;
  border-radius: 10px !important;
}

.reset-btn {
  width: 100%;
  height: 48px;
  background: linear-gradient(135deg, #1a3a6b, #2d5a8c);
  border: none;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  border-radius: 10px;
  margin-top: 8px;
}
.reset-btn:hover {
  background: linear-gradient(135deg, #2d5a8c, #3a6a9c);
}

@media (max-width: 480px) {
  .forgot-header { padding: 20px 20px 16px; }
  .forgot-body { padding: 20px; }
  .code-btn { width: 110px; font-size: 13px; }
}
</style>

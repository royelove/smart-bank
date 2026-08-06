<template>
  <div class="register-page">
    <div class="register-container">
      <div class="register-header">
        <router-link to="/login" class="back-link">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回登录</span>
        </router-link>
        <div class="header-brand">
          <div class="header-logo">
            <el-icon :size="28" color="#f5af19"><CreditCard /></el-icon>
            <span>钱钱银行</span>
          </div>
          <h2>开通银行账户</h2>
          <p>填写以下信息完成账户注册</p>
        </div>
      </div>

      <!-- Step Progress -->
      <div class="step-indicator">
        <div class="step-item" :class="{ active: step === 1, done: step > 1 }">
          <div class="step-circle">
            <span v-if="step > 1">✓</span>
            <span v-else>1</span>
          </div>
          <span class="step-label">基本信息</span>
        </div>
        <div class="step-line" :class="{ active: step > 1 }"></div>
        <div class="step-item" :class="{ active: step === 2, done: step > 2 }">
          <div class="step-circle">
            <span v-if="step > 2">✓</span>
            <span v-else>2</span>
          </div>
          <span class="step-label">账户设置</span>
        </div>
        <div class="step-line" :class="{ active: step > 2 }"></div>
        <div class="step-item" :class="{ active: step === 3 }">
          <div class="step-circle">3</div>
          <span class="step-label">完成开通</span>
        </div>
      </div>

      <!-- Step 1: Basic Info -->
      <div v-if="step === 1" class="step-content">
        <el-form :model="registerForm" label-width="0" class="register-form">
          <div class="form-row">
            <div class="form-group">
              <label class="input-label">账户账号</label>
              <el-input v-model="registerForm.accountNumber" placeholder="请输入账号（6-20位数字）" size="large" clearable maxlength="20" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="input-label">账户户名</label>
              <el-input v-model="registerForm.accountHolderName" placeholder="请输入您的真实姓名" size="large" clearable />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="input-label">手机号码</label>
              <el-input v-model="registerForm.userPhoneNumber" placeholder="请输入11位手机号码" size="large" clearable maxlength="11" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="input-label">开户人有效身份证号</label>
              <el-input v-model="registerForm.userIdCard" placeholder="请输入有效身份证号" size="large" clearable maxlength="11" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="input-label">账户类型</label>
              <el-select v-model="registerForm.accountType" placeholder="请选择账户类型" size="large" style="width:100%">
                <el-option label="储蓄账户" value="储蓄">
                  <div class="option-item">
                    <el-icon color="#f5af19"><Coin /></el-icon>
                    <span>储蓄账户</span>
                    <span class="option-desc">活期存取，灵活方便</span>
                  </div>
                </el-option>
                <el-option label="活期账户" value="活期">
                  <div class="option-item">
                    <el-icon color="#52c41a"><Wallet /></el-icon>
                    <span>活期账户</span>
                    <span class="option-desc">随存随取，按日计息</span>
                  </div>
                </el-option>
                <el-option label="定期账户" value="定期">
                  <div class="option-item">
                    <el-icon color="#409eff"><Timer /></el-icon>
                    <span>定期账户</span>
                    <span class="option-desc">固定期限，利率更高</span>
                  </div>
                </el-option>
              </el-select>
            </div>
          </div>
        </el-form>
        <div class="step-actions">
          <el-button size="large" @click="step = 2" class="next-btn">
            下一步
          </el-button>
        </div>
      </div>

      <!-- Step 2: Password -->
      <div v-if="step === 2" class="step-content">
        <el-form :model="registerForm" label-width="0" class="register-form">
          <div class="form-row">
            <div class="form-group">
              <label class="input-label">设置交易密码</label>
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="请输入6位数字交易密码"
                size="large"
                show-password
                maxlength="6"
                @input="registerForm.password = registerForm.password.replace(/\D/g, '')"
              />
              <span class="input-tip">交易密码用于账户登录和交易验证</span>
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="input-label">确认交易密码</label>
              <el-input
                v-model="registerForm.checkPassword"
                type="password"
                placeholder="请再次输入交易密码"
                size="large"
                show-password
                maxlength="6"
                @input="registerForm.checkPassword = registerForm.checkPassword.replace(/\D/g, '')"
              />
            </div>
          </div>
          <div class="password-strength" v-if="registerForm.password.length > 0">
            <div class="strength-bar">
              <div class="strength-fill" :class="strengthClass" :style="{ width: strengthPercent + '%' }"></div>
            </div>
            <span class="strength-label">{{ strengthText }}</span>
          </div>
        </el-form>
        <div class="step-actions">
          <el-button size="large" class="prev-btn" @click="step = 1">上一步</el-button>
          <el-button size="large" @click="step = 3" class="next-btn">下一步</el-button>
        </div>
      </div>

      <!-- Step 3: Confirm -->
      <div v-if="step === 3" class="step-content">
        <div class="confirm-info">
          <div class="confirm-icon">
            <el-icon :size="48" color="#52c41a"><CircleCheck /></el-icon>
          </div>
          <h3 class="confirm-title">请确认开户信息</h3>
          <div class="info-card">
            <div class="info-row">
              <span class="info-label">账户账号</span>
              <span class="info-value">{{ registerForm.accountNumber }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">账户户名</span>
              <span class="info-value">{{ registerForm.accountHolderName }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">手机号码</span>
              <span class="info-value">{{ registerForm.userPhoneNumber }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">开户人有效身份证号</span>
              <span class="info-value">{{ registerForm.userIdCard }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">账户类型</span>
              <span class="info-value">{{ registerForm.accountType }}</span>
            </div>
          </div>
          <div class="agreement">
            <el-checkbox v-model="agreed">
              <span style="font-size:13px;">我已阅读并同意 <span class="agreement-link">《钱钱银行服务协议》</span></span>
            </el-checkbox>
          </div>
        </div>
        <div class="step-actions">
          <el-button size="large" class="prev-btn" @click="step = 2">上一步</el-button>
          <el-button
            size="large"
            :loading="loading"
            :disabled="!agreed"
            @click="handleRegister"
            class="submit-btn-final"
          >
            {{ loading ? '开通中...' : '确认开通' }}
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/account'
import { ArrowLeft, CircleCheck, Coin, Timer } from '@element-plus/icons-vue'

const router = useRouter()
const step = ref(1)
const agreed = ref(false)
const loading = ref(false)

const registerForm = ref({
  accountNumber: '',
  accountHolderName: '',
  accountType: '储蓄',
  userPhoneNumber: '',
  password: '',
  checkPassword: '',
  userIdCard: ''
})

const strengthPercent = computed(() => {
  const pwd = registerForm.value.password
  if (!pwd) return 0
  let score = 0
  if (pwd.length >= 6) score += 40
  if (/(\d)\1{2,}/.test(pwd)) score -= 10
  if (/^(?:\d)\1{5,}$/.test(pwd)) score -= 20
  if (pwd !== registerForm.value.checkPassword) score -= 10
  return Math.max(10, Math.min(100, score))
})

const strengthClass = computed(() => {
  if (strengthPercent.value < 40) return 'weak'
  if (strengthPercent.value < 70) return 'medium'
  return 'strong'
})

const strengthText = computed(() => {
  if (strengthPercent.value < 40) return '密码强度：较弱'
  if (strengthPercent.value < 70) return '密码强度：中等'
  return '密码强度：强'
})

const handleRegister = async () => {
  const form = registerForm.value
  if (!form.accountNumber) { ElMessage.warning('请输入账号'); return }
  if (!form.accountHolderName) { ElMessage.warning('请输入户名'); return }
  if (!form.accountType) { ElMessage.warning('请选择账户类型'); return }
  if (!form.userPhoneNumber) { ElMessage.warning('请输入手机号'); return }
  if (!/^\d{6}$/.test(form.password)) { ElMessage.warning('密码必须为6位数字'); return }
  if (form.password !== form.checkPassword) { ElMessage.warning('两次密码不一致'); return }
  if (!agreed.value) { ElMessage.warning('请阅读并同意服务协议'); return }

  loading.value = true
  try {
    await register(form)
    ElMessage.success('🎉 账户开通成功！请登录')
    router.push('/login')
  } catch (error) {
    console.error('注册失败', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
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

.register-container {
  width: 560px;
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

.register-header {
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

/* ===== STEP INDICATOR ===== */
.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 32px;
  gap: 0;
}

.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.step-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  background: #f0f2f5;
  color: #999;
  transition: all 0.3s;
}

.step-item.active .step-circle {
  background: #1a3a6b;
  color: #fff;
  box-shadow: 0 4px 12px rgba(26,58,107,0.3);
}

.step-item.done .step-circle {
  background: #52c41a;
  color: #fff;
}

.step-label {
  font-size: 12px;
  color: #999;
  font-weight: 500;
  white-space: nowrap;
}

.step-item.active .step-label { color: #1a3a6b; font-weight: 600; }
.step-item.done .step-label { color: #52c41a; }

.step-line {
  width: 60px;
  height: 2px;
  background: #e0e4ea;
  margin: 0 8px;
  margin-bottom: 22px;
  transition: background 0.3s;
}
.step-line.active { background: #52c41a; }

/* ===== FORM ===== */
.step-content {
  padding: 24px 32px 32px;
}

.register-form {
  max-width: 400px;
  margin: 0 auto;
}

.form-row { margin-bottom: 20px; }

.input-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
}

.input-tip {
  display: block;
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

.register-form :deep(.el-input__wrapper) {
  height: 46px;
  border-radius: 10px !important;
}

.register-form :deep(.el-select__wrapper) {
  height: 46px;
  border-radius: 10px;
  box-shadow: 0 0 0 1px #d0d5de !important;
}
.register-form :deep(.el-select__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #1a3a6b !important;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.option-desc {
  font-size: 11px;
  color: #999;
  margin-left: 4px;
}

/* Password Strength */
.password-strength {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: -8px;
  margin-bottom: 20px;
}
.strength-bar {
  flex: 1;
  height: 4px;
  background: #f0f2f5;
  border-radius: 2px;
  overflow: hidden;
}
.strength-fill {
  height: 100%;
  border-radius: 2px;
  transition: all 0.3s;
}
.strength-fill.weak { background: #f56c6c; }
.strength-fill.medium { background: #e6a23c; }
.strength-fill.strong { background: #67c23a; }
.strength-label { font-size: 11px; color: #999; white-space: nowrap; }

/* ===== CONFIRM ===== */
.confirm-info { text-align: center; }
.confirm-icon { margin-bottom: 12px; }
.confirm-title {
  font-size: 18px;
  font-weight: 600;
  color: #0c1a3b;
  margin-bottom: 20px;
}

.info-card {
  background: #f8f9fc;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 20px;
  text-align: left;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #eee;
}
.info-row:last-child { border-bottom: none; }
.info-label { font-size: 14px; color: #999; }
.info-value { font-size: 14px; font-weight: 600; color: #333; }

.agreement { margin-bottom: 16px; }
.agreement-link { color: #1a3a6b; cursor: pointer; font-weight: 600; }
.agreement-link:hover { color: #f5af19; }

/* ===== ACTIONS ===== */
.step-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  max-width: 400px;
  margin: 24px auto 0;
}

.next-btn, .submit-btn-final {
  flex: 1;
  height: 48px;
  background: linear-gradient(135deg, #1a3a6b, #2d5a8c);
  border: none;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  border-radius: 10px;
}
.next-btn:hover, .submit-btn-final:hover {
  background: linear-gradient(135deg, #2d5a8c, #3a6a9c);
}
.submit-btn-final:disabled { background: #d0d5de; color: #999; }

.prev-btn {
  flex: 1;
  height: 48px;
  border: 1px solid #d0d5de;
  background: #fff;
  color: #666;
  font-size: 16px;
  font-weight: 500;
  border-radius: 10px;
}
.prev-btn:hover { border-color: #1a3a6b; color: #1a3a6b; }

.register-footer { text-align: center; padding: 16px 32px 24px; font-size: 13px; color: #999; border-top: 1px solid #f0f2f5; }
.footer-link { color: #1a3a6b; text-decoration: none; font-weight: 600; }
.footer-link:hover { color: #f5af19; }

@media (max-width: 480px) {
  .register-header { padding: 20px 20px 16px; }
  .step-content { padding: 20px; }
  .step-line { width: 30px; }
}
</style>

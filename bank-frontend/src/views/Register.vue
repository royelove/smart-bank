<template>
  <div class="register-page">
    <div class="register-split">
      <!-- Left: Branding -->
      <div class="brand-panel">
        <div class="brand-bg-grid"></div>
        <div class="brand-bg-glow"></div>
        <div class="brand-content">
          <div class="brand-top">
            <div class="brand-logo">
              <svg width="48" height="48" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M24 4L4 14V34L24 44L44 34V14L24 4Z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
                <path d="M24 4V44" stroke="currentColor" stroke-width="1.5"/>
                <path d="M4 14L24 24L44 14" stroke="currentColor" stroke-width="1.5"/>
                <path d="M24 24V44" stroke="currentColor" stroke-width="1.5" stroke-dasharray="4 2"/>
              </svg>
            </div>
            <div>
              <h1 class="brand-title">SMART<br/>BANK</h1>
              <p class="brand-tagline">Private Banking</p>
            </div>
          </div>

          <div class="steps-visual">
            <div class="step-visual-item" :class="{ active: step >= 1, done: step > 1 }">
              <div class="sv-circle">1</div>
              <div class="sv-info">
                <span class="sv-title">Basic Info</span>
                <span class="sv-desc">Account details</span>
              </div>
            </div>
            <div class="sv-line" :class="{ active: step > 1 }"></div>
            <div class="step-visual-item" :class="{ active: step >= 2, done: step > 2 }">
              <div class="sv-circle">2</div>
              <div class="sv-info">
                <span class="sv-title">Security</span>
                <span class="sv-desc">Set password</span>
              </div>
            </div>
            <div class="sv-line" :class="{ active: step > 2 }"></div>
            <div class="step-visual-item" :class="{ active: step >= 3 }">
              <div class="sv-circle">3</div>
              <div class="sv-info">
                <span class="sv-title">Confirm</span>
                <span class="sv-desc">Review & submit</span>
              </div>
            </div>
          </div>

          <div class="brand-bottom">
            <p class="brand-copy">&copy; 2026 Smart Bank. All rights reserved.</p>
          </div>
        </div>
      </div>

      <!-- Right: Form -->
      <div class="form-panel">
        <div class="form-scroll">
          <div class="form-container">
            <router-link to="/login" class="back-link">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M19 12H5"/><polyline points="12 19 5 12 12 5"/>
              </svg>
              <span>Back to Login</span>
            </router-link>

            <div class="form-header">
              <h2 class="form-title">Create Account</h2>
              <p class="form-subtitle">Complete the form to open your account</p>
            </div>

            <!-- Step 1 -->
            <div v-if="step === 1" class="step-content">
              <el-form :model="registerForm" label-width="0" class="register-form">
                <div class="field-group">
                  <label class="field-label">Account Number</label>
                  <el-input v-model="registerForm.accountNumber" placeholder="6-20 digits" size="large" clearable maxlength="20" />
                </div>
                <div class="field-group">
                  <label class="field-label">Account Holder Name</label>
                  <el-input v-model="registerForm.accountHolderName" placeholder="Enter your full name" size="large" clearable />
                </div>
                <div class="field-group">
                  <label class="field-label">Phone Number</label>
                  <el-input v-model="registerForm.userPhoneNumber" placeholder="11-digit phone number" size="large" clearable maxlength="11" />
                </div>
                <div class="field-group">
                  <label class="field-label">ID Card Number</label>
                  <el-input v-model="registerForm.userIdCard" placeholder="18-digit ID number" size="large" clearable maxlength="18" />
                </div>
                <div class="field-group">
                  <label class="field-label">Account Type</label>
                  <el-select v-model="registerForm.accountType" placeholder="Select type" size="large" style="width:100%">
                    <el-option label="Savings Account" value="储蓄" />
                    <el-option label="Current Account" value="活期" />
                    <el-option label="Fixed Deposit" value="定期" />
                  </el-select>
                </div>
              </el-form>
              <div class="step-actions">
                <el-button size="large" type="primary" @click="step = 2" class="action-btn">Next Step</el-button>
              </div>
            </div>

            <!-- Step 2 -->
            <div v-if="step === 2" class="step-content">
              <el-form :model="registerForm" label-width="0" class="register-form">
                <div class="field-group">
                  <label class="field-label">Set Transaction Password</label>
                  <el-input
                    v-model="registerForm.password"
                    type="password"
                    placeholder="6-digit password"
                    size="large"
                    show-password
                    maxlength="6"
                    @input="registerForm.password = registerForm.password.replace(/\D/g, '')"
                  />
                  <span class="field-tip">Password is used for login and transaction verification</span>
                </div>
                <div class="field-group">
                  <label class="field-label">Confirm Password</label>
                  <el-input
                    v-model="registerForm.checkPassword"
                    type="password"
                    placeholder="Re-enter password"
                    size="large"
                    show-password
                    maxlength="6"
                    @input="registerForm.checkPassword = registerForm.checkPassword.replace(/\D/g, '')"
                  />
                </div>
                <div class="password-strength" v-if="registerForm.password.length > 0">
                  <div class="strength-bar">
                    <div class="strength-fill" :class="strengthClass" :style="{ width: strengthPercent + '%' }"></div>
                  </div>
                  <span class="strength-label">{{ strengthText }}</span>
                </div>
              </el-form>
              <div class="step-actions">
                <el-button size="large" @click="step = 1" class="action-btn-outline">Previous</el-button>
                <el-button size="large" type="primary" @click="step = 3" class="action-btn">Next Step</el-button>
              </div>
            </div>

            <!-- Step 3 -->
            <div v-if="step === 3" class="step-content">
              <div class="confirm-section">
                <div class="confirm-icon">
                  <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <path d="M22 11.08V12a10 10 0 11-5.93-9.14"/>
                    <polyline points="22 4 12 14.01 9 11.01"/>
                  </svg>
                </div>
                <h3 class="confirm-title">Confirm Your Information</h3>
                <div class="info-card">
                  <div class="info-row">
                    <span class="info-label">Account Number</span>
                    <span class="info-value">{{ registerForm.accountNumber }}</span>
                  </div>
                  <div class="info-row">
                    <span class="info-label">Account Holder</span>
                    <span class="info-value">{{ registerForm.accountHolderName }}</span>
                  </div>
                  <div class="info-row">
                    <span class="info-label">Phone Number</span>
                    <span class="info-value">{{ registerForm.userPhoneNumber }}</span>
                  </div>
                  <div class="info-row">
                    <span class="info-label">ID Card</span>
                    <span class="info-value">{{ registerForm.userIdCard }}</span>
                  </div>
                  <div class="info-row">
                    <span class="info-label">Account Type</span>
                    <span class="info-value">{{ registerForm.accountType }}</span>
                  </div>
                </div>
                <div class="agreement">
                  <el-checkbox v-model="agreed">
                    <span style="font-size:13px;">I agree to the <span class="agreement-link">Terms of Service</span></span>
                  </el-checkbox>
                </div>
              </div>
              <div class="step-actions">
                <el-button size="large" @click="step = 2" class="action-btn-outline">Previous</el-button>
                <el-button
                  size="large"
                  type="primary"
                  :loading="loading"
                  :disabled="!agreed"
                  @click="handleRegister"
                  class="action-btn"
                >
                  {{ loading ? 'Creating...' : 'Create Account' }}
                </el-button>
              </div>
            </div>
          </div>
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
  if (strengthPercent.value < 40) return 'Password strength: Weak'
  if (strengthPercent.value < 70) return 'Password strength: Medium'
  return 'Password strength: Strong'
})

const handleRegister = async () => {
  const form = registerForm.value
  if (!form.accountNumber) { ElMessage.warning('Please enter account number'); return }
  if (!form.accountHolderName) { ElMessage.warning('Please enter account holder name'); return }
  if (!form.accountType) { ElMessage.warning('Please select account type'); return }
  if (!form.userPhoneNumber) { ElMessage.warning('Please enter phone number'); return }
  if (!/^\d{6}$/.test(form.password)) { ElMessage.warning('Password must be 6 digits'); return }
  if (form.password !== form.checkPassword) { ElMessage.warning('Passwords do not match'); return }
  if (!agreed.value) { ElMessage.warning('Please agree to the terms'); return }

  loading.value = true
  try {
    await register(form)
    ElMessage.success('Account created successfully! Please login.')
    router.push('/login')
  } catch (error) {
    console.error('Registration failed', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  background: var(--color-background);
}

.register-split {
  display: flex;
  min-height: 100vh;
}

/* ====== Brand Panel ====== */
.brand-panel {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background:
    radial-gradient(ellipse at 20% 80%, rgba(245, 158, 11, 0.06) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 20%, rgba(139, 92, 246, 0.06) 0%, transparent 50%),
    linear-gradient(135deg, #0A0E1A 0%, #111827 100%);
}

.brand-bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255,255,255,0.015) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.015) 1px, transparent 1px);
  background-size: 60px 60px;
}

.brand-bg-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 600px;
  height: 600px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(245, 158, 11, 0.04) 0%, transparent 70%);
  pointer-events: none;
}

.brand-content {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px;
  max-width: 420px;
  width: 100%;
}

.brand-top {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-6);
  margin-bottom: 56px;
}

.brand-logo {
  width: 80px;
  height: 80px;
  border-radius: var(--radius-xl);
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-secondary) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-on-primary);
  box-shadow: 0 0 48px rgba(245, 158, 11, 0.25), 0 0 96px rgba(245, 158, 11, 0.08);
}

.brand-title {
  font-size: 42px;
  font-weight: 700;
  color: var(--color-foreground);
  letter-spacing: 8px;
  line-height: 1.15;
  text-align: center;
  margin: 0;
}

.brand-tagline {
  font-size: 13px;
  color: var(--color-muted-foreground);
  letter-spacing: 6px;
  text-transform: uppercase;
  font-weight: 400;
  margin: 8px 0 0;
}

.steps-visual {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0;
  width: 100%;
  max-width: 280px;
  margin-bottom: 56px;
}

.step-visual-item {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  width: 100%;
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-lg);
  transition: all 250ms ease;
}

.step-visual-item.active {
  background: rgba(245, 158, 11, 0.06);
}

.sv-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  background: var(--color-muted);
  border: 2px solid var(--color-border);
  color: var(--color-muted-foreground);
  flex-shrink: 0;
  transition: all 250ms ease;
}

.step-visual-item.active .sv-circle {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: var(--color-on-primary);
  box-shadow: 0 0 16px rgba(245, 158, 11, 0.3);
}

.step-visual-item.done .sv-circle {
  background: rgba(34, 197, 94, 0.12);
  border-color: var(--color-success);
  color: var(--color-success);
}

.sv-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sv-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-foreground);
}

.sv-desc {
  font-size: 11px;
  color: var(--color-muted-foreground);
}

.sv-line {
  width: 2px;
  height: 24px;
  background: var(--color-border);
  margin-left: 17px;
  transition: background 250ms ease;
}

.sv-line.active {
  background: var(--color-success);
}

.brand-bottom {
  text-align: center;
}

.brand-copy {
  font-size: 11px;
  color: var(--color-muted-foreground);
  opacity: 0.5;
  margin: 0;
}

/* ====== Form Panel ====== */
.form-panel {
  width: 560px;
  min-width: 560px;
  display: flex;
  align-items: stretch;
  background: linear-gradient(180deg, var(--color-background) 0%, #0D1321 100%);
}

.form-scroll {
  flex: 1;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 48px;
  overflow-y: auto;
}

.form-container {
  width: 100%;
  max-width: 420px;
  padding-top: 24px;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-muted-foreground);
  text-decoration: none;
  margin-bottom: 32px;
  transition: color var(--transition-fast);
  font-weight: 500;
}

.back-link:hover {
  color: var(--color-primary);
}

.form-header {
  margin-bottom: 32px;
}

.form-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-foreground);
  margin-bottom: 8px;
}

.form-subtitle {
  font-size: 14px;
  color: var(--color-muted-foreground);
  margin: 0;
}

.field-group {
  margin-bottom: 20px;
}

.field-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-muted-foreground);
  margin-bottom: 8px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.field-tip {
  display: block;
  font-size: 12px;
  color: var(--color-muted-foreground);
  margin-top: 6px;
  opacity: 0.7;
}

.register-form :deep(.el-input__wrapper) {
  height: 50px;
  border-radius: var(--radius-md) !important;
  box-shadow: 0 0 0 1px var(--color-border) !important;
  padding: 0 16px;
  background: var(--color-muted);
  transition: all var(--transition-fast);
}

.register-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--color-border-hover) !important;
}

.register-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--color-ring), 0 0 0 4px rgba(6, 182, 212, 0.08) !important;
}

.register-form :deep(.el-input__inner) {
  font-size: 14px;
  color: var(--color-foreground);
}

.register-form :deep(.el-select__wrapper) {
  height: 50px;
  border-radius: var(--radius-md) !important;
  box-shadow: 0 0 0 1px var(--color-border) !important;
  background: var(--color-muted);
}

.register-form :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px var(--color-ring), 0 0 0 4px rgba(6, 182, 212, 0.08) !important;
}

.password-strength {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-top: 12px;
  margin-bottom: 4px;
}

.strength-bar {
  flex: 1;
  height: 4px;
  background: var(--color-muted);
  border-radius: 2px;
  overflow: hidden;
}

.strength-fill {
  height: 100%;
  border-radius: 2px;
  transition: all 250ms ease;
}

.strength-fill.weak { background: var(--color-destructive); }
.strength-fill.medium { background: var(--color-warning); }
.strength-fill.strong { background: var(--color-success); }

.strength-label {
  font-size: 12px;
  color: var(--color-muted-foreground);
  white-space: nowrap;
}

.step-actions {
  display: flex;
  gap: var(--space-3);
  margin-top: 32px;
}

.action-btn {
  flex: 1;
  height: 50px;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.action-btn-outline {
  flex: 1;
  height: 50px;
  font-size: 14px;
  font-weight: 600;
}

.confirm-section {
  text-align: center;
}

.confirm-icon {
  margin-bottom: var(--space-5);
  color: var(--color-success);
}

.confirm-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-foreground);
  margin-bottom: var(--space-5);
}

.info-card {
  background: var(--color-muted);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4) var(--space-5);
  text-align: left;
  margin-bottom: var(--space-5);
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: var(--space-2) 0;
  border-bottom: 1px solid var(--color-border);
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  font-size: 13px;
  color: var(--color-muted-foreground);
}

.info-value {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-foreground);
}

.agreement {
  margin-bottom: var(--space-4);
  text-align: left;
}

.agreement-link {
  color: var(--color-primary);
  cursor: pointer;
  font-weight: 600;
}

.agreement-link:hover {
  color: var(--color-secondary);
}

/* ====== Responsive ====== */
@media (max-width: 1024px) {
  .brand-panel {
    display: none;
  }
  .form-panel {
    width: 100%;
    min-width: unset;
  }
}

@media (max-width: 480px) {
  .form-scroll {
    padding: 32px 24px;
  }
  .form-title {
    font-size: 24px;
  }
  .step-actions {
    flex-direction: column;
  }
}
</style>

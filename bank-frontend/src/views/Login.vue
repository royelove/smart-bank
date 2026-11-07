<template>
  <div class="login-page">
    <div class="login-split">
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

          <div class="brand-features">
            <div class="feature-card" v-for="(f, i) in features" :key="i">
              <div class="feature-icon-wrap">
                <component :is="f.icon" :size="20" />
              </div>
              <div class="feature-text">
                <span class="feature-title">{{ f.title }}</span>
                <span class="feature-desc">{{ f.desc }}</span>
              </div>
            </div>
          </div>

          <div class="brand-bottom">
            <div class="trust-badges">
              <div class="trust-badge">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                </svg>
                <span>SSL 256-bit</span>
              </div>
              <div class="trust-badge">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 11.08V12a10 10 0 11-5.93-9.14"/>
                  <polyline points="22 4 12 14.01 9 11.01"/>
                </svg>
                <span>FDIC Insured</span>
              </div>
            </div>
            <p class="brand-copy">&copy; 2026 Smart Bank. All rights reserved.</p>
          </div>
        </div>
      </div>

      <!-- Right: Form -->
      <div class="form-panel">
        <div class="form-scroll">
          <div class="form-container">
            <div class="form-header">
              <div class="secure-badge">
                <span class="secure-dot"></span>
                <span>SECURE CONNECTION</span>
              </div>
              <h2 class="form-title">Welcome Back</h2>
              <p class="form-subtitle">Sign in to your private banking account</p>
            </div>

            <el-form :model="loginForm" @keyup.enter="handleLogin" class="login-form">
              <div class="field-group">
                <label class="field-label">Account Number</label>
                <el-input
                  v-model="loginForm.accountNumber"
                  placeholder="Enter your account number"
                  size="large"
                  clearable
                >
                  <template #prefix>
                    <el-icon><User /></el-icon>
                  </template>
                </el-input>
              </div>

              <div class="field-group">
                <label class="field-label">Password</label>
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  placeholder="Enter 6-digit password"
                  size="large"
                  show-password
                  maxlength="6"
                  @input="loginForm.password = loginForm.password.replace(/\D/g, '')"
                >
                  <template #prefix>
                    <el-icon><Lock /></el-icon>
                  </template>
                </el-input>
              </div>

              <el-button
                type="primary"
                size="large"
                :loading="loading"
                @click="handleLogin"
                class="login-btn"
              >
                {{ loading ? 'Verifying...' : 'Sign In' }}
              </el-button>
            </el-form>

            <div class="form-footer">
              <div class="form-links">
                <span class="link" @click="goRegister">Create Account</span>
                <span class="link-sep"></span>
                <span class="link" @click="goForgotPassword">Forgot Password?</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, h } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/account'
import { User, Lock, CircleCheck, Lightning, Timer } from '@element-plus/icons-vue'

const router = useRouter()
const loginForm = ref({
  accountNumber: '',
  password: ''
})
const loading = ref(false)

const features = [
  { icon: CircleCheck, title: 'Bank-grade Security', desc: '256-bit encryption for all transactions' },
  { icon: Lightning, title: 'Instant Transfer', desc: '24/7 real-time money transfers' },
  { icon: Timer, title: 'AI Assistant', desc: 'Intelligent customer service 24/7' }
]

const handleLogin = async () => {
  if (!loginForm.value.accountNumber || !loginForm.value.password) {
    ElMessage.warning('Please enter account and password')
    return
  }
  loading.value = true
  try {
    const res = await login(loginForm.value)
    const { token, accountVO } = res.data
    localStorage.setItem('token', token)
    localStorage.setItem('userInfo', JSON.stringify(accountVO))
    ElMessage.success('Login successful')
    router.push('/dashboard')
  } catch (error) {
    console.error('Login failed', error)
  } finally {
    loading.value = false
  }
}

const goRegister = () => router.push('/register')
const goForgotPassword = () => router.push('/forgot-password')
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: var(--color-background);
}

.login-split {
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
  max-width: 460px;
  width: 100%;
}

.brand-top {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-6);
  margin-bottom: 64px;
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
  margin: 0;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  width: 100%;
  margin-bottom: 64px;
}

.feature-card {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4) var(--space-5);
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.04);
  border-radius: var(--radius-lg);
  transition: all 250ms ease;
}

.feature-card:hover {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(245, 158, 11, 0.15);
  transform: translateX(4px);
}

.feature-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  background: rgba(245, 158, 11, 0.08);
  border: 1px solid rgba(245, 158, 11, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  flex-shrink: 0;
}

.feature-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.feature-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-foreground);
}

.feature-desc {
  font-size: 12px;
  color: var(--color-muted-foreground);
}

.brand-bottom {
  text-align: center;
}

.trust-badges {
  display: flex;
  justify-content: center;
  gap: var(--space-5);
  margin-bottom: var(--space-5);
}

.trust-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: var(--color-success);
  font-weight: 500;
  letter-spacing: 0.04em;
}

.brand-copy {
  font-size: 11px;
  color: var(--color-muted-foreground);
  opacity: 0.5;
  margin: 0;
}

/* ====== Form Panel ====== */
.form-panel {
  width: 520px;
  min-width: 520px;
  display: flex;
  align-items: stretch;
  background: linear-gradient(180deg, var(--color-background) 0%, #0D1321 100%);
}

.form-scroll {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
}

.form-container {
  width: 100%;
  max-width: 400px;
}

.form-header {
  margin-bottom: 40px;
}

.secure-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
  padding: 6px 14px;
  background: rgba(34, 197, 94, 0.06);
  border: 1px solid rgba(34, 197, 94, 0.12);
  border-radius: var(--radius-full);
  font-size: 10px;
  color: var(--color-success);
  letter-spacing: 0.15em;
  font-weight: 600;
  text-transform: uppercase;
}

.secure-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-success);
  box-shadow: 0 0 6px var(--color-success);
}

.form-title {
  font-size: 30px;
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

.login-form :deep(.el-input__wrapper) {
  height: 50px;
  border-radius: var(--radius-md) !important;
  box-shadow: 0 0 0 1px var(--color-border) !important;
  padding: 0 16px;
  background: var(--color-muted);
  transition: all var(--transition-fast);
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--color-border-hover) !important;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--color-ring), 0 0 0 4px rgba(6, 182, 212, 0.08) !important;
}

.login-form :deep(.el-input__inner) {
  font-size: 14px;
  color: var(--color-foreground);
}

.login-form :deep(.el-input__prefix) {
  margin-right: 8px;
  color: var(--color-muted-foreground);
}

.login-btn {
  width: 100%;
  height: 50px;
  margin-top: 8px;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 1px;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-secondary) 100%) !important;
  border: none !important;
  box-shadow: 0 4px 20px rgba(245, 158, 11, 0.25);
}

.login-btn:hover {
  box-shadow: 0 6px 28px rgba(245, 158, 11, 0.35);
  transform: translateY(-1px);
}

.form-footer {
  margin-top: 32px;
  text-align: center;
}

.form-links {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
}

.link {
  font-size: 13px;
  color: var(--color-primary);
  cursor: pointer;
  transition: color var(--transition-fast);
  font-weight: 600;
}

.link:hover {
  color: var(--color-secondary);
}

.link-sep {
  width: 1px;
  height: 14px;
  background: var(--color-border);
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
}
</style>

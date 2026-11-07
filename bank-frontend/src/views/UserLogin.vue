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
              <span class="brand-logo-text">钱</span>
            </div>
            <div>
              <h1 class="brand-title">钱钱银行</h1>
              <p class="brand-tagline">SMART BANKING</p>
            </div>
          </div>

          <div class="brand-features">
            <div class="feature-card" v-for="(f, i) in features" :key="i">
              <div class="feature-icon-wrap">
                <el-icon :size="20"><component :is="f.icon" /></el-icon>
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
                <span>Bank-grade Security</span>
              </div>
            </div>
            <p class="brand-copy">&copy; 2026 钱钱银行. All rights reserved.</p>
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
                <span>SECURE CHANNEL</span>
              </div>
              <h2 class="form-title">用户登录</h2>
              <p class="form-subtitle">使用手机号和密码登录您的账户</p>
            </div>

            <el-form :model="form" @keyup.enter="handleLogin" class="login-form">
              <div class="field-group">
                <label class="field-label">手机号</label>
                <el-input v-model="form.userPhone" placeholder="请输入注册手机号" size="large" clearable maxlength="11" />
              </div>
              <div class="field-group">
                <label class="field-label">密码</label>
                <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" show-password />
              </div>
              <el-button type="primary" size="large" :loading="loading" @click="handleLogin" class="login-btn">
                {{ loading ? '登录中...' : '登 录' }}
              </el-button>
            </el-form>

            <div class="form-footer">
              <div class="form-links">
                <span class="link" @click="goRegister">注册新用户</span>
                <span class="link-sep"></span>
                <span class="link" @click="goCardLogin">卡号登录</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userLogin } from '@/api/user'
import { User, Link, Coin } from '@element-plus/icons-vue'

const router = useRouter()
const form = ref({ userPhone: '', password: '' })
const loading = ref(false)

const features = [
  { icon: User, title: '一人多卡', desc: '一个账户管理多张银行卡' },
  { icon: Link, title: '快速关联', desc: '通过身份证快速绑定已有银行卡' },
  { icon: Coin, title: '便捷管理', desc: '统一查看名下所有账户资产' }
]

const handleLogin = async () => {
  if (!form.value.userPhone || !form.value.password) {
    ElMessage.warning('请填写手机号和密码')
    return
  }
  loading.value = true
  try {
    const res = await userLogin(form.value)
    localStorage.setItem('token', res.data)
    ElMessage.success('登录成功')
    router.push('/user/dashboard')
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const goRegister = () => router.push('/user/register')
const goCardLogin = () => router.push('/login')
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
  box-shadow: 0 0 48px rgba(245, 158, 11, 0.25), 0 0 96px rgba(245, 158, 11, 0.08);
}

.brand-logo-text {
  font-size: 36px;
  font-weight: 900;
  color: var(--color-on-primary);
}

.brand-title {
  font-size: 40px;
  font-weight: 900;
  color: var(--color-foreground);
  letter-spacing: 6px;
  margin: 0;
  background: linear-gradient(180deg, #ffffff 0%, var(--color-primary) 150%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.brand-tagline {
  font-size: 13px;
  color: var(--color-muted-foreground);
  letter-spacing: 8px;
  font-weight: 500;
  margin: 8px 0 0;
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

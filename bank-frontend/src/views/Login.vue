<template>
  <div class="login-page">
    <!-- Left: Branding & Atmosphere -->
    <div class="login-brand">
      <div class="brand-overlay"></div>
      <div class="brand-content">
        <div class="brand-logo">
          <h1 class="brand-title">智慧银行</h1>
          <p class="brand-subtitle">SMART BANKING</p>
        </div>
        <div class="brand-features">
          <div class="feature-item">
            <div class="feature-icon"><el-icon :size="22" color="#f5af19"><Lock /></el-icon></div>
            <div class="feature-text">
              <span class="feature-label">安全可靠</span>
              <span class="feature-desc">256位加密传输，保障资金安全</span>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon"><el-icon :size="22" color="#f5af19"><Lightning /></el-icon></div>
            <div class="feature-text">
              <span class="feature-label">极速到账</span>
              <span class="feature-desc">7×24小时实时转账，秒级到账</span>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon"><el-icon :size="22" color="#f5af19"><Service /></el-icon></div>
            <div class="feature-text">
              <span class="feature-label">智能服务</span>
              <span class="feature-desc">AI智能客服，全天候在线服务</span>
            </div>
          </div>
        </div>
        <div class="brand-footer">
          <p>全国服务热线：95588</p>
          <p>© 2026 智慧银行 版权所有</p>
        </div>
      </div>
      <!-- Animated particles -->
      <div class="particles">
        <span v-for="i in 20" :key="i" class="particle" :style="particleStyle(i)"></span>
      </div>
    </div>

    <!-- Right: Login Form -->
    <div class="login-form-wrap">
      <div class="form-container">
        <div class="form-header">
          <div class="form-welcome">
            <h2>欢迎登录</h2>
            <p>请使用您的账户信息登录</p>
          </div>
        </div>

        <el-form :model="loginForm" @keyup.enter="handleLogin" class="login-form">
          <div class="input-group">
            <label class="input-label">账户账号</label>
            <el-input
              v-model="loginForm.accountNumber"
              placeholder="请输入您的银行账号"
              size="large"
              clearable
            >
              <template #prefix>
                <el-icon color="#1a3a6b"><User /></el-icon>
              </template>
            </el-input>
          </div>

          <div class="input-group">
            <label class="input-label">登录密码</label>
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入6位数字密码"
              size="large"
              show-password
              maxlength="6"
              @input="loginForm.password = loginForm.password.replace(/\D/g, '')"
            >
              <template #prefix>
                <el-icon color="#1a3a6b"><Lock /></el-icon>
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
            {{ loading ? '安全验证中...' : '登 录' }}
          </el-button>
        </el-form>

        <div class="form-footer">
          <div class="form-links">
            <span class="link" @click="goRegister">注册新账户</span>
            <span class="divider">|</span>
            <span class="link" @click="goForgotPassword">忘记密码</span>
          </div>
          <div class="safe-tip">
            <el-icon :size="14" color="#52c41a"><SuccessFilled /></el-icon>
            <span>本次登录环境安全</span>
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
import { login } from '@/api/account'
import { Lock, Lightning, Service, SuccessFilled } from '@element-plus/icons-vue'

const router = useRouter()
const loginForm = ref({
  accountNumber: '',
  password: ''
})
const loading = ref(false)

const handleLogin = async () => {
  if (!loginForm.value.accountNumber || !loginForm.value.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const res = await login(loginForm.value)
    const { token, accountVO } = res.data
    localStorage.setItem('token', token)
    localStorage.setItem('userInfo', JSON.stringify(accountVO))
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (error) {
    console.error('登录失败', error)
  } finally {
    loading.value = false
  }
}

const goRegister = () => {
  router.push('/register')
}

const goForgotPassword = () => {
  router.push('/forgot-password')
}

const particleStyle = (i) => {
  const left = Math.random() * 100
  const delay = Math.random() * 8
  const duration = 6 + Math.random() * 8
  const size = 2 + Math.random() * 4
  return {
    left: `${left}%`,
    width: `${size}px`,
    height: `${size}px`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
  background: #0a1628;
}

/* ===== LEFT BRAND ===== */
.login-brand {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(ellipse at 20% 50%, rgba(26,58,107,0.6) 0%, transparent 60%),
    radial-gradient(ellipse at 80% 20%, rgba(245,175,25,0.08) 0%, transparent 50%),
    linear-gradient(180deg, #0c1a3b 0%, #0a1628 50%, #1a2a4a 100%);
  overflow: hidden;
}

.brand-overlay {
  position: absolute;
  inset: 0;
  background:
    repeating-linear-gradient(
      0deg,
      transparent,
      transparent 2px,
      rgba(255,255,255,0.015) 2px,
      rgba(255,255,255,0.015) 4px
    );
  pointer-events: none;
}

.brand-content {
  position: relative;
  z-index: 2;
  text-align: center;
  padding: 40px;
}

.brand-logo { margin-bottom: 48px; }

.brand-title {
  font-size: 42px;
  font-weight: 900;
  color: #fff;
  letter-spacing: 6px;
  margin-bottom: 8px;
  text-shadow: 0 2px 20px rgba(0,0,0,0.3);
}

.brand-subtitle {
  font-size: 14px;
  color: rgba(245,175,25,0.7);
  letter-spacing: 10px;
  font-weight: 500;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 24px;
  max-width: 320px;
  margin: 0 auto 48px;
  text-align: left;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: rgba(255,255,255,0.04);
  border-radius: 12px;
  border: 1px solid rgba(255,255,255,0.06);
  backdrop-filter: blur(10px);
  transition: all 0.3s;
}
.feature-item:hover {
  background: rgba(255,255,255,0.08);
  transform: translateX(4px);
}

.feature-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(245,175,25,0.1);
  border-radius: 12px;
  flex-shrink: 0;
}

.feature-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.feature-label {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
}
.feature-desc {
  font-size: 12px;
  color: rgba(255,255,255,0.5);
}

.brand-footer {
  text-align: center;
}
.brand-footer p {
  font-size: 12px;
  color: rgba(255,255,255,0.3);
  line-height: 1.6;
}

/* ===== PARTICLES ===== */
.particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 1;
}

.particle {
  position: absolute;
  bottom: -10px;
  background: rgba(245,175,25,0.4);
  border-radius: 50%;
  animation: floatUp linear infinite;
}

@keyframes floatUp {
  0% {
    transform: translateY(0) scale(1);
    opacity: 1;
  }
  100% {
    transform: translateY(-100vh) scale(0);
    opacity: 0;
  }
}

/* ===== RIGHT FORM ===== */
.login-form-wrap {
  width: 520px;
  min-width: 520px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: #fff;
}

.form-container {
  width: 100%;
  max-width: 400px;
}

.form-header {
  margin-bottom: 36px;
}

.form-welcome h2 {
  font-size: 28px;
  font-weight: 700;
  color: #0c1a3b;
  margin-bottom: 6px;
}

.form-welcome p {
  font-size: 14px;
  color: #999;
}

.input-group {
  margin-bottom: 22px;
}

.input-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
}

.login-form :deep(.el-input__wrapper) {
  height: 48px;
  border-radius: 10px !important;
  box-shadow: 0 0 0 1px #e0e4ea !important;
  padding: 0 12px;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #1a3a6b !important;
}

.login-form :deep(.el-input__inner) {
  font-size: 15px;
}

.login-form :deep(.el-input__prefix) {
  margin-right: 8px;
}

.login-btn {
  width: 100%;
  height: 50px;
  background: linear-gradient(135deg, #1a3a6b, #2d5a8c) !important;
  border: none;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 4px;
  border-radius: 10px;
  margin-top: 8px;
  transition: all 0.3s;
}

.login-btn:hover {
  background: linear-gradient(135deg, #2d5a8c, #3a6a9c) !important;
  transform: translateY(-1px);
  box-shadow: 0 8px 24px rgba(26, 58, 107, 0.3);
}

.form-footer {
  margin-top: 28px;
  text-align: center;
}

.form-links {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.link {
  font-size: 13px;
  color: #1a3a6b;
  cursor: pointer;
  transition: color 0.2s;
}
.link:hover { color: #f5af19; }

.divider { color: #d0d5de; font-size: 12px; }

.safe-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 12px;
  color: #999;
}

/* ===== RESPONSIVE ===== */
@media (max-width: 900px) {
  .login-page { flex-direction: column; }
  .login-brand { display: none; }
  .login-form-wrap {
    width: 100%;
    min-width: unset;
    padding: 60px 24px;
  }
}
</style>

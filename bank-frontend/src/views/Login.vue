<template>
  <div class="login-page">
    <!-- Left: Branding & Atmosphere -->
    <div class="login-brand">
      <div class="brand-overlay"></div>
      <div class="brand-vaultline"></div>
      <div class="brand-content">
        <div class="brand-logo">
          <div class="brand-mark"></div>
          <h1 class="brand-title">钱钱银行</h1>
          <p class="brand-subtitle">SMART BANKING</p>
        </div>
        <div class="brand-features">
          <div class="feature-item">
            <div class="feature-icon"><el-icon :size="22" color="var(--vault-accent)"><Lock /></el-icon></div>
            <div class="feature-text">
              <span class="feature-label">安全可靠</span>
              <span class="feature-desc">256位加密传输，保障资金安全</span>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon"><el-icon :size="22" color="var(--vault-accent)"><Lightning /></el-icon></div>
            <div class="feature-text">
              <span class="feature-label">极速到账</span>
              <span class="feature-desc">7×24小时实时转账，秒级到账</span>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon"><el-icon :size="22" color="var(--vault-accent)"><Service /></el-icon></div>
            <div class="feature-text">
              <span class="feature-label">智能服务</span>
              <span class="feature-desc">AI智能客服，全天候在线服务</span>
            </div>
          </div>
        </div>
        <div class="brand-footer">
          <p>全国服务热线：95588</p>
          <p>© 2026 钱钱银行 版权所有</p>
        </div>
      </div>
    </div>

    <!-- Right: Login Form -->
    <div class="login-form-wrap">
      <div class="form-container vault-panel">
        <div class="vault-contour"></div>
        <div class="form-header">
          <div class="form-status">
            <span class="vault-status-dot"></span>
            <span class="form-status-text">SECURE CHANNEL</span>
          </div>
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
                <el-icon color="var(--vault-accent)"><User /></el-icon>
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
                <el-icon color="var(--vault-accent)"><Lock /></el-icon>
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
            <span class="divider">|</span>
          </div>
          <div class="safe-tip">
            <el-icon :size="14" color="var(--vault-up)"><SuccessFilled /></el-icon>
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
</script>

<style scoped>
.login-page { display: flex; min-height: 100vh; background: var(--vault-bg); }

/* ===== LEFT BRAND ===== */
.login-brand {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(ellipse at 20% 50%, rgba(58,44,20,0.55) 0%, transparent 60%),
    radial-gradient(ellipse at 80% 20%, rgba(212,176,106,0.08) 0%, transparent 50%),
    linear-gradient(180deg, #0c1420 0%, #0a0e16 50%, #101a2c 100%);
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
      rgba(212,176,106,0.015) 2px,
      rgba(212,176,106,0.015) 4px
    );
  pointer-events: none;
}

.brand-vaultline {
  position: absolute;
  top: 0; bottom: 0; right: -1px;
  width: 1px;
  background: linear-gradient(180deg, transparent, var(--vault-line-hover) 30%, var(--vault-line-hover) 70%, transparent);
  opacity: 0.6;
  pointer-events: none;
}

.brand-content {
  position: relative;
  z-index: 2;
  text-align: center;
  padding: 40px;
}

.brand-logo { margin-bottom: 48px; }
.brand-mark {
  width: 72px; height: 72px;
  margin: 0 auto 22px;
  border-radius: 20px;
  background: linear-gradient(135deg, #e6c98a, #b08d4a);
  box-shadow: 0 0 40px var(--vault-accent-glow), inset 0 1px 0 rgba(255,255,255,0.35);
  display: flex; align-items: center; justify-content: center;
}
.brand-mark::after {
  content: '钱';
  font-size: 34px; font-weight: 900; color: #231a08;
  text-shadow: none;
}
.brand-title {
  font-size: 44px;
  font-weight: 900;
  color: var(--vault-text);
  letter-spacing: 8px;
  margin-bottom: 10px;
  background: linear-gradient(180deg, #ffffff 0%, var(--vault-gold) 130%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 2px 16px rgba(212,176,106,0.28));
}
.brand-subtitle {
  font-size: 14px;
  color: var(--vault-accent);
  letter-spacing: 10px;
  font-weight: 500;
  opacity: 0.85;
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
  background: rgba(212,176,106,0.03);
  border-radius: 12px;
  border: 1px solid var(--vault-line);
  transition: all 0.3s;
}
.feature-item:hover {
  background: rgba(212,176,106,0.06);
  border-color: var(--vault-line-hover);
  transform: translateX(4px);
}
.feature-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(212,176,106,0.1);
  border-radius: 12px;
  box-shadow: inset 0 0 0 1px rgba(255,255,255,0.08);
  flex-shrink: 0;
}
.feature-text { display: flex; flex-direction: column; gap: 2px; }
.feature-label { font-size: 15px; font-weight: 600; color: #f5ecd8; }
.feature-desc { font-size: 12px; color: var(--vault-text-3); }
.brand-footer { text-align: center; }
.brand-footer p { font-size: 12px; color: var(--vault-text-dim); line-height: 1.6; }

/* ===== RIGHT FORM ===== */
.login-form-wrap {
  width: 520px;
  min-width: 520px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: radial-gradient(800px 500px at 50% -20%, rgba(212,176,106,0.05), transparent 60%), var(--vault-graphite);
  border-left: 1px solid var(--vault-line);
}

.form-container {
  position: relative;
  width: 100%;
  max-width: 400px;
  padding: 36px;
}
.form-status { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; }
.form-status-text { font-size: 11px; color: var(--vault-text-3); letter-spacing: 0.2em; }
.form-header { margin-bottom: 28px; position: relative; }
.form-welcome h2 { font-size: 26px; font-weight: 700; color: var(--vault-text); letter-spacing: 0.1em; margin-bottom: 6px; }
.form-welcome p { font-size: 14px; color: var(--vault-text-3); }

.input-group { margin-bottom: 22px; }
.input-label { display: block; font-size: 13px; font-weight: 600; color: var(--vault-text-2); margin-bottom: 6px; letter-spacing: 0.05em; }

.login-form :deep(.el-input__wrapper) {
  height: 48px;
  border-radius: 10px !important;
  box-shadow: 0 0 0 1px var(--vault-line-strong) !important;
  padding: 0 12px;
  background: rgba(8,12,20,0.6);
}
.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--vault-accent), 0 0 14px rgba(212,176,106,0.2) !important;
}
.login-form :deep(.el-input__inner) { font-size: 15px; color: var(--vault-text); }
.login-form :deep(.el-input__prefix) { margin-right: 8px; }

.login-btn {
  width: 100%;
  height: 50px;
  background: linear-gradient(135deg, #b08d4a, #d4b06a) !important;
  border: none;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 4px;
  border-radius: 10px;
  margin-top: 8px;
  box-shadow: 0 0 22px rgba(212,176,106,0.2), inset 0 0 0 1px rgba(255,255,255,0.08);
  transition: all 0.3s;
}
.login-btn:hover {
  background: linear-gradient(135deg, #c9a458, #e6c98a) !important;
  transform: translateY(-1px);
  box-shadow: 0 8px 28px rgba(212,176,106,0.35);
}

.form-footer { margin-top: 28px; text-align: center; }
.form-links { display: flex; justify-content: center; align-items: center; gap: 12px; margin-bottom: 16px; }
.link { font-size: 13px; color: var(--vault-accent); cursor: pointer; transition: color 0.2s; }
.link:hover { color: #e6c98a; text-shadow: 0 0 10px rgba(212,176,106,0.5); }
.divider { color: var(--vault-line-strong); font-size: 12px; }
.safe-tip { display: flex; align-items: center; justify-content: center; gap: 6px; font-size: 12px; color: var(--vault-text-3); }

/* ===== RESPONSIVE ===== */
@media (max-width: 900px) {
  .login-page { flex-direction: column; }
  .login-brand { display: none; }
  .login-form-wrap {
    width: 100%;
    min-width: unset;
    padding: 40px 24px;
    border-left: none;
  }
}
</style>

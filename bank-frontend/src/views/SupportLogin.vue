<template>
  <div class="support-login">
    <div class="login-bg"></div>
    <div class="login-wrap">
      <div class="login-side">
        <div class="side-logo">
          <div class="logo-icon">
            <el-icon :size="30" color="var(--vault-accent)"><Headset /></el-icon>
          </div>
          <div class="logo-text">
            <div class="logo-title">钱钱银行</div>
            <div class="logo-sub">客服管理后台</div>
          </div>
        </div>
        <div class="side-slogan">
          <h2>客户服务 · 工单管理</h2>
          <p>为客户提供高效、专业、有温度的服务</p>
        </div>
        <ul class="side-features">
          <li><el-icon><Check /></el-icon>工单统一受理与流转</li>
          <li><el-icon><Check /></el-icon>服务进度全程可追踪</li>
          <li><el-icon><Check /></el-icon>客户问题及时响应</li>
        </ul>
        <div class="side-footer">Customer Service Platform</div>
      </div>

      <div class="login-form-box">
        <div class="form-title">
          <div class="form-status">
            <span class="vault-status-dot"></span>
            <span class="form-status-text">STAFF ACCESS</span>
          </div>
          <h3>{{ mode === 'login' ? '客服登录' : '注册客服账号' }}</h3>
          <p>{{ mode === 'login' ? '请使用客服账号登录工作台' : '注册后可进入客服工作台' }}</p>
        </div>
        <el-form :model="form" size="large" @keyup.enter="handleSubmit">
          <el-form-item>
            <el-input v-model="form.username" placeholder="客服账号" :prefix-icon="User" />
          </el-form-item>
          <el-form-item v-if="mode === 'register'">
            <el-input v-model="form.staffName" placeholder="客服姓名（选填，默认为账号）" :prefix-icon="Postcard" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password />
          </el-form-item>
          <el-form-item v-if="mode === 'register'">
            <el-input v-model="form.checkPassword" type="password" placeholder="确认密码" :prefix-icon="Lock" show-password />
          </el-form-item>
          <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="handleSubmit">
            {{ mode === 'login' ? '登 录' : '注 册' }}
          </el-button>
        </el-form>
        <div class="switch-link" @click="toggleMode">
          {{ mode === 'login' ? '没有账号？注册一个' : '已有账号？返回登录' }}
        </div>
        <div class="form-tip">如有疑问，请联系系统管理员</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { staffLogin, staffRegister } from '@/api/support'

const router = useRouter()
const mode = ref('login')
const form = ref({ username: '', password: '', checkPassword: '', staffName: '' })
const loading = ref(false)

const toggleMode = () => {
  mode.value = mode.value === 'login' ? 'register' : 'login'
  form.value.checkPassword = ''
}

const handleSubmit = async () => {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  if (mode.value === 'register' && !form.value.checkPassword) {
    ElMessage.warning('请输入确认密码')
    return
  }
  loading.value = true
  try {
    if (mode.value === 'login') {
      const res = await staffLogin({ username: form.value.username, password: form.value.password })
      localStorage.setItem('staff_token', res.data.token)
      localStorage.setItem('staffName', res.data.staffName)
      ElMessage.success('登录成功')
      router.push('/support')
    } else {
      await staffRegister({
        username: form.value.username,
        password: form.value.password,
        checkPassword: form.value.checkPassword,
        staffName: form.value.staffName
      })
      ElMessage.success('注册成功，请登录')
      mode.value = 'login'
      form.value.checkPassword = ''
      form.value.password = ''
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.support-login {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-background);
  overflow: hidden;
  font-family: var(--font-sans);
}

.login-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 15% 20%, rgba(139, 92, 246, 0.1), transparent 40%),
    radial-gradient(circle at 85% 80%, rgba(245, 158, 11, 0.08), transparent 45%),
    linear-gradient(135deg, #0F172A, #1a1f2e 55%, #0F172A);
}

.login-wrap {
  position: relative;
  width: 880px;
  max-width: 94vw;
  min-height: 520px;
  display: flex;
  background: var(--color-card);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-2xl);
  overflow: hidden;
  box-shadow: var(--shadow-lg);
}

.login-side {
  width: 46%;
  padding: var(--space-11) var(--space-9);
  display: flex;
  flex-direction: column;
  background: linear-gradient(160deg, #1a1f2e, #222735 70%, #1a1f2e);
  border-right: 1px solid var(--glass-border);
  color: var(--color-foreground);
}

.side-logo {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.logo-icon {
  width: var(--space-12);
  height: var(--space-12);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(245, 158, 11, 0.12);
}

.logo-icon svg {
  width: 28px;
  height: 28px;
  color: var(--color-primary);
}

.logo-title {
  font-size: var(--font-h3);
  font-weight: 700;
  letter-spacing: 2px;
  color: var(--color-foreground);
}

.logo-sub {
  font-size: var(--font-sm);
  color: var(--color-muted-foreground);
  letter-spacing: var(--space-4);
  margin-top: var(--space-1);
}

.side-slogan {
  margin-top: var(--space-11);
}

.side-slogan h2 {
  font-size: var(--font-title);
  margin: 0 0 var(--space-3);
  letter-spacing: 1px;
}

.side-slogan p {
  font-size: var(--font-body);
  color: var(--color-muted-foreground);
  margin: 0;
}

.side-features {
  list-style: none;
  margin: var(--space-11) 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.side-features li {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  font-size: var(--font-body);
  color: var(--color-muted-foreground);
}

.side-features li svg {
  width: 18px;
  height: 18px;
  color: var(--color-success);
}

.side-footer {
  margin-top: auto;
  font-size: 11px;
  color: var(--color-muted-foreground);
  letter-spacing: 1px;
  opacity: 0.6;
}

.login-form-box {
  flex: 1;
  padding: var(--space-16) var(--space-11);
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-status {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
}

.vault-status-dot {
  width: 8px;
  height: 8px;
  background: var(--color-success);
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.form-status-text {
  font-size: 11px;
  color: var(--color-muted-foreground);
  letter-spacing: 0.2em;
}

.form-title h3 {
  margin: 0 0 var(--space-1) var(--space-1);
  font-size: var(--font-title);
  color: var(--color-foreground);
  letter-spacing: 0.05em;
}

.form-title p {
  margin: 0 0 var(--space-9);
  font-size: var(--font-body);
  color: var(--color-muted-foreground);
}

.login-form-box :deep(.el-input__wrapper) {
  height: var(--space-12);
  border-radius: var(--radius-md) !important;
  box-shadow: 0 0 0 1px var(--color-border) !important;
  background: var(--color-muted);
  transition: all var(--transition-fast) ease;
}

.login-form-box :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px var(--color-primary), 0 0 16px rgba(245, 158, 11, 0.15) !important;
}

.login-form-box :deep(.el-input__inner) {
  color: var(--color-foreground);
}

.login-form-box :deep(.el-input__inner::placeholder) {
  color: var(--color-muted-foreground);
}

.login-form-box :deep(.el-button--primary) {
  width: 100%;
  height: var(--space-12);
  background: var(--color-primary);
  border: none;
  color: var(--color-on-primary);
  font-size: var(--font-base);
  font-weight: 600;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast) ease;
}

.login-form-box :deep(.el-button--primary:hover) {
  background: var(--color-secondary);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(245, 158, 11, 0.35);
}

.login-form-box :deep(.el-button--primary:active) {
  transform: translateY(0);
}

.switch-link {
  margin-top: var(--space-6);
  text-align: center;
  font-size: var(--font-body);
  color: var(--color-primary);
  cursor: pointer;
  transition: color var(--transition-fast) ease;
}

.switch-link:hover {
  color: var(--color-secondary);
}

.form-tip {
  margin-top: var(--space-5);
  text-align: center;
  font-size: 12px;
  color: var(--color-muted-foreground);
  opacity: 0.7;
}

@media (max-width: 760px) {
  .login-side { display: none; }
}
</style>
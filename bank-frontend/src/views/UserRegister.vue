<template>
  <div class="register-page">
      <div class="register-container">
        <div class="vault-contour"></div>
        <div class="register-header">
          <router-link to="/user/login" class="back-link">
            <el-icon><ArrowLeft /></el-icon>
            <span>返回登录</span>
          </router-link>
          <div class="header-brand">
            <div class="header-logo">
              <el-icon :size="28" color="var(--vault-accent)"><User /></el-icon>
              <span>钱钱银行</span>
            </div>
            <h2>注册新用户</h2>
            <p>注册后可绑定和管理多张银行卡</p>
          </div>
        </div>
      <div class="step-content">
        <el-form :model="form" label-width="0" class="register-form">
          <div class="form-row">
            <label class="input-label">用户名</label>
            <el-input v-model="form.userName" placeholder="请输入用户名" size="large" clearable maxlength="20" />
          </div>
          <div class="form-row">
            <label class="input-label">手机号</label>
            <el-input v-model="form.userPhone" placeholder="请输入11位手机号" size="large" clearable maxlength="11" />
          </div>
          <div class="form-row">
            <label class="input-label">身份证号</label>
            <el-input v-model="form.userIdCard" placeholder="请输入身份证号" size="large" clearable maxlength="18" />
          </div>
          <div class="form-row">
            <label class="input-label">密码</label>
            <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" show-password />
          </div>
          <div class="form-row">
            <label class="input-label">确认密码</label>
            <el-input v-model="form.checkPassword" type="password" placeholder="请再次输入密码" size="large" show-password />
          </div>
        </el-form>
        <div class="step-actions">
          <el-button size="large" :loading="loading" @click="handleRegister" class="submit-btn">
            {{ loading ? '注册中...' : '注册' }}
          </el-button>
        </div>
        <div class="form-footer">
          <span>已有账号？</span>
          <router-link to="/user/login" class="link">去登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userRegister } from '@/api/user'

const router = useRouter()
const form = ref({ userName: '', userPhone: '', userIdCard: '', password: '', checkPassword: '' })
const loading = ref(false)

const handleRegister = async () => {
  const f = form.value
  if (!f.userName || !f.userPhone || !f.password || !f.checkPassword || !f.userIdCard) {
    ElMessage.warning('请填写完整')
    return
  }
  if (f.password !== f.checkPassword) {
    ElMessage.warning('两次密码不一致')
    return
  }
  loading.value = true
  try {
    await userRegister(f)
    ElMessage.success('注册成功，请登录')
    router.push('/user/login')
  } catch (e) {
    console.error(e)
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
    radial-gradient(1200px 600px at 30% 20%, rgba(139, 92, 246, 0.06) 0%, transparent 60%),
    var(--color-background);
  padding: 40px 20px;
  font-family: var(--font-sans);
}

.register-container {
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

.register-header {
  padding: var(--space-8) var(--space-9) var(--space-7);
  border-bottom: 1px solid var(--color-border);
  position: relative;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: var(--font-sm);
  color: var(--color-muted-foreground);
  text-decoration: none;
  margin-bottom: var(--space-5);
  cursor: pointer;
  transition: color var(--transition-fast) ease;
}

.back-link:hover {
  color: var(--color-primary);
}

.back-link svg {
  width: 16px;
  height: 16px;
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
  font-size: var(--font-h3);
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

.step-content {
  padding: var(--space-7) var(--space-9) var(--space-9);
}

.register-form {
  max-width: 360px;
  margin: 0 auto;
}

.form-row {
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

.register-form :deep(.el-input__wrapper) {
  height: var(--space-12);
  border-radius: var(--radius-md) !important;
  box-shadow: 0 0 0 1px var(--color-border) !important;
  background: var(--color-muted);
  transition: all var(--transition-fast) ease;
}

.register-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px var(--color-primary), 0 0 16px rgba(245, 158, 11, 0.15) !important;
}

.register-form :deep(.el-input__inner) {
  color: var(--color-foreground);
}

.register-form :deep(.el-input__inner::placeholder) {
  color: var(--color-muted-foreground);
}

.step-actions {
  max-width: 360px;
  margin: var(--space-7) auto 0;
}

.submit-btn {
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

.submit-btn:hover {
  background: var(--color-secondary);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(245, 158, 11, 0.35);
}

.submit-btn:active {
  transform: translateY(0);
}

.form-footer {
  text-align: center;
  margin-top: var(--space-6);
  font-size: var(--font-body);
  color: var(--color-muted-foreground);
}

.link {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: 600;
  cursor: pointer;
  transition: color var(--transition-fast) ease;
}

.link:hover {
  color: var(--color-secondary);
}

@media (max-width: 480px) {
  .register-header { padding: var(--space-6) var(--space-5) var(--space-5); }
  .step-content { padding: var(--space-5); }
}
</style>
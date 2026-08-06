<template>
  <div class="register-page">
    <div class="register-container">
      <div class="register-header">
        <router-link to="/user/login" class="back-link">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回登录</span>
        </router-link>
        <div class="header-brand">
          <div class="header-logo">
            <el-icon :size="28" color="#f5af19"><User /></el-icon>
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
.register-page { min-height: 100vh; display: flex; justify-content: center; align-items: center; background: radial-gradient(ellipse at 30% 20%, rgba(26,58,107,0.4) 0%, transparent 60%), radial-gradient(ellipse at 70% 80%, rgba(245,175,25,0.05) 0%, transparent 50%), #0a1628; padding: 40px 20px; }
.register-container { width: 480px; max-width: 100%; background: #fff; border-radius: 24px; box-shadow: 0 32px 80px rgba(0,0,0,0.4); overflow: hidden; animation: fadeInUp 0.5s ease-out; }
@keyframes fadeInUp { from { opacity: 0; transform: translateY(30px); } to { opacity: 1; transform: translateY(0); } }
.register-header { padding: 28px 32px 20px; border-bottom: 1px solid #f0f2f5; }
.back-link { display: inline-flex; align-items: center; gap: 4px; font-size: 13px; color: #999; text-decoration: none; margin-bottom: 16px; transition: color 0.2s; }
.back-link:hover { color: #1a3a6b; }
.header-brand { text-align: center; }
.header-logo { display: flex; align-items: center; justify-content: center; gap: 8px; margin-bottom: 16px; }
.header-logo span { font-size: 20px; font-weight: 700; color: #1a3a6b; letter-spacing: 2px; }
.header-brand h2 { font-size: 22px; font-weight: 700; color: #0c1a3b; margin-bottom: 4px; }
.header-brand p { font-size: 13px; color: #999; }
.step-content { padding: 24px 32px 32px; }
.register-form { max-width: 360px; margin: 0 auto; }
.form-row { margin-bottom: 20px; }
.input-label { display: block; font-size: 13px; font-weight: 600; color: #333; margin-bottom: 6px; }
.register-form :deep(.el-input__wrapper) { height: 46px; border-radius: 10px !important; }
.step-actions { max-width: 360px; margin: 24px auto 0; }
.submit-btn { width: 100%; height: 48px; background: linear-gradient(135deg, #1a3a6b, #2d5a8c); border: none; color: #fff; font-size: 16px; font-weight: 600; border-radius: 10px; }
.submit-btn:hover { background: linear-gradient(135deg, #2d5a8c, #3a6a9c); }
.form-footer { text-align: center; margin-top: 16px; font-size: 13px; color: #999; }
.link { color: #1a3a6b; text-decoration: none; font-weight: 600; }
.link:hover { color: #f5af19; }
</style>

<template>
  <div class="support-login">
    <div class="login-bg"></div>
    <div class="login-wrap">
      <div class="login-side">
        <div class="side-logo">
          <div class="logo-icon">
            <el-icon :size="30" color="#f5af19"><Headset /></el-icon>
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
  display: flex; align-items: center; justify-content: center;
  background: #0c1a3b;
  overflow: hidden;
}
.login-bg {
  position: absolute; inset: 0;
  background:
    radial-gradient(circle at 15% 20%, rgba(245, 175, 25, 0.18), transparent 40%),
    radial-gradient(circle at 85% 80%, rgba(64, 158, 255, 0.22), transparent 45%),
    linear-gradient(135deg, #0c1a3b, #16325c 55%, #1d4272);
}
.login-wrap {
  position: relative;
  width: 880px; max-width: 94vw;
  min-height: 520px;
  display: flex;
  background: #fff;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.45);
}
.login-side {
  width: 46%;
  padding: 44px 38px;
  display: flex; flex-direction: column;
  background: linear-gradient(160deg, #0c1a3b, #1a3a6b 70%, #20508c);
  color: #fff;
}
.side-logo { display: flex; align-items: center; gap: 14px; }
.logo-icon {
  width: 46px; height: 46px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  background: rgba(245, 175, 25, 0.15);
  border: 1px solid rgba(245, 175, 25, 0.4);
}
.logo-title { font-size: 22px; font-weight: 700; letter-spacing: 2px; }
.logo-sub { font-size: 12px; color: rgba(255, 255, 255, 0.6); letter-spacing: 4px; margin-top: 2px; }
.side-slogan { margin-top: 72px; }
.side-slogan h2 { font-size: 26px; margin: 0 0 10px; letter-spacing: 1px; }
.side-slogan p { font-size: 14px; color: rgba(255, 255, 255, 0.65); margin: 0; }
.side-features {
  list-style: none; margin: 44px 0 0; padding: 0;
  display: flex; flex-direction: column; gap: 18px;
}
.side-features li { display: flex; align-items: center; gap: 10px; font-size: 14px; color: rgba(255, 255, 255, 0.85); }
.side-features li .el-icon { color: #f5af19; }
.side-footer { margin-top: auto; font-size: 11px; color: rgba(255, 255, 255, 0.35); letter-spacing: 1px; }
.login-form-box { flex: 1; padding: 64px 56px; display: flex; flex-direction: column; justify-content: center; }
.form-title h3 { margin: 0 0 6px; font-size: 24px; color: #1a3a6b; }
.form-title p { margin: 0 0 34px; font-size: 13px; color: #999; }
.form-tip { margin-top: 22px; text-align: center; font-size: 12px; color: #bbb; }
.switch-link { margin-top: 16px; text-align: center; font-size: 13px; color: #409eff; cursor: pointer; }
.switch-link:hover { text-decoration: underline; }
@media (max-width: 760px) {
  .login-side { display: none; }
}
</style>

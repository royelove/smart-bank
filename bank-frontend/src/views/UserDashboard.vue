<template>
  <div class="dashboard">
    <header class="dashboard-header">
      <div class="header-left">
        <div class="brand-logo-mini">
          <el-icon :size="24" color="#f5af19"><UserFilled /></el-icon>
          <span class="brand-text">钱钱银行 · 用户中心</span>
        </div>
        <span class="header-divider"></span>
        <span class="header-welcome">欢迎回来，{{ user?.userName || '用户' }}</span>
      </div>
      <div class="header-right">
        <div class="header-time">
          <el-icon><Clock /></el-icon>
          <span>{{ currentTime }}</span>
        </div>
        <div class="user-dropdown">
          <el-dropdown trigger="click">
            <span class="user-trigger">
              <div class="user-avatar">
                {{ (user?.userName || '用')[0] }}
              </div>
              <span class="user-name">{{ user?.userName || '用户' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="showProfile = true">
                  <el-icon><User /></el-icon> 个人信息
                </el-dropdown-item>
                <el-dropdown-item @click="showChangePwd = true">
                  <el-icon><Key /></el-icon> 修改密码
                </el-dropdown-item>
                <el-dropdown-item divided @click="logout">
                  <el-icon><SwitchButton /></el-icon> 安全退出
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <div class="dashboard-body">
      <main class="dashboard-main">
        <div class="profile-card">
          <div class="profile-card-bg"></div>
          <div class="profile-content">
            <div class="profile-top">
              <div class="profile-type">
                <el-icon><UserFilled /></el-icon>
                <span>用户资料</span>
              </div>
              <div class="profile-status">
                <span class="status-dot"></span>
                已认证
              </div>
            </div>
            <div class="profile-identity">
              <div class="identity-avatar">{{ (user?.userName || '用')[0] }}</div>
              <div class="identity-info">
                <span class="identity-name">{{ user?.userName || '--' }}</span>
                <span class="identity-sub">{{ user?.userPhone || '--' }}</span>
              </div>
            </div>
            <div class="profile-bottom">
              <div class="profile-item">手机号：{{ user?.userPhone || '--' }}</div>
              <div class="profile-item">身份证号：{{ maskIdCard(user?.userIdCard) }}</div>
            </div>
          </div>
          <div class="profile-card-chip"></div>
        </div>

        <div class="quick-actions">
          <div class="section-header">
            <h3>快捷操作</h3>
            <span class="section-sub">管理你的银行卡</span>
          </div>
          <div class="actions-grid">
            <div class="action-card" @click="showRegister = true">
              <div class="action-icon register-icon">
                <el-icon :size="24" color="#fff"><CreditCard /></el-icon>
              </div>
              <span class="action-label">注册新卡</span>
            </div>
            <div class="action-card" @click="showAdd = true">
              <div class="action-icon add-icon">
                <el-icon :size="24" color="#fff"><Plus /></el-icon>
              </div>
              <span class="action-label">添加银行卡</span>
            </div>
            <div class="action-card" @click="showSupport = true">
              <div class="action-icon support-icon">
                <el-icon :size="24" color="#fff"><Headset /></el-icon>
              </div>
              <span class="action-label">客服/投诉</span>
            </div>
            <div class="action-card" @click="showFace = true">
              <div class="action-icon face-icon">
                <el-icon :size="24" color="#fff"><CameraFilled /></el-icon>
              </div>
              <span class="action-label">实名认证</span>
            </div>
          </div>
        </div>

        <div class="accounts-card">
          <div class="section-header">
            <h3>我的银行卡 ({{ accounts.length }})</h3>
            <span class="section-sub">点击行进入交易</span>
          </div>
          <el-table :data="accounts" v-loading="loading" stripe style="width:100%" @row-click="goTrade" highlight-current-row>
            <el-table-column prop="accountNumber" label="卡号" width="160" />
            <el-table-column prop="accountHolderName" label="户名" width="100" />
            <el-table-column label="身份证" width="160">
              <template #default="{ row }">{{ maskIdCard(row.userIdCard) }}</template>
            </el-table-column>
            <el-table-column prop="accountType" label="类型" width="80" />
            <el-table-column prop="accountBalance" label="余额" width="120">
              <template #default="{ row }">¥ {{ Number(row.accountBalance).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="accountStatus" label="状态" width="80">
              <template #default="{ row }">{{ row.accountStatus === 1 ? '正常' : '冻结' }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loading && accounts.length === 0" description="暂无绑定银行卡" />
        </div>
      </main>
    </div>

    <el-dialog v-model="showProfile" title="个人信息" width="480px" destroy-on-close>
      <div v-if="user" class="profile-grid">
        <div><label>用户名：</label>{{ user.userName || '--' }}</div>
        <div><label>手机号：</label>{{ user.userPhone }}</div>
        <div><label>身份证：</label>{{ user.userIdCard }}</div>
      </div>
    </el-dialog>

    <el-dialog v-model="showChangePwd" title="修改密码" width="440px" destroy-on-close>
      <el-form :model="changePwdForm" label-width="100px">
        <el-form-item label="原密码">
          <el-input v-model="changePwdForm.oldPassword" type="password" placeholder="请输入原密码" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="changePwdForm.newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="changePwdForm.confirmPassword" type="password" placeholder="再次输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showChangePwd = false">取消</el-button>
        <el-button type="primary" :loading="changing" @click="handleChangePwd">确认修改</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showAdd" title="添加银行卡" width="420px" destroy-on-close>
      <el-form :model="addForm" label-width="100px">
        <el-form-item label="身份证号">
          <el-input v-model="addForm.userIdCard" placeholder="请输入该卡对应的身份证号" />
        </el-form-item>
        <el-form-item label="银行卡号">
          <el-input v-model="addForm.accountNumber" placeholder="请输入银行卡号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" :loading="adding" @click="handleAdd">确认添加</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showRegister" title="注册新银行卡" width="480px" destroy-on-close>
      <el-form :model="regForm" label-width="100px">
        <el-form-item label="账号">
          <el-input v-model="regForm.accountNumber" placeholder="6-20位数字" maxlength="20" />
        </el-form-item>
        <el-form-item label="户名">
          <el-input v-model="regForm.accountHolderName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="regForm.userPhoneNumber" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="regForm.userIdCard" placeholder="请输入18位身份证号" maxlength="18" />
        </el-form-item>
        <el-form-item label="交易密码">
          <el-input v-model="regForm.password" type="password" placeholder="6位数字" maxlength="6" show-password />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="regForm.checkPassword" type="password" placeholder="再次输入交易密码" maxlength="6" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRegister = false">取消</el-button>
        <el-button type="primary" :loading="registering" @click="handleRegister">确认注册</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showSupport" title="客服中心" width="640px" destroy-on-close>
      <SupportCenter :account-number="accounts[0]?.accountNumber || ''" />
    </el-dialog>

    <el-dialog v-model="showFace" title="人脸实名认证" width="480px" destroy-on-close>
      <div class="face-dialog">
        <div class="face-status">
          <el-tag :type="user?.faceBound === 1 ? 'success' : 'info'">
            {{ user?.faceBound === 1 ? '已绑定人脸' : '未绑定人脸' }}
          </el-tag>
          <span class="face-tip">人脸特征仅用于本人实名认证</span>
        </div>
        <el-upload
          drag
          accept="image/*"
          :auto-upload="false"
          :show-file-list="false"
          :on-change="onFaceSelect"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">点击或拖拽上传人脸照片</div>
          <template #tip>
            <div class="el-upload__tip">请正对镜头、光线充足、免冠照片</div>
          </template>
        </el-upload>
        <el-image v-if="facePreview" :src="facePreview" fit="contain" class="face-preview" />
        <div class="face-actions">
          <el-button type="primary" :loading="binding" :disabled="!faceImage" @click="handleBind">绑定人脸</el-button>
          <el-button type="success" :loading="verifying" :disabled="!faceImage || user?.faceBound !== 1" @click="handleVerify">人脸验证</el-button>
        </div>
        <div v-if="verifyResult !== null" class="face-result">
          <span :class="verifyResult.pass ? 'pass' : 'fail'">
            验证{{ verifyResult.pass ? '通过' : '未通过' }}，相似度 {{ verifyResult.score.toFixed(4) }}
          </span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserAccounts, addAccount, getUserInfo, updateUserPassword, bindFace, verifyFace } from '@/api/user'
import { register as registerAccount } from '@/api/account'
import SupportCenter from '@/components/SupportCenter.vue'

const router = useRouter()
const user = ref(null)
const accounts = ref([])
const loading = ref(false)
const showAdd = ref(false)
const adding = ref(false)
const addForm = ref({ userIdCard: '', accountNumber: '' })
const showRegister = ref(false)
const registering = ref(false)
const regForm = ref({ accountNumber: '', accountHolderName: '', userPhoneNumber: '', userIdCard: '', password: '', checkPassword: '' })
const showProfile = ref(false)
const showChangePwd = ref(false)
const changing = ref(false)
const changePwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const showSupport = ref(false)
const showFace = ref(false)
const faceImage = ref('')
const facePreview = ref('')
const binding = ref(false)
const verifying = ref(false)
const verifyResult = ref(null)
const currentTime = ref('')
let timer = null

const fetchData = async () => {
  loading.value = true
  try {
    const [infoRes, accRes] = await Promise.all([getUserInfo(), getUserAccounts()])
    user.value = infoRes.data
    accounts.value = accRes.data
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const goTrade = (row) => {
  router.push(`/trade/${row.accountNumber}`)
}

const maskIdCard = (id) => {
  if (!id || id.length < 10) return id || '--'
  return id.substring(0, 6) + '********' + id.substring(14)
}

const onFaceSelect = (file) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    faceImage.value = e.target.result.split(',')[1]
    facePreview.value = e.target.result
    verifyResult.value = null
  }
  reader.readAsDataURL(file.raw)
}

const handleBind = async () => {
  binding.value = true
  try {
    await bindFace({ image: faceImage.value })
    ElMessage.success('人脸绑定成功')
    await fetchData()
    verifyResult.value = null
  } catch (e) {
    console.error(e)
  } finally {
    binding.value = false
  }
}

const handleVerify = async () => {
  verifying.value = true
  try {
    const res = await verifyFace({ image: faceImage.value })
    verifyResult.value = res.data
    if (res.data.pass) ElMessage.success('验证通过')
  } catch (e) {
    console.error(e)
  } finally {
    verifying.value = false
  }
}

const handleAdd = async () => {
  const f = addForm.value
  if (!f.userIdCard || !f.accountNumber) {
    ElMessage.warning('请填写完整')
    return
  }
  adding.value = true
  try {
    await addAccount(f)
    ElMessage.success('添加成功')
    showAdd.value = false
    addForm.value = { userIdCard: '', accountNumber: '' }
    await fetchData()
  } catch (e) {
    console.error(e)
  } finally {
    adding.value = false
  }
}

const handleRegister = async () => {
  const f = regForm.value
  if (!f.accountNumber || !f.accountHolderName || !f.userPhoneNumber || !f.userIdCard || !f.password || !f.checkPassword) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (f.password !== f.checkPassword) {
    ElMessage.warning('两次密码不一致')
    return
  }
  registering.value = true
  try {
    await registerAccount({ ...f, accountType: '储蓄' })
    ElMessage.success('注册成功')
    showRegister.value = false
    regForm.value = { accountNumber: '', accountHolderName: '', userPhoneNumber: '', userIdCard: '', password: '', checkPassword: '' }
    await fetchData()
  } catch (e) {
    console.error(e)
  } finally {
    registering.value = false
  }
}

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', { hour12: false })
}

const handleChangePwd = async () => {
  const f = changePwdForm.value
  if (!f.oldPassword || !f.newPassword || !f.confirmPassword) {
    ElMessage.warning('请填写完整')
    return
  }
  if (f.newPassword !== f.confirmPassword) {
    ElMessage.warning('两次密码不一致')
    return
  }
  changing.value = true
  try {
    await updateUserPassword({
      oldPassword: f.oldPassword,
      newPassword: f.newPassword,
      confirmPassword: f.confirmPassword
    })
    ElMessage.success('密码修改成功')
    showChangePwd.value = false
    changePwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch (e) {
    console.error(e)
  } finally {
    changing.value = false
  }
}

const logout = () => {
  localStorage.removeItem('token')
  ElMessage.success('已安全退出')
  router.push('/user/login')
}

onMounted(() => {
  fetchData()
  updateTime()
  timer = setInterval(updateTime, 1000)
})
onUnmounted(() => { clearInterval(timer) })
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f0f2f5;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 60px;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.brand-logo-mini {
  display: flex;
  align-items: center;
  gap: 8px;
}

.brand-text {
  font-size: 18px;
  font-weight: 700;
  color: #1a3a6b;
  letter-spacing: 2px;
}

.header-divider {
  width: 1px;
  height: 24px;
  background: #e0e4ea;
}

.header-welcome {
  font-size: 14px;
  color: #666;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.header-time {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #999;
  font-weight: 500;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s;
}
.user-trigger:hover { background: #f5f7fa; }

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1a3a6b, #f5af19);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.dashboard-body {
  flex: 1;
  overflow: hidden;
}

.dashboard-main {
  height: 100%;
  padding: 20px 24px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-card {
  position: relative;
  background: linear-gradient(135deg, #0c1a3b 0%, #1a3a6b 40%, #2d5a8c 100%);
  border-radius: 16px;
  padding: 28px 32px;
  color: #fff;
  overflow: hidden;
  min-height: 170px;
}

.profile-card-bg {
  position: absolute;
  top: -50%;
  right: -20%;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(245,175,25,0.08) 0%, transparent 70%);
  pointer-events: none;
}

.profile-content { position: relative; z-index: 2; }

.profile-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.profile-type {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  opacity: 0.8;
}

.profile-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #52c41a;
  background: rgba(82, 196, 26, 0.15);
  padding: 4px 12px;
  border-radius: 20px;
}

.status-dot {
  width: 6px;
  height: 6px;
  background: #52c41a;
  border-radius: 50%;
  animation: blink 2s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.profile-identity {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.identity-avatar {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: linear-gradient(135deg, #f5af19, #e6a23c);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
}

.identity-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.identity-name {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 1px;
}

.identity-sub {
  font-size: 13px;
  opacity: 0.7;
}

.profile-bottom {
  display: flex;
  gap: 32px;
  font-size: 12px;
  opacity: 0.7;
}

.profile-card-chip {
  position: absolute;
  top: 20px;
  right: 28px;
  width: 50px;
  height: 36px;
  background: linear-gradient(135deg, #f5af19, #e6a23c);
  border-radius: 6px;
  opacity: 0.6;
  z-index: 1;
}

.quick-actions,
.accounts-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}

.section-header {
  margin-bottom: 16px;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}
.section-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0;
}
.section-sub { font-size: 12px; color: #999; }

.actions-grid {
  display: flex;
  gap: 12px;
}

.action-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 24px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #f0f2f5;
  background: #fafafa;
  min-width: 120px;
}
.action-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
  border-color: #d0d5de;
}

.action-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.register-icon { background: linear-gradient(135deg, #1a3a6b, #2d5a8c); }
.add-icon { background: linear-gradient(135deg, #67c23a, #85ce61); }
.support-icon { background: linear-gradient(135deg, #722ed1, #9254de); }
.face-icon { background: linear-gradient(135deg, #0f9b8e, #13c2c2); }

.face-dialog {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.face-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.face-tip { font-size: 12px; color: #999; }
.face-preview {
  width: 100%;
  max-height: 220px;
  border-radius: 10px;
  border: 1px solid #f0f2f5;
  background: #fafafa;
}
.face-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
.face-result {
  text-align: center;
  font-size: 14px;
  font-weight: 600;
}
.face-result .pass { color: #67c23a; }
.face-result .fail { color: #f56c6c; }

.action-label {
  font-size: 13px;
  font-weight: 600;
  color: #333;
}

:deep(.el-table__row) { cursor: pointer; }

.profile-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
  font-size: 14px;
}
.profile-grid label { color: #999; margin-right: 8px; }

@media (max-width: 768px) {
  .actions-grid { flex-wrap: wrap; }
  .action-card { min-width: 80px; flex: 1; }
  .profile-bottom { flex-direction: column; gap: 4px; }
  .header-welcome { display: none; }
  .header-time { display: none; }
}
</style>

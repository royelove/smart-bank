<template>
  <div class="dashboard">
    <!-- ============ 顶栏 · 全局导航 ============ -->
    <header class="top-nav">
      <div class="nav-left">
        <div class="brand">
          <div class="brand-emblem">钱</div>
          <div class="brand-text">
            <span class="brand-name">钱钱银行</span>
            <span class="brand-en">USER CENTER</span>
          </div>
        </div>
        <span class="header-divider"></span>
        <span class="header-welcome">欢迎回来，{{ user?.userName || '用户' }}</span>
      </div>

      <div class="nav-right">
        <div class="server-status">
          <span class="vault-status-dot"></span>
          <span class="server-text">服务在线</span>
        </div>
        <div class="header-time">
          <el-icon :size="15"><Clock /></el-icon>
          <span>{{ currentTime }}</span>
        </div>
        <el-dropdown trigger="click">
          <span class="user-trigger">
            <div class="user-avatar">{{ (user?.userName || '用')[0] }}</div>
            <span class="user-name">{{ user?.userName || '用户' }}</span>
            <el-icon :size="12"><ArrowDown /></el-icon>
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
    </header>

    <!-- ============ 会员身份面板 ============ -->
    <section class="hero">
      <div class="vault-contour"></div>
      <div class="hero-glow"></div>
      <div class="hero-inner">
        <div class="hero-top">
          <div class="hero-welcome">
            <span class="hero-greet">私人银行 · 尊享会员</span>
            <span class="hero-acct">身份认证·账户管理</span>
          </div>
          <div class="hero-quick">
            <button class="q-btn q-primary" @click="showRegister = true">
              <el-icon :size="16"><CreditCard /></el-icon><span>注册新卡</span>
            </button>
            <button class="q-btn" @click="showAdd = true">
              <el-icon :size="16"><Plus /></el-icon><span>添加银行卡</span>
            </button>
            <button class="q-btn" @click="showFace = true">
              <el-icon :size="16"><CameraFilled /></el-icon><span>实名认证</span>
            </button>
          </div>
        </div>

        <div class="hero-identity">
          <div class="identity-avatar">{{ (user?.userName || '用')[0] }}</div>
          <div class="identity-main">
            <div class="identity-name">{{ user?.userName || '--' }}</div>
            <div class="identity-sub">{{ user?.userPhone || '--' }}</div>
          </div>
          <div class="verify-badge" :class="realnameBound ? 'verified' : 'unverified'">
            <span class="status-dot"></span>
            {{ realnameBound ? '实名已验证' : '实名未验证' }}
          </div>
        </div>

        <div class="hero-meta">
          <div class="meta-item">
            <span class="meta-label">手机号</span>
            <span class="meta-value vault-num">{{ user?.userPhone || '--' }}</span>
          </div>
          <div class="meta-divider"></div>
          <div class="meta-item">
            <span class="meta-label">身份证号</span>
            <span class="meta-value vault-num">{{ user?.idCardMask || maskIdCard(user?.userIdCard) || '--' }}</span>
          </div>
          <div class="meta-divider"></div>
          <div class="meta-item">
            <span class="meta-label">绑定银行卡</span>
            <span class="meta-value">{{ accounts.length }} 张</span>
          </div>
        </div>
      </div>
    </section>

    <!-- ============ 内容区 ============ -->
    <main class="content">
      <section class="services">
        <div class="section-head">
          <div class="section-title">
            <h2>快捷服务</h2>
            <span class="section-en">QUICK SERVICES</span>
          </div>
          <span class="section-sub">管理你的银行卡与身份认证</span>
        </div>

        <div class="service-grid">
          <div class="service-card" @click="showRegister = true">
            <div class="s-icon gold"><el-icon :size="24"><CreditCard /></el-icon></div>
            <div class="s-name">注册新卡</div>
            <div class="s-desc">一键开户，即刻拥有</div>
            <div class="s-go"><el-icon :size="14"><Right /></el-icon></div>
          </div>
          <div class="service-card" @click="showAdd = true">
            <div class="s-icon blue"><el-icon :size="24"><Plus /></el-icon></div>
            <div class="s-name">添加银行卡</div>
            <div class="s-desc">绑定既有账户，统一管理</div>
            <div class="s-go"><el-icon :size="14"><Right /></el-icon></div>
          </div>
          <div class="service-card" @click="showSupport = true">
            <div class="s-icon violet"><el-icon :size="24"><Headset /></el-icon></div>
            <div class="s-name">客服中心</div>
            <div class="s-desc">专属服务，排忧解难</div>
            <div class="s-go"><el-icon :size="14"><Right /></el-icon></div>
          </div>
          <div class="service-card" @click="showFace = true">
            <div class="s-icon cyan"><el-icon :size="24"><CameraFilled /></el-icon></div>
            <div class="s-name">实名认证</div>
            <div class="s-desc">人脸核身，安全保障</div>
            <div class="s-go"><el-icon :size="14"><Right /></el-icon></div>
          </div>
        </div>
      </section>

      <section class="accounts-card">
        <div class="section-head">
          <div class="section-title">
            <h2>我的银行卡</h2>
            <span class="section-en">MY ACCOUNTS</span>
          </div>
          <span class="section-sub">点击行进入交易 · 共 {{ accounts.length }} 张</span>
        </div>
        <el-table :data="accounts" v-loading="loading" stripe style="width:100%" @row-click="goTrade" highlight-current-row>
          <el-table-column prop="accountNumber" label="卡号" width="180">
            <template #default="{ row }"><span class="vault-num">{{ row.accountNumber }}</span></template>
          </el-table-column>
          <el-table-column prop="accountHolderName" label="户名" width="100" />
          <el-table-column label="身份证" width="160">
            <template #default="{ row }">{{ maskIdCard(row.userIdCard) }}</template>
          </el-table-column>
          <el-table-column prop="accountType" label="类型" width="80" />
          <el-table-column prop="accountBalance" label="余额" width="140">
            <template #default="{ row }"><span class="vault-num">¥ {{ Number(row.accountBalance).toFixed(2) }}</span></template>
          </el-table-column>
          <el-table-column prop="accountStatus" label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.accountStatus === 1 ? 'success' : 'danger'" size="small">
                {{ row.accountStatus === 1 ? '正常' : '冻结' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && accounts.length === 0" description="暂无绑定银行卡" />
      </section>

      <footer class="page-foot">
        <span class="foot-brand">钱钱银行 · SMART BANKING</span>
        <span class="foot-slogan">稳健笃行 · 财富永续</span>
      </footer>
    </main>

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

    <el-dialog v-model="showFace" title="实名认证" width="480px" destroy-on-close @closed="stopCamera">
      <div class="face-dialog">
        <div class="face-status">
          <el-tag :type="realnameBound ? 'success' : 'info'">
            {{ realnameBound ? '已验证' : '未验证' }}
          </el-tag>
          <span class="face-tip">身份证号：{{ user?.idCardMask || maskIdCard(user?.userIdCard) || '--' }}</span>
        </div>
        <div class="face-status">
          <el-tag :type="user?.faceBound === 1 ? 'success' : 'info'" effect="plain">
            {{ user?.faceBound === 1 ? '已绑定人脸' : '未绑定人脸' }}
          </el-tag>
          <span class="face-tip">人脸特征仅用于本人实名认证</span>
        </div>

        <el-radio-group v-model="faceSource" class="face-source">
          <el-radio-button value="camera">摄像头拍摄</el-radio-button>
          <el-radio-button value="upload">上传照片</el-radio-button>
        </el-radio-group>

        <div v-if="faceSource === 'camera'" class="face-camera">
          <div class="camera-box">
            <video v-show="cameraOn" ref="cameraVideo" autoplay playsinline class="face-video"></video>
            <div v-if="!cameraOn" class="camera-placeholder">
              <el-icon :size="40" color="#c0c4cc"><Camera /></el-icon>
              <span>请开启摄像头并正对镜头</span>
            </div>
          </div>
          <div class="camera-actions">
            <el-button type="primary" :loading="cameraLoading" @click="toggleCamera">
              {{ cameraOn ? '关闭摄像头' : '开启摄像头' }}
            </el-button>
            <el-button type="success" :disabled="!cameraOn" @click="captureFrame">拍照</el-button>
          </div>
        </div>

        <el-upload
          v-if="faceSource === 'upload'"
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
        <div v-if="realnameVerifying" class="face-result">
          <span class="pass"><el-icon class="is-loading"><Loading /></el-icon> 实名认证中，请稍候…</span>
        </div>
        <div v-else-if="verifyResult !== null" class="face-result">
          <span :class="verifyResult.pass ? 'pass' : 'fail'">
            {{ verifyResult.pass ? '认证通过' : `认证未通过，相似度 ${verifyResult.score.toFixed(4)}` }}
          </span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserAccounts, addAccount, getUserInfo, updateUserPassword, verifyRealName } from '@/api/user'
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
const realnameVerifying = ref(false)
const verifyResult = ref(null)
const faceSource = ref('camera')
const cameraOn = ref(false)
const cameraLoading = ref(false)
const cameraVideo = ref(null)
let cameraStream = null
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
  reader.onload = async (e) => {
    faceImage.value = e.target.result.split(',')[1]
    facePreview.value = e.target.result
    verifyResult.value = null
    await autoRealNameVerify()
  }
  reader.readAsDataURL(file.raw)
}

const toggleCamera = async () => {
  if (cameraOn.value) {
    stopCamera()
    return
  }
  cameraLoading.value = true
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user' }, audio: false })
    cameraStream = stream
    const video = cameraVideo.value
    if (video) {
      video.srcObject = stream
      await video.play()
    }
    cameraOn.value = true
  } catch (e) {
    console.error(e)
    ElMessage.error('无法访问摄像头：' + (e.message || '请检查浏览器授权或摄像头设备'))
  } finally {
    cameraLoading.value = false
  }
}

const stopCamera = () => {
  if (cameraStream) {
    cameraStream.getTracks().forEach(t => t.stop())
    cameraStream = null
  }
  cameraOn.value = false
}

const captureFrame = async () => {
  const video = cameraVideo.value
  if (!video || video.videoWidth === 0) {
    ElMessage.warning('摄像头尚未就绪，请稍候')
    return
  }
  const canvas = document.createElement('canvas')
  canvas.width = video.videoWidth
  canvas.height = video.videoHeight
  canvas.getContext('2d').drawImage(video, 0, 0, canvas.width, canvas.height)
  const dataUrl = canvas.toDataURL('image/jpeg', 0.92)
  faceImage.value = dataUrl.split(',')[1]
  facePreview.value = dataUrl
  verifyResult.value = null
  ElMessage.success('拍照成功，正在实名认证…')
  await autoRealNameVerify()
}

const realnameBound = computed(() => Number(user.value?.realnameBound) === 1)

const autoRealNameVerify = async () => {
  if (!faceImage.value) return
  if (realnameBound.value) {
    ElMessage.info('您已完成实名认证')
    return
  }
  realnameVerifying.value = true
  try {
    const res = await verifyRealName(faceImage.value)
    verifyResult.value = res.data
    if (res.data.pass) {
      ElMessage.success('实名认证成功')
      await fetchData()
    }
  } catch (e) {
    console.error(e)
  } finally {
    realnameVerifying.value = false
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
onUnmounted(() => { clearInterval(timer); stopCamera() })
watch(faceSource, (val) => { if (val !== 'camera') stopCamera() })
</script>

<style scoped>
/* ============================================================
   深藏蓝 × 香槟金 · 奢华排布
   ============================================================ */
.dashboard {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(1400px 640px at 85% -12%, rgba(212,176,106,0.06), transparent 60%),
    radial-gradient(1200px 600px at 8% 8%, rgba(47,168,160,0.04), transparent 55%),
    var(--vault-bg);
}

/* ============ 顶栏 ============ */
.top-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 72px;
  padding: 0 32px;
  background: rgba(10,16,28,0.82);
  border-bottom: 1px solid var(--vault-line);
  backdrop-filter: blur(12px);
  position: sticky;
  top: 0;
  z-index: 100;
  flex-shrink: 0;
}
.nav-left { display: flex; align-items: center; gap: 14px; }
.brand { display: flex; align-items: center; gap: 12px; }
.brand-emblem {
  width: 40px; height: 40px;
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; font-weight: 800;
  color: #231a08;
  border-radius: 12px;
  background: linear-gradient(135deg, #e6c98a, #b08d4a);
  box-shadow: 0 0 20px var(--vault-accent-glow);
}
.brand-text { display: flex; flex-direction: column; line-height: 1.15; }
.brand-name { font-size: 17px; font-weight: 700; color: var(--vault-text); letter-spacing: 0.18em; }
.brand-en { font-size: 9px; color: var(--vault-accent); letter-spacing: 0.42em; font-weight: 600; }
.header-divider { width: 1px; height: 24px; background: var(--vault-line-strong); }
.header-welcome { font-size: 14px; color: var(--vault-text-2); letter-spacing: 0.04em; }

.nav-right { display: flex; align-items: center; gap: 22px; }
.server-status { display: flex; align-items: center; gap: 8px; }
.server-text { font-size: 12px; color: var(--vault-text-3); letter-spacing: 0.06em; }
.header-time { display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--vault-text-3); font-weight: 500; }
.user-trigger { display: flex; align-items: center; gap: 8px; cursor: pointer; padding: 5px 10px; border-radius: 10px; transition: all 0.2s; }
.user-trigger:hover { background: rgba(212,176,106,0.08); }
.user-avatar {
  width: 34px; height: 34px; border-radius: 50%;
  background: linear-gradient(135deg, #3a2c14, #5a4420);
  border: 1px solid var(--vault-line-strong);
  color: var(--vault-gold);
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 700;
}
.user-name { font-size: 14px; font-weight: 500; color: var(--vault-text); }

/* ============ 身份面板 ============ */
.hero {
  position: relative;
  padding: 40px 64px 34px;
  margin: 28px 32px 0;
  border-radius: 22px;
  overflow: hidden;
  background: linear-gradient(150deg, #131f38 0%, #0f1830 48%, #141c30 100%);
  border: 1px solid var(--vault-line-strong);
  box-shadow: inset 0 1px 0 rgba(212,176,106,0.10), 0 24px 64px rgba(0,0,0,0.45);
}
.hero-glow {
  position: absolute;
  top: -60%; right: -12%;
  width: 620px; height: 620px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(212,176,106,0.12) 0%, transparent 68%);
  pointer-events: none;
}
.hero-inner { position: relative; z-index: 2; }
.hero-top { display: flex; justify-content: space-between; align-items: center; gap: 24px; }
.hero-welcome { display: flex; flex-direction: column; gap: 4px; }
.hero-greet { font-size: 13px; color: var(--vault-text-3); letter-spacing: 0.24em; }
.hero-acct { font-size: 15px; color: var(--vault-text-2); letter-spacing: 0.1em; }

.hero-quick { display: flex; align-items: center; gap: 12px; }
.q-btn {
  display: flex; align-items: center; gap: 7px;
  padding: 11px 22px;
  font-size: 14px; font-weight: 600;
  color: var(--vault-text-2);
  background: rgba(212,176,106,0.04);
  border: 1px solid var(--vault-line-strong);
  border-radius: 12px;
  cursor: pointer;
  letter-spacing: 0.08em;
  transition: all 0.22s;
}
.q-btn:hover { color: var(--vault-gold); border-color: var(--vault-line-hover); transform: translateY(-1px); }
.q-btn.q-primary {
  color: #231a08;
  background: linear-gradient(135deg, #e6c98a, #b08d4a);
  border: none;
  box-shadow: 0 6px 22px rgba(212,176,106,0.30);
}
.q-btn.q-primary:hover { color: #1a1306; box-shadow: 0 8px 28px rgba(212,176,106,0.42); }

.hero-identity {
  display: flex;
  align-items: center;
  gap: 22px;
  margin: 44px 0 30px;
}
.identity-avatar {
  width: 76px; height: 76px;
  border-radius: 22px;
  background: linear-gradient(135deg, #3a2c14, #5a4420);
  border: 1px solid var(--vault-line-strong);
  color: var(--vault-gold);
  display: flex; align-items: center; justify-content: center;
  font-size: 32px; font-weight: 700;
  box-shadow: 0 0 30px rgba(212,176,106,0.18);
}
.identity-main { display: flex; flex-direction: column; gap: 6px; }
.identity-name {
  font-size: 30px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--vault-text);
  background: linear-gradient(180deg, #ffffff 0%, var(--vault-gold) 120%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.identity-sub { font-size: 13px; color: var(--vault-text-3); letter-spacing: 0.12em; }
.verify-badge {
  margin-left: auto;
  display: flex; align-items: center; gap: 8px;
  padding: 9px 18px;
  border-radius: 24px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.08em;
}
.verify-badge.verified { color: var(--vault-up-soft); background: rgba(47,168,160,0.12); border: 1px solid rgba(47,168,160,0.32); }
.verify-badge.unverified { color: var(--vault-gold); background: rgba(212,176,106,0.10); border: 1px solid rgba(212,176,106,0.30); }
.status-dot { width: 7px; height: 7px; border-radius: 50%; background: currentColor; }

.hero-meta { display: flex; align-items: center; gap: 0; flex-wrap: wrap; }
.meta-item { display: flex; flex-direction: column; gap: 5px; padding: 0 36px; }
.meta-item:first-child { padding-left: 0; }
.meta-label { font-size: 11px; color: var(--vault-text-dim); letter-spacing: 0.16em; }
.meta-value { font-size: 15px; color: var(--vault-text-2); font-weight: 500; letter-spacing: 0.06em; }
.meta-divider { width: 1px; height: 32px; background: var(--vault-line); }

/* ============ 内容区 ============ */
.content { flex: 1; padding: 36px 32px 24px; }
.services, .accounts-card {
  max-width: 1240px;
  margin: 0 auto;
  background: linear-gradient(180deg, var(--vault-panel-2), var(--vault-panel));
  border: 1px solid var(--vault-line);
  border-radius: 22px;
  padding: 34px 36px;
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.03), 0 16px 44px rgba(0,0,0,0.32);
  position: relative;
}
.services::before, .accounts-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(212,176,106,0.45), transparent);
}
.accounts-card { margin-top: 24px; }
.section-head { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 26px; }
.section-title { display: flex; flex-direction: column; gap: 3px; }
.section-title h2 { font-size: 26px; font-weight: 700; color: var(--vault-text); letter-spacing: 0.1em; margin: 0; }
.section-title h2::after {
  content: '';
  display: block;
  margin-top: 10px;
  width: 46px; height: 3px;
  border-radius: 3px;
  background: linear-gradient(90deg, var(--vault-accent), transparent);
}
.section-en { font-size: 10px; color: var(--vault-accent); letter-spacing: 0.4em; font-weight: 600; margin-top: 6px; }
.section-sub { font-size: 13px; color: var(--vault-text-3); letter-spacing: 0.06em; }

.service-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}
.service-card {
  position: relative;
  padding: 30px 26px 24px;
  border-radius: 18px;
  border: 1px solid var(--vault-line);
  background: linear-gradient(165deg, rgba(212,176,106,0.045), rgba(255,255,255,0.012) 55%);
  cursor: pointer;
  transition: all 0.28s;
  overflow: hidden;
}
.service-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(212,176,106,0.6), transparent);
  opacity: 0;
  transition: opacity 0.28s;
}
.service-card:hover {
  transform: translateY(-4px);
  border-color: var(--vault-line-hover);
  box-shadow: 0 18px 40px rgba(0,0,0,0.42), 0 0 24px rgba(212,176,106,0.10);
  background: linear-gradient(165deg, rgba(212,176,106,0.10), rgba(255,255,255,0.02) 55%);
}
.service-card:hover::before { opacity: 1; }
.s-icon {
  width: 54px; height: 54px;
  border-radius: 15px;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 18px;
  border: 1px solid rgba(255,255,255,0.10);
}
.s-icon.gold { color: #e6c98a; background: linear-gradient(135deg, rgba(212,176,106,0.20), rgba(212,176,106,0.04)); }
.s-icon.blue { color: #7ea8f7; background: linear-gradient(135deg, rgba(126,168,247,0.18), rgba(126,168,247,0.03)); }
.s-icon.cyan { color: #3fc9bf; background: linear-gradient(135deg, rgba(63,201,191,0.16), rgba(63,201,191,0.03)); }
.s-icon.violet { color: #a78bfa; background: linear-gradient(135deg, rgba(167,139,250,0.18), rgba(167,139,250,0.03)); }
.s-icon.rose { color: #f48fb1; background: linear-gradient(135deg, rgba(244,143,177,0.18), rgba(244,143,177,0.03)); }
.s-icon.amber { color: #f0b14e; background: linear-gradient(135deg, rgba(240,177,78,0.20), rgba(240,177,78,0.04)); }
.s-name { font-size: 17px; font-weight: 600; color: var(--vault-text); letter-spacing: 0.1em; margin-bottom: 6px; }
.s-desc { font-size: 12px; color: var(--vault-text-3); letter-spacing: 0.05em; }
.s-go {
  position: absolute;
  top: 24px; right: 24px;
  width: 28px; height: 28px;
  border-radius: 50%;
  border: 1px solid var(--vault-line);
  color: var(--vault-text-3);
  display: flex; align-items: center; justify-content: center;
  transition: all 0.25s;
}
.service-card:hover .s-go { color: var(--vault-gold); border-color: var(--vault-accent); transform: translateX(2px); }

.page-foot {
  max-width: 1240px;
  margin: 30px auto 0;
  padding: 0 8px;
  display: flex; justify-content: space-between; align-items: center;
  color: var(--vault-text-dim);
  font-size: 12px;
  letter-spacing: 0.14em;
}
.foot-slogan { color: var(--vault-accent); opacity: 0.8; letter-spacing: 0.3em; }

/* ============ 实名认证 ============ */
.face-dialog { display: flex; flex-direction: column; gap: 14px; }
.face-status { display: flex; align-items: center; justify-content: space-between; }
.face-tip { font-size: 12px; color: var(--vault-text-3); }
.face-source { display: flex; justify-content: center; }
.face-camera { display: flex; flex-direction: column; gap: 10px; }
.camera-box { width: 100%; height: 240px; border-radius: 10px; border: 1px solid var(--vault-line); background: #0d1117; overflow: hidden; display: flex; align-items: center; justify-content: center; }
.face-video { width: 100%; height: 100%; object-fit: cover; }
.camera-placeholder { display: flex; flex-direction: column; align-items: center; gap: 10px; color: #c0c4cc; font-size: 13px; }
.camera-actions { display: flex; gap: 12px; justify-content: center; }
.face-preview { width: 100%; max-height: 220px; border-radius: 10px; border: 1px solid var(--vault-line); background: var(--vault-panel); }
.face-result { text-align: center; font-size: 14px; font-weight: 600; }
.face-result .pass { color: var(--vault-up); }
.face-result .fail { color: var(--vault-down); }

:deep(.el-table__row) { cursor: pointer; }
.profile-grid { display: flex; flex-direction: column; gap: 10px; font-size: 14px; }
.profile-grid label { color: var(--vault-text-3); margin-right: 8px; }

@media (max-width: 820px) {
  .service-grid { grid-template-columns: repeat(2, 1fr); }
  .hero { padding: 28px 24px 24px; margin: 16px 16px 0; }
  .hero-top { flex-direction: column; align-items: flex-start; }
  .hero-identity { flex-wrap: wrap; }
  .verify-badge { margin-left: 0; }
  .meta-item { padding: 0 18px; }
  .server-text, .header-time, .header-welcome { display: none; }
}
@media (max-width: 520px) {
  .service-grid { grid-template-columns: 1fr; }
  .hero-quick { width: 100%; flex-wrap: wrap; }
  .q-btn { flex: 1; justify-content: center; min-width: 120px; }
  .brand-en { display: none; }
}
</style>

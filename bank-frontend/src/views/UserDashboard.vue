<template>
  <div class="dashboard">
    <header class="dash-header">
      <div class="header-left">
        <el-icon :size="24" color="#f5af19"><UserFilled /></el-icon>
        <span class="brand-text">智慧银行 · 用户中心</span>
      </div>
      <div class="header-right">
        <el-button size="small" @click="logout">退出登录</el-button>
      </div>
    </header>

    <div class="dash-body">
      <!-- User Info -->
      <el-card class="info-card">
        <template #header><span>个人信息</span></template>
        <div v-if="user" class="info-grid">
          <div><label>手机号：</label>{{ user.userPhone }}</div>
          <div><label>身份证：</label>{{ user.userIdCard }}</div>
        </div>
      </el-card>

      <!-- Accounts -->
      <el-card class="accounts-card">
        <template #header>
          <div class="card-header">
            <span>我的银行卡 ({{ accounts.length }})</span>
            <div class="card-header-actions">
              <el-button size="small" @click="showRegister = true">+ 注册新卡</el-button>
              <el-button size="small" type="primary" @click="showAdd = true">+ 添加银行卡</el-button>
            </div>
          </div>
        </template>
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
      </el-card>
    </div>

    <!-- Add Account Dialog -->
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

    <!-- Register New Card Dialog -->
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserAccounts, addAccount, getUserInfo } from '@/api/user'
import { register as registerAccount } from '@/api/account'

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

const logout = () => {
  localStorage.removeItem('token')
  router.push('/user/login')
}

onMounted(fetchData)
</script>

<style scoped>
.dashboard { height: 100vh; display: flex; flex-direction: column; background: #f0f2f5; }
.dash-header { display: flex; justify-content: space-between; align-items: center; height: 56px; padding: 0 24px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.header-left { display: flex; align-items: center; gap: 10px; }
.brand-text { font-size: 16px; font-weight: 700; color: #1a3a6b; }
.header-right { display: flex; align-items: center; gap: 12px; }
.dash-body { flex: 1; padding: 20px 24px; overflow-y: auto; display: flex; flex-direction: column; gap: 16px; }
.info-card { max-width: 480px; }
.info-grid { display: flex; flex-direction: column; gap: 8px; font-size: 14px; }
.info-grid label { color: #999; margin-right: 8px; }
.card-header { display: flex; justify-content: space-between; align-items: center; gap: 8px; flex-wrap: wrap; }
.card-header-actions { display: flex; gap: 8px; }
:deep(.el-table__row) { cursor: pointer; }
</style>

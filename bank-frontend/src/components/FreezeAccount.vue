<template>
  <div class="freeze-account">
    <div class="section-header">
      <el-icon :size="20" color="#1a3a6b"><Setting /></el-icon>
      <h3>账户管理</h3>
      <span class="header-tag">管理员</span>
    </div>

    <div class="manage-card">
      <div class="manage-icon">
        <el-icon :size="36" color="#fff"><Lock /></el-icon>
      </div>
      <div class="manage-content">
        <h4>账户冻结/解冻</h4>
        <p>输入目标账号进行账户状态管理操作</p>
      </div>
    </div>

    <el-form :model="form" class="manage-form">
      <div class="form-row">
        <label class="form-label">目标账号</label>
        <el-input
          v-model="targetAccount"
          placeholder="请输入要操作的银行账号"
          size="large"
          clearable
          style="width: 300px;"
        >
          <template #prefix>
            <el-icon color="#999"><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <div class="form-actions">
        <el-button
          type="warning"
          size="large"
          :loading="loading"
          @click="handleFreeze"
          class="action-btn freeze-btn"
        >
          <el-icon><Lock /></el-icon>
          冻结账户
        </el-button>
        <el-button
          type="success"
          size="large"
          :loading="loading"
          @click="handleUnfreeze"
          class="action-btn unfreeze-btn"
        >
          <el-icon><Unlock /></el-icon>
          解冻账户
        </el-button>
      </div>
    </el-form>

    <el-alert
      v-if="resultMsg"
      :title="resultMsg"
      :type="resultType"
      show-icon
      class="result-alert"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { freezeAccount, unfreezeAccount } from '@/api/account'
import { Search, Lock, Unlock } from '@element-plus/icons-vue'

const targetAccount = ref('')
const resultMsg = ref('')
const resultType = ref('success')
const loading = ref(false)

const handleFreeze = async () => {
  if (!targetAccount.value) { ElMessage.warning('请输入目标账号'); return }
  loading.value = true
  try {
    await freezeAccount(targetAccount.value)
    resultMsg.value = `账号 ${targetAccount.value} 已成功冻结`
    resultType.value = 'success'
    targetAccount.value = ''
  } catch (error) {
    resultMsg.value = '冻结失败，请查看提示'
    resultType.value = 'error'
  } finally { loading.value = false }
}

const handleUnfreeze = async () => {
  if (!targetAccount.value) { ElMessage.warning('请输入目标账号'); return }
  loading.value = true
  try {
    await unfreezeAccount(targetAccount.value)
    resultMsg.value = `账号 ${targetAccount.value} 已成功解冻`
    resultType.value = 'success'
    targetAccount.value = ''
  } catch (error) {
    resultMsg.value = '解冻失败，请查看提示'
    resultType.value = 'error'
  } finally { loading.value = false }
}
</script>

<style scoped>
.freeze-account { width: 100%; }

.section-header {
  display: flex; align-items: center; gap: 8px;
  margin-bottom: 20px;
}
.section-header h3 { margin: 0; font-size: 16px; font-weight: 600; color: #333; }
.header-tag {
  font-size: 11px; background: #e8f0fe; color: #1a3a6b;
  padding: 2px 8px; border-radius: 4px; font-weight: 600;
}

.manage-card {
  display: flex; align-items: center; gap: 16px;
  background: linear-gradient(135deg, #f0f4ff, #e8f0fe);
  border: 1px solid #d0ddf5;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 24px;
  max-width: 520px;
}

.manage-icon {
  width: 56px; height: 56px; border-radius: 14px;
  background: linear-gradient(135deg, #1a3a6b, #2d5a8c);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}

.manage-content h4 {
  margin: 0 0 4px; font-size: 15px; font-weight: 600; color: #333;
}
.manage-content p {
  margin: 0; font-size: 13px; color: #999;
}

.manage-form { max-width: 520px; }
.form-row { margin-bottom: 20px; }
.form-label {
  display: block; font-size: 13px; font-weight: 600;
  color: #333; margin-bottom: 6px;
}

.form-actions { display: flex; gap: 12px; }

.action-btn {
  flex: 1; height: 46px;
  font-size: 15px; font-weight: 600;
  border-radius: 10px;
}

.freeze-btn {
  background: linear-gradient(135deg, #e6a23c, #f5af19);
  border: none; color: #fff;
}
.freeze-btn:hover {
  background: linear-gradient(135deg, #cf9236, #e6a23c);
}

.unfreeze-btn {
  background: linear-gradient(135deg, #67c23a, #85ce61);
  border: none; color: #fff;
}
.unfreeze-btn:hover {
  background: linear-gradient(135deg, #5daf34, #67c23a);
}

.result-alert {
  max-width: 520px; width: 100%; margin-top: 16px;
}

@media (max-width: 480px) {
  .form-actions { flex-direction: column; }
  .manage-form :deep(.el-input) { width: 100% !important; }
}
</style>

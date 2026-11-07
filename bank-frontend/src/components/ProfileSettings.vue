<template>
  <div class="profile-settings">
    <el-form :model="form" label-width="100px" class="profile-form">
      <el-form-item label="账号">
        <el-input :model-value="userInfo?.accountNumber" disabled />
      </el-form-item>
      <el-form-item label="账户类型">
        <el-input :model-value="userInfo?.accountType" disabled />
      </el-form-item>
      <el-form-item label="户名">
        <el-input v-model="form.accountHolderName" placeholder="请输入户名" maxlength="20" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="form.userPhoneNumber" placeholder="请输入手机号" maxlength="11" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="large" :loading="loading" @click="handleSubmit" class="submit-btn">保存修改</el-button>
      </el-form-item>
    </el-form>

    <el-alert v-if="resultMsg" :title="resultMsg" :type="resultType" show-icon class="result-alert" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { updateProfile } from '@/api/account'

const loading = ref(false)
const resultMsg = ref('')
const resultType = ref('success')

const userInfo = ref(null)
const form = reactive({
  accountHolderName: '',
  userPhoneNumber: ''
})

onMounted(() => {
  try {
    const info = localStorage.getItem('userInfo')
    if (info) {
      userInfo.value = JSON.parse(info)
      form.accountHolderName = userInfo.value?.accountHolderName || ''
      form.userPhoneNumber = userInfo.value?.userPhoneNumber || ''
    }
  } catch (e) { console.warn(e) }
})

const handleSubmit = async () => {
  if (!form.accountHolderName || !form.userPhoneNumber) {
    ElMessage.warning('请填写完整')
    return
  }
  loading.value = true
  try {
    await updateProfile({
      accountHolderName: form.accountHolderName,
      userPhoneNumber: form.userPhoneNumber
    })
    const newInfo = { ...userInfo.value, accountHolderName: form.accountHolderName, userPhoneNumber: form.userPhoneNumber }
    localStorage.setItem('userInfo', JSON.stringify(newInfo))
    userInfo.value = newInfo
    resultMsg.value = '个人信息已更新'
    resultType.value = 'success'
  } catch (e) {
    resultMsg.value = e.response?.data?.msg || '保存失败'
    resultType.value = 'error'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.profile-settings {
  width: 100%;
}

.profile-form {
  max-width: 400px;
  margin: 0 auto;
  padding: var(--space-5) 0;
}

.result-alert {
  max-width: 400px;
  margin: 0 auto;
}

.submit-btn {
  width: 100%;
  font-family: var(--font-sans);
}

:deep(.el-form-item__label) {
  color: var(--color-muted-foreground);
  font-family: var(--font-sans);
}

:deep(.el-input__wrapper) {
  background: var(--color-card);
  border: 1px solid var(--glass-border);
  box-shadow: none;
}

:deep(.el-input__wrapper:hover) {
  border-color: var(--color-border-hover);
}

:deep(.el-input__wrapper:focus-within) {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--color-primary) 15%, transparent);
}

:deep(.el-input.is-disabled .el-input__wrapper) {
  background: var(--color-muted);
  border-color: var(--glass-border);
  opacity: 0.7;
}

:deep(.el-input__inner) {
  color: var(--color-foreground);
  font-family: var(--font-sans);
}

:deep(.el-input__inner::placeholder) {
  color: var(--color-muted-foreground);
}

:deep(.el-button--primary) {
  --el-button-bg-color: var(--color-primary);
  --el-button-border-color: var(--color-primary);
  --el-button-text-color: var(--color-on-primary);
  --el-button-hover-bg-color: var(--color-secondary);
  --el-button-hover-border-color: var(--color-secondary);
  --el-button-hover-text-color: var(--color-on-primary);
}
</style>
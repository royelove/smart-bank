<template>
  <div class="change-password">
    <el-form :model="form" label-width="100px" class="pwd-form">
      <el-form-item label="原密码">
        <el-input v-model="form.oldPassword" type="password" placeholder="请输入原密码" show-password maxlength="6" />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="form.newPassword" type="password" placeholder="6位数字" show-password maxlength="6" />
      </el-form-item>
      <el-form-item label="确认新密码">
        <el-input v-model="form.confirmPassword" type="password" placeholder="再次输入新密码" show-password maxlength="6" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="large" :loading="loading" @click="handleSubmit" style="width:100%">确认修改</el-button>
      </el-form-item>
    </el-form>

    <el-alert v-if="resultMsg" :title="resultMsg" :type="resultType" show-icon class="result-alert" />
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { updatePassword } from '@/api/account'

const emit = defineEmits(['close'])
const loading = ref(false)
const resultMsg = ref('')
const resultType = ref('success')

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const handleSubmit = async () => {
  if (!form.oldPassword || !form.newPassword || !form.confirmPassword) {
    ElMessage.warning('请填写完整')
    return
  }
  if (!/^\d{6}$/.test(form.newPassword)) {
    ElMessage.warning('新密码必须是6位数字')
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    ElMessage.warning('两次密码不一致')
    return
  }
  loading.value = true
  try {
    await updatePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
      confirmPassword: form.confirmPassword
    })
    resultMsg.value = '密码修改成功，下次登录请使用新密码'
    resultType.value = 'success'
    form.oldPassword = ''
    form.newPassword = ''
    form.confirmPassword = ''
  } catch (e) {
    resultMsg.value = e.response?.data?.msg || '修改失败'
    resultType.value = 'error'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.pwd-form { max-width: 400px; margin: 0 auto; padding: 20px 0; }
.result-alert { max-width: 400px; margin: 0 auto; }
</style>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { register } from '../api/auth'
import { errorMessage } from '../api/http'

const router = useRouter()
const formRef = ref<FormInstance>()
const form = reactive({ username: '', password: '', nickname: '', confirmPassword: '' })
const loading = ref(false)
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{3,50}$/, message: '用户名须为3至50位字母、数字或下划线', trigger: 'blur' }],
  nickname: [{ required: true, whitespace: true, message: '请输入昵称', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 72, message: '密码须为8至72个字符', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: (_rule, value, callback) => callback(value === form.password ? undefined : new Error('两次密码不一致')), trigger: 'blur' }],
}

async function submit() {
  if (loading.value || !formRef.value) return
  try { await formRef.value.validate() } catch { return }
  loading.value = true
  try {
    await register(form.username.trim(), form.password, form.nickname.trim())
    form.password = ''
    form.confirmPassword = ''
    ElMessage.success('注册成功，请登录')
    await router.replace('/login')
  } catch (error) { ElMessage.error(errorMessage(error)) }
  finally { loading.value = false }
}
</script>

<template>
  <el-card class="auth-card">
    <template #header><h2>用户注册</h2></template>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
      <el-form-item label="用户名" prop="username"><el-input v-model="form.username" autocomplete="username" :maxlength="50" /></el-form-item>
      <el-form-item label="昵称" prop="nickname"><el-input v-model="form.nickname" :maxlength="50" /></el-form-item>
      <el-form-item label="密码" prop="password"><el-input v-model="form.password" type="password" show-password autocomplete="new-password" :maxlength="72" /></el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword"><el-input v-model="form.confirmPassword" type="password" autocomplete="new-password" :maxlength="72" /></el-form-item>
      <el-button class="submit-button" type="primary" native-type="submit" :loading="loading">注册</el-button>
    </el-form>
    <p class="auth-link">已有账号？<router-link to="/login">返回登录</router-link></p>
  </el-card>
</template>

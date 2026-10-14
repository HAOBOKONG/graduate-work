<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { login } from '../api/auth'
import { errorMessage } from '../api/http'
import { saveToken } from '../auth/token'

const router = useRouter()
const formRef = ref<FormInstance>()
const form = reactive({ username: '', password: '' })
const loading = ref(false)
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function submit() {
  if (loading.value || !formRef.value) return
  try { await formRef.value.validate() } catch { return }
  loading.value = true
  try {
    const result = await login(form.username.trim(), form.password)
    saveToken(result.token)
    form.password = ''
    await router.replace(result.user.role === 'ADMIN' ? '/admin' : '/')
  } catch (error) { ElMessage.error(errorMessage(error)) }
  finally { loading.value = false }
}
</script>

<template>
  <el-card class="auth-card">
    <template #header><h2>用户登录</h2></template>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" autocomplete="username" :maxlength="50" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="form.password" type="password" show-password autocomplete="current-password" :maxlength="72" />
      </el-form-item>
      <el-button class="submit-button" type="primary" native-type="submit" :loading="loading">登录</el-button>
    </el-form>
    <p class="auth-link">还没有账号？<router-link to="/register">注册账号</router-link></p>
  </el-card>
</template>

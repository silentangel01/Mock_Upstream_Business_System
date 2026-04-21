<template>
  <div class="login-page">
    <el-card class="login-card" shadow="always">
      <template #header>
        <h2 style="text-align:center;margin:0">MUBS Ticket System</h2>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin" label-width="0">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="Username" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" placeholder="Password" type="password" prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading" size="large" style="width:100%">
            Login
          </el-button>
        </el-form-item>
      </el-form>
      <div v-if="error" style="color:var(--el-color-danger);text-align:center">{{ error }}</div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@shared/stores/auth'
import type { FormInstance } from 'element-plus'

const router = useRouter()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const error = ref('')

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: 'Username is required', trigger: 'blur' }],
  password: [{ required: true, message: 'Password is required', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  error.value = ''
  try {
    await auth.login(form.username, form.password)
    router.push('/dashboard')
  } catch (e: any) {
    error.value = e.response?.data?.message || 'Login failed'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
}
.login-card {
  width: 400px;
}
</style>

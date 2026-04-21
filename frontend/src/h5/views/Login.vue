<template>
  <div class="h5-login">
    <div style="text-align:center;padding:60px 0 40px">
      <h2>MUBS Mobile</h2>
    </div>
    <van-form @submit="handleLogin">
      <van-cell-group inset>
        <van-field v-model="form.username" label="Username" placeholder="Enter username" :rules="[{ required: true, message: 'Username is required' }]" />
        <van-field v-model="form.password" type="password" label="Password" placeholder="Enter password" :rules="[{ required: true, message: 'Password is required' }]" />
      </van-cell-group>
      <div style="margin:24px 16px">
        <van-button round block type="primary" native-type="submit" :loading="loading">Login</van-button>
      </div>
    </van-form>
    <div v-if="error" style="color:red;text-align:center;padding:0 16px">{{ error }}</div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@shared/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const error = ref('')
const form = reactive({ username: '', password: '' })

async function handleLogin() {
  loading.value = true
  error.value = ''
  try {
    await auth.login(form.username, form.password)
    router.push('/tasks')
  } catch (e: any) {
    error.value = e.response?.data?.message || 'Login failed'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.h5-login { min-height: 100vh; background: #f7f8fa; }
</style>

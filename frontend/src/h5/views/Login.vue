<template>
  <div class="h5-login">
    <div style="text-align:center;padding:60px 0 40px">
      <h2>MUBS 移动工单</h2>
    </div>
    <van-form @submit="handleLogin">
      <van-cell-group inset>
        <van-field v-model="form.username" label="用户名" placeholder="请输入用户名" :rules="[{ required: true, message: '请输入用户名' }]" />
        <van-field v-model="form.password" type="password" label="密码" placeholder="请输入密码" :rules="[{ required: true, message: '请输入密码' }]" />
      </van-cell-group>
      <div style="margin:24px 16px">
        <van-button round block type="primary" native-type="submit" :loading="loading">登 录</van-button>
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
    error.value = e.response?.data?.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.h5-login { min-height: 100vh; background: #f7f8fa; }
</style>

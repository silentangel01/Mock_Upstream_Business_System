<template>
  <el-container style="height:100vh">
    <el-aside width="200px" style="background:#001529">
      <div style="color:#fff;text-align:center;padding:16px 0;font-size:18px;font-weight:bold">MUBS</div>
      <el-menu :default-active="route.path" router background-color="#001529" text-color="#ffffffa6" active-text-color="#fff">
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/tickets">
          <el-icon><Tickets /></el-icon>
          <span>工单管理</span>
        </el-menu-item>
        <el-menu-item v-if="auth.isAdmin" index="/dispatch-rules">
          <el-icon><Setting /></el-icon>
          <span>派遣规则</span>
        </el-menu-item>
        <el-menu-item v-if="auth.isAdmin" index="/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="display:flex;align-items:center;justify-content:flex-end;gap:16px;border-bottom:1px solid #eee">
        <el-badge :value="notif.unreadCount" :hidden="!notif.unreadCount" @click="notif.clearUnread()">
          <el-icon :size="20" style="cursor:pointer"><Bell /></el-icon>
        </el-badge>
        <el-dropdown @command="onCommand">
          <span style="cursor:pointer;display:flex;align-items:center;gap:4px">
            {{ auth.username }}
            <el-tag size="small">{{ auth.role }}</el-tag>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main style="background:#f0f2f5;overflow:auto">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@shared/stores/auth'
import { useNotificationStore } from '@shared/stores/notification'
import { connectWebSocket, disconnectWebSocket } from '@shared/services/websocket'
import { onMounted, onUnmounted, watch } from 'vue'
import { ElNotification } from 'element-plus'
import { DataAnalysis, Tickets, Setting, Bell, ArrowDown, User } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const notif = useNotificationStore()

// Show desktop notification when new WS message arrives
watch(() => notif.messages.length, (newLen, oldLen) => {
  if (newLen > oldLen) {
    const msg = notif.messages[0]
    ElNotification({ title: msg.title, message: msg.body, type: 'info', duration: 4000 })
  }
})

onMounted(() => {
  if (auth.token) connectWebSocket(auth.token)
})
onUnmounted(() => disconnectWebSocket())

function onCommand(cmd: string) {
  if (cmd === 'logout') {
    disconnectWebSocket()
    auth.logout()
    router.push('/login')
  }
}
</script>

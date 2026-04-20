<template>
  <div>
    <router-view />
    <van-tabbar v-model="active" route>
      <van-tabbar-item to="/tasks" icon="todo-list-o">任务</van-tabbar-item>
      <van-tabbar-item to="/history" icon="clock-o">历史</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { showNotify } from 'vant'
import { useAuthStore } from '@shared/stores/auth'
import { useNotificationStore } from '@shared/stores/notification'
import { connectWebSocket, disconnectWebSocket } from '@shared/services/websocket'

const active = ref(0)
const auth = useAuthStore()
const notif = useNotificationStore()

watch(() => notif.messages.length, (newLen, oldLen) => {
  if (newLen > oldLen) {
    const msg = notif.messages[0]
    showNotify({ type: 'primary', message: `${msg.title}: ${msg.body}` })
  }
})

onMounted(() => {
  if (auth.token) connectWebSocket(auth.token)
})
onUnmounted(() => disconnectWebSocket())
</script>

<style scoped>
div { padding-bottom: 50px; }
</style>

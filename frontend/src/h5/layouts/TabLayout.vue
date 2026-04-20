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
import { ref } from 'vue'
const active = ref(0)
</script>

<style scoped>
div { padding-bottom: 50px; }
</style>

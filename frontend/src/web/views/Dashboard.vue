<template>
  <div>
    <!-- Stats Cards -->
    <el-row :gutter="16" style="margin-bottom:20px">
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="总工单" :value="stats.total" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="待处理" :value="stats.byStatus['PENDING'] || 0" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="处理中" :value="(stats.byStatus['IN_PROGRESS'] || 0) + (stats.byStatus['ACCEPTED'] || 0) + (stats.byStatus['DISPATCHED'] || 0)" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="平均处理时间" :value="avgTime" suffix="分钟" />
        </el-card>
      </el-col>
    </el-row>

    <!-- Charts -->
    <el-row :gutter="16" style="margin-bottom:20px">
      <el-col :span="12">
        <el-card header="状态分布">
          <v-chart :option="statusChartOption" style="height:300px" autoresize />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="事件类型分布">
          <v-chart :option="eventChartOption" style="height:300px" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <!-- Recent Tickets + Simulate -->
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>最近工单</span>
          <el-button v-if="auth.isAdmin" type="primary" size="small" @click="simulate" :loading="simulating">
            模拟事件
          </el-button>
        </div>
      </template>
      <el-table :data="recentTickets" stripe @row-click="(row: Ticket) => router.push(`/tickets/${row.id}`)">
        <el-table-column prop="eventType" label="事件类型" width="160">
          <template #default="{ row }">{{ EVENT_TYPE_LABEL[row.eventType] || row.eventType }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="STATUS_TYPE[row.status] as any" size="small">{{ STATUS_LABEL[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assignedTeam" label="负责团队" width="120">
          <template #default="{ row }">{{ TEAM_LABEL[row.assignedTeam] || row.assignedTeam || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { PieChart, BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getTicketStats, listTickets } from '@shared/api/tickets'
import { simulateEvent } from '@shared/api/demo'
import { useAuthStore } from '@shared/stores/auth'
import { STATUS_LABEL, STATUS_TYPE, EVENT_TYPE_LABEL, TEAM_LABEL } from '@shared/utils/constants'
import { formatDate } from '@shared/utils/format'
import type { Ticket, TicketStats } from '@shared/types'

use([PieChart, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

const router = useRouter()
const auth = useAuthStore()
const stats = ref<TicketStats>({ total: 0, byStatus: {}, byEventType: {}, byTeam: {} })
const recentTickets = ref<Ticket[]>([])
const simulating = ref(false)

const avgTime = computed(() => Math.round(stats.value.avgResolutionMinutes ?? 0))

const statusChartOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  series: [{
    type: 'pie', radius: ['40%', '70%'],
    data: Object.entries(stats.value.byStatus).map(([k, v]) => ({ name: STATUS_LABEL[k] || k, value: v })),
  }],
}))

const eventChartOption = computed(() => ({
  tooltip: {},
  xAxis: { type: 'category', data: Object.keys(stats.value.byEventType).map(k => EVENT_TYPE_LABEL[k] || k) },
  yAxis: { type: 'value' },
  series: [{ type: 'bar', data: Object.values(stats.value.byEventType) }],
}))

async function loadData() {
  try {
    const [s, t] = await Promise.all([getTicketStats(), listTickets({ size: 5 })])
    stats.value = s.data
    recentTickets.value = t.data.content
  } catch (e) {
    console.error('Failed to load dashboard data:', e)
  }
}

async function simulate() {
  simulating.value = true
  try {
    await simulateEvent()
    await loadData()
  } finally {
    simulating.value = false
  }
}

onMounted(loadData)
</script>

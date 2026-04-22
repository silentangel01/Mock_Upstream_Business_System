<template>
  <el-card>
    <!-- Filters -->
    <el-form inline style="margin-bottom:16px">
      <el-form-item label="Status">
        <el-select v-model="query.status" clearable placeholder="All" style="width:140px">
          <el-option v-for="(label, key) in STATUS_LABEL" :key="key" :label="label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item label="Event Type">
        <el-select v-model="query.eventType" clearable placeholder="All" style="width:160px">
          <el-option v-for="(label, key) in EVENT_TYPE_LABEL" :key="key" :label="label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item label="Team">
        <el-select v-model="query.assignedTeam" clearable placeholder="All" style="width:140px">
          <el-option v-for="(label, key) in TEAM_LABEL" :key="key" :label="label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">Search</el-button>
      </el-form-item>
    </el-form>

    <!-- Table -->
    <el-table :data="tickets" stripe style="width:100%" @row-click="(row: Ticket) => router.push(`/tickets/${row.id}`)">
      <el-table-column prop="eventType" label="Event Type" width="160">
        <template #default="{ row }">{{ EVENT_TYPE_LABEL[row.eventType] || row.eventType }}</template>
      </el-table-column>
      <el-table-column prop="status" label="Status" width="100">
        <template #default="{ row }">
          <el-tag :type="STATUS_TYPE[row.status] as any" size="small">{{ STATUS_LABEL[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="assignedTeam" label="Team" width="120">
        <template #default="{ row }">{{ TEAM_LABEL[row.assignedTeam] || row.assignedTeam || '-' }}</template>
      </el-table-column>
      <el-table-column prop="confidence" label="Confidence" width="90">
        <template #default="{ row }">{{ (row.confidence * 100).toFixed(0) }}%</template>
      </el-table-column>
      <el-table-column prop="location" label="Location" min-width="120">
        <template #default="{ row }">{{ row.location || '-' }}</template>
      </el-table-column>
      <el-table-column label="Photos" width="80" align="center">
        <template #default="{ row }">
          <span v-if="row.handlePhotos?.length" style="color:#67c23a;font-weight:600">{{ row.handlePhotos.length }}</span>
          <span v-else style="color:#c0c4cc">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="Created" width="180">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
    </el-table>

    <!-- Pagination -->
    <div style="display:flex;justify-content:flex-end;margin-top:16px">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadData"
      />
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listTickets } from '@shared/api/tickets'
import { STATUS_LABEL, STATUS_TYPE, EVENT_TYPE_LABEL, TEAM_LABEL } from '@shared/utils/constants'
import { formatDate } from '@shared/utils/format'
import type { Ticket } from '@shared/types'

const router = useRouter()
const tickets = ref<Ticket[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 20

const query = reactive({ status: '', eventType: '', assignedTeam: '' })

async function loadData() {
  const params: any = { page: currentPage.value - 1, size: pageSize }
  if (query.status) params.status = query.status
  if (query.eventType) params.eventType = query.eventType
  if (query.assignedTeam) params.assignedTeam = query.assignedTeam
  const { data } = await listTickets(params)
  tickets.value = data.content
  total.value = data.totalElements
}

function search() {
  currentPage.value = 1
  loadData()
}

onMounted(loadData)
</script>

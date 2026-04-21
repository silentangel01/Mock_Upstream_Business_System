<template>
  <div>
    <van-nav-bar title="My Tasks" />
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list v-model:loading="loadingMore" :finished="finished" finished-text="No more" @load="loadMore">
        <van-cell v-for="t in tickets" :key="t.id" :to="`/tasks/${t.id}`" is-link>
          <template #title>
            <span>{{ EVENT_TYPE_LABEL[t.eventType] || t.eventType }}</span>
            <van-tag :type="VANT_STATUS_TYPE[t.status]" style="margin-left:8px">{{ STATUS_LABEL[t.status] }}</van-tag>
          </template>
          <template #label>
            <span>{{ t.location || t.cameraId }} · {{ formatDate(t.createdAt) }}</span>
          </template>
        </van-cell>
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { listTickets } from '@shared/api/tickets'
import { STATUS_LABEL, EVENT_TYPE_LABEL } from '@shared/utils/constants'
import { formatDate } from '@shared/utils/format'
import type { Ticket } from '@shared/types'

const VANT_STATUS_TYPE: Record<string, string> = {
  PENDING: 'default', DISPATCHED: 'warning', ACCEPTED: 'primary',
  IN_PROGRESS: 'primary', RESOLVED: 'success', CLOSED: 'default', RETURNED: 'danger',
}

const tickets = ref<Ticket[]>([])
const refreshing = ref(false)
const loadingMore = ref(false)
const finished = ref(false)
let page = 0

async function loadMore() {
  const { data } = await listTickets({ page, size: 20 })
  tickets.value.push(...data.content)
  page++
  loadingMore.value = false
  if (page >= data.totalPages) finished.value = true
}

async function onRefresh() {
  page = 0
  tickets.value = []
  finished.value = false
  await loadMore()
  refreshing.value = false
}
</script>

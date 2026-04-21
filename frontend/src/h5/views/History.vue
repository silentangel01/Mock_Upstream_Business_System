<template>
  <div>
    <van-nav-bar title="History" />
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list v-model:loading="loadingMore" :finished="finished" finished-text="No more" @load="loadMore">
        <van-cell v-for="t in tickets" :key="t.id" :to="`/tasks/${t.id}`" is-link>
          <template #title>
            <span>{{ EVENT_TYPE_LABEL[t.eventType] || t.eventType }}</span>
            <van-tag :type="t.status === 'CLOSED' ? 'default' : 'success'" style="margin-left:8px">
              {{ STATUS_LABEL[t.status] }}
            </van-tag>
          </template>
          <template #label>
            <span>{{ t.location || t.cameraId }} · {{ formatDate(t.resolvedAt || t.closedAt || t.createdAt) }}</span>
          </template>
        </van-cell>
        <van-empty v-if="!loadingMore && !tickets.length" description="No history" />
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

const tickets = ref<Ticket[]>([])
const refreshing = ref(false)
const loadingMore = ref(false)
const finished = ref(false)
let page = 0

async function loadMore() {
  // Load RESOLVED and CLOSED tickets
  const [r1, r2] = await Promise.all([
    listTickets({ status: 'RESOLVED' as any, page, size: 10 }),
    listTickets({ status: 'CLOSED' as any, page, size: 10 }),
  ])
  const combined = [...r1.data.content, ...r2.data.content]
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
  tickets.value.push(...combined)
  page++
  loadingMore.value = false
  if (r1.data.content.length === 0 && r2.data.content.length === 0) finished.value = true
}

async function onRefresh() {
  page = 0
  tickets.value = []
  finished.value = false
  await loadMore()
  refreshing.value = false
}
</script>

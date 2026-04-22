<template>
  <div v-if="ticket">
    <van-nav-bar title="Ticket Details" left-arrow @click-left="router.back()" />

    <van-cell-group inset style="margin:12px">
      <van-cell title="Event Type" :value="EVENT_TYPE_LABEL[ticket.eventType] || ticket.eventType" />
      <van-cell title="Status">
        <template #value>
          <van-tag :type="VANT_STATUS_TYPE[ticket.status]">{{ STATUS_LABEL[ticket.status] }}</van-tag>
        </template>
      </van-cell>
      <van-cell title="Team" :value="TEAM_LABEL[ticket.assignedTeam!] || ticket.assignedTeam || '-'" />
      <van-cell title="Location" :value="ticket.location || '-'" />
      <van-cell title="Confidence" :value="`${(ticket.confidence * 100).toFixed(0)}%`" />
      <van-cell title="Created" :value="formatDate(ticket.createdAt)" />
      <van-cell v-if="ticket.description" title="Description" :label="ticket.description" />
    </van-cell-group>

    <!-- Evidence -->
    <van-cell-group v-if="ticket.imageUrl" inset title="Evidence" style="margin:12px">
      <div style="padding:12px">
        <van-image :src="ticket.imageUrl" width="100%" fit="contain" @click="previewEvidence" />
      </div>
    </van-cell-group>

    <!-- Handle Photos -->
    <van-cell-group v-if="ticket.handlePhotos.length" inset title="Handling Photos" style="margin:12px">
      <div style="padding:12px;display:flex;gap:8px;flex-wrap:wrap">
        <div v-for="(url, i) in ticket.handlePhotos" :key="i" style="position:relative">
          <van-image :src="url" width="80" height="80" fit="cover" @click="previewPhotos(i)" />
          <van-icon v-if="canHandle" name="cross" class="photo-delete-btn"
            @click.stop="confirmDeletePhoto(url)" />
        </div>
      </div>
    </van-cell-group>

    <!-- Timeline -->
    <van-cell-group inset title="Timeline" style="margin:12px">
      <van-steps direction="vertical" :active="ticket.timeline.length - 1" active-color="#07c160">
        <van-step v-for="(entry, i) in ticket.timeline" :key="i">
          <p>{{ entry.action }} — {{ entry.actor }}</p>
          <p v-if="entry.note" style="color:#999;font-size:12px">{{ entry.note }}</p>
          <p style="color:#999;font-size:12px">{{ formatDate(entry.timestamp) }}</p>
        </van-step>
      </van-steps>
    </van-cell-group>

    <!-- Actions -->
    <div style="padding:12px 16px;display:flex;gap:8px;flex-wrap:wrap">
      <van-button v-for="s in allowedTransitions" :key="s" size="small"
        :type="s === 'CLOSED' || s === 'RETURNED' ? 'danger' : 'primary'" @click="doTransition(s)">
        {{ STATUS_LABEL[s] }}
      </van-button>
      <van-button v-if="canHandle" type="success" size="small" :to="`/tasks/${ticket.id}/handle`">
        Report
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showImagePreview, showDialog, showToast } from 'vant'
import { getTicket, updateTicketStatus, deletePhoto } from '@shared/api/tickets'
import { STATUS_LABEL, EVENT_TYPE_LABEL, TEAM_LABEL, ALLOWED_TRANSITIONS } from '@shared/utils/constants'
import { formatDate } from '@shared/utils/format'
import type { Ticket } from '@shared/types'
import { TicketStatus } from '@shared/types'

const VANT_STATUS_TYPE: Record<string, string> = {
  PENDING: 'default', DISPATCHED: 'warning', ACCEPTED: 'primary',
  IN_PROGRESS: 'primary', RESOLVED: 'success', CLOSED: 'default', RETURNED: 'danger',
}

const route = useRoute()
const router = useRouter()
const ticket = ref<Ticket | null>(null)

const allowedTransitions = computed(() =>
  ticket.value ? (ALLOWED_TRANSITIONS[ticket.value.status as TicketStatus] || []).filter(s => s !== 'RESOLVED') : []
)
const canHandle = computed(() => {
  const s = ticket.value?.status
  return s === TicketStatus.ACCEPTED || s === TicketStatus.IN_PROGRESS
})

function previewEvidence() {
  if (ticket.value?.imageUrl) showImagePreview([ticket.value.imageUrl])
}
function previewPhotos(index: number) {
  showImagePreview({ images: ticket.value!.handlePhotos, startPosition: index })
}

async function doTransition(status: string) {
  try {
    await showDialog({ title: 'Confirm', message: `Update status to ${STATUS_LABEL[status]}?` })
    const { data } = await updateTicketStatus(ticket.value!.id, status)
    ticket.value = data
    showToast('Updated')
  } catch { /* cancelled */ }
}

async function confirmDeletePhoto(url: string) {
  try {
    await showDialog({ title: 'Delete Photo', message: 'Are you sure you want to delete this photo?' })
    await deletePhoto(ticket.value!.id, url)
    const { data } = await getTicket(route.params.id as string)
    ticket.value = data
    showToast('Photo deleted')
  } catch { /* cancelled */ }
}

onMounted(async () => {
  const { data } = await getTicket(route.params.id as string)
  ticket.value = data
})
</script>

<style scoped>
.photo-delete-btn {
  position: absolute;
  top: -6px;
  right: -6px;
  background: #ff1744;
  color: #fff;
  border-radius: 50%;
  font-size: 12px;
  padding: 2px;
}
</style>

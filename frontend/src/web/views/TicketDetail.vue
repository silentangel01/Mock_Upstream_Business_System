<template>
  <div v-if="ticket">
    <el-page-header @back="router.back()" style="margin-bottom:20px">
      <template #content>
        <span>Ticket Details</span>
        <el-tag :type="STATUS_TYPE[ticket.status] as any" style="margin-left:12px">{{ STATUS_LABEL[ticket.status] }}</el-tag>
      </template>
    </el-page-header>

    <el-row :gutter="16">
      <!-- Left: Info + Evidence -->
      <el-col :span="14">
        <el-card header="Basic Info" style="margin-bottom:16px">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="Event Type">{{ EVENT_TYPE_LABEL[ticket.eventType] || ticket.eventType }}</el-descriptions-item>
            <el-descriptions-item label="Confidence">{{ (ticket.confidence * 100).toFixed(0) }}%</el-descriptions-item>
            <el-descriptions-item label="Team">{{ TEAM_LABEL[ticket.assignedTeam!] || ticket.assignedTeam || '-' }}</el-descriptions-item>
            <el-descriptions-item label="Assignee">{{ ticket.assignedUser || '-' }}</el-descriptions-item>
            <el-descriptions-item label="Camera">{{ ticket.cameraId }}</el-descriptions-item>
            <el-descriptions-item label="Location">{{ ticket.location || '-' }}</el-descriptions-item>
            <el-descriptions-item label="Area Code">{{ ticket.areaCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="Created" :span="2">{{ formatDate(ticket.createdAt) }}</el-descriptions-item>
            <el-descriptions-item v-if="ticket.description" label="Description" :span="2">{{ ticket.description }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- Evidence Image -->
        <el-card v-if="ticket.imageUrl" header="Evidence" style="margin-bottom:16px">
          <el-image :src="ticket.imageUrl" fit="contain" style="max-height:400px;width:100%" :preview-src-list="[ticket.imageUrl]" />
        </el-card>

        <!-- Handle Photos -->
        <el-card v-if="ticket.handlePhotos.length" header="Handling Photos" style="margin-bottom:16px">
          <div style="display:flex;gap:8px;flex-wrap:wrap">
            <div v-for="(url, i) in ticket.handlePhotos" :key="i" style="position:relative">
              <el-image :src="url" fit="cover"
                style="width:150px;height:150px;border-radius:4px" :preview-src-list="ticket.handlePhotos" :initial-index="i" />
              <el-button v-if="canDeletePhotos" type="danger" circle size="small"
                style="position:absolute;top:-8px;right:-8px;width:22px;height:22px;font-size:12px"
                @click="doDeletePhoto(url)">X</el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- Right: Timeline + Actions -->
      <el-col :span="10">
        <!-- Actions -->
        <el-card header="Actions" style="margin-bottom:16px">
          <div style="display:flex;flex-wrap:wrap;gap:8px;margin-bottom:12px">
            <el-button v-for="s in allowedTransitions" :key="s" :type="s === 'CLOSED' || s === 'RETURNED' ? 'danger' : 'primary'"
              size="small" @click="openStatusDialog(s)">
              {{ STATUS_LABEL[s] }}
            </el-button>
            <el-button v-if="canReassign" size="small" @click="reassignVisible = true">Reassign</el-button>
          </div>
          <!-- Photo Upload -->
          <el-upload :action="`/api/tickets/${ticket.id}/photos`" :headers="{ Authorization: `Bearer ${auth.token}` }"
            :on-success="onPhotoUploaded" :show-file-list="false" accept="image/*">
            <el-button size="small">Upload Photos</el-button>
          </el-upload>
        </el-card>

        <!-- Timeline -->
        <el-card header="Timeline">
          <el-timeline>
            <el-timeline-item v-for="(entry, i) in ticket.timeline" :key="i" :timestamp="formatDate(entry.timestamp)" placement="top">
              <strong>{{ entry.action }}</strong> — {{ entry.actor }}
              <div v-if="entry.note" style="color:#666;margin-top:4px">{{ entry.note }}</div>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>

    <!-- Status Update Dialog -->
    <el-dialog v-model="statusDialogVisible" title="Update Status" width="400px">
      <p>Confirm status update to: <el-tag>{{ STATUS_LABEL[targetStatus] }}</el-tag></p>
      <el-input v-model="statusNote" type="textarea" placeholder="Note (optional)" :rows="3" />
      <template #footer>
        <el-button @click="statusDialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="updating" @click="doUpdateStatus">Confirm</el-button>
      </template>
    </el-dialog>

    <!-- Reassign Dialog -->
    <el-dialog v-model="reassignVisible" title="Reassign" width="400px" @open="loadFieldworkers">
      <el-form>
        <el-form-item label="Assignee">
          <el-select v-model="reassignUser" style="width:100%" filterable placeholder="Select person">
            <el-option v-for="fw in availableFieldworkers" :key="fw.username"
              :label="`${fw.displayName || fw.username} (${fw.team || '-'})`" :value="fw.username" />
          </el-select>
        </el-form-item>
        <el-form-item label="Note">
          <el-input v-model="reassignNote" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reassignVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="updating" @click="doReassign">Confirm</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTicket, updateTicketStatus, reassignTicket, listFieldworkers, deletePhoto } from '@shared/api/tickets'
import { useAuthStore } from '@shared/stores/auth'
import { STATUS_LABEL, STATUS_TYPE, EVENT_TYPE_LABEL, TEAM_LABEL, ALLOWED_TRANSITIONS } from '@shared/utils/constants'
import { formatDate } from '@shared/utils/format'
import type { Ticket, Fieldworker } from '@shared/types'
import { TicketStatus } from '@shared/types'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const ticket = ref<Ticket | null>(null)
const updating = ref(false)

const allowedTransitions = computed(() =>
  ticket.value ? ALLOWED_TRANSITIONS[ticket.value.status as TicketStatus] || [] : []
)
const canReassign = computed(() => auth.isAdmin || auth.isDispatcher)
const canDeletePhotos = computed(() => {
  const s = ticket.value?.status
  return s === TicketStatus.ACCEPTED || s === TicketStatus.IN_PROGRESS
})

// Status dialog
const statusDialogVisible = ref(false)
const targetStatus = ref('')
const statusNote = ref('')

function openStatusDialog(status: string) {
  targetStatus.value = status
  statusNote.value = ''
  statusDialogVisible.value = true
}

async function doUpdateStatus() {
  updating.value = true
  try {
    const { data } = await updateTicketStatus(ticket.value!.id, targetStatus.value, statusNote.value || undefined)
    ticket.value = data
    statusDialogVisible.value = false
    ElMessage.success('Status updated')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || 'Operation failed')
  } finally {
    updating.value = false
  }
}

// Reassign dialog
const reassignVisible = ref(false)
const reassignUser = ref('')
const reassignNote = ref('')
const fieldworkers = ref<Fieldworker[]>([])
const availableFieldworkers = computed(() =>
  fieldworkers.value.filter(fw => fw.username !== ticket.value?.assignedUser)
)

async function loadFieldworkers() {
  try {
    const { data } = await listFieldworkers()
    fieldworkers.value = data
  } catch (e: any) {
    ElMessage.error('Failed to load personnel')
  }
}

async function doReassign() {
  if (!reassignUser.value) return
  updating.value = true
  try {
    const { data } = await reassignTicket(ticket.value!.id, reassignUser.value, reassignNote.value || undefined)
    ticket.value = data
    reassignVisible.value = false
    ElMessage.success('Reassigned')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || 'Operation failed')
  } finally {
    updating.value = false
  }
}

function onPhotoUploaded() {
  loadTicket()
  ElMessage.success('Photo uploaded')
}

async function doDeletePhoto(url: string) {
  try {
    await ElMessageBox.confirm('Are you sure you want to delete this photo?', 'Delete Photo', { type: 'warning' })
    await deletePhoto(ticket.value!.id, url)
    await loadTicket()
    ElMessage.success('Photo deleted')
  } catch { /* cancelled */ }
}

async function loadTicket() {
  const { data } = await getTicket(route.params.id as string)
  ticket.value = data
}

onMounted(loadTicket)
</script>

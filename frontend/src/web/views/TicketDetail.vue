<template>
  <div v-if="ticket">
    <el-page-header @back="router.back()" style="margin-bottom:20px">
      <template #content>
        <span>工单详情</span>
        <el-tag :type="STATUS_TYPE[ticket.status] as any" style="margin-left:12px">{{ STATUS_LABEL[ticket.status] }}</el-tag>
      </template>
    </el-page-header>

    <el-row :gutter="16">
      <!-- Left: Info + Evidence -->
      <el-col :span="14">
        <el-card header="基本信息" style="margin-bottom:16px">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="事件类型">{{ EVENT_TYPE_LABEL[ticket.eventType] || ticket.eventType }}</el-descriptions-item>
            <el-descriptions-item label="置信度">{{ (ticket.confidence * 100).toFixed(0) }}%</el-descriptions-item>
            <el-descriptions-item label="负责团队">{{ TEAM_LABEL[ticket.assignedTeam!] || ticket.assignedTeam || '-' }}</el-descriptions-item>
            <el-descriptions-item label="负责人">{{ ticket.assignedUser || '-' }}</el-descriptions-item>
            <el-descriptions-item label="摄像头">{{ ticket.cameraId }}</el-descriptions-item>
            <el-descriptions-item label="位置">{{ ticket.location || '-' }}</el-descriptions-item>
            <el-descriptions-item label="区域编码">{{ ticket.areaCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间" :span="2">{{ formatDate(ticket.createdAt) }}</el-descriptions-item>
            <el-descriptions-item v-if="ticket.description" label="描述" :span="2">{{ ticket.description }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- Evidence Image -->
        <el-card v-if="ticket.imageUrl" header="证据图片" style="margin-bottom:16px">
          <el-image :src="ticket.imageUrl" fit="contain" style="max-height:400px;width:100%" :preview-src-list="[ticket.imageUrl]" />
        </el-card>

        <!-- Handle Photos -->
        <el-card v-if="ticket.handlePhotos.length" header="处置照片" style="margin-bottom:16px">
          <div style="display:flex;gap:8px;flex-wrap:wrap">
            <el-image v-for="(url, i) in ticket.handlePhotos" :key="i" :src="url" fit="cover"
              style="width:150px;height:150px;border-radius:4px" :preview-src-list="ticket.handlePhotos" :initial-index="i" />
          </div>
        </el-card>
      </el-col>

      <!-- Right: Timeline + Actions -->
      <el-col :span="10">
        <!-- Actions -->
        <el-card header="操作" style="margin-bottom:16px">
          <div style="display:flex;flex-wrap:wrap;gap:8px;margin-bottom:12px">
            <el-button v-for="s in allowedTransitions" :key="s" :type="s === 'CLOSED' || s === 'RETURNED' ? 'danger' : 'primary'"
              size="small" @click="openStatusDialog(s)">
              {{ STATUS_LABEL[s] }}
            </el-button>
            <el-button v-if="canReassign" size="small" @click="reassignVisible = true">重新派遣</el-button>
          </div>
          <!-- Photo Upload -->
          <el-upload :action="`/api/tickets/${ticket.id}/photos`" :headers="{ Authorization: `Bearer ${auth.token}` }"
            :on-success="onPhotoUploaded" :show-file-list="false" accept="image/*">
            <el-button size="small">上传处置照片</el-button>
          </el-upload>
        </el-card>

        <!-- Timeline -->
        <el-card header="时间线">
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
    <el-dialog v-model="statusDialogVisible" title="更新状态" width="400px">
      <p>确认将状态更新为: <el-tag>{{ STATUS_LABEL[targetStatus] }}</el-tag></p>
      <el-input v-model="statusNote" type="textarea" placeholder="备注（可选）" :rows="3" />
      <template #footer>
        <el-button @click="statusDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="updating" @click="doUpdateStatus">确认</el-button>
      </template>
    </el-dialog>

    <!-- Reassign Dialog -->
    <el-dialog v-model="reassignVisible" title="重新派遣" width="400px" @open="loadFieldworkers">
      <el-form>
        <el-form-item label="目标人员">
          <el-select v-model="reassignUser" style="width:100%" filterable placeholder="选择人员">
            <el-option v-for="fw in availableFieldworkers" :key="fw.username"
              :label="`${fw.displayName || fw.username} (${fw.team || '-'})`" :value="fw.username" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="reassignNote" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reassignVisible = false">取消</el-button>
        <el-button type="primary" :loading="updating" @click="doReassign">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTicket, updateTicketStatus, reassignTicket, listFieldworkers } from '@shared/api/tickets'
import { useAuthStore } from '@shared/stores/auth'
import { STATUS_LABEL, STATUS_TYPE, EVENT_TYPE_LABEL, TEAM_LABEL, ALLOWED_TRANSITIONS } from '@shared/utils/constants'
import { formatDate } from '@shared/utils/format'
import type { Ticket, Fieldworker } from '@shared/types'
import { TicketStatus } from '@shared/types'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const ticket = ref<Ticket | null>(null)
const updating = ref(false)

const allowedTransitions = computed(() =>
  ticket.value ? ALLOWED_TRANSITIONS[ticket.value.status as TicketStatus] || [] : []
)
const canReassign = computed(() => auth.isAdmin || auth.isDispatcher)

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
    ElMessage.success('状态已更新')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
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
    ElMessage.error('加载人员列表失败')
  }
}

async function doReassign() {
  if (!reassignUser.value) return
  updating.value = true
  try {
    const { data } = await reassignTicket(ticket.value!.id, reassignUser.value, reassignNote.value || undefined)
    ticket.value = data
    reassignVisible.value = false
    ElMessage.success('已重新派遣')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    updating.value = false
  }
}

function onPhotoUploaded() {
  loadTicket()
  ElMessage.success('照片已上传')
}

async function loadTicket() {
  const { data } = await getTicket(route.params.id as string)
  ticket.value = data
}

onMounted(loadTicket)
</script>

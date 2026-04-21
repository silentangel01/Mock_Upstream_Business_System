<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span>Dispatch Rules</span>
        <el-button type="primary" size="small" @click="openDialog()">Add Rule</el-button>
      </div>
    </template>

    <el-table :data="rules" stripe>
      <el-table-column prop="eventType" label="Event Type" width="180">
        <template #default="{ row }">{{ EVENT_TYPE_LABEL[row.eventType] || row.eventType }}</template>
      </el-table-column>
      <el-table-column prop="areaCode" label="Area Code" width="120" />
      <el-table-column prop="targetTeam" label="Target Team" width="140">
        <template #default="{ row }">{{ TEAM_LABEL[row.targetTeam] || row.targetTeam }}</template>
      </el-table-column>
      <el-table-column prop="priority" label="Priority" width="80" />
      <el-table-column prop="enabled" label="Enabled" width="80">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? 'Yes' : 'No' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Actions" width="160">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">Edit</el-button>
          <el-popconfirm title="Confirm delete?" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button size="small" type="danger">Delete</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- Add/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="editingId ? 'Edit Rule' : 'Add Rule'" width="480px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="Event Type">
          <el-select v-model="form.eventType" style="width:100%">
            <el-option v-for="(label, key) in EVENT_TYPE_LABEL" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="Area Code">
          <el-input v-model="form.areaCode" placeholder="* for all areas" />
        </el-form-item>
        <el-form-item label="Target Team">
          <el-select v-model="form.targetTeam" style="width:100%">
            <el-option v-for="(label, key) in TEAM_LABEL" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="Priority">
          <el-input-number v-model="form.priority" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="Enabled">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">Save</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { listRules, createRule, updateRule, deleteRule } from '@shared/api/dispatch'
import { EVENT_TYPE_LABEL, TEAM_LABEL } from '@shared/utils/constants'
import type { DispatchRule } from '@shared/types'
import { ElMessage } from 'element-plus'

const rules = ref<DispatchRule[]>([])
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref('')

const form = reactive({
  eventType: '',
  areaCode: '*',
  targetTeam: '',
  priority: 1,
  enabled: true,
})

function openDialog(rule?: DispatchRule) {
  if (rule) {
    editingId.value = rule.id
    Object.assign(form, { eventType: rule.eventType, areaCode: rule.areaCode, targetTeam: rule.targetTeam, priority: rule.priority, enabled: rule.enabled })
  } else {
    editingId.value = ''
    Object.assign(form, { eventType: '', areaCode: '*', targetTeam: '', priority: 1, enabled: true })
  }
  dialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    if (editingId.value) {
      await updateRule(editingId.value, { ...form })
    } else {
      await createRule({ ...form })
    }
    dialogVisible.value = false
    ElMessage.success('Saved')
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || 'Save failed')
  } finally {
    saving.value = false
  }
}

async function handleDelete(id: string) {
  await deleteRule(id)
  ElMessage.success('Deleted')
  await loadData()
}

async function loadData() {
  const { data } = await listRules()
  rules.value = data
}

onMounted(loadData)
</script>

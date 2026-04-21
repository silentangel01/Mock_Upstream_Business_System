<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span>Users</span>
        <el-button type="primary" size="small" @click="openCreateDialog">Add User</el-button>
      </div>
    </template>

    <el-table :data="users" stripe>
      <el-table-column prop="username" label="Username" width="120" />
      <el-table-column prop="displayName" label="Display Name" width="120" />
      <el-table-column prop="role" label="Role" width="120">
        <template #default="{ row }">
          <el-tag :type="ROLE_TAG_TYPE[row.role] || 'info'" size="small">{{ ROLE_LABEL[row.role] || row.role }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="team" label="Team" width="120">
        <template #default="{ row }">{{ TEAM_LABEL[row.team] || row.team || '-' }}</template>
      </el-table-column>
      <el-table-column prop="email" label="Email" min-width="180" />
      <el-table-column prop="phone" label="Phone" width="140" />
      <el-table-column prop="enabled" label="Status" width="80">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">{{ row.enabled ? 'Active' : 'Disabled' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Actions" width="240">
        <template #default="{ row }">
          <el-button size="small" @click="openEditDialog(row)">Edit</el-button>
          <el-button size="small" type="warning" @click="openPwdDialog(row)">Reset Pwd</el-button>
          <el-popconfirm title="Confirm delete?" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button size="small" type="danger">Delete</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="editingId ? 'Edit User' : 'Add User'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="Username">
          <el-input v-model="form.username" :disabled="!!editingId" />
        </el-form-item>
        <el-form-item v-if="!editingId" label="Password">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="Display Name">
          <el-input v-model="form.displayName" />
        </el-form-item>
        <el-form-item label="Role">
          <el-select v-model="form.role" style="width:100%">
            <el-option v-for="(label, key) in ROLE_LABEL" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="Team">
          <el-select v-model="form.team" clearable style="width:100%">
            <el-option v-for="(label, key) in TEAM_LABEL" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="Email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="Phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item v-if="editingId" label="Enabled">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">Save</el-button>
      </template>
    </el-dialog>

    <!-- Reset Password Dialog -->
    <el-dialog v-model="pwdDialogVisible" title="Reset Password" width="400px">
      <el-form label-width="100px">
        <el-form-item label="New Password">
          <el-input v-model="newPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="saving" @click="handleResetPassword">Confirm</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { listUsers, createUser, updateUser, resetPassword, deleteUser } from '@shared/api/users'
import { TEAM_LABEL } from '@shared/utils/constants'
import type { User } from '@shared/types'
import { ElMessage } from 'element-plus'

const ROLE_LABEL: Record<string, string> = {
  ADMIN: 'Admin',
  DISPATCHER: 'Dispatcher',
  FIELDWORKER: 'Fieldworker',
}

const ROLE_TAG_TYPE: Record<string, string> = {
  ADMIN: 'danger',
  DISPATCHER: 'warning',
  FIELDWORKER: '',
}

const users = ref<User[]>([])
const dialogVisible = ref(false)
const pwdDialogVisible = ref(false)
const saving = ref(false)
const editingId = ref('')
const pwdUserId = ref('')
const newPassword = ref('')

const form = reactive({
  username: '',
  password: '',
  displayName: '',
  role: 'FIELDWORKER',
  team: '',
  email: '',
  phone: '',
  enabled: true,
})

function openCreateDialog() {
  editingId.value = ''
  Object.assign(form, { username: '', password: '', displayName: '', role: 'FIELDWORKER', team: '', email: '', phone: '', enabled: true })
  dialogVisible.value = true
}

function openEditDialog(user: User) {
  editingId.value = user.id
  Object.assign(form, {
    username: user.username,
    password: '',
    displayName: user.displayName || '',
    role: user.role,
    team: user.team || '',
    email: user.email || '',
    phone: user.phone || '',
    enabled: user.enabled,
  })
  dialogVisible.value = true
}

function openPwdDialog(user: User) {
  pwdUserId.value = user.id
  newPassword.value = ''
  pwdDialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    if (editingId.value) {
      await updateUser(editingId.value, {
        email: form.email || undefined,
        phone: form.phone || undefined,
        displayName: form.displayName || undefined,
        team: form.team || undefined,
        role: form.role as any,
        enabled: form.enabled,
      })
    } else {
      await createUser({
        username: form.username,
        password: form.password,
        role: form.role as any,
        team: form.team || undefined,
        email: form.email || undefined,
        phone: form.phone || undefined,
        displayName: form.displayName || undefined,
      })
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

async function handleResetPassword() {
  if (!newPassword.value) {
    ElMessage.warning('Please enter a new password')
    return
  }
  saving.value = true
  try {
    await resetPassword(pwdUserId.value, newPassword.value)
    pwdDialogVisible.value = false
    ElMessage.success('Password reset')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || 'Reset failed')
  } finally {
    saving.value = false
  }
}

async function handleDelete(id: string) {
  await deleteUser(id)
  ElMessage.success('Deleted')
  await loadData()
}

async function loadData() {
  const { data } = await listUsers()
  users.value = data
}

onMounted(loadData)
</script>
<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span>用户管理</span>
        <el-button type="primary" size="small" @click="openCreateDialog">新增用户</el-button>
      </div>
    </template>

    <el-table :data="users" stripe>
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="displayName" label="显示名" width="120" />
      <el-table-column prop="role" label="角色" width="120">
        <template #default="{ row }">
          <el-tag :type="ROLE_TAG_TYPE[row.role] || 'info'" size="small">{{ ROLE_LABEL[row.role] || row.role }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="team" label="团队" width="120">
        <template #default="{ row }">{{ TEAM_LABEL[row.team] || row.team || '-' }}</template>
      </el-table-column>
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column prop="enabled" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
          <el-button size="small" type="warning" @click="openPwdDialog(row)">重置密码</el-button>
          <el-popconfirm title="确认删除该用户?" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑用户' : '新增用户'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" :disabled="!!editingId" />
        </el-form-item>
        <el-form-item v-if="!editingId" label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="显示名">
          <el-input v-model="form.displayName" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width:100%">
            <el-option v-for="(label, key) in ROLE_LABEL" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="团队">
          <el-select v-model="form.team" clearable style="width:100%">
            <el-option v-for="(label, key) in TEAM_LABEL" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item v-if="editingId" label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- Reset Password Dialog -->
    <el-dialog v-model="pwdDialogVisible" title="重置密码" width="400px">
      <el-form label-width="80px">
        <el-form-item label="新密码">
          <el-input v-model="newPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleResetPassword">确认</el-button>
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
  ADMIN: '管理员',
  DISPATCHER: '调度员',
  FIELDWORKER: '外勤人员',
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
    ElMessage.success('保存成功')
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleResetPassword() {
  if (!newPassword.value) {
    ElMessage.warning('请输入新密码')
    return
  }
  saving.value = true
  try {
    await resetPassword(pwdUserId.value, newPassword.value)
    pwdDialogVisible.value = false
    ElMessage.success('密码已重置')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '重置失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(id: string) {
  await deleteUser(id)
  ElMessage.success('已删除')
  await loadData()
}

async function loadData() {
  const { data } = await listUsers()
  users.value = data
}

onMounted(loadData)
</script>
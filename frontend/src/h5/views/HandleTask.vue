<template>
  <div>
    <van-nav-bar title="处置上报" left-arrow @click-left="router.back()" />

    <van-cell-group inset style="margin:12px">
      <van-field label="拍照上传">
        <template #input>
          <van-uploader v-model="fileList" :after-read="afterRead" :max-count="5" accept="image/*" />
        </template>
      </van-field>
      <van-field v-model="note" type="textarea" label="处置备注" placeholder="请输入处置说明" rows="3" />
    </van-cell-group>

    <div style="padding:16px">
      <van-button type="primary" block :loading="submitting" @click="submit">提交处置</van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { uploadPhoto, updateTicketStatus } from '@shared/api/tickets'
import type { UploaderFileListItem } from 'vant'

const route = useRoute()
const router = useRouter()
const ticketId = route.params.id as string
const fileList = ref<UploaderFileListItem[]>([])
const note = ref('')
const submitting = ref(false)

function afterRead(file: any) {
  // file is already added to fileList by v-model
}

async function submit() {
  if (!fileList.value.length) {
    showToast('请至少上传一张照片')
    return
  }
  submitting.value = true
  try {
    // Upload all photos
    for (const item of fileList.value) {
      if (item.file) {
        await uploadPhoto(ticketId, item.file)
      }
    }
    // Transition to RESOLVED
    await updateTicketStatus(ticketId, 'RESOLVED', note.value || undefined)
    showToast('处置已提交')
    router.replace(`/tasks/${ticketId}`)
  } catch (e: any) {
    showToast(e.response?.data?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

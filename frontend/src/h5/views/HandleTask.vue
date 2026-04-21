<template>
  <div>
    <van-nav-bar title="Report" left-arrow @click-left="router.back()" />

    <van-cell-group inset style="margin:12px">
      <van-field label="Photos">
        <template #input>
          <van-uploader v-model="fileList" :after-read="afterRead" :max-count="5" accept="image/*" />
        </template>
      </van-field>
      <van-field v-model="note" type="textarea" label="Note" placeholder="Enter handling notes" rows="3" />
    </van-cell-group>

    <div style="padding:16px">
      <van-button type="primary" block :loading="submitting" @click="submit">Submit</van-button>
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
    showToast('Please upload at least one photo')
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
    showToast('Submitted')
    router.replace(`/tasks/${ticketId}`)
  } catch (e: any) {
    showToast(e.response?.data?.message || 'Submit failed')
  } finally {
    submitting.value = false
  }
}
</script>

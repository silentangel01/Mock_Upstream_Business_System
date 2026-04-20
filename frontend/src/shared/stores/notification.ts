import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface NotificationMessage {
  id: number
  title: string
  body: string
  time: string
}

let nextId = 1

export const useNotificationStore = defineStore('notification', () => {
  const messages = ref<NotificationMessage[]>([])
  const unreadCount = ref(0)

  function add(title: string, body: string) {
    messages.value.unshift({ id: nextId++, title, body, time: new Date().toISOString() })
    unreadCount.value++
  }

  function clearUnread() {
    unreadCount.value = 0
  }

  return { messages, unreadCount, add, clearUnread }
})

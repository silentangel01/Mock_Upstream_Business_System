import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useNotificationStore } from '@shared/stores/notification'

let stompClient: Client | null = null

export function connectWebSocket(token: string, team?: string) {
  if (stompClient?.active) return

  const handler = (msg: any) => {
    const data = JSON.parse(msg.body)
    const store = useNotificationStore()
    store.add(
      `工单更新: ${data.eventType || ''}`,
      data.description || `工单 ${data.id} 状态变更为 ${data.status}`
    )
  }

  stompClient = new Client({
    webSocketFactory: () => new SockJS('/ws') as any,
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 5000,
    onConnect: () => {
      stompClient!.subscribe('/topic/tickets/all', handler)
      if (team) {
        stompClient!.subscribe(`/topic/tickets/${team}`, handler)
      }
    },
  })

  stompClient.activate()
}

export function disconnectWebSocket() {
  stompClient?.deactivate()
  stompClient = null
}

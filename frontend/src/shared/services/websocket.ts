import { Client } from '@stomp/stompjs'
import { useNotificationStore } from '@shared/stores/notification'

let stompClient: Client | null = null

export function connectWebSocket(token: string, team?: string) {
  if (stompClient?.active) return

  try {
    const handler = (msg: any) => {
      try {
        const data = JSON.parse(msg.body)
        const store = useNotificationStore()
        store.add(
          `工单更新: ${data.eventType || ''}`,
          data.description || `工单 ${data.id} 状态变更为 ${data.status}`
        )
      } catch (e) {
        console.warn('[WS] Failed to parse message:', e)
      }
    }

    stompClient = new Client({
      brokerURL: `ws://${window.location.host}/ws/websocket`,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        console.log('[WS] Connected')
        stompClient!.subscribe('/topic/tickets/all', handler)
        if (team) {
          stompClient!.subscribe(`/topic/tickets/${team}`, handler)
        }
      },
      onStompError: (frame) => {
        console.warn('[WS] STOMP error:', frame.headers['message'])
      },
      onWebSocketError: (evt) => {
        console.warn('[WS] WebSocket error:', evt)
      },
    })

    stompClient.activate()
  } catch (e) {
    console.warn('[WS] Failed to connect:', e)
  }
}

export function disconnectWebSocket() {
  try {
    stompClient?.deactivate()
  } catch { /* ignore */ }
  stompClient = null
}

import client from './client'
import type { Ticket } from '@shared/types'

export function simulateEvent(eventType?: string, areaCode?: string, description?: string) {
  return client.post<Ticket>('/demo/simulate-event', { eventType, areaCode, description })
}

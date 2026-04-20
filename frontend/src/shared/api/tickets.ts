import client from './client'
import type { Ticket, Page, TicketQuery, TicketStats } from '@shared/types'

export function listTickets(query: TicketQuery = {}) {
  return client.get<Page<Ticket>>('/tickets', { params: query })
}

export function getTicket(id: string) {
  return client.get<Ticket>(`/tickets/${id}`)
}

export function updateTicketStatus(id: string, status: string, note?: string) {
  return client.patch<Ticket>(`/tickets/${id}/status`, { status, note })
}

export function reassignTicket(id: string, targetTeam: string, note?: string) {
  return client.patch<Ticket>(`/tickets/${id}/reassign`, { targetTeam, note })
}

export function uploadPhoto(id: string, file: File) {
  const form = new FormData()
  form.append('file', file)
  return client.post<{ url: string; filename: string }>(`/tickets/${id}/photos`, form)
}

export function getTicketStats() {
  return client.get<TicketStats>('/tickets/stats')
}

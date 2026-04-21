import { TicketStatus } from '@shared/types'

export const ALLOWED_TRANSITIONS: Record<TicketStatus, TicketStatus[]> = {
  [TicketStatus.PENDING]: [TicketStatus.DISPATCHED, TicketStatus.CLOSED],
  [TicketStatus.DISPATCHED]: [TicketStatus.ACCEPTED, TicketStatus.RETURNED],
  [TicketStatus.ACCEPTED]: [TicketStatus.IN_PROGRESS, TicketStatus.RETURNED],
  [TicketStatus.IN_PROGRESS]: [TicketStatus.RESOLVED, TicketStatus.RETURNED],
  [TicketStatus.RESOLVED]: [TicketStatus.CLOSED],
  [TicketStatus.RETURNED]: [TicketStatus.DISPATCHED, TicketStatus.CLOSED],
  [TicketStatus.CLOSED]: [],
}

export const STATUS_LABEL: Record<string, string> = {
  PENDING: 'Pending',
  DISPATCHED: 'Dispatched',
  ACCEPTED: 'Accepted',
  IN_PROGRESS: 'In Progress',
  RESOLVED: 'Resolved',
  CLOSED: 'Closed',
  RETURNED: 'Returned',
}

export const STATUS_TYPE: Record<string, string> = {
  PENDING: 'info',
  DISPATCHED: 'warning',
  ACCEPTED: '',
  IN_PROGRESS: 'primary',
  RESOLVED: 'success',
  CLOSED: 'info',
  RETURNED: 'danger',
}

export const EVENT_TYPE_LABEL: Record<string, string> = {
  smoke_flame: 'Smoke & Fire',
  parking_violation: 'Parking Violation',
  common_space_utilization: 'Public Space Occupation',
}

export const TEAM_LABEL: Record<string, string> = {
  fire_team: 'Fire Team',
  traffic_team: 'Traffic Team',
  urban_mgmt_team: 'Urban Mgmt Team',
}

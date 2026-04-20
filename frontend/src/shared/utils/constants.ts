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
  PENDING: '待处理',
  DISPATCHED: '已派遣',
  ACCEPTED: '已接单',
  IN_PROGRESS: '处理中',
  RESOLVED: '已解决',
  CLOSED: '已关闭',
  RETURNED: '已退回',
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
  smoke_flame: '烟火检测',
  parking_violation: '违停检测',
  common_space_utilization: '公共空间占用',
}

export const TEAM_LABEL: Record<string, string> = {
  fire_team: '消防队',
  traffic_team: '交通队',
  urban_mgmt_team: '城管队',
}

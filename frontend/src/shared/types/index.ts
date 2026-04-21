export enum TicketStatus {
  PENDING = 'PENDING',
  DISPATCHED = 'DISPATCHED',
  ACCEPTED = 'ACCEPTED',
  IN_PROGRESS = 'IN_PROGRESS',
  RESOLVED = 'RESOLVED',
  CLOSED = 'CLOSED',
  RETURNED = 'RETURNED',
}

export enum UserRole {
  ADMIN = 'ADMIN',
  DISPATCHER = 'DISPATCHER',
  FIELDWORKER = 'FIELDWORKER',
}

export interface TimelineEntry {
  action: string
  actor: string
  timestamp: string
  note?: string
}

export interface Ticket {
  id: string
  hvasEventId: string
  eventType: string
  cameraId: string
  eventTimestamp: number
  createdAt: string
  confidence: number
  imageUrl?: string
  description?: string
  objectCount?: number
  latLng?: string
  location?: string
  areaCode?: string
  group?: string
  status: TicketStatus
  assignedTeam?: string
  assignedUser?: string
  dispatchedAt?: string
  resolvedAt?: string
  closedAt?: string
  timeline: TimelineEntry[]
  handlePhotos: string[]
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface TicketStats {
  total: number
  byStatus: Record<string, number>
  byEventType: Record<string, number>
  byTeam: Record<string, number>
  avgResolutionMinutes?: number
}

export interface DispatchRule {
  id: string
  eventType: string
  areaCode: string
  targetTeam: string
  priority: number
  enabled: boolean
}

export interface DispatchRuleDto {
  eventType: string
  areaCode: string
  targetTeam: string
  priority: number
  enabled: boolean
}

export interface Fieldworker {
  username: string
  displayName?: string
  team?: string
}

export interface LoginResponse {
  token: string
  username: string
  role: string
}

export interface TicketQuery {
  status?: TicketStatus
  eventType?: string
  assignedTeam?: string
  page?: number
  size?: number
}

export interface User {
  id: string
  username: string
  role: UserRole
  team?: string
  email?: string
  phone?: string
  displayName?: string
  enabled: boolean
}

export interface CreateUserDto {
  username: string
  password: string
  role: UserRole
  team?: string
  email?: string
  phone?: string
  displayName?: string
}

export interface UpdateUserDto {
  email?: string
  phone?: string
  displayName?: string
  team?: string
  role?: UserRole
  enabled?: boolean
}

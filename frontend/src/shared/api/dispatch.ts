import client from './client'
import type { DispatchRule, DispatchRuleDto } from '@shared/types'

export function listRules() {
  return client.get<DispatchRule[]>('/dispatch-rules')
}

export function createRule(dto: DispatchRuleDto) {
  return client.post<DispatchRule>('/dispatch-rules', dto)
}

export function updateRule(id: string, dto: DispatchRuleDto) {
  return client.put<DispatchRule>(`/dispatch-rules/${id}`, dto)
}

export function deleteRule(id: string) {
  return client.delete(`/dispatch-rules/${id}`)
}

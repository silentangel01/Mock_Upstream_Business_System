import client from './client'
import type { LoginResponse } from '@shared/types'

export function login(username: string, password: string) {
  return client.post<LoginResponse>('/auth/login', { username, password })
}

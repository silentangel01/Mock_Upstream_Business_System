import client from './client'
import type { User, CreateUserDto, UpdateUserDto } from '@shared/types'

export function listUsers() {
  return client.get<User[]>('/users')
}

export function createUser(dto: CreateUserDto) {
  return client.post<User>('/users', dto)
}

export function updateUser(id: string, dto: UpdateUserDto) {
  return client.put<User>(`/users/${id}`, dto)
}

export function resetPassword(id: string, newPassword: string) {
  return client.put(`/users/${id}/password`, { newPassword })
}

export function deleteUser(id: string) {
  return client.delete(`/users/${id}`)
}

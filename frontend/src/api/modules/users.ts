import http from '@/api/http'
import type { UserSessionView, UserView } from '@/types/user'

export interface LoginPayload {
  email: string
  password: string
}

export interface CreateUserPayload {
  username: string
  email: string
  password: string
  role: string
}

export interface RegisterPayload {
  username: string
  email: string
  password: string
}

export interface UpdateUserRolePayload {
  role: string
  status: string
}

export async function loginUser(payload: LoginPayload) {
  const { data } = await http.post<UserSessionView>('/users/login', payload)
  return data
}

export async function registerUser(payload: RegisterPayload) {
  const { data } = await http.post<UserSessionView>('/users/register', payload)
  return data
}

export async function listUsers() {
  const { data } = await http.get<UserView[]>('/users')
  return data
}

export async function createUser(payload: CreateUserPayload) {
  const { data } = await http.post<UserView>('/users', payload)
  return data
}

export async function updateUserRole(id: number, payload: UpdateUserRolePayload) {
  const { data } = await http.patch<UserView>(`/users/${id}/role`, payload)
  return data
}

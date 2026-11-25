import { login1 as adminLoginApi } from '@/api/adminAuthController'
import { login as studentLoginApi } from '@/api/studentAuthController'

export type LoginPayload = { username: string; password: string }
export type LoginResult = { token: string; userId: number; username: string; name: string; side: 'admin' | 'student' }

export function adminLogin(payload: LoginPayload) {
  return adminLoginApi(payload) as Promise<LoginResult & { side: 'admin' }>
}

export function studentLogin(payload: LoginPayload) {
  return studentLoginApi(payload) as Promise<LoginResult & { side: 'student' }>
}

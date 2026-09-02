// ============================================================
// 认证相关 API
// ============================================================

import { post } from './request'
import type { LoginResult, User } from '@/types'

/** 登录 */
export function login(data: { username: string; password: string }) {
  return post<LoginResult>('/auth/login', data)
}

/** 注册 */
export function register(data: { username: string; password: string; nickname?: string }) {
  return post<void>('/auth/register', data)
}

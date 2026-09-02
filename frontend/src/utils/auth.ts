// ============================================================
// Token 存储工具
// ============================================================

const TOKEN_KEY = 'library_token'

/** 获取 Token */
export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

/** 保存 Token */
export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

/** 移除 Token */
export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

/** 判断是否已登录 */
export function isLoggedIn(): boolean {
  return !!getToken()
}

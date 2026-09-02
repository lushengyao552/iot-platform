// ============================================================
// 用户状态管理（Zustand）
// ============================================================

import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { User } from '@/types'
import { getToken, setToken, removeToken } from '@/utils/auth'

interface UserState {
  token: string | null
  user: User | null
  setAuth: (token: string, user: User) => void
  setUser: (user: User) => void
  logout: () => void
}

export const useUserStore = create<UserState>()(
  persist(
    (set) => ({
      token: getToken(),
      user: null,

      /** 设置认证信息（登录成功后调用） */
      setAuth: (token: string, user: User) => {
        setToken(token)
        set({ token, user })
      },

      /** 更新用户信息 */
      setUser: (user: User) => set({ user }),

      /** 退出登录 */
      logout: () => {
        removeToken()
        set({ token: null, user: null })
      },
    }),
    {
      name: 'library-user-storage',
      // 只持久化 user，token 已经在 localStorage 中单独管理
      partialize: (state) => ({ user: state.user }),
    }
  )
)

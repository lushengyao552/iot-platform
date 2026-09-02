// ============================================================
// 路由配置 + 路由守卫
// ============================================================

import { Routes, Route, Navigate } from 'react-router-dom'
import { useEffect } from 'react'
import { useUserStore } from '@/store/userStore'
import { isLoggedIn } from '@/utils/auth'
import MainLayout from '@/layouts/MainLayout'
import Login from '@/pages/Login'
import Register from '@/pages/Register'
import Dashboard from '@/pages/Dashboard'
import BookList from '@/pages/BookList'
import CategoryList from '@/pages/CategoryList'
import MyBorrows from '@/pages/MyBorrows'
import AllBorrows from '@/pages/AllBorrows'

/** 路由守卫组件：未登录跳转登录页 */
function RequireAuth({ children }: { children: React.ReactNode }) {
  const loggedIn = isLoggedIn()
  if (!loggedIn) {
    return <Navigate to="/login" replace />
  }
  return <>{children}</>
}

export default function AppRouter() {
  const { token } = useUserStore()

  // 监听 token 变化，未登录时跳转
  useEffect(() => {
    if (!token && window.location.pathname !== '/login' && window.location.pathname !== '/register') {
      window.location.href = '/login'
    }
  }, [token])

  return (
    <Routes>
      {/* 公开路由 */}
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />

      {/* 需要登录的路由 */}
      <Route
        path="/"
        element={
          <RequireAuth>
            <MainLayout />
          </RequireAuth>
        }
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<Dashboard />} />
        <Route path="books" element={<BookList />} />
        <Route path="categories" element={<CategoryList />} />
        <Route path="my-borrows" element={<MyBorrows />} />
        <Route path="all-borrows" element={<AllBorrows />} />
      </Route>

      {/* 404 */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

// ============================================================
// 全局类型定义
// ============================================================

/** 后端统一响应格式 */
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: string
}

/** 分页响应 */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/** 用户信息 */
export interface User {
  id: number
  username: string
  nickname: string
  email?: string
  phone?: string
  role: 'ADMIN' | 'USER'
  status: number
}

/** 登录响应 */
export interface LoginResult {
  token: string
  user: User
}

/** 图书分类 */
export interface BookCategory {
  id: number
  name: string
  description?: string
  sort: number
}

/** 图书 */
export interface Book {
  id: number
  isbn?: string
  title: string
  author: string
  publisher?: string
  publishDate?: string
  categoryId?: number
  categoryName?: string
  price?: number
  stock: number
  totalStock: number
  description?: string
  coverUrl?: string
  createTime?: string
}

/** 图书新增/编辑表单 */
export interface BookForm {
  title: string
  author: string
  isbn?: string
  publisher?: string
  categoryId?: number
  price?: number
  stock: number
  description?: string
}

/** 图书查询参数 */
export interface BookQuery {
  keyword?: string
  categoryId?: number
  pageNum: number
  pageSize: number
}

/** 借阅记录 */
export interface BorrowRecord {
  id: number
  userId: number
  username?: string
  bookId: number
  bookTitle?: string
  borrowDate: string
  dueDate: string
  returnDate?: string
  status: 'BORROWED' | 'RETURNED' | 'OVERDUE'
  fine: number
}

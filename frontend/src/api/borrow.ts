// ============================================================
// 借阅相关 API
// ============================================================

import { get, post, put } from './request'
import type { BorrowRecord, PageResult } from '@/types'

/** 借阅图书 */
export function borrowBook(bookId: number) {
  return post<BorrowRecord>(`/borrows/${bookId}`)
}

/** 归还图书 */
export function returnBook(recordId: number) {
  return put<BorrowRecord>(`/borrows/${recordId}/return`)
}

/** 我的借阅记录 */
export function pageMyBorrows(pageNum: number, pageSize: number) {
  return get<PageResult<BorrowRecord>>('/borrows/my', { pageNum, pageSize })
}

/** 所有借阅记录（管理员） */
export function pageAllBorrows(pageNum: number, pageSize: number) {
  return get<PageResult<BorrowRecord>>('/borrows', { pageNum, pageSize })
}

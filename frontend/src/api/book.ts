// ============================================================
// 图书相关 API
// ============================================================

import { get, post, put, del } from './request'
import type { Book, BookForm, BookQuery, PageResult } from '@/types'

/** 分页查询图书 */
export function pageBooks(params: BookQuery) {
  return get<PageResult<Book>>('/books', params)
}

/** 查询图书详情 */
export function getBookById(id: number) {
  return get<Book>(`/books/${id}`)
}

/** 新增图书 */
export function addBook(data: BookForm) {
  return post<void>('/books', data)
}

/** 更新图书 */
export function updateBook(id: number, data: BookForm) {
  return put<Book>(`/books/${id}`, data)
}

/** 删除图书 */
export function deleteBook(id: number) {
  return del<void>(`/books/${id}`)
}

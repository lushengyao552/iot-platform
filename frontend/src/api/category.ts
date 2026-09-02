// ============================================================
// 分类相关 API
// ============================================================

import { get } from './request'
import type { BookCategory } from '@/types'

/** 查询所有分类 */
export function listCategories() {
  return get<BookCategory[]>('/categories')
}

// ============================================================
// 分类管理页面
// ============================================================

import { useEffect, useState } from 'react'
import { Table, Card } from 'antd'
import { listCategories } from '@/api/category'
import type { BookCategory } from '@/types'

export default function CategoryList() {
  const [loading, setLoading] = useState(false)
  const [categories, setCategories] = useState<BookCategory[]>([])

  useEffect(() => {
    loadCategories()
  }, [])

  const loadCategories = async () => {
    setLoading(true)
    try {
      const res = await listCategories()
      setCategories(res.data)
    } catch (error) {
      console.error('加载分类失败', error)
    } finally {
      setLoading(false)
    }
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
    { title: '分类名称', dataIndex: 'name', key: 'name' },
    { title: '描述', dataIndex: 'description', key: 'description', render: (text: string) => text || '-' },
    { title: '排序', dataIndex: 'sort', key: 'sort', width: 80 },
  ]

  return (
    <div className="content-card">
      <Table
        dataSource={categories}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={false}
      />
    </div>
  )
}

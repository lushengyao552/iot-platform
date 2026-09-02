// ============================================================
// 全部借阅记录页面（管理员）
// ============================================================

import { useEffect, useState } from 'react'
import { Table, Tag } from 'antd'
import { pageAllBorrows } from '@/api/borrow'
import type { BorrowRecord } from '@/types'
import dayjs from 'dayjs'

export default function AllBorrows() {
  const [loading, setLoading] = useState(false)
  const [records, setRecords] = useState<BorrowRecord[]>([])
  const [total, setTotal] = useState(0)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(10)

  useEffect(() => {
    loadData()
  }, [pageNum, pageSize])

  const loadData = async () => {
    setLoading(true)
    try {
      const res = await pageAllBorrows(pageNum, pageSize)
      setRecords(res.data.records)
      setTotal(res.data.total)
    } catch (error) {
      console.error('加载借阅记录失败', error)
    } finally {
      setLoading(false)
    }
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: '用户', dataIndex: 'username', key: 'username', width: 120 },
    { title: '书名', dataIndex: 'bookTitle', key: 'bookTitle' },
    {
      title: '借阅日期',
      dataIndex: 'borrowDate',
      key: 'borrowDate',
      render: (date: string) => dayjs(date).format('YYYY-MM-DD'),
    },
    {
      title: '应还日期',
      dataIndex: 'dueDate',
      key: 'dueDate',
      render: (date: string) => dayjs(date).format('YYYY-MM-DD'),
    },
    {
      title: '归还日期',
      dataIndex: 'returnDate',
      key: 'returnDate',
      render: (date: string) => date ? dayjs(date).format('YYYY-MM-DD') : '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => {
        const colorMap: Record<string, string> = {
          BORROWED: 'blue',
          RETURNED: 'green',
          OVERDUE: 'red',
        }
        const textMap: Record<string, string> = {
          BORROWED: '借阅中',
          RETURNED: '已归还',
          OVERDUE: '已逾期',
        }
        return <Tag color={colorMap[status]}>{textMap[status]}</Tag>
      },
    },
    {
      title: '罚款',
      dataIndex: 'fine',
      key: 'fine',
      width: 100,
      render: (fine: number) => fine > 0 ? <span style={{ color: '#ff4d4f' }}>¥{fine}</span> : '-',
    },
  ]

  return (
    <div className="content-card">
      <Table
        dataSource={records}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={{
          current: pageNum,
          pageSize,
          total,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 条`,
          onChange: (page, size) => {
            setPageNum(page)
            setPageSize(size)
          },
        }}
      />
    </div>
  )
}

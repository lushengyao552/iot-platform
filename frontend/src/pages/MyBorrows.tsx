// ============================================================
// 我的借阅页面
// ============================================================

import { useEffect, useState } from 'react'
import { Table, Button, Tag, message, Popconfirm, Space } from 'antd'
import { pageMyBorrows, returnBook } from '@/api/borrow'
import type { BorrowRecord } from '@/types'
import dayjs from 'dayjs'

export default function MyBorrows() {
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
      const res = await pageMyBorrows(pageNum, pageSize)
      setRecords(res.data.records)
      setTotal(res.data.total)
    } catch (error) {
      console.error('加载借阅记录失败', error)
    } finally {
      setLoading(false)
    }
  }

  const handleReturn = async (recordId: number) => {
    try {
      const res = await returnBook(recordId)
      if (res.data.fine > 0) {
        message.success(`归还成功，逾期罚款 ¥${res.data.fine}`)
      } else {
        message.success('归还成功')
      }
      loadData()
    } catch (error) {
      console.error('归还失败', error)
    }
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
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
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_: any, record: BorrowRecord) =>
        record.status === 'BORROWED' || record.status === 'OVERDUE' ? (
          <Popconfirm title="确定归还这本书吗？" onConfirm={() => handleReturn(record.id)}>
            <Button type="link" size="small">归还</Button>
          </Popconfirm>
        ) : (
          '-'
        ),
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

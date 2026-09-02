// ============================================================
// 首页：统计概览
// ============================================================

import { useEffect, useState } from 'react'
import { Row, Col, Card, Statistic, Table, Tag, Space } from 'antd'
import {
  BookOutlined,
  ShoppingCartOutlined,
  UserOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import { pageBooks } from '@/api/book'
import { pageMyBorrows, pageAllBorrows } from '@/api/borrow'
import { useUserStore } from '@/store/userStore'
import type { Book, BorrowRecord } from '@/types'
import dayjs from 'dayjs'

export default function Dashboard() {
  const { user } = useUserStore()
  const [bookTotal, setBookTotal] = useState(0)
  const [borrowingCount, setBorrowingCount] = useState(0)
  const [overdueCount, setOverdueCount] = useState(0)
  const [recentBooks, setRecentBooks] = useState<Book[]>([])
  const [myBorrows, setMyBorrows] = useState<BorrowRecord[]>([])

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    try {
      // 图书总数
      const bookRes = await pageBooks({ pageNum: 1, pageSize: 5 })
      setBookTotal(bookRes.data.total)
      setRecentBooks(bookRes.data.records)

      // 借阅统计
      const borrowRes = user?.role === 'ADMIN'
        ? await pageAllBorrows(1, 100)
        : await pageMyBorrows(1, 100)
      const records = borrowRes.data.records
      setBorrowingCount(records.filter((r) => r.status === 'BORROWED').length)
      setOverdueCount(records.filter((r) => r.status === 'OVERDUE').length)
      setMyBorrows(records.slice(0, 5))
    } catch (error) {
      console.error('加载统计数据失败', error)
    }
  }

  const bookColumns = [
    { title: '书名', dataIndex: 'title', key: 'title' },
    { title: '作者', dataIndex: 'author', key: 'author' },
    {
      title: '库存',
      dataIndex: 'stock',
      key: 'stock',
      render: (stock: number) => (
        <Tag color={stock > 0 ? 'green' : 'red'}>{stock > 0 ? `可借 ${stock}` : '已借完'}</Tag>
      ),
    },
  ]

  const borrowColumns = [
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
      title: '状态',
      dataIndex: 'status',
      key: 'status',
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
  ]

  return (
    <Space direction="vertical" size={24} style={{ width: '100%' }}>
      {/* 统计卡片 */}
      <Row gutter={24}>
        <Col span={6}>
          <Card>
            <Statistic
              title="图书总数"
              value={bookTotal}
              prefix={<BookOutlined style={{ color: '#1677ff' }} />}
              valueStyle={{ color: '#1677ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="借阅中"
              value={borrowingCount}
              prefix={<ShoppingCartOutlined style={{ color: '#52c41a' }} />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="已逾期"
              value={overdueCount}
              prefix={<WarningOutlined style={{ color: '#ff4d4f' }} />}
              valueStyle={{ color: '#ff4d4f' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="当前用户"
              value={user?.nickname || user?.username || '-'}
              prefix={<UserOutlined style={{ color: '#722ed1' }} />}
              valueStyle={{ color: '#722ed1', fontSize: 20 }}
            />
          </Card>
        </Col>
      </Row>

      {/* 最新图书 + 我的借阅 */}
      <Row gutter={24}>
        <Col span={12}>
          <Card title="最新图书">
            <Table
              dataSource={recentBooks}
              columns={bookColumns}
              rowKey="id"
              pagination={false}
              size="small"
            />
          </Card>
        </Col>
        <Col span={12}>
          <Card title={user?.role === 'ADMIN' ? '最近借阅' : '我的借阅'}>
            <Table
              dataSource={myBorrows}
              columns={borrowColumns}
              rowKey="id"
              pagination={false}
              size="small"
            />
          </Card>
        </Col>
      </Row>
    </Space>
  )
}

// ============================================================
// 图书管理页面：搜索、列表、新增、编辑、删除、借阅
// ============================================================

import { useEffect, useState } from 'react'
import {
  Table,
  Button,
  Input,
  Select,
  Space,
  Modal,
  Form,
  InputNumber,
  message,
  Popconfirm,
  Tag,
  Descriptions,
} from 'antd'
import { PlusOutlined, SearchOutlined, EditOutlined, DeleteOutlined, EyeOutlined, ShoppingCartOutlined } from '@ant-design/icons'
import { pageBooks, addBook, updateBook, deleteBook, getBookById } from '@/api/book'
import { listCategories } from '@/api/category'
import { borrowBook } from '@/api/borrow'
import { useUserStore } from '@/store/userStore'
import type { Book, BookCategory, BookForm } from '@/types'
import dayjs from 'dayjs'

export default function BookList() {
  const { user } = useUserStore()
  const [loading, setLoading] = useState(false)
  const [books, setBooks] = useState<Book[]>([])
  const [total, setTotal] = useState(0)
  const [categories, setCategories] = useState<BookCategory[]>([])
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [keyword, setKeyword] = useState('')
  const [categoryId, setCategoryId] = useState<number | undefined>()

  // 弹窗状态
  const [modalOpen, setModalOpen] = useState(false)
  const [modalTitle, setModalTitle] = useState('')
  const [editingBook, setEditingBook] = useState<Book | null>(null)
  const [detailOpen, setDetailOpen] = useState(false)
  const [detailBook, setDetailBook] = useState<Book | null>(null)
  const [form] = Form.useForm<BookForm>()

  useEffect(() => {
    loadCategories()
    loadBooks()
  }, [pageNum, pageSize])

  // 加载分类
  const loadCategories = async () => {
    try {
      const res = await listCategories()
      setCategories(res.data)
    } catch (error) {
      console.error('加载分类失败', error)
    }
  }

  // 加载图书列表
  const loadBooks = async () => {
    setLoading(true)
    try {
      const res = await pageBooks({ keyword, categoryId, pageNum, pageSize })
      setBooks(res.data.records)
      setTotal(res.data.total)
    } catch (error) {
      console.error('加载图书失败', error)
    } finally {
      setLoading(false)
    }
  }

  // 搜索
  const handleSearch = () => {
    setPageNum(1)
    loadBooks()
  }

  // 重置
  const handleReset = () => {
    setKeyword('')
    setCategoryId(undefined)
    setPageNum(1)
    setTimeout(loadBooks, 0)
  }

  // 新增
  const handleAdd = () => {
    setModalTitle('新增图书')
    setEditingBook(null)
    form.resetFields()
    setModalOpen(true)
  }

  // 编辑
  const handleEdit = (book: Book) => {
    setModalTitle('编辑图书')
    setEditingBook(book)
    form.setFieldsValue({
      title: book.title,
      author: book.author,
      isbn: book.isbn,
      publisher: book.publisher,
      categoryId: book.categoryId,
      price: book.price,
      stock: book.stock,
      description: book.description,
    })
    setModalOpen(true)
  }

  // 详情
  const handleDetail = async (id: number) => {
    try {
      const res = await getBookById(id)
      setDetailBook(res.data)
      setDetailOpen(true)
    } catch (error) {
      console.error('加载图书详情失败', error)
    }
  }

  // 删除
  const handleDelete = async (id: number) => {
    try {
      await deleteBook(id)
      message.success('删除成功')
      loadBooks()
    } catch (error) {
      console.error('删除失败', error)
    }
  }

  // 借阅
  const handleBorrow = async (bookId: number) => {
    try {
      await borrowBook(bookId)
      message.success('借阅成功')
      loadBooks()
    } catch (error) {
      console.error('借阅失败', error)
    }
  }

  // 提交表单
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (editingBook) {
        await updateBook(editingBook.id, values)
        message.success('更新成功')
      } else {
        await addBook(values)
        message.success('新增成功')
      }
      setModalOpen(false)
      loadBooks()
    } catch (error) {
      console.error('提交失败', error)
    }
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: '书名', dataIndex: 'title', key: 'title', ellipsis: true },
    { title: '作者', dataIndex: 'author', key: 'author', width: 100 },
    {
      title: '分类',
      dataIndex: 'categoryName',
      key: 'categoryName',
      width: 100,
      render: (name: string) => name ? <Tag color="blue">{name}</Tag> : '-',
    },
    {
      title: '价格',
      dataIndex: 'price',
      key: 'price',
      width: 80,
      render: (price: number) => price ? `¥${price}` : '-',
    },
    {
      title: '库存',
      dataIndex: 'stock',
      key: 'stock',
      width: 100,
      render: (stock: number, record: Book) => (
        <Space>
          <Tag color={stock > 0 ? 'green' : 'red'}>
            {stock > 0 ? `可借 ${stock}` : '已借完'}
          </Tag>
          <span style={{ color: '#999', fontSize: 12 }}>共{record.totalStock}</span>
        </Space>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 240,
      render: (_: any, record: Book) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleDetail(record.id)}>
            详情
          </Button>
          {record.stock > 0 && (
            <Button
              type="link"
              size="small"
              icon={<ShoppingCartOutlined />}
              onClick={() => handleBorrow(record.id)}
            >
              借阅
            </Button>
          )}
          {user?.role === 'ADMIN' && (
            <>
              <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
                编辑
              </Button>
              <Popconfirm title="确定删除这本书吗？" onConfirm={() => handleDelete(record.id)}>
                <Button type="link" size="small" danger icon={<DeleteOutlined />}>
                  删除
                </Button>
              </Popconfirm>
            </>
          )}
        </Space>
      ),
    },
  ]

  return (
    <div className="content-card">
      {/* 搜索栏 */}
      <div className="search-bar">
        <Input
          placeholder="搜索书名/作者"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onPressEnter={handleSearch}
          style={{ width: 240 }}
          prefix={<SearchOutlined />}
        />
        <Select
          placeholder="选择分类"
          value={categoryId}
          onChange={setCategoryId}
          style={{ width: 160 }}
          allowClear
        >
          {categories.map((cat) => (
            <Select.Option key={cat.id} value={cat.id}>
              {cat.name}
            </Select.Option>
          ))}
        </Select>
        <Button type="primary" onClick={handleSearch}>
          搜索
        </Button>
        <Button onClick={handleReset}>重置</Button>
        {user?.role === 'ADMIN' && (
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd} style={{ marginLeft: 'auto' }}>
            新增图书
          </Button>
        )}
      </div>

      {/* 表格 */}
      <Table
        dataSource={books}
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

      {/* 新增/编辑弹窗 */}
      <Modal
        title={modalTitle}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        width={600}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item name="title" label="书名" rules={[{ required: true, message: '请输入书名' }]}>
            <Input placeholder="请输入书名" />
          </Form.Item>
          <Form.Item name="author" label="作者" rules={[{ required: true, message: '请输入作者' }]}>
            <Input placeholder="请输入作者" />
          </Form.Item>
          <Space style={{ width: '100%' }}>
            <Form.Item name="isbn" label="ISBN" style={{ flex: 1 }}>
              <Input placeholder="请输入ISBN" />
            </Form.Item>
            <Form.Item name="publisher" label="出版社" style={{ flex: 1 }}>
              <Input placeholder="请输入出版社" />
            </Form.Item>
          </Space>
          <Space style={{ width: '100%' }}>
            <Form.Item name="categoryId" label="分类" style={{ flex: 1 }}>
              <Select placeholder="请选择分类">
                {categories.map((cat) => (
                  <Select.Option key={cat.id} value={cat.id}>
                    {cat.name}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
            <Form.Item name="price" label="价格" style={{ flex: 1 }}>
              <InputNumber min={0} precision={2} style={{ width: '100%' }} placeholder="请输入价格" />
            </Form.Item>
            <Form.Item name="stock" label="库存" rules={[{ required: true, message: '请输入库存' }]} style={{ flex: 1 }}>
              <InputNumber min={0} style={{ width: '100%' }} placeholder="请输入库存" />
            </Form.Item>
          </Space>
          <Form.Item name="description" label="简介">
            <Input.TextArea rows={3} placeholder="请输入图书简介" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 详情弹窗 */}
      <Modal title="图书详情" open={detailOpen} onCancel={() => setDetailOpen(false)} footer={null} width={600}>
        {detailBook && (
          <Descriptions column={2} bordered size="small">
            <Descriptions.Item label="书名" span={2}>{detailBook.title}</Descriptions.Item>
            <Descriptions.Item label="作者">{detailBook.author}</Descriptions.Item>
            <Descriptions.Item label="ISBN">{detailBook.isbn || '-'}</Descriptions.Item>
            <Descriptions.Item label="出版社">{detailBook.publisher || '-'}</Descriptions.Item>
            <Descriptions.Item label="分类">{detailBook.categoryName || '-'}</Descriptions.Item>
            <Descriptions.Item label="价格">{detailBook.price ? `¥${detailBook.price}` : '-'}</Descriptions.Item>
            <Descriptions.Item label="可借库存">{detailBook.stock}</Descriptions.Item>
            <Descriptions.Item label="总藏书">{detailBook.totalStock}</Descriptions.Item>
            <Descriptions.Item label="创建时间">{dayjs(detailBook.createTime).format('YYYY-MM-DD HH:mm')}</Descriptions.Item>
            <Descriptions.Item label="简介" span={2}>{detailBook.description || '暂无简介'}</Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </div>
  )
}

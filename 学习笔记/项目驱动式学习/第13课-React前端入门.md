# 第13课：React 前端入门——JSX、useState、useEffect、组件、Props

> **本课目标**：从零理解 React 的核心概念，搞懂 JSX、函数组件、useState、useEffect、Props，能看懂项目里的 BookList 页面。你的目标是后端，React 只需要达到"能看懂、能改简单页面"的程度，不需要深入前端工程化。

---

## 一、React 是什么？

React 是一个用于构建用户界面的 JavaScript 库，由 Facebook（Meta）开发。它的核心思想是**组件化**和**声明式编程**。

### 项目里的前端技术栈

| 技术 | 作用 | 项目里的位置 |
|------|------|-------------|
| **React 18** | UI 库，构建用户界面 | 所有页面组件 |
| **TypeScript** | JavaScript 的超集，加了类型系统 | 所有 .tsx 文件 |
| **Vite** | 构建工具，开发服务器 + 打包 | vite.config.ts |
| **Ant Design (antd)** | UI 组件库，提供 Table、Button、Form、Modal 等组件 | 页面里大量使用 |
| **React Router** | 路由库，管理页面跳转 | router/index.tsx |
| **Axios** | HTTP 客户端，发请求到后端 | api/request.ts、api/book.ts |
| **Zustand** | 状态管理库，管理全局状态（如用户信息） | store/userStore.ts |
| **dayjs** | 日期处理库 | 格式化日期显示 |

### 前端项目结构

```
frontend/
├── index.html              # HTML 入口
├── package.json            # 依赖配置
├── vite.config.ts          # Vite 配置（代理、端口等）
├── tsconfig.json           # TypeScript 配置
└── src/
    ├── main.tsx            # 应用入口（React 挂载点）
    ├── App.tsx             # 根组件
    ├── index.css           # 全局样式
    ├── api/                # API 调用层
    │   ├── request.ts      # Axios 封装（拦截器、统一错误处理）
    │   ├── auth.ts         # 认证相关 API
    │   ├── book.ts         # 图书相关 API
    │   ├── borrow.ts       # 借阅相关 API
    │   └── category.ts     # 分类相关 API
    ├── layouts/            # 布局组件
    │   └── MainLayout.tsx  # 主布局（侧边栏 + 内容区）
    ├── pages/              # 页面组件
    │   ├── Login.tsx       # 登录页
    │   ├── Register.tsx    # 注册页
    │   ├── Dashboard.tsx   # 仪表盘
    │   ├── BookList.tsx    # 图书列表（最典型的页面）
    │   ├── CategoryList.tsx
    │   ├── MyBorrows.tsx
    │   └── AllBorrows.tsx
    ├── router/             # 路由配置
    │   └── index.tsx
    ├── store/              # 全局状态
    │   └── userStore.ts    # 用户状态（Zustand）
    ├── types/              # TypeScript 类型定义
    │   └── index.ts
    └── utils/              # 工具函数
        └── auth.ts         # Token 存取
```

---

## 二、应用入口——main.tsx

```tsx
import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import App from './App'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider locale={zhCN} theme={{ token: { colorPrimary: '#1677ff' } }}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </ConfigProvider>
  </React.StrictMode>
)
```

### 逐行解释

1. **`ReactDOM.createRoot(document.getElementById('root')!)`**：找到 HTML 里 id 为 `root` 的元素，作为 React 应用的挂载点。React 会把所有组件渲染到这个 div 里
2. **`.render(...)`**：把 React 组件渲染到挂载点
3. **`<React.StrictMode>`**：严格模式，开发环境下会额外检查常见问题（如副作用、废弃 API），生产环境不影响
4. **`<ConfigProvider locale={zhCN}>`**：Ant Design 的全局配置，设置中文语言包和主题色
5. **`<BrowserRouter>`**：React Router 的路由提供者，让应用支持前端路由（URL 变化时不刷新页面，只切换组件）
6. **`<App />`**：根组件，所有页面都在它里面

### 组件嵌套关系

```
React.StrictMode
└── ConfigProvider (Ant Design 中文+主题)
    └── BrowserRouter (路由)
        └── App (根组件)
            └── AppRouter (路由配置)
                ├── /login → Login 页面
                ├── /register → Register 页面
                └── / → MainLayout (需要登录)
                    ├── /dashboard → Dashboard
                    ├── /books → BookList
                    ├── /categories → CategoryList
                    ├── /my-borrows → MyBorrows
                    └── /all-borrows → AllBorrows
```

---

## 三、JSX——在 JavaScript 里写 HTML

JSX 是 React 的语法扩展，让你可以在 JavaScript 里写类似 HTML 的代码。

### 基本用法

```tsx
// 这就是 JSX：在函数里返回 HTML 标签
function Hello() {
  return <div>Hello, React!</div>
}
```

JSX 最终会被编译成 JavaScript（`React.createElement` 调用），但你写的时候就像写 HTML 一样。

### 在 JSX 里用 JavaScript 表达式

用 `{}` 包裹 JavaScript 表达式：

```tsx
function BookItem({ book }) {
  return (
    <div>
      <h3>{book.title}</h3>           {/* 变量 */}
      <p>作者：{book.author}</p>
      <p>价格：¥{book.price * 0.8}</p>  {/* 计算表达式 */}
      <p>{book.stock > 0 ? '可借' : '已借完'}</p>  {/* 三元表达式 */}
      <button onClick={() => console.log('点击了')}>借阅</button>  {/* 事件处理 */}
    </div>
  )
}
```

### JSX 的注意事项

1. **只能有一个根元素**：return 里只能有一个最外层标签，如果有多个可以用 `<>`（Fragment）包裹
   ```tsx
   // 错误：两个根元素
   return <div>标题</div><p>内容</p>

   // 正确：用 Fragment 包裹
   return <><div>标题</div><p>内容</p></>
   ```

2. **class 要写成 className**：因为 class 是 JavaScript 关键字
   ```tsx
   <div className="content-card">...</div>
   ```

3. **事件用驼峰命名**：onclick → onClick，onchange → onChange
   ```tsx
   <button onClick={handleClick}>点击</button>
   <input onChange={(e) => setValue(e.target.value)} />
   ```

4. **样式用对象**：style 接收一个对象，属性用驼峰
   ```tsx
   <div style={{ width: 240, color: '#999', fontSize: 12 }}>...</div>
   ```

5. **列表渲染要有 key**：map 渲染列表时，每个元素要有唯一的 key
   ```tsx
   {categories.map((cat) => (
     <Select.Option key={cat.id} value={cat.id}>{cat.name}</Select.Option>
   ))}
   ```

---

## 四、函数组件——React 的基本单位

React 组件有两种：类组件（class component）和函数组件（function component）。现代 React 用**函数组件** + Hooks，项目里全是函数组件。

### 最简单的函数组件

```tsx
// 函数组件就是一个返回 JSX 的函数
function Welcome() {
  return <h1>欢迎使用图书管理系统</h1>
}

// 导出组件
export default Welcome
```

### 带 Props 的组件

Props（属性）是父组件传给子组件的数据，子组件通过参数接收：

```tsx
// 子组件：通过 props 接收父组件传的数据
function BookCard({ title, author, price }: { title: string; author: string; price: number }) {
  return (
    <div className="book-card">
      <h3>{title}</h3>
      <p>作者：{author}</p>
      <p>价格：¥{price}</p>
    </div>
  )
}

// 父组件：使用子组件，通过属性传数据
function BookList() {
  return (
    <div>
      <BookCard title="Java核心技术" author="Cay S. Horstmann" price={119} />
      <BookCard title="深入理解Java虚拟机" author="周志明" price={89} />
    </div>
  )
}
```

**Props 的特点**：
- 只读：子组件不能修改 props，只能由父组件修改
- 单向数据流：数据从父组件流向子组件
- 可以传任何类型：字符串、数字、对象、函数、组件

### 项目里的组件用法

项目里大量使用 Ant Design 的组件（也是 React 组件）：

```tsx
import { Table, Button, Input, Modal, Form, message } from 'antd'

function BookList() {
  return (
    <div>
      <Input placeholder="搜索书名" />
      <Button type="primary" onClick={handleSearch}>搜索</Button>
      <Table dataSource={books} columns={columns} rowKey="id" />
      <Modal title="新增图书" open={modalOpen} onOk={handleSubmit}>
        <Form>...</Form>
      </Modal>
    </div>
  )
}
```

Ant Design 组件也是通过 props 传数据和事件回调，和自定义组件一样。

---

## 五、useState——组件的状态

### 什么是状态？

状态（state）是组件内部的可变数据。状态变化时，React 会**重新渲染**组件，更新页面显示。

和 props 的区别：
- **props**：父组件传进来的，只读，变化由父组件控制
- **state**：组件自己管理的，可写，变化由组件自己控制

### useState 的用法

```tsx
import { useState } from 'react'

function Counter() {
  // useState 返回一个数组：[当前值, 更新函数]
  // 0 是初始值
  const [count, setCount] = useState(0)

  return (
    <div>
      <p>当前计数：{count}</p>
      <button onClick={() => setCount(count + 1)}>加1</button>
      <button onClick={() => setCount(count - 1)}>减1</button>
    </div>
  )
}
```

### 项目里的 useState

看 BookList.tsx 里的状态：

```tsx
export default function BookList() {
  // 加载状态：控制 Table 的 loading 动画
  const [loading, setLoading] = useState(false)

  // 图书列表数据
  const [books, setBooks] = useState<Book[]>([])

  // 总记录数（用于分页）
  const [total, setTotal] = useState(0)

  // 分类列表
  const [categories, setCategories] = useState<BookCategory[]>([])

  // 分页参数
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(10)

  // 搜索条件
  const [keyword, setKeyword] = useState('')
  const [categoryId, setCategoryId] = useState<number | undefined>()

  // 弹窗状态
  const [modalOpen, setModalOpen] = useState(false)
  const [modalTitle, setModalTitle] = useState('')
  const [editingBook, setEditingBook] = useState<Book | null>(null)
  const [detailOpen, setDetailOpen] = useState(false)
  const [detailBook, setDetailBook] = useState<Book | null>(null)

  // ...
}
```

每个状态都控制页面的一部分显示：
- `loading` → Table 的 loading 动画
- `books` → Table 的数据
- `modalOpen` → 弹窗是否显示
- `keyword` → 搜索框的输入值
- `pageNum`/`pageSize` → 分页

### 状态更新触发重新渲染

当你调用 `setBooks(newBooks)` 时：
1. React 更新 books 状态
2. React 重新渲染 BookList 组件
3. 组件返回新的 JSX（包含新的 books 数据）
4. React 对比新旧 JSX，更新变化的 DOM
5. 页面显示最新的图书列表

这就是 React 的**响应式**：数据变化，页面自动更新。你不需要手动操作 DOM，只需要管理状态。

### useState 的注意事项

1. **只能在函数组件顶层调用**：不能在 if、for、嵌套函数里调用
2. **更新函数是异步的**：`setCount(count + 1)` 后立即读 count 还是旧值
3. **对象/数组要创建新引用**：不能直接修改原对象，要创建新对象
   ```tsx
   // 错误：直接修改原对象
   book.title = '新标题'
   setBook(book)

   // 正确：创建新对象
   setBook({ ...book, title: '新标题' })
   ```

---

## 六、useEffect——副作用

### 什么是副作用？

副作用（side effect）是指组件渲染之外的操作，比如：
- 发送 HTTP 请求获取数据
- 操作 DOM
- 设置定时器
- 订阅事件
- 本地存储读写

这些操作不能直接写在组件函数体里（会在每次渲染时执行），需要用 `useEffect` 管理。

### useEffect 的基本用法

```tsx
import { useState, useEffect } from 'react'

function UserProfile({ userId }) {
  const [user, setUser] = useState(null)

  // useEffect 接收两个参数：
  // 1. 副作用函数（组件渲染后执行）
  // 2. 依赖数组（控制什么时候执行）
  useEffect(() => {
    // 发送请求获取用户信息
    fetch(`/api/users/${userId}`)
      .then(res => res.json())
      .then(data => setUser(data))
  }, [userId])  // 依赖数组：userId 变化时重新执行

  return <div>{user?.name}</div>
}
```

### 依赖数组的三种情况

| 依赖数组 | 执行时机 |
|---------|---------|
| `[]`（空数组） | 只在组件首次渲染后执行一次 |
| `[a, b]`（有依赖） | 首次渲染执行，之后 a 或 b 变化时执行 |
| 不写依赖数组 | 每次渲染后都执行（慎用，可能死循环） |

### 项目里的 useEffect

看 BookList.tsx：

```tsx
useEffect(() => {
  loadCategories()   // 加载分类列表
  loadBooks()        // 加载图书列表
}, [pageNum, pageSize])  // pageNum 或 pageSize 变化时重新加载
```

这个 effect 的作用：
- 组件首次渲染时，加载分类和图书列表
- 当 pageNum（页码）或 pageSize（每页条数）变化时，重新加载图书列表（翻页）

注意：搜索时调用 `handleSearch` 手动调用 `loadBooks()`，不依赖 effect，因为搜索条件变化时需要重置页码。

### 清理函数

useEffect 可以返回一个清理函数，在组件卸载或下次 effect 执行前调用：

```tsx
useEffect(() => {
  const timer = setInterval(() => {
    console.log('每秒执行一次')
  }, 1000)

  // 清理函数：组件卸载时清除定时器
  return () => {
    clearInterval(timer)
  }
}, [])
```

项目里的 effect 没有清理函数，因为只是发请求，不需要清理。

---

## 七、项目里的 BookList 页面完整解析

现在你已经有了 React 基础知识，我们来完整解析 BookList.tsx。

### 7.1 组件结构

```tsx
export default function BookList() {
  // 1. 获取全局状态（当前登录用户）
  const { user } = useUserStore()

  // 2. 定义所有状态（useState）
  const [loading, setLoading] = useState(false)
  const [books, setBooks] = useState<Book[]>([])
  const [total, setTotal] = useState(0)
  const [categories, setCategories] = useState<BookCategory[]>([])
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [keyword, setKeyword] = useState('')
  const [categoryId, setCategoryId] = useState<number | undefined>()
  const [modalOpen, setModalOpen] = useState(false)
  const [modalTitle, setModalTitle] = useState('')
  const [editingBook, setEditingBook] = useState<Book | null>(null)
  const [detailOpen, setDetailOpen] = useState(false)
  const [detailBook, setDetailBook] = useState<Book | null>(null)
  const [form] = Form.useForm<BookForm>()

  // 3. useEffect：组件挂载时加载数据
  useEffect(() => {
    loadCategories()
    loadBooks()
  }, [pageNum, pageSize])

  // 4. 定义各种事件处理函数
  const loadBooks = async () => { ... }
  const handleSearch = () => { ... }
  const handleAdd = () => { ... }
  const handleEdit = (book: Book) => { ... }
  const handleDelete = async (id: number) => { ... }
  const handleBorrow = async (bookId: number) => { ... }
  const handleSubmit = async () => { ... }

  // 5. 定义表格列配置
  const columns = [ ... ]

  // 6. 返回 JSX（页面渲染）
  return (
    <div className="content-card">
      {/* 搜索栏 */}
      <div className="search-bar">...</div>
      {/* 表格 */}
      <Table ... />
      {/* 新增/编辑弹窗 */}
      <Modal ...>...</Modal>
      {/* 详情弹窗 */}
      <Modal ...>...</Modal>
    </div>
  )
}
```

### 7.2 数据加载流程

```tsx
// 加载图书列表
const loadBooks = async () => {
  setLoading(true)  // 开始加载，显示 loading
  try {
    // 调用 API（封装好的 Axios 请求）
    const res = await pageBooks({ keyword, categoryId, pageNum, pageSize })
    // 更新状态，触发重新渲染
    setBooks(res.data.records)   // 图书列表
    setTotal(res.data.total)      // 总记录数
  } catch (error) {
    console.error('加载图书失败', error)
  } finally {
    setLoading(false)  // 加载结束，隐藏 loading
  }
}
```

流程：
1. 设置 loading=true → 表格显示 loading 动画
2. 调用 `pageBooks()` 发 HTTP 请求到后端 `GET /api/books`
3. 等待响应（await）
4. 拿到数据后，`setBooks`、`setTotal` 更新状态
5. React 重新渲染组件，表格显示新数据
6. `finally` 里设置 loading=false，隐藏 loading

### 7.3 搜索流程

```tsx
const handleSearch = () => {
  setPageNum(1)  // 搜索时重置到第1页
  loadBooks()    // 重新加载（用新的 keyword 和 categoryId）
}
```

搜索框输入时：
```tsx
<Input
  placeholder="搜索书名/作者"
  value={keyword}                                    // 输入框的值绑定到 keyword 状态
  onChange={(e) => setKeyword(e.target.value)}      // 输入时更新 keyword
  onPressEnter={handleSearch}                        // 按回车触发搜索
/>
```

这是 React 的**受控组件**模式：输入框的值由 state 控制，输入时更新 state，state 变化触发重新渲染，输入框显示新值。

### 7.4 新增/编辑流程

```tsx
// 点击新增
const handleAdd = () => {
  setModalTitle('新增图书')
  setEditingBook(null)       // 标记为新增模式
  form.resetFields()         // 重置表单
  setModalOpen(true)         // 打开弹窗
}

// 点击编辑
const handleEdit = (book: Book) => {
  setModalTitle('编辑图书')
  setEditingBook(book)       // 标记为编辑模式，保存当前编辑的图书
  form.setFieldsValue({      // 表单填充当前图书数据
    title: book.title,
    author: book.author,
    // ...
  })
  setModalOpen(true)         // 打开弹窗
}

// 提交表单
const handleSubmit = async () => {
  try {
    const values = await form.validateFields()  // 校验表单
    if (editingBook) {
      await updateBook(editingBook.id, values)  // 编辑：调用更新 API
      message.success('更新成功')
    } else {
      await addBook(values)                      // 新增：调用新增 API
      message.success('新增成功')
    }
    setModalOpen(false)  // 关闭弹窗
    loadBooks()          // 重新加载列表
  } catch (error) {
    console.error('提交失败', error)
  }
}
```

### 7.5 权限控制

```tsx
// 只有管理员才能看到新增/编辑/删除按钮
{user?.role === 'ADMIN' && (
  <>
    <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
      新增图书
    </Button>
    {/* 编辑、删除按钮 */}
  </>
)}
```

用 `user?.role === 'ADMIN'` 判断当前用户角色，只有管理员才渲染操作按钮。普通用户只能看和借阅。

---

## 八、本课必须记住的 7 件事

1. **React 核心思想**：组件化 + 声明式编程，数据变化自动更新页面（响应式）
2. **JSX**：在 JavaScript 里写 HTML，用 `{}` 包裹表达式，class 写 className，事件用驼峰（onClick）
3. **函数组件**：就是一个返回 JSX 的函数，项目里全是函数组件
4. **Props**：父组件传给子组件的数据，只读，单向数据流
5. **useState**：`const [value, setValue] = useState(初始值)`，状态变化触发重新渲染
6. **useEffect**：处理副作用（发请求、定时器等），`useEffect(fn, [依赖])`，依赖变化时重新执行
7. **受控组件**：输入框的值由 state 控制，`value={state}` + `onChange={setState}`

---

## 九、本节练习

### 练习1：理解状态变化触发重新渲染

在 BookList 页面里，给搜索框加一个实时显示：在搜索框下面显示 `当前搜索关键词：{keyword}`。输入关键词时观察这个文字是否实时变化，理解 useState 触发重新渲染的机制。

### 练习2：给图书列表加一个"按价格排序"功能

1. 在搜索栏加一个下拉选择，选项是"默认排序"、"价格从低到高"、"价格从高到低"
2. 用 useState 管理排序状态
3. 调用 `pageBooks` 时把排序参数传给后端（需要后端也支持，先只改前端传参）

> **提示**：参考项目里 BookQueryDTO 已经有 orderBy 和 orderDir 字段。

### 练习3：理解 useEffect 的依赖数组

把 BookList 里的 useEffect 依赖数组改成 `[]`（空数组），然后翻页，观察图书列表会不会更新。然后改回 `[pageNum, pageSize]`，理解依赖数组的作用。

---

## 十、自测题

### Q1：React 里 props 和 state 有什么区别？

<details>
<summary>点击查看答案</summary>

- **props（属性）**：父组件传给子组件的数据，子组件通过函数参数接收。props 是只读的，子组件不能修改，只能由父组件更新。数据流向是单向的（父→子）。
- **state（状态）**：组件内部自己管理的可变数据，通过 `useState` 创建。state 变化时 React 会重新渲染组件。state 由组件自己控制，可以通过 `setState` 更新。

简单理解：props 是外部传进来的，state 是组件自己的。比如 BookList 组件里，`books` 是 state（组件自己管理的图书列表数据），而如果有一个子组件 BookCard，它接收的 `book` 就是 props。

</details>

### Q2：useState 返回什么？怎么更新状态？

<details>
<summary>点击查看答案</summary>

`useState(初始值)` 返回一个数组，包含两个元素：
- 第一个：当前状态值
- 第二个：更新状态的函数

用法：`const [count, setCount] = useState(0)`

更新状态：调用 `setCount(新值)`，比如 `setCount(count + 1)`。

注意事项：
1. 状态更新是异步的，调用 setCount 后立即读 count 还是旧值
2. 状态更新会触发组件重新渲染
3. 对象/数组更新要创建新引用（`setBook({...book, title: '新标题'})`），不能直接修改原对象
4. useState 只能在函数组件顶层调用，不能在 if/for/嵌套函数里调用

</details>

### Q3：useEffect 的作用是什么？依赖数组有哪几种情况？

<details>
<summary>点击查看答案</summary>

**useEffect 的作用**：处理副作用（side effect），即组件渲染之外的操作，比如发送 HTTP 请求、设置定时器、操作 DOM、订阅事件、本地存储读写等。这些操作不能直接写在组件函数体里（会在每次渲染时执行），需要用 useEffect 管理。

**依赖数组的三种情况**：
1. **空数组 `[]`**：副作用只在组件首次渲染后执行一次。适合初始化数据、设置定时器等。
2. **有依赖 `[a, b]`**：首次渲染执行，之后当 a 或 b 变化时重新执行。适合依赖特定数据的副作用，如项目里 `[pageNum, pageSize]`，翻页时重新加载数据。
3. **不写依赖数组**：每次渲染后都执行。慎用，可能导致死循环（如果 effect 里更新状态，状态更新触发渲染，渲染又触发 effect）。

useEffect 可以返回一个清理函数，在组件卸载或下次 effect 执行前调用，用于清除定时器、取消订阅等。

</details>

### Q4：什么是受控组件？项目里的搜索框是怎么实现的？

<details>
<summary>点击查看答案</summary>

**受控组件**：表单元素（input、select 等）的值由 React state 控制，而不是由 DOM 自己管理。输入时触发 onChange 事件，更新 state，state 变化触发重新渲染，输入框显示新的 state 值。

项目里的搜索框：
```tsx
const [keyword, setKeyword] = useState('')

<Input
  placeholder="搜索书名/作者"
  value={keyword}                               // 输入框的值绑定到 keyword state
  onChange={(e) => setKeyword(e.target.value)} // 输入时更新 keyword
  onPressEnter={handleSearch}                   // 按回车触发搜索
/>
```

流程：
1. 用户输入字符
2. 触发 onChange 事件，调用 `setKeyword(e.target.value)` 更新 state
3. state 变化触发组件重新渲染
4. 输入框的 value 变成新的 keyword，显示用户输入的字符

和受控组件相对的是非受控组件（用 ref 直接操作 DOM 获取值），React 推荐用受控组件，因为数据由 state 统一管理，更符合 React 的响应式思想。

</details>

### Q5：项目里的 BookList 页面加载数据的完整流程是什么？

<details>
<summary>点击查看答案</summary>

BookList 页面加载图书列表的流程：

1. **组件首次渲染**：React 渲染 BookList 组件，初始化所有 state（loading=false, books=[], total=0, pageNum=1, pageSize=10 等）
2. **useEffect 执行**：组件挂载后，useEffect 执行，调用 `loadCategories()` 和 `loadBooks()`
3. **loadBooks 执行**：
   - `setLoading(true)` → 表格显示 loading 动画
   - 调用 `pageBooks({ keyword, categoryId, pageNum, pageSize })` → 内部通过 Axios 发送 `GET /api/books?pageNum=1&pageSize=10` 请求到后端
   - 后端 Spring Boot 接收请求 → JwtInterceptor 验证 Token → BookController.pageBooks → BookServiceImpl.pageBooks → BookRepository.page → BookMapper 查 MySQL → 返回 Result<IPage<BookVO>>
   - 前端 Axios 响应拦截器处理响应，返回数据
   - `setBooks(res.data.records)` 更新图书列表
   - `setTotal(res.data.total)` 更新总记录数
   - `finally` 里 `setLoading(false)` 隐藏 loading
4. **重新渲染**：books 和 total 状态变化，React 重新渲染组件，Table 显示图书数据
5. **翻页时**：用户点击下一页，`setPageNum(2)` → pageNum 变化 → useEffect 依赖数组检测到变化 → 重新执行 loadBooks → 加载第2页数据

整个流程是 React 响应式的体现：状态变化 → 自动重新渲染 → 页面更新。

</details>

---

## 十一、面试题（前端方向，了解即可）

### 面试题1：React Hooks 有哪些？useState 和 useEffect 的原理是什么？

> **答题要点**（了解即可，你的方向是后端）：
> 1. **常用 Hooks**：
>    - `useState`：管理组件状态
>    - `useEffect`：处理副作用
>    - `useContext`：消费 Context，跨组件共享数据
>    - `useReducer`：管理复杂状态（类似 Redux）
>    - `useCallback` / `useMemo`：性能优化，缓存函数/计算结果
>    - `useRef`：获取 DOM 引用或存储不触发渲染的可变值
> 2. **useState 原理**：React 内部用链表/数组存储 Hooks 的状态，按调用顺序对应。每次渲染时按顺序读取状态值，调用 setState 时更新对应位置的状态并触发重新渲染。这就是为什么 Hooks 不能在条件语句里调用（会打乱顺序）。
> 3. **useEffect 原理**：在组件渲染提交到 DOM 后异步执行。React 会保存 effect 函数和依赖数组，每次渲染后对比依赖是否变化，变化了就执行 effect（先执行上一次的清理函数，再执行新的 effect）。
> 4. **Hooks 的规则**：只在函数组件顶层调用 Hooks；不要在循环、条件、嵌套函数里调用 Hooks。

---

## 十二、下一课预告

**第14课：前后端联调——Axios 封装、拦截器、跨域、API 调用全流程**

我们会搞清楚：
- 前端怎么发请求到后端？Axios 是什么？
- 项目里的 request.ts 封装了什么？请求拦截器和响应拦截器分别做什么？
- Token 是怎么自动携带到请求头的？
- 跨域问题是什么？项目里怎么解决的（Vite 代理 + WebMvcConfig CORS）？
- 前端调用 `pageBooks()` 到后端返回数据的完整流程
- 前端怎么处理错误？401 未登录怎么处理？
- 前端环境变量（.env.development / .env.production）怎么配置 API 地址？

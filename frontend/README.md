# 图书管理系统 - 前端（React）

基于 React 18 + TypeScript + Vite + Ant Design 5 的企业级前端项目。

## 技术栈

| 分类 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 框架 | React | 18.x | 最主流的前端框架 |
| 语言 | TypeScript | 5.x | 类型安全 |
| 构建工具 | Vite | 5.x | 下一代前端构建工具，极速热更新 |
| UI 组件库 | Ant Design | 5.x | 企业级 UI 设计体系 |
| 路由 | React Router | 6.x | 声明式路由 |
| HTTP 客户端 | Axios | 1.x | 请求拦截、响应拦截、统一错误处理 |
| 状态管理 | Zustand | 4.x | 轻量级状态管理，比 Redux 简洁 |
| 日期处理 | dayjs | 1.x | 轻量级日期库 |

## 项目结构

```
library-frontend/
├── index.html                    # HTML 入口
├── package.json                  # 依赖配置
├── vite.config.ts                # Vite 配置（含代理）
├── tsconfig.json                 # TypeScript 配置
├── .env.development              # 开发环境变量
├── .env.production               # 生产环境变量
└── src/
    ├── main.tsx                  # 应用入口
    ├── App.tsx                   # 根组件
    ├── index.css                 # 全局样式
    ├── vite-env.d.ts             # Vite 类型声明
    ├── api/                      # API 层
    │   ├── request.ts            # Axios 封装（拦截器、统一错误处理）
    │   ├── auth.ts               # 认证 API
    │   ├── book.ts               # 图书 API
    │   ├── category.ts           # 分类 API
    │   └── borrow.ts             # 借阅 API
    ├── store/                    # 状态管理
    │   └── userStore.ts          # 用户状态（Zustand + 持久化）
    ├── router/                   # 路由
    │   └── index.tsx             # 路由配置 + 路由守卫
    ├── layouts/                  # 布局
    │   └── MainLayout.tsx        # 主布局（侧边栏 + 顶部栏 + 内容区）
    ├── pages/                    # 页面
    │   ├── Login.tsx             # 登录页
    │   ├── Register.tsx          # 注册页
    │   ├── Dashboard.tsx         # 首页（统计概览）
    │   ├── BookList.tsx          # 图书管理（搜索、列表、新增、编辑、删除、借阅）
    │   ├── CategoryList.tsx      # 分类管理
    │   ├── MyBorrows.tsx         # 我的借阅
    │   └── AllBorrows.tsx        # 全部借阅（管理员）
    ├── types/                    # TypeScript 类型定义
    │   └── index.ts
    └── utils/                    # 工具函数
        └── auth.ts               # Token 存储工具
```

## 快速开始

### 前置条件
- Node.js >= 16
- 后端服务已启动（http://localhost:8080）

### 安装依赖
```bash
npm install
```

### 启动开发服务器
```bash
npm run dev
```
访问 http://localhost:5173，Vite 会自动打开浏览器。

### 构建生产版本
```bash
npm run build
```
构建产物在 `dist/` 目录。

### 预览生产构建
```bash
npm run preview
```

## 核心功能

### 1. Axios 封装（api/request.ts）
- 请求拦截器：自动携带 Token（`Authorization: Bearer <token>`）
- 响应拦截器：统一处理业务错误码，401 自动跳转登录页
- HTTP 错误处理：401/403/404/500 等状态码统一提示
- 封装 get/post/put/del 方法，直接返回 `ApiResponse<T>`

### 2. 路由守卫（router/index.tsx）
- `RequireAuth` 组件：未登录自动跳转登录页
- 监听 Token 变化，Token 失效时自动跳转
- 管理员路由动态显示（借阅记录菜单仅管理员可见）

### 3. 状态管理（store/userStore.ts）
- Zustand 轻量级状态管理
- persist 中间件持久化用户信息到 localStorage
- Token 单独管理（utils/auth.ts），便于拦截器访问

### 4. 权限控制
- 管理员可见：新增图书、编辑图书、删除图书、全部借阅记录菜单
- 普通用户可见：借阅图书、我的借阅
- 前端权限控制仅做 UI 隐藏，真正的权限控制在后端

### 5. 开发代理
- Vite 开发服务器配置代理：`/api` 请求转发到 `http://localhost:8080`
- 解决开发环境跨域问题
- 生产环境由 Nginx 或后端服务统一处理

## 与后端对接

后端 API 基础路径：`/api`（由 Vite 代理到 `http://localhost:8080/api`）

| 模块 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 认证 | /auth/login | POST | 登录 |
| 认证 | /auth/register | POST | 注册 |
| 图书 | /books | GET | 分页查询图书 |
| 图书 | /books/:id | GET | 图书详情 |
| 图书 | /books | POST | 新增图书（管理员） |
| 图书 | /books/:id | PUT | 更新图书（管理员） |
| 图书 | /books/:id | DELETE | 删除图书（管理员） |
| 分类 | /categories | GET | 分类列表 |
| 借阅 | /borrows/:bookId | POST | 借阅图书 |
| 借阅 | /borrows/:recordId/return | PUT | 归还图书 |
| 借阅 | /borrows/my | GET | 我的借阅记录 |
| 借阅 | /borrows | GET | 全部借阅记录（管理员） |

## 学习要点

1. **React 18 函数组件 + Hooks**：所有组件使用函数式写法，useState/useEffect 管理状态和副作用
2. **TypeScript 类型安全**：完整的类型定义，API 响应、表单数据、组件 Props 都有类型
3. **Ant Design 5 组件库**：Table/Form/Modal/Select/Pagination 等企业级组件的使用
4. **React Router v6**：嵌套路由、动态路由、编程式导航
5. **Axios 拦截器**：请求/响应拦截的实际应用
6. **Zustand 状态管理**：轻量级状态管理的最佳实践
7. **Vite 构建工具**：极速开发体验、代码分割、环境变量配置

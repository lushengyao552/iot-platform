# 图书馆管理系统 - 项目说明

## 项目概述

基于 Spring Boot + React 的前后端分离图书管理系统，支持用户认证、图书管理、借阅归还、分类管理、操作日志等功能，集成 Redis 缓存、分布式锁与 RabbitMQ 异步消息。

## 技术栈

### 后端
- **框架**: Spring Boot 3.2.x, Spring MVC, Spring Validation
- **ORM**: MyBatis-Plus 3.5.x
- **数据库**: MySQL 8.0
- **缓存**: Redis（缓存、分布式锁、计数器）
- **消息队列**: RabbitMQ（Topic 交换机、死信队列、延迟消息、Confirm 机制）
- **认证**: JWT 无状态认证 + BCrypt 密码加密 + 自定义拦截器
- **接口文档**: Knife4j (Swagger)
- **构建工具**: Maven
- **JDK**: 17

### 前端
- **框架**: React 18 + TypeScript
- **UI 组件库**: Ant Design 5
- **HTTP 客户端**: Axios
- **构建工具**: Vite
- **路由**: React Router

## 目录结构

```
library-management/
├── backend/                            # 后端（Spring Boot）
│   ├── src/main/java/com/example/library/
│   │   ├── LibraryApplication.java     # 启动类
│   │   ├── common/
│   │   │   ├── result/                 # 统一响应封装 (Result, ResultCode)
│   │   │   ├── exception/              # 全局异常处理 (BusinessException, GlobalExceptionHandler)
│   │   │   └── constant/               # 常量定义
│   │   ├── config/                     # 配置类 (Redis, RabbitMQ, Knife4j, MyBatisPlus, WebMvc)
│   │   ├── controller/                 # 控制器层 (Auth, Book, Borrow, Category, User)
│   │   ├── dto/                        # 数据传输对象 (请求参数)
│   │   ├── entity/                     # 实体类 (User, Book, Borrow, BookCategory, NotificationMessage, OperationLogMessage)
│   │   ├── interceptor/                # JWT 认证拦截器
│   │   ├── mapper/                     # MyBatis Mapper 接口
│   │   ├── service/                    # 服务层接口
│   │   │   └── impl/                   # 服务层实现
│   │   ├── util/                       # 工具类 (JwtUtil, RedisService, MessageProducer)
│   │   └── vo/                         # 视图对象 (响应数据)
│   ├── src/main/resources/
│   │   ├── application.yml             # 主配置文件
│   │   └── db/schema.sql               # 数据库初始化脚本
│   ├── notification-service/           # 通知微服务（RabbitMQ 消费者）
│   ├── pom.xml                         # Maven 构建
│   └── build.gradle / settings.gradle  # Gradle 构建
├── frontend/                           # 前端（React 18 + Vite + TS）
│   ├── src/
│   ├── package.json
│   └── vite.config.ts
├── docs/                               # 学习文档
├── 学习笔记/                           # 学习笔记
└── docker-compose.yml                  # 中间件编排（MySQL/Redis/RabbitMQ）
```

## 核心模块

### 1. 用户认证模块 (Auth)
- 登录/注册/登出
- JWT Token 生成与校验
- BCrypt 密码加密
- 角色权限控制 (ADMIN / USER)

### 2. 图书管理模块 (Book)
- 图书 CRUD
- 多条件分页查询
- 库存管理
- 分类关联

### 3. 借阅管理模块 (Borrow)
- 借书/还书
- 借阅记录查询
- @Transactional 事务保证库存扣减与记录创建的原子性
- Redis 分布式锁防止并发超卖
- RabbitMQ 异步发送借阅/归还通知与操作日志

### 4. 分类管理模块 (Category)
- 图书分类 CRUD
- 分类下图书数量统计

### 5. 消息队列模块 (RabbitMQ)
- Topic 交换机路由不同类型消息
- 死信队列 + TTL 实现借阅到期延迟提醒
- 生产者 Confirm 回调保证消息可靠发送
- 消费者手动 ACK 保证消息可靠消费

## 数据库表

- `sys_user` - 用户表
- `book` - 图书表
- `book_category` - 图书分类表
- `borrow_record` - 借阅记录表
- `operation_log` - 操作日志表

## 常用命令

### 后端
```bash
cd backend

# 编译
mvn clean compile
# 或
./gradlew build -x test

# 启动
mvn spring-boot:run
# 或
./gradlew bootRun

# 打包
mvn clean package -DskipTests
```

### 前端
```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建
npm run build
```

## 开发规范

1. **统一响应**: 所有接口返回 `Result<T>` 格式，包含 code、message、data、timestamp
2. **异常处理**: 业务异常抛出 `BusinessException`，由 `GlobalExceptionHandler` 统一捕获
3. **参数校验**: 使用 `@Valid` + `jakarta.validation` 注解校验请求参数
4. **事务管理**: 涉及多表写操作的方法使用 `@Transactional(rollbackFor = Exception.class)`
5. **缓存使用**: 热点数据读缓存，写操作后更新/删除缓存
6. **日志记录**: 使用 `@Slf4j` 记录关键操作日志

## API 接口

- 基础路径: `/api`
- 接口文档: `http://localhost:8080/api/doc.html`
- 后端端口: 8080
- 前端端口: 5173

## 注意事项

1. Redis 和 RabbitMQ 为可选依赖，未启动时代码有降级处理，不影响基础功能
2. 数据库密码配置在 `application.yml` 中，本地开发环境密码为 `ll521521`
3. JWT 密钥配置在 `application.yml` 的 `library.jwt.secret` 中
4. 前端通过 Vite 代理将 `/api` 请求转发到后端 `http://localhost:8080`

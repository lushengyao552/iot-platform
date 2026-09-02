# 图书管理系统 - Java 主流技术栈学习项目

> 一个基于 Spring Boot 3 + MyBatis-Plus 的企业级 Java 后端项目，涵盖主流开发技术与最佳实践。

## 一、项目简介

本项目是一个功能完整的图书管理系统，包含用户认证、图书管理、分类管理、借阅管理等核心模块。
项目严格遵循企业级开发规范，采用分层架构，是学习主流 Java 后端开发的理想练手项目。

### 核心功能

- **用户认证**：注册、登录、JWT 无状态认证
- **图书管理**：图书的增删改查、分页搜索、库存管理
- **分类管理**：图书分类的增删改查
- **借阅管理**：借书、还书、逾期罚款、借阅记录查询
- **权限控制**：管理员 / 普通用户角色区分
- **Redis 缓存**：图书详情缓存 + 借阅分布式锁
- **RabbitMQ 消息队列**：异步通知 + 延迟到期提醒 + 死信队列
- **微服务架构**：独立通知微服务，通过 MQ 异步通信
- **Docker Compose**：一键启动 MySQL + Redis + RabbitMQ

## 学习文档（重点推荐）

| 文档 | 内容 | 适合场景 |
|------|------|----------|
| [01-知识点深度解析](docs/01-知识点深度解析.md) | 每个技术点的原理、为什么这么做、如何修改调整 | 深入理解技术原理 |
| [02-面试八股文大全](docs/02-面试八股文大全.md) | Java基础/JVM/并发/Spring/MySQL/Redis/RabbitMQ/微服务/设计模式/项目相关，13大主题全覆盖 | 面试准备 |
| [03-从0开始手写步骤](docs/03-从0开始手写步骤.md) | 17步从零搭建完整项目，每步有详细代码和验证方法 | 动手实践，自己复现一遍 |

> 建议学习路径：先跑通项目 → 读知识点解析理解原理 → 跟着手写步骤自己写一遍 → 用八股文准备面试

## 二、技术栈

| 分类 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 语言 | Java | 17 LTS | 长期支持版本，企业主流 |
| 框架 | Spring Boot | 3.2.x | 微服务开发框架 |
| ORM | MyBatis-Plus | 3.5.x | 国内最主流的 ORM 框架 |
| 数据库 | MySQL | 8.0+ | 关系型数据库 |
| 构建工具 | Maven | 3.9.x | 使用 Wrapper，无需全局安装 |
| 认证 | JWT (JJWT) | 0.11.x | 无状态 Token 认证 |
| 参数校验 | Spring Validation | - | JSR-380 规范实现 |
| 接口文档 | Knife4j | 4.4.x | Swagger 增强版 |
| 工具库 | Hutool | 5.8.x | Java 工具类库 |
| 代码简化 | Lombok | - | 注解简化 POJO |
| 测试 | JUnit 5 + Mockito | - | 单元测试框架 |

## 三、项目结构

```
library-management/                 # 前后端一体仓库
├── backend/                        # 后端（Spring Boot + MyBatis-Plus）
│   ├── mvnw / mvnw.cmd             # Maven Wrapper 脚本
│   ├── gradlew / gradlew.bat       # Gradle Wrapper 脚本
│   ├── .mvn/wrapper/               # Maven Wrapper 配置
│   ├── gradle/                     # Gradle Wrapper 配置
│   ├── pom.xml                     # Maven 依赖管理
│   ├── build.gradle / settings.gradle  # Gradle 构建配置
│   ├── notification-service/       # 通知微服务（RabbitMQ 消费者）
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/library/
│   │   │   │   ├── LibraryApplication.java       # 启动类
│   │   │   │   ├── common/                        # 通用组件
│   │   │   │   │   ├── config/                    # 配置类
│   │   │   │   │   │   ├── MyBatisPlusConfig.java    # MyBatis-Plus 配置（分页插件）
│   │   │   │   │   │   ├── WebMvcConfig.java         # Web 配置（拦截器、跨域）
│   │   │   │   │   │   └── MyMetaObjectHandler.java  # 自动填充处理器
│   │   │   │   │   ├── exception/                 # 异常处理
│   │   │   │   │   │   ├── BusinessException.java     # 业务异常
│   │   │   │   │   │   └── GlobalExceptionHandler.java # 全局异常处理器
│   │   │   │   │   └── result/                    # 统一响应
│   │   │   │   │       ├── Result.java                # 统一响应封装
│   │   │   │   │       └── ResultCode.java            # 状态码枚举
│   │   │   │   ├── controller/                    # 控制层（接收请求）
│   │   │   │   │   ├── AuthController.java
│   │   │   │   │   ├── UserController.java
│   │   │   │   │   ├── BookController.java
│   │   │   │   │   ├── BookCategoryController.java
│   │   │   │   │   └── BorrowController.java
│   │   │   │   ├── service/                       # 业务层（业务逻辑）
│   │   │   │   │   ├── UserService.java
│   │   │   │   │   ├── BookService.java
│   │   │   │   │   ├── BookCategoryService.java
│   │   │   │   │   ├── BorrowService.java
│   │   │   │   │   └── impl/                      # 业务实现
│   │   │   │   ├── mapper/                        # 数据访问层
│   │   │   │   │   ├── UserMapper.java
│   │   │   │   │   ├── BookMapper.java
│   │   │   │   │   ├── BookCategoryMapper.java
│   │   │   │   │   └── BorrowRecordMapper.java
│   │   │   │   ├── entity/                        # 实体类（对应数据库表）
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── Book.java
│   │   │   │   │   ├── BookCategory.java
│   │   │   │   │   └── BorrowRecord.java
│   │   │   │   ├── dto/                           # 数据传输对象（接收请求参数）
│   │   │   │   │   ├── LoginDTO.java
│   │   │   │   │   ├── RegisterDTO.java
│   │   │   │   │   ├── BookQueryDTO.java
│   │   │   │   │   ├── BookAddDTO.java
│   │   │   │   │   └── BookUpdateDTO.java
│   │   │   │   ├── vo/                            # 视图对象（返回前端数据）
│   │   │   │   │   ├── LoginVO.java
│   │   │   │   │   ├── UserVO.java
│   │   │   │   │   ├── BookVO.java
│   │   │   │   │   ├── CategoryVO.java
│   │   │   │   │   └── BorrowRecordVO.java
│   │   │   │   ├── interceptor/                   # 拦截器
│   │   │   │   │   └── JwtInterceptor.java
│   │   │   │   └── util/                          # 工具类
│   │   │   │       ├── JwtUtil.java
│   │   │   │       └── UserContext.java
│   │   │   └── resources/
│   │   │       ├── application.yml                 # 应用配置
│   │   │       ├── mapper/                         # MyBatis XML 映射文件
│   │   │       └── db/
│   │   │           └── schema.sql                   # 数据库初始化脚本
│   │   └── test/                                    # 测试代码
│   │       └── java/com/example/library/
│   │           ├── LibraryApplicationTests.java
│   │           └── service/
│   │               ├── UserServiceTest.java
│   │               └── BorrowServiceTest.java
├── frontend/                       # 前端（React 18 + Vite + TS）
│   ├── src/                        # 页面、组件、API、路由、状态管理
│   ├── package.json
│   ├── vite.config.ts
│   └── ...
├── docs/                           # 学习文档
├── 学习笔记/                       # 学习笔记
├── docker-compose.yml              # 中间件编排（MySQL/Redis/RabbitMQ）
└── README.md
```

## 四、快速开始

### 4.1 环境准备

- **JDK 17+**（已确认安装）
- **MySQL 8.0+**（推荐通过 docker-compose 启动）
- **Maven / Gradle**：无需安装，项目自带 Wrapper

### 4.2 启动 MySQL / Redis / RabbitMQ

```powershell
# 在项目根目录启动三个中间件
docker-compose up -d
```

### 4.3 初始化数据库

Docker Compose 首次启动时会自动执行 `backend/src/main/resources/db/schema.sql` 初始化数据库。

如需手动初始化：
```powershell
# 登录 MySQL（输入你的 root 密码）
mysql -u root -p

# 执行初始化脚本
source C:/path/to/library-management/backend/src/main/resources/db/schema.sql
```

或者直接在命令行执行：
```powershell
mysql -u root -p < backend/src/main/resources/db/schema.sql
```

### 4.4 修改数据库配置

编辑 `backend/src/main/resources/application.yml`，修改数据库密码：

```yaml
spring:
  datasource:
    username: root
    password: 你的MySQL密码  # 修改这里
```

### 4.5 启动项目

```powershell
# 进入项目目录
cd library-management\backend

# 使用 Maven Wrapper 编译并运行（首次会自动下载 Maven 和依赖）
.\mvnw.cmd spring-boot:run
# 或使用 Gradle Wrapper
.\gradlew.bat bootRun
```

启动成功后访问：
- **接口文档**：http://localhost:8080/api/doc.html
- **健康检查**：http://localhost:8080/api/auth/health

### 4.6 运行测试

```powershell
cd library-management\backend

# 运行所有单元测试
.\mvnw.cmd test

# 运行指定测试类
.\mvnw.cmd test -Dtest=UserServiceTest
```

## 五、学习路径

### 第一阶段：理解项目骨架（1-2 天）

1. **阅读 `pom.xml`**：理解 Maven 依赖管理，认识每个依赖的作用
2. **阅读 `application.yml`**：理解 Spring Boot 配置方式
3. **阅读 `LibraryApplication.java`**：理解 Spring Boot 启动原理
4. **运行项目**：通过接口文档（Knife4j）体验所有接口

### 第二阶段：掌握分层架构（2-3 天）

1. **Entity 层**：阅读 `entity/` 包，理解 MyBatis-Plus 注解
2. **Mapper 层**：阅读 `mapper/` 包，理解 BaseMapper 提供的 CRUD 方法
3. **Service 层**：阅读 `service/` 包，理解业务逻辑封装和事务管理
4. **Controller 层**：阅读 `controller/` 包，理解 RESTful API 设计

**重点理解**：DTO → Entity → VO 的数据转换流程

### 第三阶段：深入基础设施（2-3 天）

1. **统一响应**：`Result.java` + `ResultCode.java`
2. **全局异常处理**：`GlobalExceptionHandler.java` + `BusinessException.java`
3. **JWT 认证**：`JwtUtil.java` + `JwtInterceptor.java` + `UserContext.java`
4. **参数校验**：DTO 中的 `@NotBlank`、`@Size` 等注解
5. **自动填充**：`MyMetaObjectHandler.java`

### 第四阶段：核心业务深入（2-3 天）

1. **借阅流程**：`BorrowServiceImpl.borrowBook()` — 理解事务、库存原子操作
2. **归还流程**：`BorrowServiceImpl.returnBook()` — 理解逾期罚款计算
3. **分页查询**：`BookServiceImpl.pageBooks()` — 理解动态条件构建
4. **权限控制**：Controller 中的 `checkAdmin()` 方法

### 第五阶段：测试与优化（1-2 天）

1. **单元测试**：阅读 `UserServiceTest.java`，学习 Mockito 用法
2. **集成测试**：`LibraryApplicationTests.java`
3. **尝试扩展**：添加新功能（如图书搜索高亮、借阅提醒等）

## 六、核心知识点解析

### 6.1 分层架构

```
请求 → Controller → Service → Mapper → 数据库
         ↑           ↑         ↑
        DTO       Entity    Entity
         ↓           ↓
        VO         VO
```

- **Controller**：接收请求，参数校验，调用 Service，返回 Result
- **Service**：业务逻辑，事务管理，调用 Mapper
- **Mapper**：数据访问，SQL 执行
- **DTO**：接收前端请求参数
- **VO**：返回前端的数据（去除敏感字段，补充关联信息）
- **Entity**：对应数据库表的实体类

### 6.2 统一响应格式

所有接口返回统一格式：
```json
{
  "code": 20000,
  "message": "操作成功",
  "data": { ... },
  "timestamp": 1696000000000
}
```

### 6.3 JWT 认证流程

```
1. 用户登录 → 验证用户名密码 → 生成 JWT Token → 返回给前端
2. 前端请求时在 Header 中携带: Authorization: Bearer <token>
3. JwtInterceptor 拦截请求 → 验证 Token → 解析用户信息 → 存入 UserContext
4. Controller/Service 通过 UserContext 获取当前用户
5. 请求结束后清除 UserContext（防止内存泄漏）
```

### 6.4 事务管理

使用 `@Transactional` 注解声明事务：
```java
@Transactional(rollbackFor = Exception.class)
public BorrowRecordVO borrowBook(Long userId, Long bookId) {
    // 多个数据库操作在同一事务中，要么全部成功，要么全部回滚
    bookMapper.decreaseStock(bookId, 1);  // 扣减库存
    borrowRecordMapper.insert(record);      // 创建借阅记录
}
```

### 6.5 库存原子操作

防止并发超卖，使用 SQL 层面的原子操作：
```java
@Update("UPDATE book SET stock = stock - 1 WHERE id = #{bookId} AND stock >= 1")
int decreaseStock(Long bookId, Integer count);
```
通过 `WHERE stock >= 1` 保证不会扣减为负数，返回值为 0 表示库存不足。

## 七、API 接口概览

### 认证接口（无需登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/login | 用户登录 |
| POST | /api/auth/register | 用户注册 |
| GET | /api/auth/health | 健康检查 |

### 用户接口（需登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/users/me | 获取当前用户信息 |

### 图书接口（需登录）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /api/books | 分页查询图书 | 所有用户 |
| GET | /api/books/{id} | 查询图书详情 | 所有用户 |
| POST | /api/books | 新增图书 | 管理员 |
| PUT | /api/books/{id} | 更新图书 | 管理员 |
| DELETE | /api/books/{id} | 删除图书 | 管理员 |

### 分类接口（需登录）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /api/categories | 查询所有分类 | 所有用户 |
| GET | /api/categories/{id} | 查询分类详情 | 所有用户 |
| POST | /api/categories | 新增分类 | 管理员 |
| PUT | /api/categories/{id} | 更新分类 | 管理员 |
| DELETE | /api/categories/{id} | 删除分类 | 管理员 |

### 借阅接口（需登录）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | /api/borrows/{bookId} | 借阅图书 | 所有用户 |
| PUT | /api/borrows/{recordId}/return | 归还图书 | 所有用户 |
| GET | /api/borrows/me | 查询我的借阅记录 | 所有用户 |
| GET | /api/borrows | 查询所有借阅记录 | 管理员 |
| GET | /api/borrows/{id} | 查询借阅记录详情 | 所有用户（仅自己的） |

## 八、测试账号

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 管理员 | admin | admin123 | 拥有所有权限 |
| 普通用户 | user | user123 | 可借阅图书 |
| 普通用户 | zhangsan | user123 | 测试用户 |

## 九、常见问题

### Q1: 启动时报错 "Communications link failure"
**A**: MySQL 服务未启动。执行 `Start-Service -Name MySQL80` 启动服务。

### Q2: 启动时报错 "Access denied for user 'root'@'localhost'"
**A**: 数据库密码错误。修改 `application.yml` 中的 `spring.datasource.password`。

### Q3: 首次运行 mvnw.cmd 很慢
**A**: 首次需要下载 Maven 和项目依赖，取决于网络速度。后续运行会使用本地缓存。

### Q4: 接口文档页面打不开
**A**: 确认项目已启动，访问地址为 `http://localhost:8080/api/doc.html`。

### Q5: 如何添加新的业务模块？
**A**: 按照以下步骤：
1. 在 `entity/` 创建实体类
2. 在 `mapper/` 创建 Mapper 接口
3. 在 `service/` 创建 Service 接口和实现
4. 在 `controller/` 创建 Controller
5. 在 `dto/` 和 `vo/` 创建对应的数据对象

## 十、扩展练习建议

学完基础后，可以尝试以下扩展：

1. **图书搜索**：集成 Elasticsearch 实现全文检索
2. **文件上传**：实现图书封面上传功能
3. **数据导出**：导出借阅记录为 Excel
4. **消息通知**：图书到期前发送邮件提醒
5. **缓存优化**：使用 Redis 缓存热门图书
6. **接口限流**：使用 AOP + 注解实现接口限流
7. **操作日志**：使用 AOP 记录用户操作日志
8. **Docker 部署**：编写 Dockerfile 和 docker-compose.yml

---

**祝你学习愉快！如有疑问，可通过接口文档（Knife4j）调试每个接口，结合代码阅读加深理解。**

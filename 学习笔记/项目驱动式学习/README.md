# 项目驱动式学习课程索引

> 基于 `library-management` 项目的真实代码，从零带你吃透这个 Java 后端项目。
>
> 学习方式：**项目源码 → 基础知识 → 原理 → 修改代码 → 实践**，每课结合项目真实代码，包含练习、自测题、面试题。

---

## 学习路线总览

| 阶段 | 目标 | 课程 | 状态 |
|------|------|------|------|
| **阶段一：看懂项目** | 完全看懂项目所有代码 | 第1-10课 | ✅ |
| **阶段二：核心技术深入** | 深入理解 Redis、MQ、前端、校验、分页 | 第11-16课 | ✅ |
| **阶段三：能扩展功能** | 独立新增功能、AOP、Docker、文档 | 第17-20课 | ✅ |
| **阶段四：能独立开发 + 面试** | Code Review、SQL优化、Redis进阶、MQ进阶、面试高频 | 第21-25课 | 🔧 第21课已生成，其余待续 |
| **阶段五：AI 应用开发** | Spring AI、RAG、Tool Calling、Agent | 第26课+ | ⏳ 待生成 |

---

## 第 0 课：学习总纲与知识地图（先读这篇）

- 我的当前技术水平判断（0-5 级）
- 项目技术栈地图（React → Spring Boot → MySQL，旁路 Redis / RabbitMQ / 通知微服务）
- 知识地图（⭐ 必须掌握 · 🟡 了解即可 · ⚪ 暂时不用学）
- 项目代码 ABC 分类（哪些值得学、哪些别浪费时间）
- 学习路线 + 升级路线（V1 → V7 → AI/Agent）
- 项目文件学习顺序（先看哪个、后看哪个、哪些先别碰）

---

## 阶段一：看懂项目（第1-10课）

### 第01课：项目整体运行流程
- 从浏览器到数据库的完整请求链路
- 项目技术栈全景图（Spring Boot + MyBatis-Plus + MySQL + Redis + RabbitMQ + React）
- 项目目录结构详解
- 一个借阅请求的完整生命周期

### 第02课：Spring Boot 启动原理
- `@SpringBootApplication` 注解拆解（@Configuration + @EnableAutoConfiguration + @ComponentScan）
- 自动配置原理（spring.factories / AutoConfiguration.imports）
- 启动流程源码级解析
- 内嵌 Tomcat 启动原理

### 第03课：Controller 层详解
- `@RestController` vs `@Controller`
- `@RequestMapping` / `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping`
- `@PathVariable` / `@RequestParam` / `@RequestBody` 的区别
- HTTP 基础（方法、状态码、Header、Body、JSON）
- 项目里 BookController 逐行解析

### 第04课：Spring IOC 与依赖注入
- 什么是 IOC（控制反转）？什么是 DI（依赖注入）？
- Bean 的生命周期
- `@Autowired` vs `@Resource` vs 构造器注入
- `@Component` / `@Service` / `@Repository` / `@Configuration` 的区别
- `@Bean` 的用法
- 项目里的依赖注入实战

### 第05课：Service 层与事务
- Service 层的职责
- `@Transactional` 注解详解
- 事务的传播行为（7种）
- 事务的隔离级别
- 事务失效的场景（重点！）
- 项目里 BorrowServiceImpl 借阅业务解析（扣库存、创建记录、事务）

### 第06课：MyBatis-Plus 入门
- MyBatis-Plus 是什么？和 MyBatis 的关系
- BaseMapper 内置方法（CRUD）
- IService / ServiceImpl
- QueryWrapper / LambdaQueryWrapper 条件构造器
- 分页插件配置
- 逻辑删除 `@TableLogic`
- 自动填充 `@TableField`
- 项目里 BookMapper 实战

### 第07课：Entity / DTO / VO 三层对象
- 为什么需要三层对象？
- Entity（实体）：对应数据库表
- DTO（数据传输对象）：接收前端请求参数
- VO（视图对象）：返回给前端的数据
- BeanUtils.copyProperties 拷贝
- 项目里 Book / BookAddDTO / BookVO 对比
- 什么时候可以合并？什么时候必须分开？

### 第08课：统一返回与全局异常
- 为什么需要统一返回格式？Result 类设计
- 业务异常 BusinessException
- 全局异常处理器 @RestControllerAdvice + @ExceptionHandler
- 异常分类：业务异常、参数校验异常、系统异常
- 项目里 Result / ResultCode / GlobalExceptionHandler 解析
- HTTP 状态码 vs 业务 code 的设计

### 第09课：JWT 认证与拦截器
- 什么是 JWT？结构（Header.Payload.Signature）
- 登录流程：发 Token → 前端存 Token → 请求带 Token → 后端验证
- JwtUtil 工具类（生成、验证、解析）
- HandlerInterceptor 拦截器
- WebMvcConfig 配置拦截路径和放行路径
- UserContext（ThreadLocal）存当前用户
- 项目里登录认证全流程解析

### 第10课：MySQL 表设计与索引
- 项目4张表设计解析（sys_user、book_category、book、borrow_record）
- 字段类型选择（BIGINT、VARCHAR、DECIMAL、DATETIME、TINYINT）
- 主键设计（自增ID vs 雪花ID）
- 索引原理（B+树）
- 聚簇索引 vs 非聚簇索引
- 联合索引与最左前缀原则
- 覆盖索引
- 项目里的索引设计分析

---

## 阶段二：核心技术深入（第11-16课）

### 第11课：Redis 缓存实战
- 什么是 Redis？和 MySQL 的区别
- Redis 数据结构（String、Hash、List、Set、ZSet）
- 项目里 RedisService 封装解析
- Cache Aside 模式（先查缓存→未命中查DB→写缓存；更新时删缓存）
- 缓存三大问题：穿透、击穿、雪崩
- 缓存一致性（最终一致性）
- 分布式锁（SET NX + Lua 脚本解锁）
- 项目里图书详情缓存和借阅分布式锁解析

### 第12课：RabbitMQ 消息队列
- 什么是消息队列？三大作用（异步、解耦、削峰）
- RabbitMQ 核心概念（Producer、Exchange、Queue、Binding、RoutingKey、Consumer）
- Exchange 四种类型（Direct、Topic、Fanout、Headers）
- 项目里 Topic Exchange 和队列配置
- 消息可靠性（ConfirmCallback、ReturnCallback、手动 ACK、持久化）
- 死信队列（DLX）实现延迟消息
- 消息幂等性
- 项目里借阅成功通知和延迟到期提醒解析

### 第13课：React 前端入门
- React 是什么？项目前端技术栈（React 18 + TypeScript + Vite + Ant Design）
- 前端项目结构
- JSX 语法（在 JS 里写 HTML）
- 函数组件
- Props（父传子）
- useState（状态管理）
- useEffect（副作用）
- 受控组件
- 项目里 BookList 页面完整解析

### 第14课：前后端联调
- Axios 是什么？为什么用 Axios
- 项目里 request.ts 封装解析
- 请求拦截器（自动携带 Token）
- 响应拦截器（统一处理 code 和错误）
- Token 存储（localStorage）
- 跨域问题（同源策略）
- 开发环境 Vite 代理
- 生产环境 CORS 配置
- 一个 API 调用的完整流程

### 第15课：参数校验
- 为什么需要参数校验？
- 常用校验注解（@NotNull、@NotBlank、@NotEmpty、@Size、@Min、@Max、@DecimalMin、@Email、@Pattern）
- @Valid vs @Validated 的区别
- 分组校验
- 自定义校验注解
- 级联校验
- 校验失败的全局异常处理
- 项目里 BookAddDTO 校验解析

### 第16课：分页查询实现
- 为什么需要分页？
- MyBatis-Plus 分页插件配置和原理（COUNT + LIMIT）
- IPage 接口核心属性（records、total、current、size、pages）
- 单表分页（selectPage + LambdaQueryWrapper）
- 多表 JOIN 分页（自定义 Mapper + XML）
- 分页查询完整流程
- 前端分页配合（Ant Design Table pagination）
- 分页性能问题（深分页优化、游标分页）

---

## 阶段三：能扩展功能（第17-20课）

### 第17课：新增功能全链路实战
- 以"图书评论"功能为例，完整走一遍新增功能流程
- 需求分析
- 数据库设计（建表 + 索引）
- 后端代码（Entity → DTO → VO → Mapper → XML → Service → Controller）
- 前端代码（API 层 → 类型定义 → 组件 → 引入页面）
- 测试（接口测试 + 边界测试 + 权限测试）
- 综合运用前面所有课程的知识

### 第18课：Spring AOP 面向切面编程
- 什么是 AOP？为什么需要 AOP？
- 核心概念（切面、切点、通知、连接点、织入）
- 五种通知类型（@Before、@After、@AfterReturning、@AfterThrowing、@Around）
- 底层原理：动态代理（JDK 代理 vs CGLIB 代理）
- AOP 不生效的坑（同类内部调用、private/final/static）
- 实战：操作日志切面
- 实战：权限校验切面
- AOP 的应用场景（事务、日志、权限、缓存、监控）

### 第19课：Docker 部署
- 什么是 Docker？为什么需要容器化？
- 核心概念（镜像、容器、Dockerfile、docker-compose、仓库）
- Docker vs 虚拟机
- 项目里 docker-compose.yml 详解（MySQL、Redis、RabbitMQ）
- 数据卷（持久化）
- 网络（容器间用服务名通信）
- 健康检查和 depends_on
- 写后端应用的 Dockerfile（多阶段构建）
- 前端 Nginx 部署
- 常用 Docker 命令

### 第20课：Knife4j 接口文档与阶段三总结
- 什么是 Knife4j/Swagger？为什么需要接口文档？
- 项目里 Knife4j 配置
- 常用注解（@Tag、@Operation、@Parameter、@Schema）
- 文档注解 vs 校验注解
- 接口文档的使用（在线调试、导出、全局 Token）
- 写好接口文档的最佳实践
- 生产环境关闭文档
- 阶段三总结：你现在能做什么？
- 阶段四、五学习路线规划

---

## 每课的标准结构

每课都遵循以下结构，确保学练结合：

1. **从项目代码开始**：先看项目里的真实代码
2. **核心概念讲解**：解释技术是什么、为什么需要
3. **结合项目代码逐行分析**：回到项目，解释每一行代码的作用
4. **本课必须记住的 N 件事**：核心要点总结（3-7条）
5. **本节关键代码**：可复用的代码片段
6. **本节练习**：3个动手练习（从简单到复杂）
7. **自测题**：3-5道带折叠答案的自测题
8. **面试题**：1-3道面试高频题及答题要点
9. **下一课预告**：下节课学什么

---

## 学习建议

1. **按顺序学习**：课程是递进的，前面的知识是后面的基础，不要跳课
2. **边看边动手**：每课的练习都要动手做，不要只看不动手
3. **结合项目代码**：学习时打开项目源码，对照着看，理解每一行
4. **先理解再深入**：先理解概念和用法，再深入原理，不要一开始就钻源码
5. **不要跳过基础学 Agent**：你的目标是 Java 后端 + AI 应用开发，但先把 Java 后端基础打牢（阶段一到四），再学 AI（阶段五）
6. **定期复习**：学完几课后回头复习，用自测题检验掌握程度
7. **主动思考"为什么"**：每学一个技术，都问自己"为什么项目里需要它？不用会怎么样？有没有其他写法？"

---

## 项目信息

- **项目路径**：`library-management/`
- **技术栈**：Spring Boot 3.2.5 + Java 17 + MyBatis-Plus 3.5.5 + MySQL 8.0 + Redis + RabbitMQ + JWT + Knife4j + React 18 + TypeScript + Vite + Ant Design
- **后端端口**：8080，context-path: `/api`
- **接口文档**：http://localhost:8080/api/doc.html
- **前端端口**：5173（开发环境）
- **初始账号**：admin/admin123（管理员），user/user123（普通用户）

---

## 后续课程（待生成）

如果需要继续生成以下课程，告诉我即可：

**阶段四：能独立开发 + 面试（第21-25课）**
- ✅ 第21课：项目 Code Review——找出设计问题、安全问题、性能问题（已生成，见本目录《第21课-项目CodeReview.md》）
- 第22课：SQL 优化——EXPLAIN、慢查询、索引优化、N+1 问题
- 第23课：Redis 进阶——缓存击穿解决方案、Redisson 分布式锁、缓存一致性方案
- 第24课：RabbitMQ 进阶——消息幂等性实战、延迟插件、死信队列实战、消息积压处理
- 第25课：面试高频题——Spring Boot、MySQL、Redis、MQ、项目介绍

**阶段五：AI 应用开发（第26课+）**
- 第26课：Spring AI 入门
- 第27课：LLM API 调用
- 第28课：RAG 检索增强生成
- 第29课：Tool Calling 工具调用
- 第30课：MCP 协议
- 第31课：Agent 智能体开发

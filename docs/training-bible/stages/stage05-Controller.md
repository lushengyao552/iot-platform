# Stage 05：Controller（接口层）

## 阶段目标

把 Service 暴露成 HTTP 接口。掌握 RESTful 风格、请求方式、路径设计、参数绑定、Knife4j 注解。本阶段先不做统一响应和异常（stage06/07 做），先把接口跑通。

## 前置要求

- stage04 毕业

## 本阶段知识点

- @RestController / @Controller 区别
- @RequestMapping 组合注解（@GetMapping/@PostMapping/@PutMapping/@DeleteMapping）
- @RequestBody / @RequestParam / @PathVariable
- @RequestHeader
- RESTful：GET 查、POST 增、PUT 改、DELETE 删
- Knife4j / OpenAPI 注解：@Tag / @Operation / @Parameter / @Schema
- 路径设计：/api/books /api/books/{id} /api/books/page

---

## 题目

### 【第 5-1 题：图书 Controller 骨架】

**业务背景**
图书 Service 有了，现在暴露 HTTP 接口。

**我的任务**
写 BookController：
1. GET /api/books/{id} 查详情
2. GET /api/books/page 分页查询
3. POST /api/admin/books 新增图书
4. PUT /api/admin/books 更新图书
5. DELETE /api/admin/books/{id} 删除图书

**限制条件**
- 接口路径自己设计，要符合 RESTful
- 普通用户和管理员接口路径先不严格区分权限，stage10 再做
- 用 Knife4j 注解给每个接口加描述
- Controller 方法里只做：接参 → 调 Service → 返回，不写业务

**验收标准**
- 启动后访问 http://localhost:8080/doc.html 能看到接口
- 能用 Knife4j 调试每个接口
- 你能说清 @RequestBody 和 @RequestParam 的区别、什么时候用哪个

**思考题**
1. 为什么 POST 新增是 /api/books 而不是 /api/books/add？
2. PUT 和 PATCH 的区别？
3. @RestController 里写 @Controller 会怎么样？
4. GET 请求能加 @RequestBody 吗？为什么不推荐？

---

### 【第 5-2 题：参数绑定细节】

**业务背景**
分页查询接口要接一堆查询参数。

**我的任务**
实现 GET /api/books/page：
- 接 BookQueryDTO 作为查询参数（不用 @RequestBody）
- 分页参数 page、size
- 可选参数 bookName、categoryId、author

**限制条件**
- 必须用对象接参（不要写一长串 @RequestParam）
- 用 @Parameter 注解描述每个参数
- size 上限要限制（比如最大 100），在 Service 或 Controller 做

**验收标准**
- 不传参数能查默认页
- 传 bookName 能模糊查
- size 传 1000 时被钳制到 100
- 你能说清 SpringMVC 是怎么把 query string 绑到对象上的

**思考题**
1. 对象接参和 @RequestParam 逐个接，本质区别？
2. @DateTimeFormat 和 @JsonFormat 分别用在什么场景？
3. 路径变量 /{id} 和请求参数 ?id=，什么时候用哪个？
4. 接口路径大小写、复数（book vs books）有讲究吗？

---

### 【第 5-3 题：分类 Controller 与嵌套路径】

**业务背景**
分类有父子关系，接口可能要表达"某个分类下的子分类"。

**我的任务**
写 CategoryController：
1. GET /api/categories/roots 查一级分类
2. GET /api/categories/{parentId}/children 查二级
3. POST /api/admin/categories 新增
4. PUT /api/admin/categories/{id} 更新
5. DELETE /api/admin/categories/{id} 删除

**限制条件**
- 嵌套路径要符合 RESTful 资源层级
- Knife4j 文档要清晰

**验收标准**
- 嵌套路径能正确路由
- 你能说清 /api/categories/1/children 和 /api/categories?parentId=1 的取舍

**思考题**
1. 什么时候用嵌套路径，什么时候用平铺查询参数？
2. 一个资源在两个路径下出现（比如 /me/books 和 /users/{id}/books），怎么处理？

---

### 【第 5-4 题：用户 Controller（只读 + 注册占位）】

**业务背景**
用户模块的 Controller 先把"查自己信息"和"注册"接口占个位。登录接口 stage09 才做。

**我的任务**
写 UserController：
1. POST /api/auth/register 注册（先接收 username / password / nickname）
2. GET /api/users/me 查当前用户信息（本阶段先写死一个用户 ID，stage09 改成从 Token 解析）

**限制条件**
- 注册接口先不加密密码（stage09 一起做），但要预留位置
- /me 接口现在返回 Mock 数据，stage09 接 JWT

**验收标准**
- 注册接口能写进数据库
- /me 能返回假数据
- 你能说出为什么这两个接口路径不在 /api/admin 下

**思考题**
1. 注册接口为什么不叫 /api/users 然后 POST？
2. /me 这种"当前用户"接口和 /users/{id} 接口怎么选？

---

## 本阶段毕业考试

不看任何资料：
1. 给你"图书收藏"新需求（用户收藏某本书），15 分钟设计出全部 RESTful 接口路径和入参出参
2. Knife4j 文档要能直接调试
3. 随机抽一个接口，让你说出每个注解的作用

## 本阶段反向面试题

1. SpringMVC 的请求处理流程（DispatcherServlet → HandlerMapping → HandlerAdapter → Controller）
2. @RequestMapping 和 @GetMapping 的关系
3. GET / POST / PUT / DELETE 语义，幂等性
4. @RequestBody 底层用什么消息转换器？
5. Knife4j 是怎么把接口信息收集成文档的？

## 常见坑

- POST 接口写成 @GetMapping
- 分页接口用 @RequestBody 接 GET 参数
- 返回 Entity 把密码带出去
- 接口路径复数单数混乱
- Knife4j 注解漏加，文档里参数名看不清

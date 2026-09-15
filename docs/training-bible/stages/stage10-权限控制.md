# Stage 10：权限控制

## 阶段目标

区分普通用户和管理员。用户能借书、还书、查自己记录；管理员能管图书、分类、用户。掌握"路径级"权限和"数据级"权限（越权）。

## 前置要求

- stage09 毕业（有 UserContext）

## 本阶段知识点

- 角色字段：USER / ADMIN
- 拦截器里加角色判断（简单版）
- @PreAuthorize / Spring Security（本阶段可选，先用手写版）
- 水平越权 vs 垂直越权
  - 垂直：普通用户访问管理员接口
  - 水平：用户 A 看用户 B 的借阅记录
- 数据权限：SQL 里带 user_id 条件
- 自定义注解 @RequireAdmin

---

## 题目

### 【第 10-1 题：角色字段与登录返回】

**业务背景**
user 表有 role 字段。登录时要把 role 塞进 JWT。

**我的任务**
1. 注册时默认 role = USER
2. 登录发 Token 时 Claims 加 role
3. 拦截器解析后 UserContext 里带 role
4. /api/users/me 返回 role

**限制条件**
- role 不要用中文
- 枚举用常量或枚举类

**验收标准**
- 普通用户登录 token 里 role=USER
- 手动改数据库把某用户改成 ADMIN，重登后 role 变 ADMIN
- 你能说清为什么 role 放 token 里会有"改了角色但 token 没更新"的问题

**思考题**
1. 改了用户角色，旧 token 怎么办？
2. role 放 JWT 还是每次查数据库？
3. 一个用户能有多个角色吗？

---

### 【第 10-2 题：管理员接口拦截】

**业务背景**
之前 /api/admin/** 路径只是命名，没真拦。

**我的任务**
1. 在 AuthInterceptor 里加判断：如果请求路径是 /api/admin/**，且 UserContext.getRole() != ADMIN → 返回 403
2. 或者写一个注解 @RequireAdmin，在拦截器里检查 HandlerMethod 上有没有这个注解
3. 把所有管理员接口加上 @RequireAdmin

**限制条件**
- 两种方案选一种，自己讲清利弊
- 403 和 401 要区分（未登录 vs 已登录但没权限）

**验收标准**
- 普通用户调 /api/admin/books 返回 403
- 管理员调成功
- 你能说清路径匹配和注解匹配的区别

**思考题**
1. 为什么 401 和 403 要分开？
2. 注解方式比路径方式好在哪？
3. 如果要支持"编辑者"这种中间角色，现在的设计要改吗？

---

### 【第 10-3 题：水平越权防护】

**业务背景**
用户 A 不能看用户 B 的借阅记录。

**我的任务**
1. 写 GET /api/borrow/records 查"我的借阅记录"
2. Service 里强制用 UserContext.getUserId()，不接受前端传 userId
3. 写 GET /api/borrow/records/{id} 查单条借阅记录：
   - 如果记录的 user_id != 当前用户，且当前用户不是 ADMIN → 403
4. （管理员版）GET /api/admin/users/{userId}/borrow/records 只有 ADMIN 能调

**限制条件**
- 禁止"前端不传 userId 就不校验"的写法
- 所有"我的 xxx"接口都必须从 UserContext 取 userId

**验收标准**
- 用户 A 拿自己 token 访问 /api/borrow/records/{B 的记录 id} → 403
- 管理员能访问任何人的记录
- 你能说清水平越权和垂直越权的区别

**思考题**
1. 为什么前端传 userId 不可信？
2. 数据权限在 SQL 层做还是 Java 层做？
3. 怎么用 MyBatis-Plus 拦截器自动加 user_id 条件？（伏笔）

---

### 【第 10-4 题：为什么不直接上 Spring Security】

**业务背景**
本阶段手写了一套。面试官会问为什么不用 Spring Security。

**我的任务**
不写代码，回答：
1. Spring Security 解决了什么问题？
2. 手写版有什么缺陷？
3. 什么时候该上 Spring Security？

**限制条件**
- 本阶段不引入 Spring Security
- 但你要能讲清楚它的存在价值

**验收标准**
- 你能说出 Spring Security 的过滤器链大概长什么样
- 你能说出 @PreAuthorize("hasRole('ADMIN')") 的作用
- 你能判断"这个小项目手写够不够"

**思考题**
1. Spring Security 的认证流程？
2. 过滤器链里有哪些关键过滤器？
3. JWT + Spring Security 怎么集成？

---

## 本阶段毕业考试

不看任何资料：
1. 给你一个新接口"管理员查看任意用户详情"，让你加权限控制
2. 故意写一个"用户 A 改用户 B 昵称"的漏洞，让你找出来
3. 解释垂直/水平越权区别

## 本阶段反向面试题

1. RBAC 是什么？
2. 菜单权限和按钮权限怎么做？
3. 数据权限（部门级）怎么设计？
4. JWT 无状态怎么实现"踢人下线"？（伏笔 Redis）
5. CSRF 是什么？JWT 放在 Header 为什么能防？
6. 接口签名怎么做？

## 常见坑

- 401 和 403 不分
- 管理员接口只靠 URL 命名，没真校验
- "我的"接口接受前端 userId 参数
- 角色改了但 token 里没更新（用户以为自己还是管理员）
- 用 == 比较字符串角色（equals 坑）

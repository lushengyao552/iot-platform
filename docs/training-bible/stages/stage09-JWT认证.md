# Stage 09：JWT 登录认证

## 阶段目标

独立实现"注册 → 登录 → 发 Token → 后续请求带 Token → 拦截器解析 Token → 拿到当前用户"完整链路。理解 JWT 结构、无状态认证、ThreadLocal、拦截器执行顺序。

## 前置要求

- stage08 毕业
- 知道 HTTP 是无状态的

## 本阶段知识点

- 密码加密：BCryptPasswordEncoder（不要 MD5）
- JWT 结构：Header.Payload.Signature
- JJWT / java-jwt 库
- Claims：userId / username / role / exp
- Token 放哪：Authorization: Bearer xxx
- 拦截器 HandlerInterceptor
- ThreadLocal / UserContext
- WebMvcConfigurer 注册拦截器、放行路径
- 拦截器里抛异常为什么全局处理器接不到（要自己写 Response）

---

## 题目

### 【第 9-1 题：密码加密与注册】

**业务背景**
之前注册接口密码是明文存的。现在改成 BCrypt。

**我的任务**
1. 引入 spring-security-crypto（只取 BCryptPasswordEncoder，不引整套 Spring Security）
2. 配置类里 @Bean BCryptPasswordEncoder
3. 注册时：
   - 检查用户名是否已存在
   - 密码 encode 后存库
4. 写一个登录接口：
   - 查用户
   - BCrypt.matches(rawPassword, encodedPassword)
   - 通过后发 Token

**限制条件**
- 禁止 MD5 / SHA-1
- 禁止自己拼加密算法
- 登录失败统一返回"用户名或密码错误"，不要说具体哪个错（防枚举）

**验收标准**
- 数据库里密码是 $2a$ 开头的 BCrypt 串
- 错误密码登录失败
- 你能说清为什么 BCrypt 比 MD5 安全（盐、慢哈希）

**思考题**
1. BCrypt 每次加密结果不一样，怎么比密码？
2. 为什么不能 MD5？彩虹表是什么？
3. 登录失败为什么不区分"用户不存在"和"密码错"？
4. 密码加盐是什么意思？BCrypt 自动加盐吗？

---

### 【第 9-2 题：JWT 工具类】

**业务背景**
登录成功要发 Token。

**我的任务**
1. 引入 JJWT（io.jsonwebtoken:jjwt-api / jjwt-impl / jjwt-jackson）
2. 写 JwtUtil：
   - generateToken(Long userId, String username, String role)
   - parseToken(String token) → Claims
   - 常量：secret、过期时间（比如 2 小时）
3. secret 从 application.yml 读

**限制条件**
- 不要把 secret 硬编码
- 过期时间可配置
- Token 里不要放密码
- 用 HS256 算法

**验收标准**
- 登录成功返回 token
- 用 jwt.io 工具能解开，看到 userId / username
- 过期 Token 解析抛异常
- 你能说清 JWT 三段各是什么

**思考题**
1. JWT 为什么是无状态的？
2. Payload 是 base64 不是加密，能存密码吗？
3. secret 泄露了会怎样？
4. 为什么需要 exp 字段？
5. JWT 被盗怎么办？（伏笔，后面讨论）

---

### 【第 9-3 题：登录接口】

**业务背景**
把 9-1 和 9-2 拼起来。

**我的任务**
1. POST /api/auth/login
2. LoginDTO: username / password
3. 返回 LoginVO: token / nickname / role
4. 登录失败抛 BizException(UNAUTHORIZED)

**限制条件**
- 登录接口要放行（不用 Token）
- Knife4j 上要能调试

**验收标准**
- 正确账密返回 token
- 错误账密返回统一错误
- 你能说出登录成功后前端该把 token 存哪（localStorage vs cookie）

**思考题**
1. 登录成功要不要在 Redis 存一份 Token？（伏笔 stage13）
2. 退出登录怎么实现？无状态 JWT 怎么"注销"？
3. 记住我（更长有效期）怎么设计？

---

### 【第 9-4 题：拦截器 + UserContext】

**业务背景**
后续接口都要知道"当前是谁"。

**我的任务**
1. 写 UserContext（基于 ThreadLocal）：
   - set(UserInfo) / get() / clear()
2. 写 AuthInterceptor implements HandlerInterceptor：
   - preHandle：
     - 从 Authorization 头取 Bearer token
     - 解析失败 → 直接写 401 JSON 响应（不能靠全局处理器）
     - 成功 → UserContext.set(...)
     - return true
   - afterCompletion：UserContext.clear()
3. 写 WebMvcConfig implements WebMvcConfigurer：
   - 注册 AuthInterceptor
   - addPathPatterns("/api/**")
   - excludePathPatterns("/api/auth/**", "/doc.html", "/webjars/**", "/v3/api-docs/**", "/swagger-resources/**")

**限制条件**
- 必须在 afterCompletion 里 clear，防止内存泄漏 / 线程复用串号
- 拦截器里不能直接抛异常让全局处理器接（试试就知道）
- Knife4j 静态资源必须放行

**验收标准**
- 不带 token 访问 /api/users/me 返回 401
- 带 token 返回当前用户
- 并发请求 ThreadLocal 不串
- 你能说清为什么要 clear

**思考题**
1. ThreadLocal 为什么会内存泄漏？怎么解决？
2. Filter 和 Interceptor 区别？执行顺序？
3. 为什么拦截器里抛异常全局处理器接不到？
4. 异步方法（@Async）里 ThreadLocal 还能用吗？
5. WebMvcConfigurer 是干嘛的？

---

### 【第 9-5 题：/api/users/me】

**业务背景**
之前 /me 是 Mock，现在接真数据。

**我的任务**
1. GET /api/users/me
2. 从 UserContext 拿 userId
3. 查数据库返回 UserVO（不要带 password）

**限制条件**
- 禁止从前端传 userId
- 必须从 Token 解析

**验收标准**
- 带 A 的 token 查出来是 A 的信息
- 改 token 里的 userId 解析失败（签名不对）
- 你能说清 JWT 防篡改原理

**思考题**
1. 为什么不能信任前端传的 userId？
2. Token 里的 userId 和数据库里的用户被删了怎么办？
3. 为什么 JWT 能防篡改但不能防泄露？

---

## 本阶段毕业考试

不看任何资料：
1. 从零实现 注册 → 登录 → 发 token → 拦截器解析 → /me 返回当前用户
2. 解释 JWT 三段、ThreadLocal、拦截器执行顺序
3. 让你故意制造一个"拦截器里抛异常全局处理器接不到"的 Bug 并修掉

## 本阶段反向面试题

1. Session vs JWT 区别？
2. JWT 放在 localStorage 还是 Cookie？XSS / CSRF 风险？
3. JWT 续签怎么做？
4. 为什么需要 refresh token？
5. 拦截器和 Filter 谁先执行？
6. ThreadLocal 在 Tomcat 线程池里为什么要 remove？
7. JWT 被盗用怎么办？怎么踢人下线？（伏笔 Redis）

## 常见坑

- secret 太短 / 硬编码
- 没在 afterCompletion clear ThreadLocal
- 拦截器没放行 Knife4j
- 拦截器里抛异常，全局处理器接不到（要手动写 response）
- BCryptPasswordEncoder 没配 Bean
- Token 过期时间太短/太长

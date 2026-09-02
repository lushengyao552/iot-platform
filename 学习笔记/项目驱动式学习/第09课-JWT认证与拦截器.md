# 第9课：JWT 认证与拦截器——登录全链路、`JwtUtil`、`JwtInterceptor`、`UserContext`

> **本课目标**：搞懂 JWT 认证的完整流程，理解 Token 的生成、验证、解析原理，掌握拦截器和 ThreadLocal 的用法。学完这课，你应该能向面试官讲清楚 JWT 的原理和项目里的认证全链路。

---

## 一、从项目代码开始

看登录接口 `AuthController.login`：

```java
@PostMapping("/login")
public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
    LoginVO loginVO = userService.login(loginDTO);
    return Result.success(loginVO);
}
```

再看 `UserServiceImpl.login`：

```java
@Override
public LoginVO login(LoginDTO loginDTO) {
    // 1. 查询用户
    User user = getByUsername(loginDTO.getUsername());
    if (user == null) {
        throw new BusinessException(ResultCode.USERNAME_NOT_FOUND);
    }

    // 2. 检查账号状态
    if (user.getStatus() != null && user.getStatus() == 0) {
        throw new BusinessException(ResultCode.USER_DISABLED);
    }

    // 3. 验证密码（BCrypt 校验）
    if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
        throw new BusinessException(ResultCode.PASSWORD_ERROR);
    }

    // 4. 生成 JWT Token
    String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

    // 5. 构建登录响应
    return LoginVO.builder()
            .token(token)
            .tokenPrefix(tokenPrefix)
            .expiresIn(expiration)
            .user(toVO(user))
            .build();
}
```

登录成功后，前端拿到 Token，后续请求都在请求头带上 `Authorization: Bearer <token>`。

然后看 `JwtInterceptor` 怎么验证：

```java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    // 1. 放行 OPTIONS 预检请求
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
        return true;
    }

    // 2. 从请求头获取 Token
    String authHeader = request.getHeader(header);  // header = "Authorization"
    if (authHeader == null || authHeader.isEmpty()) {
        throw new BusinessException(ResultCode.UNAUTHORIZED);
    }

    // 3. 验证 Token 前缀
    if (!authHeader.startsWith(prefix)) {  // prefix = "Bearer "
        throw new BusinessException(ResultCode.TOKEN_INVALID);
    }

    // 4. 提取 Token
    String token = authHeader.substring(prefix.length());

    // 5. 验证 Token 有效性
    if (!jwtUtil.validateToken(token)) {
        if (jwtUtil.isTokenExpired(token)) {
            throw new BusinessException(ResultCode.TOKEN_EXPIRED);
        }
        throw new BusinessException(ResultCode.TOKEN_INVALID);
    }

    // 6. 解析用户信息，存入 UserContext
    Claims claims = jwtUtil.getClaimsFromToken(token);
    Long userId = claims.get("userId", Long.class);
    String username = claims.getSubject();
    String role = claims.get("role", String.class);

    UserContext.LoginUser loginUser = UserContext.LoginUser.builder()
            .userId(userId).username(username).role(role).build();
    UserContext.setCurrentUser(loginUser);

    return true;  // 放行
}

@Override
public void afterCompletion(...) {
    UserContext.clear();  // 请求结束，清除 ThreadLocal
}
```

这就是完整的认证流程：**登录生成 Token → 后续请求带 Token → 拦截器验证 Token → 解析用户信息 → 存入 ThreadLocal → Controller/Service 使用 → 请求结束清除**。

---

## 二、什么是 JWT？为什么用 JWT？

### JWT 是什么？

JWT（JSON Web Token）是一种**无状态的认证方案**，是一个经过签名的 JSON 字符串，用于在客户端和服务端之间安全地传输信息。

JWT 的标准格式：
```
xxxxx.yyyyy.zzzzz
Header.Payload.Signature
```

由三部分组成，用 `.` 分隔：

| 部分 | 内容 | 例子 |
|------|------|------|
| **Header（头部）** | 类型和加密算法，Base64URL 编码 | `{"alg":"HS256","typ":"JWT"}` → `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9` |
| **Payload（载荷）** | 存放用户信息和声明（claims），Base64URL 编码 | `{"userId":1,"username":"admin","role":"ADMIN","exp":1693660800}` → `eyJ1c2VySWQiOjEsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4iLCJleHAiOjE2OTM2NjA4MDB9` |
| **Signature（签名）** | 用密钥对 Header+Payload 签名，防止篡改 | `HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret)` → `SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c` |

完整 Token 例子：
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4iLCJleHAiOjE2OTM2NjA4MDB9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

### 重要：Payload 是编码不是加密！

JWT 的 Header 和 Payload 只是 **Base64URL 编码**，不是加密。任何人拿到 Token 都可以解码出 Payload 里的内容。

所以：
- **不要在 Payload 里放敏感信息**（如密码、身份证号）
- 项目里只放了 userId、username、role，这些是非敏感信息
- 签名的作用是**防止篡改**，不是**加密内容**

如果 Payload 被篡改了（比如把 role 从 USER 改成 ADMIN），签名验证就会失败，因为签名是基于原始 Payload 计算的。

### 为什么用 JWT 而不是 Session/Cookie？

| 方案 | 原理 | 优点 | 缺点 |
|------|------|------|------|
| **Session/Cookie** | 服务端存 Session，客户端 Cookie 存 SessionID | 服务端可主动失效、安全性高 | 有状态，分布式环境需要 Session 共享（Redis），跨域麻烦 |
| **JWT** | 服务端生成 Token，客户端存 Token，每次请求带上 | 无状态，服务端不需要存，分布式友好，跨域简单 | 无法主动失效（除非黑名单），Token 一旦签发在过期前一直有效，Payload 不能放敏感信息 |

项目用 JWT 的原因：
1. **无状态**：服务端不需要存储 Session，水平扩展方便
2. **前后端分离**：前端是 React，存在 localStorage，每次请求带在 Header 里，跨域简单
3. **微服务友好**：项目有 notification-service 微服务，JWT 可以在多个服务间共享认证
4. **学习价值**：JWT 是当前企业项目的主流认证方案

---

## 三、JWT 的生成——`JwtUtil.generateToken`

看 `JwtUtil.generateToken`：

```java
public String generateToken(Long userId, String username, String role) {
    // 1. 构建 claims（载荷）
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);
    claims.put("username", username);
    claims.put("role", role);

    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + expiration);  // expiration = 86400000ms = 24小时

    // 2. 用 JJWT 库构建 Token
    return Jwts.builder()
            .setClaims(claims)              // 设置自定义声明（userId、username、role）
            .setSubject(username)            // 设置主题（标准声明，存用户名）
            .setIssuedAt(now)                // 签发时间
            .setExpiration(expiryDate)       // 过期时间
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)  // 用 HS256 签名
            .compact();                      // 生成紧凑的 Token 字符串
}
```

### 标准声明（Registered Claims）

JWT 定义了一些标准声明字段：

| 字段 | 全称 | 含义 | 项目里的值 |
|------|------|------|-----------|
| `sub` | subject | 主题，通常存用户名 | username |
| `iat` | issued at | 签发时间 | now |
| `exp` | expiration | 过期时间 | now + 24小时 |
| `iss` | issuer | 签发者 | （项目没设） |
| `aud` | audience | 受众 | （项目没设） |

### 自定义声明（Custom Claims）

除了标准声明，还可以加自定义字段。项目里加了：
- `userId`：用户ID
- `username`：用户名（和 sub 重复了，小问题）
- `role`：角色（ADMIN/USER）

这些自定义声明在验证 Token 后可以解析出来，用于权限判断。

### HS256 对称加密

项目用的是 `SignatureAlgorithm.HS256`，这是**对称加密算法**：
- 加密（签名）和解密（验证）用**同一个密钥**
- 密钥存在服务端的 `application.yml` 里：`library.jwt.secret`
- 客户端不知道密钥，无法伪造签名

对称加密的优缺点：
- 优点：简单、性能好
- 缺点：密钥需要在所有服务间共享，密钥泄露后果严重

另一种是 **RS256（非对称加密）**：用私钥签名，公钥验证。微服务场景下更安全（认证服务持私钥，其他服务持公钥），但性能稍差。项目规模小，用 HS256 足够。

---

## 四、JWT 的验证——`JwtUtil.validateToken`

```java
public boolean validateToken(String token) {
    try {
        Claims claims = getClaimsFromToken(token);
        return claims != null && !claims.getExpiration().before(new Date());
    } catch (Exception e) {
        return false;
    }
}

public Claims getClaimsFromToken(String token) {
    try {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())   // 设置签名密钥
                .build()
                .parseClaimsJws(token)             // 解析并验证签名
                .getBody();                         // 获取载荷
    } catch (Exception e) {
        log.warn("JWT Token 解析失败: {}", e.getMessage());
        return null;
    }
}
```

### 验证过程

1. **解析 Token**：把 Token 按 `.` 拆成三部分
2. **验证签名**：用密钥对 Header+Payload 重新计算签名，和 Token 里的 Signature 对比。如果不一致，说明 Token 被篡改了，抛出异常
3. **验证过期时间**：检查 `exp` 字段是否小于当前时间，如果小于说明已过期
4. **返回 Claims**：验证通过，返回解析出的载荷（用户信息）

如果任何一步失败，`getClaimsFromToken` 返回 null，`validateToken` 返回 false。

---

## 五、拦截器——`JwtInterceptor`

### 拦截器是什么？

Spring MVC 拦截器（`HandlerInterceptor`）可以在 Controller 方法执行**前**、执行**后**、请求**完成后**插入逻辑，类似于 Servlet 的 Filter，但更精细（可以获取 Controller 方法信息）。

三个方法：

| 方法 | 执行时机 | 返回值 | 项目里的用途 |
|------|---------|--------|-------------|
| `preHandle` | Controller 方法执行前 | true=放行，false=拦截 | 验证 Token，解析用户信息 |
| `postHandle` | Controller 方法执行后，视图渲染前 | - | （项目没用） |
| `afterCompletion` | 请求完成后（视图渲染后） | - | 清除 ThreadLocal |

### 项目里的拦截流程

```
请求到达
    │
    ▼
DispatcherServlet
    │
    ▼
JwtInterceptor.preHandle()
    ├─ OPTIONS 请求 → 直接放行
    ├─ 没有 Authorization 头 → 抛 UNAUTHORIZED
    ├─ Token 前缀不对 → 抛 TOKEN_INVALID
    ├─ Token 过期 → 抛 TOKEN_EXPIRED
    ├─ Token 无效 → 抛 TOKEN_INVALID
    └─ 验证通过 → 解析用户信息 → 存入 UserContext → 放行（return true）
    │
    ▼
Controller 方法执行（可以用 UserContext.getCurrentUser() 获取当前用户）
    │
    ▼
JwtInterceptor.afterCompletion()
    └─ UserContext.clear() 清除 ThreadLocal
```

### 白名单配置

不是所有接口都需要登录。看 `WebMvcConfig.addInterceptors`：

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(jwtInterceptor)
            .addPathPatterns("/**")                 // 拦截所有请求
            .excludePathPatterns(                     // 白名单，不拦截
                    "/auth/**",                        // 登录、注册、健康检查
                    "/doc.html",                       // Knife4j 文档
                    "/v3/api-docs/**",                 // OpenAPI 文档
                    "/swagger-ui/**",
                    "/swagger-resources/**",
                    "/webjars/**",
                    "/favicon.ico",
                    "/error"
            );
}
```

白名单里的接口不需要 Token 就能访问：
- `/auth/login`、`/auth/register`：登录注册本身就不能要求登录（死循环）
- `/auth/health`：健康检查
- 接口文档相关：开发调试用

其他所有接口（`/books/**`、`/borrow/**`、`/users/**` 等）都需要 Token。

### OPTIONS 预检请求

```java
if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
    return true;
}
```

浏览器在发送**跨域请求**前，会先发一个 OPTIONS 预检请求，询问服务器是否允许跨域。这个请求不带 Token，如果拦截器不放行，跨域请求就会失败。

项目里 `WebMvcConfig` 配置了 CORS 允许跨域，所以 OPTIONS 请求要直接放行。

---

## 六、`UserContext`——ThreadLocal 存用户信息

### 为什么需要 UserContext？

Controller 方法需要知道"当前是哪个用户在操作"。比如：
- 借阅图书时，需要知道当前用户ID
- 新增/删除图书时，需要判断当前用户是不是管理员
- 查询"我的借阅记录"时，需要当前用户ID

如果每个 Controller 方法都从 Token 里解析用户信息，太重复了。所以在拦截器里统一解析，存入 `UserContext`，Controller/Service 里直接取。

### ThreadLocal 是什么？

`ThreadLocal` 是 Java 提供的**线程局部变量**，每个线程有自己的一份副本，线程之间互不干扰。

```java
public class UserContext {
    private static final ThreadLocal<LoginUser> CURRENT_USER = new ThreadLocal<>();

    public static void setCurrentUser(LoginUser user) {
        CURRENT_USER.set(user);      // 存入当前线程
    }

    public static LoginUser getCurrentUser() {
        return CURRENT_USER.get();    // 从当前线程取
    }

    public static void clear() {
        CURRENT_USER.remove();        // 清除当前线程的变量
    }
}
```

### 为什么用 ThreadLocal？

Spring Boot 用 Tomcat 线程池处理请求，**每个请求由一个线程处理**。在拦截器里把用户信息存入 ThreadLocal，整个请求处理过程中（Controller、Service），同一个线程都能取到这个用户信息。

请求处理完后，线程会被回收到线程池，处理下一个请求。如果不清除 ThreadLocal，下一个请求可能取到上一个请求的用户信息（数据错乱、安全漏洞），还可能导致内存泄漏。

所以 `afterCompletion` 里必须调用 `UserContext.clear()`。

### 项目里的用法

```java
// Controller 里判断是否管理员
private void checkAdmin() {
    if (!UserContext.isAdmin()) {
        throw new BusinessException(ResultCode.FORBIDDEN);
    }
}

// Service 里获取当前用户ID
Long userId = UserContext.getCurrentUserId();
```

---

## 七、登录全链路总结

```
┌─────────────────────────────────────────────────────────────────┐
│                        登录流程（首次）                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  前端 React                        后端 Spring Boot               │
│  ┌──────────┐                     ┌──────────────────┐          │
│  │ 输入账号密码│                     │  AuthController  │          │
│  └────┬─────┘                     │  .login()        │          │
│       │ POST /api/auth/login      └────────┬─────────┘          │
│       │ {username, password}                │                    │
│       ▼                                      ▼                    │
│  ┌──────────┐                     ┌──────────────────┐          │
│  │ 收到响应  │                     │  UserServiceImpl  │          │
│  │ {token,  │◄────────────────────│  .login()         │          │
│  │  user}   │  200 + JSON        │  1.查用户         │          │
│  └────┬─────┘                     │  2.检查状态        │          │
│       │                           │  3.BCrypt校验密码  │          │
│       ▼                           │  4.JwtUtil生成Token│          │
│  ┌──────────┐                     │  5.返回LoginVO    │          │
│  │ 存Token到 │                     └──────────────────┘          │
│  │localStorage│                                                  │
│  └──────────┘                                                   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      后续请求流程（带 Token）                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  前端 React                        后端 Spring Boot               │
│  ┌──────────┐                     ┌──────────────────┐          │
│  │ 点击查询  │                     │  JwtInterceptor  │          │
│  └────┬─────┘                     │  .preHandle()     │          │
│       │ GET /api/books/1          │  1.取Authorization│          │
│       │ Header: Bearer <token>    │  2.提取Token      │          │
│       ▼                           │  3.验证签名+过期   │          │
│  ┌──────────┐                     │  4.解析用户信息    │          │
│  │ 收到图书  │◄────────────────────│  5.存UserContext  │          │
│  │ 详情数据  │  200 + JSON        │  6.放行            │          │
│  └──────────┘                     └────────┬─────────┘          │
│                                              │                    │
│                                     ┌────────▼─────────┐          │
│                                     │  BookController  │          │
│                                     │  .getBookById()  │          │
│                                     │  调用Service      │          │
│                                     └────────┬─────────┘          │
│                                              │                    │
│                                     ┌────────▼─────────┐          │
│                                     │  BookServiceImpl │          │
│                                     │  .getBookById()  │          │
│                                     │  查Redis→查MySQL  │          │
│                                     └──────────────────┘          │
│                                                                   │
│  请求结束后：                                                     │
│  JwtInterceptor.afterCompletion() → UserContext.clear()          │
└─────────────────────────────────────────────────────────────────┘
```

---

## 八、BCrypt 密码加密

项目里密码用 BCrypt 加密存储：

```java
// 注册时加密
user.setPassword(BCrypt.hashpw(registerDTO.getPassword(), BCrypt.gensalt()));

// 登录时校验
if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
    throw new BusinessException(ResultCode.PASSWORD_ERROR);
}
```

### 为什么不用 MD5？

- MD5 是哈希算法，不是加密算法，相同密码哈希值相同，容易被彩虹表破解
- BCrypt 是**自适应哈希算法**，每次加密都包含**随机盐**，相同密码每次加密结果不同
- BCrypt 可以通过调整工作因子（cost）增加计算时间，抵抗暴力破解

### BCrypt 的特点

1. **自动加盐**：`BCrypt.gensalt()` 生成随机盐，盐存在加密结果里，不需要单独存
2. **相同密码不同结果**：因为盐是随机的，`hashpw("123456")` 每次结果都不一样
3. **校验方式**：`checkpw(明文, 密文)` 从密文中提取盐，用相同算法计算明文的哈希，和密文对比
4. **慢哈希**：BCrypt 计算慢（默认工作因子 10，约 100ms），暴力破解成本高

数据库里存的密码（schema.sql 里的初始数据）：
```
$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIu
```
这就是 BCrypt 加密后的结果，包含了算法版本（$2a$）、工作因子（$10$）、盐和哈希值。

---

## 九、Token 过期和刷新

### 当前项目的方案

项目里 Token 过期时间是 24 小时（`library.jwt.expiration: 86400000`）。过期后需要重新登录。

这是最简单的方案，但用户体验不好——用户正在操作时 Token 过期了，被踢回登录页。

### 企业项目的常见方案：双 Token

| Token | 过期时间 | 用途 | 存在哪 |
|-------|---------|------|--------|
| **Access Token** | 短（如 2 小时） | 日常接口认证 | 前端内存/localStorage |
| **Refresh Token** | 长（如 7 天） | 刷新 Access Token | HttpOnly Cookie |

流程：
1. 登录时同时返回 Access Token 和 Refresh Token
2. 日常请求用 Access Token
3. Access Token 过期后，前端用 Refresh Token 调用刷新接口，获取新的 Access Token
4. Refresh Token 也过期了，才需要重新登录
5. Refresh Token 可以存到 Redis 黑名单，实现主动失效

项目目前是单 Token 方案，作为学习项目足够了。如果要做生产级项目，建议升级成双 Token。

### JWT 的缺点：无法主动失效

JWT 是无状态的，Token 一旦签发，在过期前一直有效。如果用户修改密码、被封禁，服务端无法主动让已签发的 Token 失效。

解决方案：
1. **Redis 黑名单**：把需要失效的 Token 存到 Redis，设置过期时间=Token剩余过期时间。拦截器验证 Token 时先查黑名单，如果在黑名单里就拒绝
2. **缩短过期时间**：Token 有效期短一点（如 15 分钟），配合 Refresh Token，降低泄露风险
3. **版本号**：Payload 里加 tokenVersion，用户修改密码时版本号+1，拦截器对比版本号

项目目前没有实现主动失效，作为学习项目可以接受。

---

## 十、本课必须记住的 7 件事

1. **JWT 三部分**：Header（算法类型）、Payload（用户信息，Base64编码非加密）、Signature（签名防篡改），用 `.` 分隔
2. **登录流程**：前端发账号密码 → Service 查用户 → BCrypt 校验密码 → JwtUtil 生成 Token → 返回前端，前端存 localStorage
3. **验证流程**：后续请求 Header 带 `Authorization: Bearer <token>` → JwtInterceptor 拦截 → 验证签名和过期 → 解析用户信息 → 存入 UserContext → 放行
4. **`JwtUtil`**：`generateToken` 用 HS256 对称加密签名，`validateToken` 验证签名和过期，`getClaimsFromToken` 解析载荷
5. **拦截器三个方法**：`preHandle`（执行前，验证Token）、`postHandle`（执行后）、`afterCompletion`（请求完成，清除ThreadLocal）
6. **`UserContext` 用 ThreadLocal**：每个线程独立存用户信息，请求结束必须 clear，否则线程复用时数据错乱+内存泄漏
7. **BCrypt 密码加密**：自动加盐，相同密码每次加密结果不同，`checkpw` 校验，比 MD5 安全

---

## 十一、本节关键代码

```java
// JwtUtil：生成和验证 Token
@Component
public class JwtUtil {
    @Value("${library.jwt.secret}")
    private String secret;
    @Value("${library.jwt.expiration}")
    private Long expiration;

    // 生成 Token
    public String generateToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // 验证 Token
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims != null && !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}

// JwtInterceptor：拦截请求验证 Token
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {
    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, ...) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        Claims claims = jwtUtil.getClaimsFromToken(token);
        UserContext.setCurrentUser(UserContext.LoginUser.builder()
                .userId(claims.get("userId", Long.class))
                .username(claims.getSubject())
                .role(claims.get("role", String.class))
                .build());
        return true;
    }

    @Override
    public void afterCompletion(...) {
        UserContext.clear();  // 必须清除！
    }
}

// UserContext：ThreadLocal 存当前用户
public class UserContext {
    private static final ThreadLocal<LoginUser> CURRENT_USER = new ThreadLocal<>();
    public static void setCurrentUser(LoginUser user) { CURRENT_USER.set(user); }
    public static LoginUser getCurrentUser() { return CURRENT_USER.get(); }
    public static Long getCurrentUserId() { /* ... */ }
    public static boolean isAdmin() { /* ... */ }
    public static void clear() { CURRENT_USER.remove(); }
}
```

---

## 十二、本节练习

### 练习1：手动解码 JWT

登录获取 Token 后，把 Token 复制到 [jwt.io](https://jwt.io) 的 Decoder 里，观察：
- Header 里的 alg 和 typ 是什么？
- Payload 里有哪些字段？userId、username、role、exp、iat、sub 分别是什么值？
- 把 exp 的时间戳转换成日期，是不是 24 小时后？
- 修改 Payload 里的 role 为 ADMIN，Signature 会变吗？验证能通过吗？

> **目的**：理解 JWT 的结构和 Payload 是编码不是加密。

### 练习2：验证 ThreadLocal 必须清除

在 `JwtInterceptor.afterCompletion` 里临时注释掉 `UserContext.clear()`，然后：
1. 用 admin 登录，调用一个接口
2. 用 user 登录，调用同一个接口
3. 在 Controller 里打印 `UserContext.getCurrentUser()`，观察是否出现用户信息错乱

然后恢复 `clear()`。

> **目的**：理解 ThreadLocal 不清除的后果（线程复用导致数据错乱）。

### 练习3：给项目加 Token 黑名单（主动失效）

在 Redis 里加一个黑名单 key 前缀 `library:token:blacklist:`，实现：
1. 登出接口：把当前 Token 存入 Redis，过期时间=Token剩余过期时间
2. 拦截器验证 Token 时，先查 Redis 黑名单，如果在黑名单里就拒绝

> **提示**：用 `redisService.set(key, value, timeout, unit)` 存黑名单，用 `redisService.hasKey(key)` 判断。

---

## 十三、自测题

### Q1：JWT 由哪几部分组成？每部分存什么？Payload 是加密的吗？

<details>
<summary>点击查看答案</summary>

JWT 由三部分组成，用 `.` 分隔：
1. **Header（头部）**：声明类型（typ=JWT）和加密算法（alg=HS256），Base64URL 编码
2. **Payload（载荷）**：存放用户信息和声明，包括标准声明（sub 主题、iat 签发时间、exp 过期时间）和自定义声明（userId、role 等），Base64URL 编码
3. **Signature（签名）**：用密钥对 Header+Payload 签名，防止篡改

**Payload 不是加密的，只是 Base64URL 编码**。任何人拿到 Token 都可以解码出 Payload 内容。所以不要在 Payload 里放敏感信息（如密码）。签名的作用是防止篡改，不是加密内容。如果 Payload 被篡改，签名验证会失败。

</details>

### Q2：项目里登录的完整流程是什么？

<details>
<summary>点击查看答案</summary>

1. 前端发送 POST `/api/auth/login`，请求体带 `{username, password}`
2. `AuthController.login` 接收 `LoginDTO`，调用 `userService.login(loginDTO)`
3. `UserServiceImpl.login`：
   - 根据用户名查询用户（`getByUsername`），不存在抛 `USERNAME_NOT_FOUND`
   - 检查账号状态，禁用抛 `USER_DISABLED`
   - BCrypt 校验密码（`BCrypt.checkpw`），错误抛 `PASSWORD_ERROR`
   - 调用 `jwtUtil.generateToken(userId, username, role)` 生成 JWT Token
   - 构建 `LoginVO`（包含 token、tokenPrefix、expiresIn、userVO）返回
4. Controller 包装成 `Result.success(loginVO)` 返回
5. 前端收到 Token，存到 localStorage
6. 后续请求在 Header 带 `Authorization: Bearer <token>`

</details>

### Q3：`JwtInterceptor` 的 `preHandle` 里做了什么？为什么 `afterCompletion` 里要调用 `UserContext.clear()`？

<details>
<summary>点击查看答案</summary>

`preHandle`（Controller 执行前）：
1. 放行 OPTIONS 预检请求（跨域用，不带 Token）
2. 从请求头获取 `Authorization`，为空抛 `UNAUTHORIZED`
3. 验证前缀是否是 `Bearer `，不对抛 `TOKEN_INVALID`
4. 提取 Token（去掉前缀）
5. 调用 `jwtUtil.validateToken(token)` 验证签名和过期时间，失败抛 `TOKEN_EXPIRED` 或 `TOKEN_INVALID`
6. 解析 Claims，获取 userId、username、role
7. 构建 `LoginUser`，存入 `UserContext`（ThreadLocal）
8. 返回 true 放行

`afterCompletion` 里调用 `UserContext.clear()` 的原因：
- `UserContext` 用 ThreadLocal 存用户信息，每个线程独立
- Spring Boot 用 Tomcat 线程池，处理完请求的线程会被回收，处理下一个请求
- 如果不清除 ThreadLocal，下一个请求可能取到上一个请求的用户信息（数据错乱、安全漏洞）
- 还可能导致内存泄漏（线程池线程长期存活，ThreadLocal 里的对象无法被 GC）
- 所以请求结束后必须清除，这是规范写法

</details>

### Q4：BCrypt 和 MD5 有什么区别？为什么项目用 BCrypt 存密码？

<details>
<summary>点击查看答案</summary>

- **MD5**：是哈希算法，不是加密算法。相同密码哈希值相同，计算快，容易被彩虹表和暴力破解。不适合存密码。
- **BCrypt**：是自适应哈希算法。每次加密自动生成随机盐（盐存在加密结果里），相同密码每次加密结果不同；计算慢（可通过工作因子调整速度），抵抗暴力破解；提供 `checkpw` 方法校验。

项目用 BCrypt 的原因：
1. **自动加盐**：不需要单独存盐字段，`BCrypt.gensalt()` 自动生成
2. **相同密码不同结果**：防止彩虹表攻击，即使两个用户密码相同，数据库里的密文也不同
3. **慢哈希**：BCrypt 计算慢（默认约 100ms），暴力破解成本高
4. **安全标准**：BCrypt 是业界公认的密码存储安全方案，Spring Security 也默认用 BCrypt

</details>

### Q5：JWT 有什么缺点？怎么实现主动失效？

<details>
<summary>点击查看答案</summary>

JWT 的缺点：
1. **无法主动失效**：Token 一旦签发，在过期前一直有效。用户修改密码、被封禁，服务端无法主动让已签发的 Token 失效
2. **Payload 非加密**：不能放敏感信息
3. **Token 体积较大**：比 SessionID 长，每次请求都要带
4. **无法统计在线用户**：无状态，服务端不知道哪些 Token 正在使用

实现主动失效的方案：
1. **Redis 黑名单**：把需要失效的 Token 存到 Redis，过期时间=Token 剩余过期时间。拦截器验证时先查黑名单，在黑名单里就拒绝。这是最常用的方案
2. **缩短过期时间 + Refresh Token**：Access Token 有效期短（如 15 分钟），即使泄露风险也小；用 Refresh Token 刷新 Access Token
3. **版本号机制**：Payload 里加 tokenVersion，用户修改密码时版本号+1，拦截器对比版本号，不一致就拒绝
4. **Redis 白名单**：登录时把 Token 存 Redis（白名单），登出时删除。拦截器验证时查白名单，不在就拒绝。但这违背了 JWT 无状态的初衷，不如直接用 Session

项目目前是单 Token 方案，没有实现主动失效，作为学习项目可以接受。

</details>

---

## 十四、面试题

### 面试题1：JWT 的原理是什么？和 Session 认证有什么区别？

> **答题要点**：
> 1. **JWT 原理**：JWT（JSON Web Token）是一种无状态认证方案，由 Header（算法类型）、Payload（用户信息+过期时间，Base64编码）、Signature（密钥签名防篡改）三部分组成，用 `.` 分隔。登录时服务端生成 Token 返回客户端，客户端后续请求在 Header 携带 Token，服务端验证签名和过期时间后解析用户信息。
> 2. **Session 原理**：用户登录后，服务端创建 Session 存用户信息，生成 SessionID 返回客户端（存在 Cookie），后续请求客户端带 SessionID，服务端从 Session 存储（内存/Redis）取用户信息。
> 3. **区别**：
>    - **状态**：JWT 无状态，服务端不需要存储；Session 有状态，服务端需要存储 Session
>    - **分布式**：JWT 天然支持分布式，任何服务都能验证；Session 需要共享存储（如 Redis Session）
>    - **跨域**：JWT 放在 Header，跨域简单；Session 依赖 Cookie，跨域麻烦
>    - **主动失效**：JWT 无法主动失效（除非黑名单）；Session 可以主动删除
>    - **安全性**：JWT Payload 非加密，不能放敏感信息；Session 信息存在服务端，更安全
>    - **性能**：JWT 每次请求都要验证签名，有一定开销；Session 查存储也有开销
> 4. **项目选择**：项目是前后端分离 + 微服务架构，用 JWT 更合适，无状态、分布式友好、跨域简单。

### 面试题2：你们项目的认证流程是怎样的？拦截器和 ThreadLocal 怎么配合的？

> **答题要点**：
> 1. **登录流程**：前端发账号密码 → AuthController → UserService 查用户 + BCrypt 校验密码 → JwtUtil 生成 Token（HS256 签名，Payload 存 userId/username/role，过期 24 小时）→ 返回 LoginVO（含 Token 和用户信息）→ 前端存 localStorage
> 2. **验证流程**：后续请求 Header 带 `Authorization: Bearer <token>` → JwtInterceptor.preHandle 拦截 → 放行 OPTIONS → 取 Token → JwtUtil 验证签名和过期 → 解析 Claims 获取用户信息 → 存入 UserContext（ThreadLocal）→ 放行
> 3. **使用**：Controller/Service 里通过 `UserContext.getCurrentUser()`、`UserContext.getCurrentUserId()`、`UserContext.isAdmin()` 获取当前用户信息，不需要从 Token 重新解析
> 4. **清除**：请求结束后 JwtInterceptor.afterCompletion 调用 `UserContext.clear()` 清除 ThreadLocal
> 5. **为什么用 ThreadLocal**：Spring Boot 用 Tomcat 线程池，每个请求由一个线程处理，ThreadLocal 保证线程安全（每个线程独立的用户信息）。但线程会复用，请求结束必须清除，否则下一个请求可能取到上一个请求的用户信息（数据错乱+内存泄漏）
> 6. **白名单**：WebMvcConfig 配置拦截器拦截 `/**`，排除 `/auth/**`（登录注册）、接口文档路径等不需要登录的接口

### 面试题3：JWT 怎么实现主动失效？Token 过期了怎么处理？

> **答题要点**：
> 1. **JWT 无法主动失效的原因**：JWT 是无状态的，Token 一旦签发，在过期前一直有效，服务端没有存储 Token 状态，无法主动让某个 Token 失效
> 2. **主动失效方案**：
>    - **Redis 黑名单**：登出/修改密码/封禁用户时，把 Token 存入 Redis 黑名单，key 用 Token 或 Token 的唯一标识（jti），过期时间设为 Token 剩余过期时间。拦截器验证 Token 时先查黑名单，如果在黑名单里就拒绝。这是最常用的方案
>    - **版本号机制**：Payload 里加 tokenVersion 字段，用户表也存当前版本号。用户修改密码时版本号+1，拦截器验证时对比 Token 里的版本号和用户表的版本号，不一致就拒绝
>    - **缩短过期时间**：Access Token 有效期设短（如 15 分钟），即使泄露风险窗口也小，配合 Refresh Token 机制
> 3. **Token 过期处理**：
>    - **单 Token 方案**（项目当前）：过期后返回 401，前端跳登录页，用户重新登录。简单但体验差
>    - **双 Token 方案**（企业级）：Access Token（短，2小时）+ Refresh Token（长，7天）。Access Token 过期后，前端用 Refresh Token 调用刷新接口获取新的 Access Token，用户无感知。Refresh Token 也过期才重新登录。Refresh Token 一般存在 HttpOnly Cookie 防止 XSS，且可以存 Redis 实现主动失效
> 4. **项目现状**：学习项目用单 Token + 24 小时过期，没有实现主动失效和刷新机制。生产环境建议升级为双 Token + Redis 黑名单。

---

## 十五、下一课预告

**第10课：MySQL 表设计与索引——结合项目 4 张表讲数据库设计**

我们会搞清楚：
- 项目里 4 张表的设计思路：sys_user、book_category、book、borrow_record
- 主键、唯一索引、普通索引分别是什么？项目里哪些字段建了索引？为什么？
- 什么是联合索引？最左前缀原则是什么？
- 字段类型怎么选？BIGINT、VARCHAR、DECIMAL、DATETIME、TINYINT 分别适合什么场景？
- 什么是逻辑删除？为什么项目里用 deleted 字段而不是物理删除？
- 表关系设计：一对多、多对多，项目里为什么没有物理外键？
- 什么是数据库三大范式？项目的表设计符合第几范式？
- EXPLAIN 怎么看？type、key、rows 字段分别是什么意思？
- SQL 性能优化的常见手段：索引、避免全表扫描、覆盖索引

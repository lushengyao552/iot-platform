# Stage 13：Redis

## 阶段目标

引入 Redis，解决三类问题：登录 Token 管理（踢人下线）、热门图书缓存、缓存三大问题（穿透/击穿/雪崩）。理解 Redis 数据结构、Spring Data Redis、序列化、过期策略。

## 前置要求

- stage12 毕业
- 本机装了 Redis 或有云 Redis

## 本阶段知识点

- Spring Data Redis / RedisTemplate / StringRedisTemplate
- Redis 五大数据结构（String / Hash / List / Set / ZSet）
- key 设计：业务:类型:id
- TTL 过期
- 序列化：JSON vs JDK
- 缓存 aside 模式
- 缓存穿透（布隆过滤器 / 空值缓存）
- 缓存击穿（热点 key 过期，互斥锁）
- 缓存雪崩（过期时间加随机值）
- Token 存 Redis 实现"注销"

---

## 题目

### 【第 13-1 题：Redis 接入与配置】

**业务背景**
Spring Boot 工程要能连 Redis。

**我的任务**
1. 引入 spring-boot-starter-data-redis
2. application.yml 配 host / port / password / database
3. 写 RedisConfig：RedisTemplate<String, Object>，key 用 StringRedisSerializer，value 用 Jackson2JsonRedisSerializer
4. 写一个临时接口 GET /redis/check，set 一个 key 再 get 回来

**限制条件**
- 不要用 JDK 序列化（存进去乱码）
- key 命名规范："library:模块:标识"
- 必须能序列化 LocalDateTime

**验收标准**
- 接口能 set/get
- redis-cli 里能看到中文不乱码
- 你能说清为什么默认 JDK 序列化不能用

**思考题**
1. RedisTemplate 和 StringRedisTemplate 区别？
2. 为什么用 JSON 序列化？
3. Redis 单线程模型是什么意思？
4. Redis 为什么快？

---

### 【第 13-2 题：登录 Token 存 Redis，支持注销】

**业务背景**
之前 JWT 是无状态的，无法强制下线。现在把 Token 存 Redis。

**我的任务**
1. 登录成功后，token 存 Redis：key = "library:token:{userId}"，value = token，TTL = 2 小时
2. 拦截器解析 Token 后，再查 Redis：
   - key 不存在 → 401（已注销）
   - value 不匹配 → 401（被新登录踢下线）
3. 写 POST /api/auth/logout：删 Redis 里的 key

**限制条件**
- Redis 里的 TTL 和 JWT 本身 exp 要一致
- 同一用户多次登录怎么处理？（新 token 覆盖旧 token，旧 token 失效）
- 你要意识到这一步把"无状态"变成了"有状态"，讲清利弊

**验收标准**
- 登录后 Redis 有 key
- logout 后再带原 token → 401
- 同一账号在两台设备登录，前一台被踢
- 你能说出这种方案和纯 JWT 的取舍

**思考题**
1. 为什么这一步让 JWT 不再"无状态"？
2. Redis 挂了怎么办？所有用户都被踢下线吗？
3. 为什么不直接用 Session？
4. 续期怎么做？每次请求刷新 TTL？

---

### 【第 13-3 题：热门图书缓存】

**业务背景**
图书详情接口被频繁调用，每次查数据库。

**我的任务**
1. GET /api/books/{id} 改成：
   - 先查 Redis key "library:book:{id}"
   - 命中直接返回
   - 未命中 → 查数据库 → 写 Redis（TTL 30 分钟）→ 返回
2. 更新图书时，删 Redis 里的 key（cache aside）

**限制条件**
- 必须用 cache aside 模式（先更库再删缓存）
- 不要用 @Cacheable（先手写理解原理）
- 缓存空值防穿透（查不存在的 id 也缓存一个标记，TTL 短一点）

**验收标准**
- 第一次查打数据库
- 第二次走缓存（可以打开 MyBatis SQL 日志看有没有 SQL）
- 更新图书后缓存被删
- 查不存在的 id，不会每次打数据库

**思考题**
1. 为什么是"更库后删缓存"，而不是"更库后更缓存"？
2. 先删缓存再更库，并发下会出什么问题？
3. 缓存和数据库一致性怎么保证？
4. 为什么缓存空值能防穿透？

---

### 【第 13-4 题：缓存三大问题】

**业务背景**
上面的简单缓存还有三个经典坑。

**我的任务**
1. **穿透**：查不存在的 id（比如 id=-1），每次都打数据库
   - 修复：缓存空值（13-3 已做）或布隆过滤器（讲思路）
2. **击穿**：某个热点 key 过期瞬间，大量请求同时打到数据库
   - 修复：互斥锁（setnx），只放一个请求去查库
3. **雪崩**：大量 key 同时过期
   - 修复：TTL 加随机值（30min + random 0-5min）

**限制条件**
- 三个问题分别写代码或写方案
- 击穿用 setnx 或 Redisson 都行，先手写 setnx
- 布隆过滤器讲思路即可

**验收标准**
- 你能清楚说出三个问题的区别
- 你能说出每个问题的修复方案
- 你能说出 setnx 怎么实现互斥

**思考题**
1. 穿透和击穿区别？
2. 雪崩怎么从根源上防？
3. 布隆过滤器原理？为什么会有假阳性？
4. Redisson 解决了什么？
5. 缓存预热怎么做？

---

## 本阶段毕业考试

不看任何资料：
1. 给你一个新需求"用户信息缓存"，设计 key、TTL、更新策略、失效策略
2. 现场解释穿透/击穿/雪崩区别
3. 给一段有缓存问题的代码，让你找出来并修

## 本阶段反向面试题

1. Redis 为什么单线程还快？
2. Redis 持久化 RDB vs AOF？
3. Redis 分布式锁怎么实现？setnx 的坑？
4. Redis 和 MySQL 一致性方案？
5. 缓存和 DB 双写，T+0 还是 T+1？
6. Redis 集群主从、哨兵、Cluster 区别？

## 常见坑

- JDK 序列化乱码
- key 没设 TTL，内存爆
- 缓存忘了删，数据脏
- 缓存 null 不设短 TTL
- setnx 不带过期时间，死锁
- 击穿用 sync（JVM 锁，分布式失效）

# 第11课：Redis 缓存实战——图书详情缓存、Cache Aside、缓存一致性

> **本课目标**：搞懂项目里 Redis 是怎么用的，理解缓存的核心概念、Cache Aside 模式、缓存一致性问题，以及分布式锁的实现原理。学完这课，你应该能独立在项目中正确使用 Redis 缓存，并能向面试官讲清楚缓存的常见问题和解决方案。

---

## 一、从项目代码开始

看 `BookServiceImpl.getBookById` 方法：

```java
@Override
public BookVO getBookById(Long id) {
    String cacheKey = BOOK_CACHE_PREFIX + id;  // "library:book:1"

    // 1. 先查缓存
    BookVO cachedBook = redisService.get(cacheKey, BookVO.class);
    if (cachedBook != null) {
        log.debug("图书详情缓存命中, bookId={}", id);
        return cachedBook;  // 缓存命中，直接返回，不查数据库
    }

    // 2. 缓存未命中，查数据库
    Book book = getById(id);
    if (book == null) {
        throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
    }

    // 3. 转换为 VO 并写入缓存（30分钟过期）
    BookVO bookVO = toVO(book);
    redisService.set(cacheKey, bookVO, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
    log.debug("图书详情写入缓存, bookId={}, expire={}分钟", id, CACHE_EXPIRE_MINUTES);

    return bookVO;
}
```

再看更新和删除时怎么处理缓存：

```java
// 更新图书时
@Override
@Transactional(rollbackFor = Exception.class)
public BookVO updateBook(Long id, BookUpdateDTO updateDTO) {
    // ... 更新数据库 ...
    updateById(book);

    // 清除缓存（保证缓存一致性）
    evictBookCache(id);

    return getBookById(id);  // 重新查询（会重新写入缓存）
}

// 删除图书时
@Override
@Transactional(rollbackFor = Exception.class)
public void deleteBook(Long id) {
    // ... 删除数据库 ...
    removeById(id);

    // 清除缓存
    evictBookCache(id);
}

// 清除缓存的方法
private void evictBookCache(Long bookId) {
    try {
        redisService.delete(BOOK_CACHE_PREFIX + bookId);
        log.debug("清除图书缓存, bookId={}", bookId);
    } catch (Exception e) {
        // 缓存清除失败不影响主业务
        log.warn("清除图书缓存失败, bookId={}", bookId, e);
    }
}
```

这就是项目里 Redis 缓存的完整用法：**查询时先查缓存，未命中查数据库并写缓存；更新/删除时清除缓存**。

---

## 二、什么是 Redis？为什么需要缓存？

### Redis 是什么？

Redis（Remote Dictionary Server）是一个**基于内存的键值对（Key-Value）数据库**，数据存在内存中，读写速度极快。

和 MySQL 的区别：

| | MySQL | Redis |
|---|-------|-------|
| 存储位置 | 磁盘 | 内存 |
| 速度 | 慢（毫秒级，磁盘 IO） | 快（微秒级，内存读写） |
| 数据结构 | 表（行和列） | 键值对（String、Hash、List、Set、ZSet 等） |
| 持久化 | 永久持久化 | 支持 RDB/AOF 持久化，但主要用作缓存 |
| 用途 | 持久化存储、复杂查询 | 缓存、计数器、分布式锁、排行榜、消息队列 |

### 为什么需要缓存？

以图书详情查询为例：
- 不使用缓存：每次查询都要访问 MySQL，执行 SQL，磁盘 IO，耗时约 10-100ms
- 使用缓存：第一次查 MySQL 并写入 Redis，后续查询直接从 Redis 读，耗时约 0.1-1ms

缓存的作用：
1. **提高读取速度**：内存比磁盘快 100-1000 倍
2. **减轻数据库压力**：热点数据从缓存读，减少数据库查询次数
3. **提升系统吞吐量**：数据库能承载的 QPS 有限，缓存可以承接大量读请求

### 什么数据适合缓存？

- **读多写少**：图书详情、用户信息、分类列表（读多写少，缓存命中率高）
- **热点数据**：经常被访问的数据（如热门图书）
- **一致性要求不高**：允许短时间内数据不一致（如图书详情，更新后 30 分钟内缓存可能是旧的，但项目里更新时会主动清除缓存）

**不适合缓存的数据**：
- 写多读少（如库存，每次借阅都要改，缓存意义不大）
- 一致性要求极高（如账户余额，必须实时准确）
- 数据量大且访问频率低（缓存浪费内存）

---

## 三、Redis 的数据结构

Redis 支持多种数据结构，项目里主要用了 **String**：

| 数据结构 | 说明 | 项目里的用法 |
|---------|------|-------------|
| **String** | 最基础的键值对，value 可以是字符串、数字、序列化对象 | 图书详情缓存 `library:book:1` → BookVO JSON；分布式锁 `library:lock:borrow:1:2` → UUID |
| Hash | 键值对里的 value 又是一个键值对（类似 Map） | （项目没用，适合存对象的多个字段） |
| List | 有序列表，可重复 | （项目没用，适合消息队列、最新列表） |
| Set | 无序集合，不可重复 | （项目没用，适合标签、去重） |
| ZSet | 有序集合，每个元素有分数，按分数排序 | （项目没用，适合排行榜） |

### 项目里的 String 用法

```java
// 缓存图书详情：key = "library:book:1", value = BookVO 对象（序列化为 JSON）
redisService.set("library:book:1", bookVO, 30, TimeUnit.MINUTES);

// 读取缓存
BookVO book = redisService.get("library:book:1", BookVO.class);

// 删除缓存
redisService.delete("library:book:1");

// 分布式锁：key = "library:lock:borrow:1:2", value = UUID
redisService.tryLock("library:lock:borrow:1:2", uuid, 10, TimeUnit.SECONDS);
redisService.unlock("library:lock:borrow:1:2", uuid);
```

### Key 的命名规范

项目里的 key 用冒号 `:` 分隔，形成层级结构：
- `library:book:1`：library 项目 → book 模块 → id=1 的图书
- `library:lock:borrow:1:2`：library 项目 → lock（锁）→ borrow 模块 → userId=1, bookId=2

这是 Redis 的通用命名规范，用冒号分隔，在 Redis 客户端里会显示成文件夹结构，便于管理。

---

## 四、Cache Aside 模式（旁路缓存）

项目里用的缓存策略是 **Cache Aside（旁路缓存）**，这是最常用的缓存模式。

### 读取流程

```
请求查询数据
    │
    ▼
先查 Redis 缓存
    ├─ 缓存命中 → 直接返回数据
    └─ 缓存未命中
         │
         ▼
    查 MySQL 数据库
         │
         ▼
    把数据写入 Redis 缓存（设置过期时间）
         │
         ▼
    返回数据
```

对应项目代码：
```java
// 1. 先查缓存
BookVO cachedBook = redisService.get(cacheKey, BookVO.class);
if (cachedBook != null) {
    return cachedBook;  // 命中直接返回
}

// 2. 未命中查数据库
Book book = getById(id);

// 3. 写入缓存
BookVO bookVO = toVO(book);
redisService.set(cacheKey, bookVO, 30, TimeUnit.MINUTES);

return bookVO;
```

### 更新/删除流程

```
请求更新/删除数据
    │
    ▼
更新 MySQL 数据库
    │
    ▼
删除 Redis 缓存（不是更新缓存，是删除）
    │
    ▼
下次查询时缓存未命中，重新从数据库加载最新数据
```

对应项目代码：
```java
// 更新数据库
updateById(book);

// 删除缓存（不是更新，是删除）
evictBookCache(id);
```

### 为什么是删除缓存而不是更新缓存？

1. **并发安全**：如果两个请求同时更新，更新缓存可能导致缓存是旧值（A 更新数据库→B 更新数据库→B 更新缓存→A 更新缓存，最后缓存是 A 的旧值）。删除缓存不会有这个问题，下次查询重新加载最新值
2. **性能**：更新缓存需要序列化对象，删除缓存只是一个 DEL 操作，更快。而且如果数据更新后很少被查询，更新缓存是浪费
3. **简单**：删除缓存逻辑更简单，不容易出错

### 为什么先更新数据库再删除缓存？

顺序很重要：
- **先更新数据库，再删除缓存**（项目采用）：如果删除缓存失败，缓存是旧值，但有过期时间兜底，最终一致
- **先删除缓存，再更新数据库**：如果更新数据库期间有查询，会把旧值重新写入缓存，导致缓存一直是旧值（直到过期），问题更严重

所以正确顺序是：**先更新数据库，再删除缓存**。

---

## 五、缓存的三大问题：穿透、击穿、雪崩

这是面试高频考点，必须掌握。

### 缓存穿透

**问题**：查询一个**数据库里根本不存在**的数据，缓存里也没有，每次请求都打到数据库。

比如有人恶意请求 `GET /api/books/999999`（ID 不存在），每次都缓存未命中，都查数据库，数据库压力大。

**解决方案**：
1. **缓存空值**：查询不到数据时，在缓存里存一个空值（如 null 或特殊标记），设置较短的过期时间。后续查询直接从缓存返回空，不查数据库
2. **布隆过滤器**：在缓存前加一层布隆过滤器，把所有存在的 ID 存到布隆过滤器里。查询时先过布隆过滤器，如果布隆过滤器说不存在，直接返回，不查缓存和数据库

项目里目前没有处理缓存穿透（图书不存在时直接抛异常，没有缓存空值）。作为学习项目可以接受，生产项目应该处理。

### 缓存击穿

**问题**：某个**热点 key** 在缓存过期的瞬间，有**大量并发请求**同时查询这个 key，缓存都未命中，所有请求都打到数据库。

比如某本热门图书的缓存在 12:00 过期，12:00 刚好有 1000 个用户同时查询这本书，1000 个请求都查数据库。

**解决方案**：
1. **互斥锁**：缓存未命中时，先获取分布式锁，只有获取到锁的请求查数据库并写缓存，其他请求等待后重试查缓存
2. **逻辑过期**：缓存里不设置物理过期时间，而是在 value 里存一个逻辑过期时间。查询时如果逻辑过期了，异步更新缓存，当前请求返回旧值（不阻塞）
3. **热点数据永不过期**：对于确定的热点数据，不设置过期时间，更新时主动删除缓存

项目里的图书详情缓存用了 30 分钟过期，没有处理缓存击穿。但图书详情的并发量一般不会特别高，作为学习项目可以接受。

### 缓存雪崩

**问题**：**大量 key 在同一时间集中过期**，或者 Redis 宕机，导致大量请求同时打到数据库。

比如 1000 本图书的缓存都在 12:00 过期，12:00 后所有查询都未命中，数据库压力骤增。

**解决方案**：
1. **过期时间加随机值**：在基础过期时间上加一个随机数（如 30 分钟 + 0-5 分钟随机），避免大量 key 同时过期
2. **Redis 集群**：Redis 主从/集群部署，避免单点故障
3. **服务降级**：数据库压力过大时，返回默认值或降级页面，保护数据库
4. **多级缓存**：本地缓存（Caffeine）+ Redis 缓存，Redis 挂了还有本地缓存兜底

项目里所有图书缓存都是固定 30 分钟过期，有雪崩风险。但项目规模小，风险可控。生产项目应该给过期时间加随机值。

### 三个问题的区别

| 问题 | 原因 | 特点 |
|------|------|------|
| **穿透** | 查询不存在的数据 | 缓存和数据库都没有，每次都打数据库 |
| **击穿** | 热点 key 过期 | 一个 key，大量并发，都打数据库 |
| **雪崩** | 大量 key 同时过期 / Redis 宕机 | 很多 key，都打数据库 |

记忆口诀：**穿透查不存在，击穿热点过期，雪崩大量过期**。

---

## 六、缓存一致性

### 问题

缓存和数据库是两个存储，更新数据库后删除缓存，但在删除缓存之前，可能有查询读到了旧缓存。或者在高并发下，可能出现缓存和数据库不一致的情况。

### 项目里的做法

1. **更新数据库后删除缓存**（Cache Aside 模式）
2. **缓存设置过期时间**（30 分钟），即使不一致，最多 30 分钟后自动恢复（最终一致性）
3. **删除缓存失败时记录日志，不影响主业务**（`evictBookCache` 里 try-catch）

### 为什么是最终一致性而不是强一致性？

要做到缓存和数据库**强一致性**（任何时刻缓存和数据库都一致），需要加分布式锁，性能很差，一般业务不需要。

大多数业务场景接受**最终一致性**：允许短时间内不一致，但经过一段时间后最终会一致。项目里通过"删除缓存 + 过期时间"保证最终一致性：
- 删除缓存后，下次查询会重新加载最新数据
- 即使删除缓存失败，30 分钟后缓存过期，也会重新加载最新数据

### 双写不一致的极端情况

在极高并发下，可能出现：
1. 请求 A 更新数据库（值从 1 改成 2）
2. 请求 B 查询数据库（读到旧值 1，因为 A 的事务还没提交）
3. 请求 A 提交事务，删除缓存
4. 请求 B 把旧值 1 写入缓存
5. 缓存里是旧值 1，数据库里是新值 2 → 不一致

这种情况发生概率极低（需要精确的时序），而且有过期时间兜底。生产项目如果要求更高一致性，可以用延迟双删（更新数据库前后各删一次缓存）或订阅 binlog 更新缓存。

---

## 七、分布式锁——Redis 的另一个用途

项目里借阅图书时用了 Redis 分布式锁：

```java
@Override
@Transactional(rollbackFor = Exception.class)
public BorrowRecordVO borrowBook(Long userId, Long bookId) {
    // 1. 获取分布式锁
    String lockKey = "library:lock:borrow:" + userId + ":" + bookId;
    String lockValue = UUID.randomUUID().toString();  // 唯一标识，防止误删
    boolean locked = redisService.tryLock(lockKey, lockValue, 10, TimeUnit.SECONDS);
    if (!locked) {
        throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "操作太频繁，请稍后再试");
    }

    try {
        return doBorrowBook(userId, bookId);  // 执行业务逻辑
    } finally {
        redisService.unlock(lockKey, lockValue);  // 释放锁
    }
}
```

### 为什么需要分布式锁？

防止同一用户并发重复借阅同一本书：
- 用户 A 快速点击两次"借阅"按钮，两个请求同时到达
- 没有锁的话，两个请求都查到"未借阅"，都创建借阅记录 → 重复借阅
- 有了分布式锁，第一个请求获取锁执行，第二个请求获取锁失败，提示"操作太频繁"

### 分布式锁的原理

**加锁**：`SET key value NX EX timeout`
- `NX`：key 不存在时才设置成功（如果 key 已存在，设置失败）
- `EX timeout`：同时设置过期时间，防止死锁
- 这是一个原子操作，要么成功要么失败

对应 RedisService 里的代码：
```java
Boolean result = redisTemplate.opsForValue().setIfAbsent(lockKey, value, timeout, unit);
```

**解锁**：不能直接 `DEL key`，因为可能误删别人的锁。比如：
1. 请求 A 获取锁，设置 10 秒过期
2. 请求 A 业务执行了 15 秒（超过 10 秒），锁自动过期了
3. 请求 B 获取了同一个锁
4. 请求 A 执行完，直接 DEL key → 把请求 B 的锁删了！

所以解锁时要先判断 value 是不是自己的，是自己的才删。用 **Lua 脚本**保证判断和删除的原子性：

```lua
if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
else
    return 0
end
```

对应 RedisService 里的代码：
```java
private static final String UNLOCK_SCRIPT =
    "if redis.call('get', KEYS[1]) == ARGV[1] then " +
    "return redis.call('del', KEYS[1]) else return 0 end";

public Boolean unlock(String lockKey, String value) {
    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    script.setScriptText(UNLOCK_SCRIPT);
    script.setResultType(Long.class);
    Long result = redisTemplate.execute(script, Collections.singletonList(lockKey), value);
    return result != null && result == 1;
}
```

### 分布式锁的三个要素

1. **互斥性**：同一时间只有一个客户端能获取锁（SET NX 保证）
2. **防死锁**：锁有过期时间，即使客户端崩溃，锁也会自动释放
3. **解铃还须系铃人**：只能释放自己加的锁（value 唯一标识 + Lua 脚本判断）

---

## 八、`RedisService` 封装分析

项目里的 `RedisService` 是对 `RedisTemplate` 的封装，提供了：

| 方法 | 作用 | 底层 Redis 命令 |
|------|------|----------------|
| `set(key, value)` | 设置缓存（永不过期） | SET |
| `set(key, value, timeout, unit)` | 设置缓存（带过期时间） | SET EX |
| `get(key)` | 获取缓存（返回 Object） | GET |
| `get(key, clazz)` | 获取缓存并转换类型 | GET |
| `delete(key)` | 删除缓存 | DEL |
| `hasKey(key)` | 判断 key 是否存在 | EXISTS |
| `expire(key, timeout, unit)` | 设置过期时间 | EXPIRE |
| `tryLock(key, value, timeout, unit)` | 尝试获取分布式锁 | SET NX EX |
| `unlock(key, value)` | 释放分布式锁（Lua 脚本） | GET + DEL（原子） |
| `increment(key, delta)` | 原子递增 | INCRBY |
| `decrement(key, delta)` | 原子递减 | DECRBY |

### 封装的好处

1. **统一异常处理**：每个方法都 try-catch，Redis 不可用时降级（返回 null/false），不影响主业务
2. **简化 API**：`RedisTemplate` 的 API 比较底层，封装后更易用
3. **统一序列化**：在 `RedisConfig` 里配置了 JSON 序列化，存对象时自动序列化为 JSON

### 降级处理的设计

注意 `RedisService` 里每个方法都有 try-catch，异常时返回 null 或 false：

```java
public Object get(String key) {
    try {
        return redisTemplate.opsForValue().get(key);
    } catch (Exception e) {
        log.warn("Redis get 失败（降级处理）, key={}", key, e);
        return null;  // 降级：返回 null，调用方会认为缓存未命中，去查数据库
    }
}
```

这是**缓存降级**的设计：Redis 挂了，系统还能正常工作（只是性能下降，所有查询都打数据库）。缓存是用来提升性能的，不应该成为系统的单点故障。

---

## 九、本课必须记住的 7 件事

1. **Cache Aside 模式**：查询时先查缓存→未命中查数据库→写缓存；更新时先更新数据库→再删除缓存（不是更新缓存）
2. **缓存三大问题**：穿透（查不存在的数据）、击穿（热点 key 过期大量并发）、雪崩（大量 key 同时过期/Redis宕机）
3. **缓存一致性**：项目用"删除缓存 + 过期时间"保证最终一致性，不是强一致性；删除缓存失败不影响主业务（降级）
4. **Redis 数据结构**：项目主要用 String（缓存对象、分布式锁）；还有 Hash、List、Set、ZSet 等
5. **分布式锁三要素**：互斥性（SET NX）、防死锁（过期时间）、解铃还须系铃人（value唯一标识+Lua脚本原子解锁）
6. **Key 命名规范**：用冒号分隔形成层级，如 `library:book:1`、`library:lock:borrow:1:2`
7. **缓存降级**：Redis 不可用时降级返回 null，调用方查数据库，缓存不应该成为单点故障

---

## 十、本节关键代码

```java
// 查询：Cache Aside 模式
public BookVO getBookById(Long id) {
    String cacheKey = "library:book:" + id;

    // 1. 先查缓存
    BookVO cachedBook = redisService.get(cacheKey, BookVO.class);
    if (cachedBook != null) {
        return cachedBook;  // 命中直接返回
    }

    // 2. 未命中查数据库
    Book book = getById(id);
    if (book == null) {
        throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
    }

    // 3. 写缓存（30分钟过期）
    BookVO bookVO = toVO(book);
    redisService.set(cacheKey, bookVO, 30, TimeUnit.MINUTES);
    return bookVO;
}

// 更新：先更新数据库，再删除缓存
@Transactional(rollbackFor = Exception.class)
public BookVO updateBook(Long id, BookUpdateDTO updateDTO) {
    // 更新数据库
    Book book = new Book();
    book.setId(id);
    BeanUtils.copyProperties(updateDTO, book, "stock");
    updateById(book);

    // 删除缓存（不是更新）
    redisService.delete("library:book:" + id);

    return getBookById(id);  // 重新查询，重新写缓存
}

// 分布式锁：借阅图书
@Transactional(rollbackFor = Exception.class)
public BorrowRecordVO borrowBook(Long userId, Long bookId) {
    String lockKey = "library:lock:borrow:" + userId + ":" + bookId;
    String lockValue = UUID.randomUUID().toString();  // 唯一标识

    // 加锁：SET NX EX（原子操作）
    boolean locked = redisService.tryLock(lockKey, lockValue, 10, TimeUnit.SECONDS);
    if (!locked) {
        throw new BusinessException("操作太频繁，请稍后再试");
    }

    try {
        return doBorrowBook(userId, bookId);  // 业务逻辑
    } finally {
        redisService.unlock(lockKey, lockValue);  // 解锁：Lua脚本判断value后删除（原子）
    }
}
```

---

## 十一、本节练习

### 练习1：给分类列表加缓存

项目里 `BookCategoryService` 的分类列表查询没有缓存。请：
1. 在 `BookCategoryServiceImpl` 里加缓存，key 为 `library:category:list`
2. 查询时先查缓存，未命中查数据库并写缓存（过期时间 1 小时）
3. 新增/更新/删除分类时清除缓存
4. 调用分类列表接口两次，观察日志，第二次应该是缓存命中

### 练习2：验证缓存穿透

调用一个不存在的图书 ID（如 `GET /api/books/999999`），连续调用 10 次，观察日志：
- 每次都查数据库了吗？（是的，因为没有缓存空值）
- 这就是缓存穿透问题

然后优化：在 `getBookById` 方法里，图书不存在时缓存一个空值（用特殊标记，如缓存一个 `id=-1` 的 BookVO），过期时间 1 分钟。再次调用不存在的 ID，观察第二次是否直接从缓存返回。

### 练习3：理解分布式锁的 value 作用

在 `BorrowServiceImpl.borrowBook` 里，把 `unlock` 方法的 value 参数去掉（直接删 key，不判断 value），然后模拟一个场景：
1. 请求 A 获取锁，设置 10 秒过期
2. 请求 A 业务执行 15 秒（可以 Thread.sleep(15000) 模拟）
3. 10 秒后锁自动过期，请求 B 获取锁
4. 请求 A 执行完，直接 DEL key → 把请求 B 的锁删了
5. 请求 C 又能获取锁了 → 锁失效

观察不加 value 判断的危害，然后恢复 value 判断。

---

## 十二、自测题

### Q1：Cache Aside 模式的读取和更新流程是什么？为什么更新时是删除缓存而不是更新缓存？

<details>
<summary>点击查看答案</summary>

**读取流程**：
1. 先查 Redis 缓存，命中则直接返回
2. 未命中则查 MySQL 数据库
3. 把数据库查询结果写入 Redis 缓存（设置过期时间）
4. 返回数据

**更新/删除流程**：
1. 先更新 MySQL 数据库
2. 再删除 Redis 缓存（不是更新缓存）
3. 下次查询时缓存未命中，重新从数据库加载最新数据

**为什么删除缓存而不是更新缓存**：
1. **并发安全**：两个请求同时更新时，更新缓存可能导致缓存是旧值（A更新DB→B更新DB→B更新缓存→A更新缓存，最后缓存是A的旧值）。删除缓存不会有这个问题
2. **性能**：删除缓存只是一个 DEL 操作，比更新缓存（序列化对象）更快。而且如果数据更新后很少被查询，更新缓存是浪费
3. **简单可靠**：删除缓存逻辑更简单，不容易出错，下次查询自动加载最新值

</details>

### Q2：缓存穿透、击穿、雪崩分别是什么？怎么解决？

<details>
<summary>点击查看答案</summary>

**缓存穿透**：查询数据库里根本不存在的数据，缓存和数据库都没有，每次请求都打到数据库。
- 解决：①缓存空值（查询不到时缓存 null 或特殊标记，短过期时间）；②布隆过滤器（把存在的 ID 存到布隆过滤器，不存在的直接返回）

**缓存击穿**：某个热点 key 在缓存过期的瞬间，有大量并发请求同时查询，都未命中，都打到数据库。
- 解决：①互斥锁（缓存未命中时先获取锁，只有获取锁的请求查DB并写缓存，其他等待重试）；②逻辑过期（value 里存逻辑过期时间，过期后异步更新，当前返回旧值）；③热点数据永不过期

**缓存雪崩**：大量 key 在同一时间集中过期，或者 Redis 宕机，导致大量请求同时打到数据库。
- 解决：①过期时间加随机值（避免同时过期）；②Redis 集群（避免单点故障）；③服务降级（数据库压力大时返回默认值）；④多级缓存（本地缓存+Redis）

**区别**：穿透是查不存在的数据，击穿是一个热点 key 过期，雪崩是大量 key 同时过期或 Redis 宕机。

</details>

### Q3：分布式锁怎么实现？为什么解锁要用 Lua 脚本？

<details>
<summary>点击查看答案</summary>

**分布式锁实现（基于 Redis）**：
1. **加锁**：`SET key value NX EX timeout`，原子操作。NX 表示 key 不存在时才设置成功（互斥），EX 设置过期时间（防死锁）
2. **value 用唯一标识**（如 UUID），防止误删别人的锁
3. **解锁**：先判断 value 是不是自己的，是自己的才删除。用 Lua 脚本保证判断和删除的原子性

**为什么解锁要用 Lua 脚本**：
如果不用 Lua 脚本，分两步：①GET key 判断 value；②如果匹配则 DEL key。这两步之间不是原子的，可能出现：
1. 请求 A GET key，发现 value 是自己的，准备删除
2. 此时锁过期了，请求 B 获取了同一个锁（设置了新的 value）
3. 请求 A 执行 DEL key → 把请求 B 的锁删了！

用 Lua 脚本把 GET 和 DEL 打包成一个原子操作，在 Redis 里执行，不会被其他命令打断，就不会出现误删。

Lua 脚本：
```lua
if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
else
    return 0
end
```

**分布式锁三要素**：互斥性（SET NX）、防死锁（过期时间）、解铃还须系铃人（value唯一标识+Lua脚本）。

</details>

### Q4：项目里 Redis 缓存的一致性是怎么保证的？为什么是最终一致性而不是强一致性？

<details>
<summary>点击查看答案</summary>

**项目里的做法**：
1. 更新/删除图书时，先更新数据库，再删除缓存（Cache Aside 模式）
2. 缓存设置 30 分钟过期时间
3. 删除缓存失败时 try-catch 记录日志，不影响主业务（降级）

**为什么是最终一致性**：
- 删除缓存后，下次查询会重新从数据库加载最新数据，最终缓存和数据库一致
- 即使删除缓存失败，30 分钟后缓存自动过期，也会重新加载最新数据
- 在删除缓存之前的短暂时间内，缓存可能是旧值，所以不是强一致性

**为什么不做强一致性**：
- 强一致性需要缓存和数据库在同一个事务里，或者用分布式锁把读和写都串行化，性能很差
- 大多数业务场景（如图书详情）不需要强一致性，允许短时间内数据不一致
- 最终一致性通过"删除缓存 + 过期时间"实现，简单、性能好、可靠性高

**极端不一致场景**：极高并发下可能出现"读旧值写缓存"的情况（A更新DB→B读旧值→A删缓存→B写旧缓存），但概率极低，且有过期时间兜底。

</details>

### Q5：项目里的 RedisService 为什么每个方法都 try-catch？这是什么设计模式？

<details>
<summary>点击查看答案</summary>

这是**缓存降级**设计，也是**熔断/降级**思想的体现。

**为什么每个方法都 try-catch**：
- Redis 是用来提升性能的缓存，不是系统的核心存储
- 如果 Redis 挂了（网络故障、Redis 宕机），不应该导致整个系统不可用
- 每个方法 try-catch，异常时返回 null（get）或 false（tryLock），调用方会认为缓存未命中，转而查数据库
- 这样 Redis 挂了系统还能正常工作，只是性能下降（所有查询都打数据库）

**具体降级逻辑**：
- `get` 异常 → 返回 null → 调用方查数据库
- `set` 异常 → 只记日志 → 不影响主业务，只是没写入缓存
- `delete` 异常 → 返回 false → 缓存可能没清除，但有过期时间兜底
- `tryLock` 异常 → 返回 false → 获取锁失败，提示"操作太频繁"（保守策略）

**设计原则**：缓存是辅助组件，不应该成为单点故障。缓存可用时提升性能，缓存不可用时降级到数据库，保证系统可用性。这也是"缓存穿透"里说的"缓存应该兜底"的思想。

</details>

---

## 十三、面试题

### 面试题1：你们项目里 Redis 是怎么用的？缓存的更新策略是什么？

> **答题要点**：
> 1. **Redis 用途**：
>    - **缓存**：图书详情缓存，key=`library:book:{id}`，value=BookVO（JSON序列化），过期时间 30 分钟
>    - **分布式锁**：借阅图书时防止并发重复借阅，key=`library:lock:borrow:{userId}:{bookId}`，value=UUID，过期 10 秒
> 2. **缓存策略（Cache Aside 旁路缓存）**：
>    - **读取**：先查 Redis，命中直接返回；未命中查 MySQL，把结果写入 Redis（设过期时间），返回
>    - **更新/删除**：先更新 MySQL 数据库，再删除 Redis 缓存（不是更新缓存），下次查询自动加载最新数据
> 3. **缓存一致性**：通过"删除缓存 + 过期时间"保证最终一致性。删除缓存失败时 try-catch 降级（不影响主业务，有过期时间兜底）
> 4. **封装**：RedisService 封装 RedisTemplate，统一异常处理和降级，每个方法 try-catch，Redis 不可用时返回 null/false，系统降级到数据库
> 5. **序列化**：RedisConfig 配置 Jackson2JsonRedisSerializer，对象序列化为 JSON 存储，key 用 StringRedisSerializer
> 6. **待优化点（主动说，体现思考）**：目前没有处理缓存穿透（不存在的 ID 没有缓存空值）、过期时间固定有雪崩风险（应加随机值）、没有处理缓存击穿（热点 key 过期无互斥锁），生产项目应补充这些

### 面试题2：缓存穿透、击穿、雪崩的区别和解决方案？

> **答题要点**：
> 1. **缓存穿透**：查询数据库中不存在的数据，缓存和数据库都没有，每次请求都打到数据库。常见于恶意攻击（查询不存在的 ID）。
>    - 解决方案：①缓存空值/默认值，设置较短过期时间；②布隆过滤器（Bloom Filter），把所有存在的 key 存到布隆过滤器，查询前先过滤，不存在直接返回；③接口层加参数校验（如 ID 范围校验）
> 2. **缓存击穿**：某个热点 key 在缓存过期的瞬间，大量并发请求同时查询，都未命中，全部打到数据库。注意是**一个 key**，且是**热点数据**。
>    - 解决方案：①互斥锁（mutex key）：缓存未命中时先获取分布式锁，只有获取锁的请求查数据库并写缓存，其他请求等待后重试查缓存；②逻辑过期：缓存 value 里存逻辑过期时间，不设物理过期，查询发现逻辑过期后异步更新缓存，当前请求返回旧值（不阻塞）；③热点数据永不过期，更新时主动删除
> 3. **缓存雪崩**：大量 key 在同一时间集中过期，或者 Redis 宕机/集群故障，导致大量请求同时打到数据库，数据库压力骤增甚至宕机。注意是**大量 key** 或 **Redis 整体不可用**。
>    - 解决方案：①过期时间加随机值（如 30分钟 + 0~5分钟随机），避免大量 key 同时过期；②Redis 集群/主从部署，避免单点故障；③服务降级/熔断：数据库压力过大时，非核心接口返回默认值或降级页面，保护核心业务；④多级缓存：本地缓存（Caffeine）+ Redis 缓存，Redis 挂了还有本地缓存兜底；⑤提前预热：热点数据提前加载到缓存
> 4. **区别总结**：穿透是查不存在的数据（缓存和DB都没有），击穿是一个热点 key 过期（大量并发打一个key），雪崩是大量 key 同时过期或 Redis 整体故障（大量请求打DB）

### 面试题3：Redis 分布式锁怎么实现？有什么问题？RedLock 知道吗？

> **答题要点**：
> 1. **基础实现**：
>    - 加锁：`SET key value NX EX timeout`，原子操作。NX（Not Exists）保证互斥，EX 设置过期时间防死锁
>    - value 用唯一标识（UUID/线程ID），防止误删别人的锁
>    - 解锁：用 Lua 脚本保证"判断 value + 删除 key"的原子性，防止误删
> 2. **三要素**：互斥性（同一时间只有一个客户端持有锁）、防死锁（过期时间，客户端崩溃也能释放）、解铃还须系铃人（只能释放自己加的锁）
> 3. **存在的问题**：
>    - **锁过期问题**：业务执行时间超过锁过期时间，锁自动释放，其他客户端获取锁，导致并发问题。解决：续期机制（watch dog 后台线程定时续期），或 Redisson 框架
>    - **主从切换问题**：Redis 主节点加锁成功后，锁还没同步到从节点，主节点宕机，从节点升为主，其他客户端又能加锁，导致锁失效。解决：RedLock 算法
>    - **不可重入**：基础实现不可重入，同一线程不能重复获取同一把锁。解决：用 Hash 结构存线程ID和重入次数
> 4. **RedLock（红锁）**：Redis 官方提出的分布式锁算法，用于解决主从切换导致的锁失效问题。
>    - 原理：部署多个独立的 Redis 主节点（一般 5 个），客户端向所有节点请求加锁，超过半数（N/2+1，如 5 个里 3 个）加锁成功，且总耗时小于锁过期时间，才算加锁成功
>    - 解锁时向所有节点发送解锁命令
>    - 优点：抗单点故障，半数以下节点宕机不影响锁
>    - 缺点：实现复杂，性能稍差，需要部署多组 Redis；业界对 RedLock 的安全性有争议（Martin Kleppmann 与 antirez 的争论），实际项目用 Redisson 更普遍
> 5. **生产实践**：一般不用自己实现，用 **Redisson** 框架，它提供了可重入锁、公平锁、读写锁、RedLock 等实现，内置 watch dog 续期机制，API 简单（`RLock lock = redisson.getLock("key"); lock.lock(); lock.unlock();`）

---

## 十四、下一课预告

**第12课：RabbitMQ 消息队列——为什么用 MQ、生产者消费者、手动 ACK、死信队列**

我们会搞清楚：
- 什么是消息队列？项目里为什么要用 RabbitMQ？
- RabbitMQ 的核心概念：Producer、Exchange、Queue、Binding、RoutingKey、Consumer
- Exchange 的四种类型：Direct、Topic、Fanout、Headers，项目里用的哪种？
- 项目里的消息流程：借阅成功发消息 → notification-service 消费 → 发送通知
- 消息可靠性：ConfirmCallback（消息到达Exchange）、ReturnCallback（无法路由到Queue）、手动 ACK
- 死信队列（DLX）：项目里怎么用死信队列实现延迟消息（到期提醒）？
- 消息重复消费和幂等性怎么处理？
- 消息丢失的三种场景和解决方案

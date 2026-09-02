# 第 21 课 · 项目 Code Review（找出真实问题，学会像工程师一样审视代码）

> 这一课不是讲新知识，而是**把你前 20 课学到的知识，用来「挑刺」**。
>
> 你能看懂代码 ≠ 你能看出代码的问题。而「能不能发现别人代码的问题」，是初级和高级之间最关键的分水岭，也是面试官最爱问的：「**你觉得你这个项目有什么可以改进的地方？**」
>
> 本课严格基于本项目真实代码，逐条指出：**为什么这是问题 → 初学者怎么理解 → 企业项目一般怎么解决**。

---

## 一、先明确：Code Review 在看什么

企业里 Code Review（代码评审）不是「找茬」，而是从 6 个维度审视代码：

| 维度 | 看什么 | 本项目对应 |
|------|--------|-----------|
| **正确性** | 逻辑对不对、有没有 bug | ⚠️ 数据库种子数据的密码 hash 问题 |
| **安全性** | 会不会被攻击、泄露 | ⚠️ 硬编码密钥、CORS 过宽 |
| **性能** | 会不会慢、浪费资源 | ⚠️ N+1 查询、ObjectMapper 重复创建 |
| **可维护性** | 好不好改、好不好读 | ⚠️ Controller 直接用实体、DTO 缺失 |
| **一致性** | 数据会不会错乱 | ⚠️ 事务内发 MQ、逻辑删除+唯一索引 |
| **规范** | 是否符合团队约定 | ✅ 整体规范，有少量可改进点 |

---

## 二、问题总览（按严重程度分级）

| 编号 | 问题 | 严重度 | 位置 |
|------|------|:---:|------|
| P1 | 三个账号用了**同一个 BCrypt 密码 hash**，与注释「admin123 / user123」矛盾 | 🔴 严重 | `schema.sql` 109-111 行 |
| P2 | 数据库密码、JWT 密钥**硬编码**在配置文件 | 🔴 严重（生产） | `application.yml` |
| P3 | CORS 配置 `allowedOriginPatterns("*")` 过宽 | 🟠 一般 | `WebMvcConfig.java` |
| P4 | `toVO()` 里的 **N+1 查询** | 🟠 一般 | `BookServiceImpl` 等 |
| P5 | **事务内发送 MQ 消息**，事务回滚会导致消息已发出 | 🟠 一般 | `BorrowServiceImpl` |
| P6 | **逻辑删除 + 唯一索引**冲突：删除后同名无法再注册 | 🟠 一般 | `schema.sql` uk_username |
| P7 | `MessageProducer` 每次 `new ObjectMapper()` | 🟡 建议 | `MessageProducer.java` |
| P8 | `RedisService.get(key, clazz)` 的 `clazz` 参数没用到 | 🟡 建议 | `RedisService.java` |
| P9 | `BookCategoryController` 用实体接收参数，缺校验 | 🟡 建议 | `BookCategoryController.java` |
| P10 | 分布式锁释放早于事务提交的窗口期 | 🟡 建议（进阶） | `BorrowServiceImpl` |

> 下面挑**最重要的 6 个**（P1～P6）详细展开，P7～P10 给简要点评。你不需要一天全懂，重点先吃透 P1、P4、P5、P6。

---

## 三、重点问题逐条详解

### P1：三个账号用了同一个 BCrypt hash（严重，必懂）

**真实代码**（`src/main/resources/db/schema.sql` 第 108-111 行）：

```sql
INSERT INTO sys_user (username, password, nickname, email, phone, role, status) VALUES
('admin',    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIu', '系统管理员', ..., 'ADMIN', 1),
('user',     '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIu', '普通用户', ..., 'USER', 1),
('zhangsan', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIu', '张三', ..., 'USER', 1);
```

**为什么是问题？**

先回顾第 9 课学的 BCrypt：BCrypt 的 hash 里**自带随机盐（salt）**，所以同一个密码每次加密结果都不一样；反过来，**两个不同密码的 hash 也绝不可能相同**。

现在这三个账号的 hash **完全相同**，只能得出一个结论：**这三个账号的实际密码是同一个**。但文件开头的注释写的是：

```sql
-- 管理员账号：admin / admin123（BCrypt加密）
-- 普通用户：user / user123
```

也就是说注释声称 admin 密码是 `admin123`、user 是 `user123`，但 hash 相同 → 矛盾。**真实结果是：这个 hash 只对应某一个密码，另外两个账号（如果密码真不同）根本登录不进去。**

**初学者怎么理解？**

> 这是一个典型的「数据造假 / 复制粘贴」问题。作者写种子数据时，可能只生成了一次 hash，然后复制粘贴到三行。它不会让程序报错，但会让「测试账号」形同虚设——你可能用 `user/user123` 登录一直失败，却不知道原因。

**企业怎么解决？**

1. 每个账号用**各自真实密码**的 BCrypt hash（用 `BCrypt.hashpw("user123", BCrypt.gensalt())` 分别生成）。
2. 或改用 `CommandLineRunner` 在启动时代码初始化账号（密码在代码里 BCrypt 加密后插入），保证 hash 一定正确。

> 你可以自己验证：写个 `main` 方法，用 Hutool `BCrypt.checkpw("admin123", hash)` 和 `BCrypt.checkpw("user123", hash)` 分别测，看哪个返回 true，就知道这个 hash 实际对应哪个密码了。

---
### P2：密码和密钥硬编码在配置文件（生产环境严重）

**真实代码**（`application.yml`）：

```yaml
spring:
  datasource:
    password: ll521521      # ← 明文密码

library:
  jwt:
    secret: library-management-secret-key-2024-please-change-in-production  # ← JWT 密钥
```

**为什么是问题？**

- `application.yml` 会提交到 Git 仓库，任何能看仓库的人都能看到数据库密码。
- JWT 密钥一旦泄露，攻击者可以**伪造任意用户（包括 admin）的 Token**，等于整道认证防线失效。

**初学者怎么理解？**

> 注释里其实已经写了「生产环境请使用环境变量注入，不要硬编码」——作者是**知道**这个问题的，只是作为学习项目偷懒了。学习项目可以接受，但你面试时必须能说出企业做法。

**企业怎么解决？**

1. 敏感配置用**环境变量**注入：`password: ${DB_PASSWORD}`，部署时从配置中心/密钥管理（Vault、K8s Secret）读取。
2. JWT 密钥用更长的随机串（至少 256 位），并用环境变量注入。
3. 仓库里只留模板 `application.yml.example`，真实敏感配置不进仓库。

---

### P4：N+1 查询（一般，但高频面试题）

**真实代码**（`BookServiceImpl.toVO()`）：

```java
@Override
public BookVO toVO(Book book) {
    // ...
    if (book.getCategoryId() != null) {
        BookCategory category = categoryService.getById(book.getCategoryId()); // 每条 book 查一次分类
        if (category != null) {
            vo.setCategoryName(category.getName());
        }
    }
    // ...
}
```

再看 `BookServiceImpl.pageBooks()`：

```java
IPage<Book> bookPage = page(page, wrapper);   // 第 1 条 SQL：查 10 条 book
return bookPage.convert(this::toVO);          // 每条 book 又调 toVO → 再查 10 次分类
```

**为什么是问题？**

分页查 10 本书，实际执行了 **1 + 10 = 11 条 SQL**。如果一页 100 条，就是 101 条 SQL。这种「查 N 条主记录，再为每条记录查关联数据」的模式叫 **N+1 查询**，是后端性能问题里的经典高频考点。

同样的问题还出现在：
- `BorrowServiceImpl.toVO()`：每条借阅记录再查一次 `userService.getById` + 一次 `bookService.getById`（每页 10 条 = 额外 20 次查询）。
- `BookCategoryServiceImpl.toVO()`：每个分类再 `selectCount` 查一次图书数量。

**初学者怎么理解？**

> 数据库查询是「昂贵」的（网络往返 + 磁盘 IO）。你的直觉可能是「查一次关联数据很快啊」，但当数据量大、并发高时，N+1 会让接口越来越慢。企业里用「批量查询 + 内存组装」来避免。

**企业怎么解决？**

1. **批量查询**：先把这一页 book 的所有 `categoryId` 收集成 `Set`，用 `categoryMapper.selectBatchIds(ids)` 一次查回所有分类，放进 `Map<Long, String>`，再在循环里从 Map 取值。
2. **写 JOIN SQL**（自定义 Mapper + XML）：一条 SQL 直接 `LEFT JOIN book_category`，一次性带回 categoryName。

> 本项目的 Mapper 层目前是「单表查询 + Java 拼装」，是**故意**为了演示逻辑删除、分页插件、DTO↔VO 转换，所以没写 JOIN。但你要知道，真实企业项目里大量用 JOIN 或批量查询来避免 N+1。

---

### P5：事务内发送 MQ 消息（一般，进阶必懂）

**真实代码**（`BorrowServiceImpl`，注意方法上的 `@Transactional`）：

```java
@Override
@Transactional(rollbackFor = Exception.class)
public BorrowRecordVO borrowBook(Long userId, Long bookId) {
    // ... 加锁、校验、扣库存、insert 借阅记录 ...
    sendBorrowNotification(user, book, record);  // ← 在事务里发 MQ 消息
    sendDelayReminder(user, book, record);
    return toVO(record);
}
```

**为什么是问题？**

`@Transactional` 的意思是：方法内的数据库操作要么全成功、要么全回滚。但**事务的提交，发生在方法正常返回之后**（Spring 代理在方法结束后才 `commit`）。

那么就有一种情况：`sendBorrowNotification` 已经把「借阅成功」消息发到了 RabbitMQ，**紧接着事务却因为某种原因回滚了**（比如后续操作抛异常）。结果是：**通知服务已经发消息告诉用户「借阅成功」，但数据库里借阅记录根本没保存**——数据不一致。

**初学者怎么理解？**

> 数据库事务只能管「数据库里的操作」，管不了「已经发出去的 MQ 消息」。消息一旦发出去就收不回来了。所以「在事务还没确定成功之前就发消息」是有风险的——你可能通知了一件其实没发生的事。

**企业怎么解决？**

用 Spring 的**事务同步机制**，把「发消息」延迟到**事务提交成功之后**再执行：

```java
TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
    @Override
    public void afterCommit() {
        messageProducer.sendBorrowSuccessMessage(message); // 提交成功后才发
    }
});
```

或者更简单的做法：**不要在事务方法里发 MQ**，把发消息挪到事务方法外（Controller 层或单独的方法），确保只有数据落库成功才发。

> 本项目里发消息用 try-catch 包着做了「降级处理」（失败只记日志），这解决了「MQ 挂了不影响主业务」，但**没解决「事务回滚但消息已发出」**的反向问题。两者要区分开。

---

### P6：逻辑删除 + 唯一索引冲突（一般，很实用的坑）

**真实代码**（`schema.sql`）：

```sql
CREATE TABLE sys_user (
    ...
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),  -- ← 用户名唯一索引
    ...
);
```

同时 `application.yml` 里配置了逻辑删除：

```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted   # 删除时把 deleted 改成 1，而不是物理 DELETE
```

**为什么是问题？**

「逻辑删除」= 执行删除时，MyBatis-Plus 自动把 SQL 变成 `UPDATE sys_user SET deleted=1 WHERE id=?`，**记录还在表里**。

但 `uk_username` 是**唯一索引**，它不管 `deleted` 是 0 还是 1，只要 `username` 列值重复就报错。

于是：假设用户名 `tom` 被逻辑删除了（`deleted=1`），之后新用户想注册 `tom`，INSERT 时会因为 `username='tom'` 已存在（即使那条是 deleted=1 的）而**撞唯一索引报错**。用户会觉得「这名字怎么注册不了，明明删掉了啊」。

**初学者怎么理解？**

> 逻辑删除只是「打了个标记」，数据物理上还在。唯一索引看的是「列值是否重复」，它可不管你标记没标记删除。这是「逻辑删除」和「唯一约束」天生的一对矛盾。

**企业怎么解决？**（几种方案，从简到繁）

1. **方案 A（最简单）**：唯一索引改为「联合索引」`(username, deleted)`——但这样 `deleted=0` 和 `deleted=1` 两条同名记录能共存，又会引入别的边界问题，一般不推荐。
2. **方案 B（常用）**：删除时把 username 改成「`username + 时间戳/随机串`」，让被删记录不再占用原名字。比如 `tom` 删除后变成 `tom_1696000000`。
3. **方案 C（推荐）**：不要用 `username` 做唯一索引，改在**业务层**用「先查 deleted=0 的 username 是否存在」来保证唯一（本项目 `UserServiceImpl.register` 其实已经做了 `getByUsername` 判重，再叠加数据库唯一索引只是双保险）。对「注销后想立即复用名字」的场景，逻辑删除本身就不太适合。

> 关键认知：**逻辑删除不适合「有唯一约束且需要复用唯一值」的字段**。这是很多初学者踩过的坑，面试能讲出来很加分。

---

## 四、其余问题简要点评（P3、P7～P10）

### P3：CORS 过宽（`WebMvcConfig.java`）

```java
registry.addMapping("/**")
        .allowedOriginPatterns("*")   // 允许所有来源
        .allowCredentials(true);      // 且允许携带 Cookie/凭证
```

- **问题**：允许任意网站跨域访问你的接口，若接口有敏感操作会有风险。
- **解决**：生产环境把 `"*"` 改成具体前端域名，如 `https://myapp.com`。

### P7：每次 new ObjectMapper（`MessageProducer.sendMessage()`）

```java
String json = com.fasterxml.jackson.databind.ObjectMapper.class
        .getDeclaredConstructor().newInstance().writeValueAsString(data);
```

- **问题**：`ObjectMapper` 是线程安全的重对象，创建成本高，每次都 new 一个还用了反射，既慢又别扭。
- **解决**：Spring Boot 已经自动配置了一个单例 `ObjectMapper`，直接 `@Autowired` 注入复用即可。

### P8：`RedisService.get(key, clazz)` 的 clazz 没用上

```java
public <T> T get(String key, Class<T> clazz) {
    Object value = redisTemplate.opsForValue().get(key);
    if (value == null) return null;
    return (T) value;   // clazz 参数完全没参与逻辑
}
```

- **问题**：方法签名里声明了 `Class<T> clazz`，但实现里直接强转，属于「签名骗人」——调用者以为会做类型转换，实际不会。
- **解决**：要么真正用 `clazz.cast(value)` 或 `objectMapper.convertValue(value, clazz)`，要么删掉这个参数。

### P9：Controller 直接用实体接收参数（`BookCategoryController.java`）

```java
@PostMapping
public Result<CategoryVO> addCategory(@RequestBody BookCategory category) { ... }
```

- **问题**：用 `BookCategory`（实体）直接接收请求体，而不是 DTO，导致：①没有参数校验（name 可为空、sort 可为负数）；②实体里的 `createTime/deleted` 等字段可能被前端塞入。
- **解决**：像 `BookAddDTO` 一样，为分类建 `CategoryDTO`，加 `@NotBlank` 校验，再转实体。

### P10：分布式锁释放早于事务提交（进阶，可暂时了解）

`borrowBook` 的锁在 `finally` 里释放，但 `@Transactional` 的提交发生在方法返回后（代理层）。所以**锁已经释放了，事务可能还没提交**，中间有一个极小的窗口期，另一个请求可能读到「库存已扣但记录未提交」的中间态。

- **解决**：把「扣库存 + 插记录」的原子性完全交给数据库事务和「原子扣库存 SQL」（`WHERE stock >= count`，本项目已有），Redis 锁只是「防同一用户重复点击」的辅助，两者职责要理清。

---

## 五、本课必须记住（6 条）

1. **BCrypt hash 自带随机盐**：同一密码每次结果不同；hash 相同 = 密码相同。种子数据三个账号 hash 相同是 bug。
2. **N+1 查询**：循环里查关联数据，N 条主记录产生 N 次额外查询。用批量查询或 JOIN 解决。
3. **事务管不了 MQ 消息**：事务回滚 ≠ 消息撤回。发消息应放到事务提交之后。
4. **逻辑删除 + 唯一索引 = 冲突**：被删记录仍占用唯一值。敏感唯一字段慎用逻辑删除。
5. **敏感配置不能硬编码**：数据库密码、JWT 密钥要用环境变量注入。
6. **「能不能看出问题」比「能不能看懂」更重要**：这是面试「项目有什么改进点」的答案来源。

---

## 六、本节练习

### 练习 1（验证 P1，必做）
写一个临时 `main` 方法，用 Hutool `BCrypt.checkpw("admin123", hash)` 和 `BCrypt.checkpw("user123", hash)` 分别测试，判断 schema.sql 里那个 hash 到底对应哪个密码，验证「三个账号密码其实相同」的结论。

### 练习 2（修复 P7）
把 `MessageProducer.sendMessage()` 里的反射 `new ObjectMapper()` 改成注入 Spring 单例 `ObjectMapper`（提示：在 `@RequiredArgsConstructor` 类里加一个 `private final ObjectMapper objectMapper;`）。

### 练习 3（修复 P4，进阶）
把 `BookServiceImpl.toVO()` 的「每条 book 查一次分类」改成「先收集所有 categoryId 批量查询，再组装」，消除 N+1。可以只改 `pageBooks` 流程。

---

## 七、自测题

<details>
<summary>1. 为什么两个不同账号的 BCrypt hash 不可能相同？</summary>
因为 BCrypt 加密时生成随机盐（salt）并混入 hash 结果，同一密码两次加密结果都不同，不同密码的结果更不可能相同。若两行 hash 相同，说明它们就是同一个密码。
</details>

<details>
<summary>2. 什么是 N+1 查询？本项目哪里出现了？</summary>
查 N 条主记录后，又为每条记录发一条关联查询，共 N+1 条 SQL。本项目出现在 BookServiceImpl/BorrowServiceImpl/BookCategoryServiceImpl 的 toVO 里（每条记录分别查分类/用户/图书）。
</details>

<details>
<summary>3. 为什么「在 @Transactional 方法里发 MQ 消息」有风险？</summary>
事务在方法返回后才提交，若之后回滚，消息已经发出无法撤回，导致「通知发出去了但数据没存」。应在事务提交后（如 afterCommit 回调）再发消息。
</details>

<details>
<summary>4. 逻辑删除遇到唯一索引会怎样？</summary>
逻辑删除只是 UPDATE deleted=1，记录仍在表中，唯一索引仍按列值判重，导致被删的 username 无法被新用户重新注册。
</details>

---

## 八、面试题

**Q1：你觉得你的图书管理项目有哪些可以改进的地方？**

> 答题思路（挑 2-3 个最有把握的展开）：
> - 数据一致性：事务内发 MQ 消息存在「事务回滚但消息已发出」的风险，计划用 TransactionSynchronization 的 afterCommit 解决。
> - 性能：toVO 里存在 N+1 查询，分页查询会放大成 N 次额外查询，计划改成批量查询 + Map 组装或 JOIN。
> - 安全：数据库密码和 JWT 密钥硬编码在 yml，生产应改为环境变量注入；CORS 应限制具体域名。
> - 设计：种子数据里三个账号共用了同一个 BCrypt hash，实际是同一密码，需要重新生成。
> - 工程化：分类接口用实体直接接收参数，缺少 DTO 和参数校验。

**Q2：逻辑删除有什么优缺点？什么场景不适合逻辑删除？**

> 优点：数据可恢复、保留审计痕迹、避免误删。
> 缺点：查询都要带 deleted=0、唯一索引冲突、表会越来越大、和物理删除混用易出错。
> 不适合：有唯一约束且需要复用唯一值的字段（如 username、手机号）；数据量极大且需要清理的场景。

---

## 九、下一课预告

第 22 课：**SQL 优化** —— 用 EXPLAIN 分析本项目的查询语句，理解索引怎么生效、为什么 `LIKE '%xx%'` 不走索引、深分页怎么优化。


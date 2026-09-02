# 第5课：Service 层 + 事务——`@Transactional`、ACID、事务传播与隔离

> **本课目标**：彻底搞懂 Spring 事务的底层原理，理解为什么借阅/归还流程必须用事务，掌握事务传播行为和隔离级别。学完这课，你应该能向面试官讲清楚 `@Transactional` 的实现原理和失效场景。

---

## 一、从项目代码开始

看 `BorrowServiceImpl.java` 的借阅方法：

```java
@Override
@Transactional(rollbackFor = Exception.class)   // ← 事务注解
public BorrowRecordVO borrowBook(Long userId, Long bookId) {
    // 分布式锁...
    try {
        return doBorrowBook(userId, bookId);
    } finally {
        redisService.unlock(lockKey, lockValue);
    }
}

private BorrowRecordVO doBorrowBook(Long userId, Long bookId) {
    // 1. 校验用户
    User user = userService.getById(userId);
    // ...

    // 2. 校验图书
    Book book = bookService.getById(bookId);
    // ...

    // 3. 校验是否已借阅
    int existingCount = baseMapper.countByUserAndBook(userId, bookId);
    // ...

    // 4. 校验借阅数量
    int borrowingCount = baseMapper.countBorrowingByUserId(userId);
    // ...

    // 5. 原子扣减库存（SQL: UPDATE book SET stock = stock - 1 WHERE id = ? AND stock >= 1）
    int affectedRows = bookMapper.decreaseStock(bookId, 1);
    if (affectedRows == 0) {
        throw new BusinessException(ResultCode.BOOK_OUT_OF_STOCK);
    }

    // 6. 创建借阅记录（INSERT INTO borrow_record ...）
    BorrowRecord record = new BorrowRecord();
    // ...
    save(record);

    // 7. 发送 MQ 通知（异步，不影响事务）
    sendBorrowNotification(user, book, record);

    return toVO(record);
}
```

**关键问题**：第 5 步扣减库存和第 6 步创建借阅记录，是两个独立的数据库操作。如果扣减库存成功了，但创建借阅记录时失败了（比如数据库崩了），会怎么样？

→ **库存被扣了，但借阅记录没创建** → 数据不一致 → 这本书"消失"了。

这就是为什么需要**事务**：把多个数据库操作绑成一个整体，要么全部成功，要么全部失败回滚。

---

## 二、什么是事务？

### 定义

事务（Transaction）是**一组要么全部成功、要么全部失败的数据库操作集合**。

经典例子：银行转账。A 给 B 转 100 元，需要两个操作：
1. A 的账户减 100 元
2. B 的账户加 100 元

这两个操作必须都成功或都失败。如果第 1 步成功第 2 步失败，A 的钱没了，B 没收到——数据不一致。

### 项目里的事务场景

| 操作 | 涉及的数据库操作 | 为什么需要事务 |
|------|----------------|--------------|
| **借阅图书** | ①扣减图书库存 ②创建借阅记录 | 扣库存成功但记录失败 → 书"消失" |
| **归还图书** | ①增加图书库存 ②更新借阅记录（状态、归还日期、罚款） | 加库存成功但记录失败 → 库存多了，记录还显示借阅中 |
| **新增图书** | ①INSERT 图书 ②（如果有）关联分类 | 单表操作，事务意义不大，但加上更安全 |
| **更新图书** | ①UPDATE 图书 ②清除 Redis 缓存 | Redis 操作不在事务里，但 DB 操作需要事务 |

---

## 三、ACID——事务的四大特性

这是面试必背。

| 特性 | 英文 | 含义 | 项目里的体现 |
|------|------|------|-------------|
| **原子性** | Atomicity | 事务中的操作要么全部成功，要么全部失败回滚，不存在中间状态 | 借阅时扣库存和建记录要么都成功，要么都回滚 |
| **一致性** | Consistency | 事务执行前后，数据库从一个一致状态转变为另一个一致状态 | 借阅前后，图书总库存 + 借阅中的数量 = 总藏书量，始终成立 |
| **隔离性** | Isolation | 多个事务并发执行时，一个事务的执行不应被其他事务干扰 | 用户 A 借书和用户 B 借书同时进行，互不影响（通过锁和隔离级别保证） |
| **持久性** | Durability | 事务一旦提交，对数据的修改就是永久的，即使系统崩溃也不会丢失 | 借阅成功提交后，即使服务器重启，借阅记录和库存变化都还在 |

### 怎么记住 ACID？

> **A**tomicity（原子性）：不可分割
> **C**onsistency（一致性）：数据始终正确
> **I**solation（隔离性）：并发互不干扰
> **D**urability（持久性）：提交后永久保存

口诀：**原一隔持**（原子、一致、隔离、持久）

---

## 四、`@Transactional` 到底做了什么？

### 注解本身什么都没做

`@Transactional` 只是一个**标记注解**，它本身不包含任何事务逻辑。真正的事务管理是 Spring 通过 **AOP 代理**实现的。

### 底层原理（简化版）

```
1. Spring 容器启动时
   │  - 扫描所有 Bean，检查方法上是否有 @Transactional
   │  - 如果有，为这个 Bean 创建 AOP 代理对象（CGLIB 代理）
   │  - 代理对象在目标方法执行前后，加入事务逻辑
   │
2. 调用被 @Transactional 标记的方法时
   │  实际调用的是代理对象，而不是原始对象
   │
3. 代理对象的执行流程
   │
   │  ① 开启事务（调用事务管理器，设置 autoCommit = false）
   │  │
   │  ② 执行目标方法（你的业务逻辑，包含多个 SQL）
   │  │
   │  ③ 正常返回 → 提交事务（commit）
   │  │     异常返回 → 回滚事务（rollback）
   │  │
   │  ④ 返回结果给调用者
```

### 结合项目理解

当 `BorrowController` 调用 `borrowService.borrowBook()` 时：

1. `borrowService` 实际上是 Spring 生成的 **CGLIB 代理对象**（不是原始的 `BorrowServiceImpl`）
2. 代理对象检测到 `borrowBook()` 方法上有 `@Transactional`
3. 代理对象先调用事务管理器（`DataSourceTransactionManager`），从数据库连接池获取一个连接，设置 `autoCommit = false`
4. 然后执行原始的 `borrowBook()` 方法，里面的所有 SQL 都用这个连接执行
5. 如果方法正常返回，代理对象调用 `connection.commit()` 提交事务
6. 如果方法抛出异常（且符合回滚规则），代理对象调用 `connection.rollback()` 回滚事务
7. 最后把连接归还连接池

### 关键：同一个连接

事务的核心是**事务内的所有 SQL 都使用同一个数据库连接**。这样才能保证这些 SQL 要么一起提交，要么一起回滚。

Spring 通过 `TransactionSynchronizationManager` 把连接绑定到当前线程，事务内的 MyBatis 操作会从线程中获取这个连接，而不是从连接池拿新连接。

---

## 五、`rollbackFor = Exception.class` 是什么意思？

```java
@Transactional(rollbackFor = Exception.class)
```

### 默认回滚规则

Spring 事务**默认只在遇到运行时异常（`RuntimeException`）和 `Error` 时才回滚**，遇到**受检异常（`Exception` 的直接子类，非 `RuntimeException`）时不回滚**。

Java 异常体系：

```
Throwable
├── Error（系统错误，如 OutOfMemoryError）→ 默认回滚
└── Exception
    ├── RuntimeException（运行时异常，如 NullPointerException、BusinessException）→ 默认回滚
    └── 受检异常（如 IOException、SQLException）→ 默认不回滚！
```

### 为什么要写 `rollbackFor = Exception.class`？

项目里的 `BusinessException` 继承自 `RuntimeException`，所以默认会回滚。但写 `rollbackFor = Exception.class` 是**企业项目的最佳实践**：

1. **更安全**：即使未来代码里抛出了受检异常（如 `IOException`），事务也会回滚，不会出现"异常了但数据没回滚"的 bug
2. **明确意图**：告诉读代码的人"这个方法任何异常都要回滚"
3. **阿里开发规范强制要求**：`@Transactional` 必须指定 `rollbackFor`

### 不写会怎么样？

如果不写 `rollbackFor`，当方法抛出受检异常（如 `IOException`、`SQLException`）时，**事务不会回滚**，数据已经提交了，但方法报错了——这是非常隐蔽的 bug。

### 其他常用属性

```java
@Transactional(
    rollbackFor = Exception.class,    // 什么异常回滚
    noRollbackFor = BusinessException.class,  // 什么异常不回滚
    readOnly = true,                   // 只读事务（查询方法用，优化性能）
    timeout = 30,                      // 超时时间（秒），超时自动回滚
    propagation = Propagation.REQUIRED, // 传播行为
    isolation = Isolation.READ_COMMITTED // 隔离级别
)
```

---

## 六、事务传播行为

当一个事务方法调用另一个事务方法时，事务怎么处理？这就是**事务传播行为**。

Spring 定义了 7 种传播行为，最常用的是前 3 种：

| 传播行为 | 含义 | 适用场景 |
|---------|------|---------|
| **REQUIRED**（默认） | 如果当前有事务，加入；没有就新建 | 大多数业务方法 |
| **REQUIRES_NEW** | 不管当前有没有事务，都新建一个独立事务 | 日志记录、操作记录（需要独立提交，不受主事务影响） |
| **NESTED** | 如果当前有事务，嵌套一个子事务（保存点）；没有就新建 | 子操作失败只回滚子操作，不影响主操作 |
| SUPPORTS | 有事务就加入，没有就非事务运行 | 查询方法 |
| NOT_SUPPORTED | 挂起当前事务，非事务运行 | 不需要事务的方法 |
| MANDATORY | 必须在事务中运行，没有就报错 | 被事务方法调用的工具方法 |
| NEVER | 必须非事务运行，有事务就报错 | 不应该在事务中的方法 |

### 结合项目理解

项目里的 `borrowBook()` 是 `REQUIRED`（默认）。假设它调用了另一个 `@Transactional` 方法 `userService.addBorrowCount()`：

- **REQUIRED**：`addBorrowCount` 加入 `borrowBook` 的事务，两者共用一个事务，要么一起提交要么一起回滚
- **REQUIRES_NEW**：`addBorrowCount` 新建一个独立事务，先提交自己的事务，再继续主事务。如果主事务后面回滚了，`addBorrowCount` 的修改**不会回滚**（因为已经提交了）

项目里没有复杂的传播行为，都是默认的 `REQUIRED`。但面试经常考，必须记住。

---

## 七、事务隔离级别

多个事务并发执行时，可能出现以下问题：

| 问题 | 含义 | 例子 |
|------|------|------|
| **脏读** | 一个事务读到了另一个事务**未提交**的修改 | A 改了库存但没提交，B 读到了改后的值，A 回滚了，B 读到的是脏数据 |
| **不可重复读** | 同一个事务内，两次读取同一行数据，结果不一样（因为另一个事务修改并提交了） | A 第一次读库存=5，B 修改库存=3 并提交，A 第二次读库存=3 |
| **幻读** | 同一个事务内，两次查询的结果集行数不一样（因为另一个事务插入/删除了数据并提交） | A 第一次查"借阅中"的记录有 3 条，B 新增一条借阅记录并提交，A 第二次查出 4 条 |

### 四种隔离级别

| 隔离级别 | 脏读 | 不可重复读 | 幻读 | 说明 |
|---------|------|-----------|------|------|
| **READ_UNCOMMITTED**（读未提交） | 可能 | 可能 | 可能 | 最低级别，几乎不用 |
| **READ_COMMITTED**（读已提交） | 不会 | 可能 | 可能 | Oracle、SQL Server 默认 |
| **REPEATABLE_READ**（可重复读） | 不会 | 不会 | 可能 | **MySQL InnoDB 默认**（通过 MVCC 解决幻读） |
| **SERIALIZABLE**（串行化） | 不会 | 不会 | 不会 | 最高级别，性能最差，加锁串行执行 |

### 项目里用的什么隔离级别？

项目没有在 `@Transactional` 里指定 `isolation`，所以用的是**数据库默认隔离级别**。

项目用的是 MySQL，InnoDB 引擎的默认隔离级别是 **REPEATABLE_READ（可重复读）**。

MySQL 的可重复读通过 **MVCC（多版本并发控制）** 解决了幻读问题（在普通查询下），所以实际效果接近 SERIALIZABLE，但性能好很多。

### 面试怎么答？

> - 事务隔离级别有 4 种：读未提交、读已提交、可重复读、串行化
> - 并发问题有 3 种：脏读、不可重复读、幻读
> - 读未提交：三个问题都可能
> - 读已提交：解决脏读，可能不可重复读和幻读
> - 可重复读：解决脏读和不可重复读，可能幻读（MySQL 通过 MVCC 解决了）
> - 串行化：三个问题都解决，但性能最差
> - MySQL InnoDB 默认是可重复读，Oracle 默认是读已提交
> - 项目用默认的可重复读，满足业务需求

---

## 八、`@Transactional` 失效的常见场景（面试高频）

这是面试必考题。`@Transactional` 看起来简单，但有很多坑会导致事务不生效。

### 场景1：方法不是 public 的

```java
@Transactional
private void borrowBook() { }  // ❌ 私有方法，事务不生效
```

**原因**：Spring AOP 基于 CGLIB 代理，只能拦截 public 方法。private 方法不能被代理，事务不生效。

**解决**：事务方法必须是 public。

### 场景2：同类中方法直接调用（this 调用）

```java
@Service
public class BorrowServiceImpl {
    public void borrowBook() {
        this.doBorrow();  // ❌ 同类直接调用，绕过了代理对象，事务不生效
    }

    @Transactional
    public void doBorrow() { }
}
```

**原因**：`this` 指向的是原始对象，不是代理对象。事务逻辑在代理对象里，直接调用原始对象的方法，事务不生效。

**解决**：
- 把 `doBorrow()` 放到另一个 Service 里
- 或者注入自己：`@Autowired private BorrowService self; self.doBorrow();`
- 或者用 `AopContext.currentProxy()` 获取代理对象

### 场景3：异常被 catch 吞掉了

```java
@Transactional(rollbackFor = Exception.class)
public void borrowBook() {
    try {
        // 业务逻辑，抛出异常
    } catch (Exception e) {
        e.printStackTrace();  // ❌ 异常被捕获了，Spring 不知道发生了异常，不会回滚
    }
}
```

**原因**：Spring 事务是通过捕获异常来触发回滚的。如果异常被 catch 了，Spring 认为方法正常执行，就提交事务了。

**解决**：
- catch 后重新抛出：`throw new RuntimeException(e);`
- 或者手动设置回滚：`TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();`

### 场景4：抛出的异常不在回滚范围内

```java
@Transactional  // 默认只回滚 RuntimeException 和 Error
public void borrowBook() throws IOException {
    throw new IOException();  // ❌ 受检异常，默认不回滚
}
```

**原因**：默认只回滚 `RuntimeException` 和 `Error`。

**解决**：`@Transactional(rollbackFor = Exception.class)`

### 场景5：数据库不支持事务

- MySQL 的 MyISAM 引擎不支持事务（项目用的是 InnoDB，没问题）
- 或者方法里的操作不是数据库操作（如只操作 Redis），事务不生效

### 场景6：多线程调用

```java
@Transactional
public void borrowBook() {
    new Thread(() -> {
        // 数据库操作  ❌ 新线程里的操作不在事务里
    }).start();
}
```

**原因**：Spring 事务基于线程（ThreadLocal 绑定连接），新线程没有事务上下文。

### 场景7：`@Transactional` 加在接口上（某些场景）

一般加在接口上是可以的（JDK 动态代理），但如果用 CGLIB 代理（Spring Boot 默认），接口上的注解可能不被继承到实现类。**建议加在实现类的方法上**。

### 记忆口诀

> **方法非 public、同类 this 调、异常被 catch、异常不匹配、引擎不支持、多线程调用**——这 6 种场景事务失效。

---

## 九、项目里的分布式锁和事务的关系

注意 `borrowBook()` 方法里：

```java
@Transactional(rollbackFor = Exception.class)
public BorrowRecordVO borrowBook(Long userId, Long bookId) {
    String lockKey = "library:lock:borrow:" + userId + ":" + bookId;
    String lockValue = UUID.randomUUID().toString();
    boolean locked = redisService.tryLock(lockKey, lockValue, 10, TimeUnit.SECONDS);
    if (!locked) {
        throw new BusinessException("操作太频繁");
    }
    try {
        return doBorrowBook(userId, bookId);
    } finally {
        redisService.unlock(lockKey, lockValue);
    }
}
```

### 为什么需要分布式锁？

事务只能保证**单个数据库操作的原子性**，但在并发场景下：

- 用户 A 和用户 B 同时借同一本书，库存只剩 1 本
- 两个事务都读到库存=1，都判断"有库存"
- 两个事务都执行扣减库存 → 超卖（库存变成 -1）

虽然 `decreaseStock` 用了 `WHERE stock >= 1` 的原子 SQL 来防止超卖，但在高并发下，还是会有大量请求打到数据库。分布式锁在**应用层**就把并发请求串行化了，减轻数据库压力。

### 锁和事务的顺序问题

注意一个细节：**锁在事务外面获取，在事务提交后释放**。

```
获取分布式锁
    │
    ▼
开启事务（@Transactional）
    │
执行业务逻辑（扣库存 + 建记录）
    │
提交事务
    │
    ▼
释放分布式锁
```

这是正确的顺序。如果锁在事务里面获取（或者事务提交前就释放锁），会有问题：
- 事务还没提交，锁就释放了
- 另一个请求获取锁，读到的还是旧数据（因为前一个事务还没提交）
- 导致并发问题

所以**锁的范围要大于事务的范围**：先获取锁，再开启事务；事务提交后，再释放锁。

---

## 十、本课必须记住的 7 件事

1. **事务 = 一组要么全成功要么全失败的数据库操作**，通过 `@Transactional` 声明
2. **ACID**：原子性、一致性、隔离性、持久性
3. **`@Transactional` 底层是 AOP 代理**：代理对象在方法前后加入事务逻辑（开启→执行→提交/回滚），核心是事务内所有 SQL 用同一个连接
4. **必须写 `rollbackFor = Exception.class`**：默认只回滚 RuntimeException，受检异常不回滚
5. **传播行为默认 REQUIRED**：有事务就加入，没有就新建。REQUIRES_NEW 是独立事务
6. **隔离级别 MySQL 默认 REPEATABLE_READ**（可重复读），通过 MVCC 解决幻读
7. **事务失效 6 大场景**：方法非 public、同类 this 调用、异常被 catch、异常不匹配、引擎不支持、多线程调用

---

## 十一、本节关键代码

```java
// 借阅方法：事务 + 分布式锁
@Override
@Transactional(rollbackFor = Exception.class)   // 声明事务：任何异常都回滚
public BorrowRecordVO borrowBook(Long userId, Long bookId) {
    // 1. 获取分布式锁（在事务外获取，锁范围 > 事务范围）
    String lockKey = "library:lock:borrow:" + userId + ":" + bookId;
    String lockValue = UUID.randomUUID().toString();
    boolean locked = redisService.tryLock(lockKey, lockValue, 10, TimeUnit.SECONDS);
    if (!locked) {
        throw new BusinessException("操作太频繁");
    }

    try {
        return doBorrowBook(userId, bookId);  // 业务逻辑在事务内执行
    } finally {
        redisService.unlock(lockKey, lockValue);  // 事务提交后释放锁
    }
}

private BorrowRecordVO doBorrowBook(Long userId, Long bookId) {
    // 校验...（查询操作，不修改数据）

    // 操作1：原子扣减库存（UPDATE book SET stock = stock - 1 WHERE id = ? AND stock >= 1）
    int affectedRows = bookMapper.decreaseStock(bookId, 1);
    if (affectedRows == 0) {
        throw new BusinessException(ResultCode.BOOK_OUT_OF_STOCK);  // 抛异常 → 事务回滚
    }

    // 操作2：创建借阅记录（INSERT INTO borrow_record ...）
    BorrowRecord record = new BorrowRecord();
    save(record);

    // 如果操作2失败抛异常，操作1的扣库存也会回滚 → 保证数据一致性
    return toVO(record);
}
```

---

## 十二、本节练习

### 练习1：验证事务回滚

在 `doBorrowBook` 方法的 `save(record)` 之后，故意加一行抛异常：

```java
save(record);
throw new RuntimeException("故意抛异常，测试回滚");
```

然后调用借阅接口，观察：
- 接口返回错误
- 数据库里图书库存没有被扣减（回滚了）
- 借阅记录没有创建（回滚了）

然后去掉这行代码。

### 练习2：验证异常被 catch 导致事务失效

把 `doBorrowBook` 里的代码用 try-catch 包起来：

```java
try {
    int affectedRows = bookMapper.decreaseStock(bookId, 1);
    save(record);
    throw new RuntimeException("测试");
} catch (Exception e) {
    e.printStackTrace();  // 异常被吞了
}
```

调用借阅接口，观察：
- 接口没有报错（因为异常被 catch 了）
- 但数据库里库存被扣了，记录也建了 → 事务没有回滚！

然后改成 catch 后重新抛出，验证事务恢复正常。

### 练习3：理解只读事务

给 `pageBooks` 方法加上 `@Transactional(readOnly = true)`，然后启动项目，观察日志。只读事务会告诉数据库"这个事务只查询不修改"，数据库可以做一些优化（如不加锁）。

---

## 十三、自测题

### Q1：什么是事务？ACID 四大特性是什么？

<details>
<summary>点击查看答案</summary>

事务是一组要么全部成功、要么全部失败的数据库操作集合。

ACID：
- **原子性（Atomicity）**：事务中的操作不可分割，要么全部成功，要么全部回滚
- **一致性（Consistency）**：事务执行前后，数据库从一个一致状态变为另一个一致状态
- **隔离性（Isolation）**：多个并发事务互不干扰
- **持久性（Durability）**：事务一旦提交，修改永久保存，系统崩溃也不丢失

</details>

### Q2：`@Transactional` 的底层原理是什么？

<details>
<summary>点击查看答案</summary>

`@Transactional` 基于 Spring AOP 代理实现：

1. Spring 容器启动时，为带 `@Transactional` 的 Bean 创建 CGLIB 代理对象
2. 调用方法时，实际调用的是代理对象
3. 代理对象在目标方法执行前：调用事务管理器获取数据库连接，设置 `autoCommit = false`，把连接绑定到当前线程
4. 执行目标方法（业务逻辑），方法内的所有 SQL 都从线程中获取同一个连接
5. 方法正常返回 → 代理对象调用 `commit()` 提交事务
6. 方法抛出异常（符合回滚规则）→ 代理对象调用 `rollback()` 回滚事务
7. 最后把连接归还连接池

核心：**事务内所有 SQL 使用同一个数据库连接**，才能保证一起提交或一起回滚。

</details>

### Q3：为什么 `@Transactional` 要写 `rollbackFor = Exception.class`？不写会怎么样？

<details>
<summary>点击查看答案</summary>

Spring 事务默认只在遇到 `RuntimeException`（运行时异常）和 `Error` 时回滚，遇到受检异常（`Exception` 的直接子类，如 `IOException`、`SQLException`）时**不回滚**。

写 `rollbackFor = Exception.class` 表示任何 `Exception` 及其子类都触发回滚，更安全。

不写的风险：如果方法里抛出了受检异常（如 `IOException`），事务不会回滚，数据已经提交但方法报错了，导致数据不一致。这是非常隐蔽的 bug。

企业项目规范（如阿里开发手册）强制要求 `@Transactional` 必须指定 `rollbackFor`。

</details>

### Q4：`@Transactional` 失效的常见场景有哪些？

<details>
<summary>点击查看答案</summary>

1. **方法不是 public**：CGLIB 代理只能拦截 public 方法
2. **同类中 this 直接调用**：`this` 指向原始对象，不是代理对象，绕过了事务逻辑
3. **异常被 catch 吞掉**：Spring 通过捕获异常触发回滚，异常被 catch 了就不知道要回滚
4. **抛出的异常不在回滚范围**：默认只回滚 RuntimeException，受检异常不回滚
5. **数据库引擎不支持事务**：如 MySQL MyISAM 引擎
6. **多线程调用**：事务基于线程（ThreadLocal），新线程没有事务上下文
7. **类没有被 Spring 管理**：没有 `@Service` 等注解，不会创建代理

</details>

### Q5：事务的隔离级别有哪些？MySQL 默认是哪个？

<details>
<summary>点击查看答案</summary>

四种隔离级别：
1. **READ_UNCOMMITTED（读未提交）**：可能脏读、不可重复读、幻读，性能最好但最不安全
2. **READ_COMMITTED（读已提交）**：解决脏读，可能不可重复读、幻读。Oracle、SQL Server 默认
3. **REPEATABLE_READ（可重复读）**：解决脏读和不可重复读，可能幻读。**MySQL InnoDB 默认**（通过 MVCC 解决了幻读）
4. **SERIALIZABLE（串行化）**：都解决，性能最差，加锁串行执行

并发问题：
- **脏读**：读到另一个事务未提交的修改
- **不可重复读**：同一事务内两次读同一行数据结果不同（另一个事务修改并提交了）
- **幻读**：同一事务内两次查询结果集行数不同（另一个事务插入/删除并提交了）

</details>

---

## 十四、面试题

### 面试题1：请解释 Spring 事务的实现原理。

> **答题要点**：
> 1. Spring 事务基于 **AOP 代理**实现，`@Transactional` 注解的 Bean 在启动时会被创建 CGLIB 代理对象
> 2. 调用事务方法时，实际调用的是代理对象，代理对象在目标方法前后加入事务逻辑
> 3. 方法执行前：事务管理器（`DataSourceTransactionManager`）从连接池获取连接，设置 `autoCommit=false`，通过 `TransactionSynchronizationManager` 把连接绑定到当前线程（ThreadLocal）
> 4. 方法执行中：MyBatis 的 SQL 操作从线程中获取这个连接（而不是从连接池拿新连接），保证事务内所有 SQL 用同一个连接
> 5. 方法正常返回：代理对象调用 `connection.commit()` 提交事务
> 6. 方法抛出异常（符合回滚规则）：代理对象调用 `connection.rollback()` 回滚事务
> 7. 最后把连接归还连接池，清除线程中的绑定
> 8. 事务的核心是**同一个连接**，这样才能保证多个 SQL 要么一起提交要么一起回滚

### 面试题2：`@Transactional` 什么时候会失效？怎么避免？

> **答题要点**（至少说出 5 种）：
> 1. **方法非 public**：CGLIB 只能代理 public 方法 → 事务方法必须 public
> 2. **同类 this 调用**：`this.method()` 绕过代理 → 把方法放到另一个 Bean，或注入自己，或用 `AopContext.currentProxy()`
> 3. **异常被 catch**：Spring 靠异常触发回滚 → catch 后重新抛出，或手动 `setRollbackOnly()`
> 4. **异常类型不匹配**：默认只回滚 RuntimeException → 加 `rollbackFor = Exception.class`
> 5. **数据库不支持事务**：如 MyISAM 引擎 → 用 InnoDB
> 6. **多线程**：事务基于 ThreadLocal，新线程无事务上下文 → 不要在事务方法里开新线程做 DB 操作
> 7. **类没被 Spring 管理**：没有 `@Service` 等注解 → 加上注解
> 8. **传播行为设置错误**：如 `NOT_SUPPORTED` 会挂起事务 → 用默认的 `REQUIRED`

### 面试题3：事务的传播行为有哪些？REQUIRED 和 REQUIRES_NEW 有什么区别？

> **答题要点**：
> 7 种传播行为：REQUIRED、SUPPORTS、MANDATORY、REQUIRES_NEW、NOT_SUPPORTED、NEVER、NESTED
>
> 最常用的 3 种：
> - **REQUIRED（默认）**：当前有事务就加入，没有就新建。子方法和主方法共用一个事务，要么一起提交要么一起回滚
> - **REQUIRES_NEW**：不管当前有没有事务，都新建一个独立事务。子方法的事务先提交，主事务后续回滚不影响子方法。适用于日志记录、操作记录等需要独立提交的场景
> - **NESTED**：有事务就嵌套子事务（保存点），子方法失败只回滚子操作，不影响主操作。没有事务就新建
>
> REQUIRED vs REQUIRES_NEW 的核心区别：
> - REQUIRED：共用事务，子方法异常会导致主事务回滚，主事务异常也会导致子方法回滚
> - REQUIRES_NEW：独立事务，子方法先提交，主事务后续回滚不影响子方法已提交的数据

---

## 十五、下一课预告

**第6课：MyBatis-Plus 入门——`BaseMapper`、`ServiceImpl`、`LambdaQueryWrapper`**

我们会搞清楚：
- MyBatis 和 MyBatis-Plus 的关系？为什么项目不用写 SQL 就能 CRUD？
- `BaseMapper` 提供了哪些方法？每个方法对应什么 SQL？
- `ServiceImpl` 继承了什么？为什么 `getById`、`save`、`page` 不用自己写？
- `LambdaQueryWrapper` 怎么构建动态查询条件？`like`、`eq`、`gt`、`orderByDesc` 怎么用？
- 分页插件怎么配置的？`IPage` 对象包含哪些信息？
- 逻辑删除 `@TableLogic` 是怎么工作的？
- 项目里的 `decreaseStock` 自定义 SQL 怎么写的？`@Update` 注解和 `@Param` 注解

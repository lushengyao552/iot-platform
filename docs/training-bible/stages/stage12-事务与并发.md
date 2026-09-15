# Stage 12：事务与并发

## 阶段目标

把 stage11 留下的并发问题和事务问题解决掉。理解 @Transactional 原理、传播行为、隔离级别、乐观锁/悲观锁、库存超卖。

## 前置要求

- stage11 毕业
- 知道数据库有事务这个东西

## 本阶段知识点

- @Transactional
- 事务的 ACID
- 传播行为 REQUIRES / REQUIRES_NEW（本阶段先了解）
- 隔离级别：读未提交 / 读提交 / 可重复读 / 串行化
- 事务失效的 N 个场景（自调用、非 public、异常被吞、RuntimeException vs Exception）
- 并发问题：超卖、丢失更新
- 乐观锁 @Version
- 悲观锁 select ... for update
- 原子 SQL：update set stock = stock - 1 where stock > 0

---

## 题目

### 【第 12-1 题：给借书加事务】

**业务背景**
借书现在有两步：插 borrow_record、减 book.available_stock。如果中间崩了，就会出现"借了但库存没减"或"减了库存但没记录"。

**我的任务**
1. Service.borrowBook 方法加 @Transactional
2. 自己制造一次异常（比如减库存后手动 throw），观察数据库回滚
3. 同样给 returnBook 加事务

**限制条件**
- @Transactional 加在 public 方法上
- 不要加在 Controller
- 异常必须是 RuntimeException（或 Error），checked 不回滚

**验收标准**
- 故意抛异常后，数据库两步都回滚
- 你能说出 @Transactional 为什么能回滚（AOP 代理）
- 你能说出事务失效的常见场景

**思考题**
1. 为什么 private 方法加 @Transactional 不生效？
2. 自调用（this.borrowBook()）为什么事务失效？
3. try-catch 把异常吃了，事务还回滚吗？
4. @Transactional(rollbackFor = Exception.class) 为什么要加？

---

### 【第 12-2 题：超卖问题】

**业务背景**
最后一本书，两个用户同时点借书。现在的代码可能两个都成功，库存变 -1。

**我的任务**
1. 写一个并发测试（可以用 JMeter / 简单 CountDownRunner / 或者手动两个线程模拟）
2. 复现"库存变 -1"
3. 用原子 SQL 修复：
   `UPDATE book SET available_stock = available_stock - 1 WHERE id = ? AND available_stock > 0`
4. 检查 affected rows = 0 就抛异常
5. 再测并发，库存不为负

**限制条件**
- 不允许"先 select 库存 → Java 判断 → update"
- 必须用数据库条件保证
- 借记录插入也要在事务里

**验收标准**
- 100 个线程同时借最后一本，只有一个成功，库存不为负
- 你能说清为什么这种写法能防超卖
- 你能说清 WHERE available_stock > 0 的作用

**思考题**
1. 为什么 update ... where stock > 0 是原子的？
2. 行锁什么时候加？
3. 这个方案在高并发下性能怎么样？
4. 乐观锁和这种"条件更新"有什么区别？

---

### 【第 12-3 题：乐观锁 @Version】

**业务背景**
库存用原子 update 解决了，但通用的并发更新怎么搞？MyBatis-Plus 提供乐观锁插件。

**我的任务**
1. book 表加 version 字段（int 默认 0）
2. Entity 加 @Version
3. 注册 OptimisticLockerInnerInterceptor
4. 写一个测试：两个线程同时更新同一本书的 price，看乐观锁怎么冲突

**限制条件**
- 理解乐观锁 vs 悲观锁区别
- 冲突时要重试或报错

**验收标准**
- 并发更新时一个成功一个失败
- 你能说清乐观锁原理（版本号）
- 你能说出乐观锁适合什么场景、不适合什么场景

**思考题**
1. 乐观锁为什么不适合库存扣减？（提示：库存是连续数字）
2. 悲观锁 select for update 什么时候用？
3. 乐观锁冲突了要不要自动重试？重试几次？
4. 数据库隔离级别和锁的关系？

---

### 【第 12-4 题：事务失效与排查】

**业务背景**
手写事务一定会踩坑。

**我的任务**
1. 故意写一个事务失效的例子（比如 private 方法加 @Transactional）
2. 自己用日志或调试发现它没回滚
3. 改成正确写法
4. 整理一份"事务失效 Checklist"

**限制条件**
- 至少列出 5 种失效场景
- 每种都要能复现

**验收标准**
- 你能现场说出 5 种事务失效
- 你能判断一段代码里事务有没有生效

**思考题**
1. Spring 事务什么时候用 Java 动态代理，什么时候用 cglib？
2. 同类自调用为什么失效？怎么解决？
3. @Transactional 加在接口上和实现类上，哪个对？
4. 多数据源下事务怎么管？
5. 分布式事务了解吗？（伏笔，不要求现在会）

---

## 本阶段毕业考试

不看任何资料：
1. 给你一段"超卖"代码，让你改对
2. 给你一段"事务没回滚"代码，让你找原因
3. 让你现场写一个并发测试，证明库存不为负
4. 解释 @Transactional 原理（AOP + 动态代理 + 线程连接绑定）

## 本阶段反向面试题

1. ACID 各是什么？
2. 隔离级别怎么选？MySQL 默认是什么？
3. MVCC 是什么？
4. 幻读怎么解决？
5. 脏读、不可重复读、幻读区别？
6. @Transactional 传播行为有哪些？REQUIRED 和 REQUIRES_NEW 区别？
7. 事务和线程的关系？为什么异步方法里事务失效？

## 常见坑

- @Transactional 加在非 public 方法
- 自调用 this.xxx()
- catch 了异常没抛
- rollbackFor 没配 checked 异常
- 用了数据源但没配事务管理器
- 超卖靠 Java 层判断，不靠数据库

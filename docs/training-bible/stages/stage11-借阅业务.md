# Stage 11：借阅业务（借书 / 还书 / 逾期）

## 阶段目标

实现核心业务：借书、还书、查借阅记录、逾期判断。本阶段先不管并发和事务（stage12 专门讲），把业务流程跑通。

## 前置要求

- stage10 毕业
- 图书表有 total_stock / available_stock

## 本阶段知识点

- 业务状态机：BORROWED / RETURNED / OVERDUE
- 应还时间：借出时算（now + 30 天）
- 逾期判断：查的时候现算 vs 定时任务扫
- 库存变更：available_stock -1 / +1
- 一个用户最多借几本（需求变更伏笔）
- 罚款：逾期每天 0.5 元（需求变更伏笔）

---

## 题目

### 【第 11-1 题：借书接口】

**业务背景**
用户点"借这本书"。

**我的任务**
1. POST /api/borrow
2. BorrowDTO: bookId
3. Service.borrowBook(userId, bookId)：
   - 查书，不存在 → 404
   - 书状态必须在架
   - available_stock 必须 > 0
   - 该用户没有这本书未还的记录
   - 插 borrow_record：status=BORROWED, dueTime=now+30天
   - book.available_stock - 1

**限制条件**
- 本阶段先不考虑并发（stage12 再来）
- 库存变更用 UpdateWrapper 原子减（available_stock = available_stock - 1）
- 业务校验全在 Service
- 抛 BizException

**验收标准**
- 借成功，库存减 1
- 重复借同一本书 → 报错
- 库存为 0 → 报错
- 你能说出这里有几个"必须原子"的操作

**思考题**
1. 为什么要校验"该用户没借过未还"？
2. available_stock = available_stock - 1 这条 SQL 为什么安全？
3. 插记录和减库存，顺序重要吗？（事务伏笔）
4. 应还时间为什么不在借出时算，而要查的时候算？

---

### 【第 11-2 题：还书接口】

**业务背景**
用户还书。

**我的任务**
1. POST /api/borrow/{recordId}/return
2. Service.returnBook(userId, recordId)：
   - 查记录
   - 必须是当前用户的记录（或管理员）
   - 状态必须是 BORROWED
   - 更新 return_time、status=RETURNED
   - book.available_stock + 1
   - 如果 now > due_time，标记 OVERDUE（或在还书时算逾期天数）

**限制条件**
- 已还的书不能再还
- 还书不校验库存为负（但要意识到）
- 逾期判断逻辑要写清楚

**验收标准**
- 还书后库存加 1
- 还了一本已逾期的，状态正确
- 重复还报错
- 你能说清逾期是"还书那一刻算"还是"查的时候算"

**思考题**
1. 如果书已经丢了，还要不要加库存？
2. 罚款金额什么时候算？
3. 还书接口谁能调？本人还是管理员也能代还？

---

### 【第 11-3 题：我的借阅记录】

**业务背景**
用户要看自己借了哪些书。

**我的任务**
1. GET /api/borrow/records?status=&page=&size=
2. 联表 book 查书名、封面、作者
3. 返回字段：recordId / bookId / bookName / borrowTime / dueTime / returnTime / status / overdueDays

**限制条件**
- 不允许 N+1（在 Service 里循环查 book）
- 用 JOIN 或批量查 in
- overdueDays 在 Service 算好，不要前端算

**验收标准**
- 返回的记录里书名正确
- 逾期天数正确（还没还的：max(0, today - dueTime)；已还的：0）
- 你能说清 N+1 在哪、怎么避免

**思考题**
1. 为什么不在 SQL 里直接算 overdueDays？
2. 联表时如果书被删了（逻辑删），还要不要显示？
3. 这个接口要不要分页？

---

### 【第 11-4 题：管理员借阅管理】

**业务背景**
管理员要看所有借阅记录、标记逾期。

**我的任务**
1. GET /api/admin/borrow/records：按用户、按状态、按时间范围筛选
2. 一个"标记逾期"的接口（本阶段先做查询时把超过 dueTime 的 BORROWED 标成 OVERDUE，定时任务 stage14/MQ 阶段做）

**限制条件**
- 普通用户不能访问
- 查询条件要走 MyBatis-Plus 分页

**验收标准**
- 管理员能查所有借阅
- 能按状态筛选
- 你能说清为什么"标记逾期"更适合定时任务而不是每次查询

**思考题**
1. 为什么不每次查询时实时判断逾期？
2. 定时任务怎么做？（伏笔 RabbitMQ / XXL-Job）
3. 罚款怎么收？还书时一起收？

---

## 本阶段毕业考试

不看任何资料：
1. 给你"续借"新需求（用户可以续借一次，应还时间延后 15 天），让你实现
2. 给一段有 N+1 的借阅记录查询，让你优化
3. 让你画出借书的完整状态机

## 本阶段反向面试题

1. 借书业务有几个写库操作？为什么需要事务？
2. 库存为负怎么防？
3. 一本书同时被两个人借怎么办？
4. 逾期判断为什么要定时任务？
5. 借阅记录要不要归档？

## 常见坑

- 借书时不校验"该用户未还"
- 还书时不校验记录归属（越权还别人的书）
- N+1 查书名
- 逾期天数负数不处理
- 库存减到负数（并发伏笔）

# Stage 14：RabbitMQ

## 阶段目标

引入消息队列，解决异步通知（借书成功后发通知）、逾期提醒（定时扫表）、削峰。理解 Exchange / Queue / Binding / ACK / 重试 / 死信 / 幂等。

## 前置要求

- stage13 毕业
- 本机有 RabbitMQ（docker 最快）

## 本阶段知识点

- RabbitMQ 核心角色：Producer / Exchange / Queue / Consumer
- Exchange 类型：direct / fanout / topic / headers
- 路由键 routing key
- 消息确认：publisher confirm / consumer ACK
- 重试与死信队列 DLX
- 消息幂等（消费端不能重复处理）
- Spring AMQP：@RabbitListener
- 业务场景：借书成功发通知、逾期提醒

---

## 题目

### 【第 14-1 题：RabbitMQ 接入与 Hello World】

**业务背景**
工程要能连 RabbitMQ。

**我的任务**
1. 引入 spring-boot-starter-amqp
2. application.yml 配 host / port / username / password / virtual-host
3. 写一个简单 demo：
   - 定义一个 queue "library.hello"
   - 写一个接口发消息
   - 写一个 @RabbitListener 消费并打印
4. 用 Knife4j 触发，控制台看到消费

**限制条件**
- 理解 Exchange / Queue / Binding 三者关系
- 不要用 fanout 图省事，先用 direct

**验收标准**
- 发消息后消费者收到
- 你能说清四个概念
- 你能说清为什么消息队列需要 Exchange

**思考题**
1. 为什么不直接 Producer → Queue？
2. direct / fanout / topic 区别？
3. 消息不消费会怎样？
4. RabbitMQ 和 Kafka 区别？

---

### 【第 14-2 题：借书成功异步通知】

**业务背景**
借书成功后要给用户发通知（站内信 / 邮件 / 短信）。同步发会拖慢接口。

**我的任务**
1. 借书成功后，发一条消息到 MQ："用户 X 借了书 Y"
2. 消费者异步处理：
   - 本阶段先打日志 / 存通知表
   - 邮件 / 短信真实接入可选
3. 用 routing key "library.borrow.success"
4. 用 direct exchange

**限制条件**
- 发消息要在事务提交后（@TransactionalEventListener 或 transactionSynchronization）
- 不能因为发消息失败让借书回滚
- 消费失败要有重试

**验收标准**
- 借书接口响应时间和不发消息差不多
- MQ 消费者收到消息
- 你能说清为什么发消息要在事务提交后

**思考题**
1. 如果先提交事务再发消息，发消息失败了怎么办？
2. 本地消息表听过吗？
3. 消费者挂了，消息会丢吗？
4. 为什么要手动 ACK？

---

### 【第 14-3 题：消费 ACK 与重试】

**业务背景**
消费者处理消息时可能失败（比如通知服务挂了）。

**我的任务**
1. 配置手动 ACK（spring.rabbitmq.listener.simple.acknowledge-mode=manual）
2. 消费成功 basicAck
3. 消费失败 basicNack + requeue（限制重试次数）
4. 超过重试次数 → 投递到死信队列 DLX
5. 写一个死信队列的消费者，把失败消息落库人工处理

**限制条件**
- 不要无脑 requeue（会一直死循环）
- 重试次数要有限
- 死信里要有失败原因、原始消息、时间

**验收标准**
- 故意让消费者抛异常，消息重试 N 次后进死信
- 死信队列能收到
- 你能说清 ACK / NACK / Reject 区别

**思考题**
1. 自动 ACK 有什么风险？
2. 死信队列的常见用途？
3. 消息会不会重复消费？怎么防？
4. 幂等性怎么做？（消息里带唯一 ID，Redis 存一下）

---

### 【第 14-4 题：逾期提醒】

**业务背景**
之前 stage11 留的坑：要把逾期的记录标成 OVERDUE 并提醒用户。

**我的任务**
1. 选一种方案：
   - 定时任务（@Scheduled）每天扫一次 borrow_record，把 due_time < now 且 status=BORROWED 的改成 OVERDUE，发 MQ 通知
   - 或者：每条借阅记录到期前 1 天发延迟消息提醒
2. 本阶段先用定时任务
3. 通知通过 MQ 异步发

**限制条件**
- 定时任务要加分布式锁（单机版可以先用 @Scheduled 占位，讲清楚生产上怎么用 ShedLock / XXL-Job）
- 不要每次扫全表，要按 due_time 索引查
- 发通知要幂等（同一条记录一天只提醒一次）

**验收标准**
- 手动改一条 borrow_record 的 due_time 到昨天
- 定时任务跑后状态变 OVERDUE
- 消费者收到逾期通知
- 你能说清楚为什么不用 MQ 延迟队列更优雅

**思考题**
1. 定时任务在多实例部署下怎么不重复跑？
2. 为什么不用 MQ 延迟队列做到期提醒？
3. 每天扫全表数据量大了怎么办？
4. 逾期提醒一天发几次？怎么去重？

---

## 本阶段毕业考试

不看任何资料：
1. 给你新需求"图书新增后通知管理员审核"，设计 Exchange / Queue / 消息体
2. 解释 ACK、重试、死信、幂等
3. 给一段"消费失败无限重投"的代码，让你修

## 本阶段反向面试题

1. RabbitMQ 怎么保证消息不丢？
2. 消息顺序性怎么保证？
3. 消息积压怎么处理？
4. 为什么要幂等？怎么设计消息唯一 ID？
5. Exchange 四种类型使用场景？
6. RabbitMQ 和 Kafka 选型？
7. 本地消息表 vs 事务消息？

## 常见坑

- 自动 ACK，处理完之前消费者挂了消息丢
- 无脑 requeue 死循环
- 消费不幂等，重复消息处理两遍
- 发消息在事务里，事务回滚消息已发出去
- 没配死信，失败消息丢失
- 定时任务多实例重复跑

# 第12课：RabbitMQ 消息队列——为什么用 MQ、生产者消费者、手动 ACK、死信队列

> **本课目标**：搞懂项目里 RabbitMQ 是怎么用的，理解消息队列的核心概念、Exchange 类型、消息可靠性保证、死信队列实现延迟消息。学完这课，你应该能向面试官讲清楚 RabbitMQ 的核心概念和消息可靠性方案。

---

## 一、从项目代码开始

看 `BorrowServiceImpl` 里借阅成功后发消息：

```java
// 7. 异步发送借阅成功通知（通过 RabbitMQ，不影响主业务响应）
sendBorrowNotification(user, book, record);

// 8. 发送延迟到期提醒消息（60秒后触发，实际项目应设置为到期前1天）
sendDelayReminder(user, book, record);

private void sendBorrowNotification(User user, Book book, BorrowRecord record) {
    try {
        NotificationMessage message = NotificationMessage.builder()
                .operationType("BORROW")
                .userId(user.getId())
                .username(user.getUsername())
                .bookId(book.getId())
                .bookTitle(book.getTitle())
                .borrowRecordId(record.getId())
                .dueDate(record.getDueDate().toString())
                .content(String.format("您已成功借阅《%s》，请在 %s 前归还", book.getTitle(), record.getDueDate()))
                .sendTime(LocalDateTime.now())
                .build();
        messageProducer.sendBorrowSuccessMessage(message);  // 发送消息
    } catch (Exception e) {
        // 消息发送失败不影响主业务，只记录日志
        log.error("发送借阅通知失败, recordId={}", record.getId(), e);
    }
}
```

再看 `MessageProducer` 怎么发消息：

```java
public void sendBorrowSuccessMessage(NotificationMessage message) {
    message.setMessageId(generateMessageId());
    sendMessage(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.BORROW_SUCCESS_ROUTING_KEY, message);
}

private void sendMessage(String exchange, String routingKey, Object data) {
    try {
        MessageProperties properties = new MessageProperties();
        properties.setMessageId(generateMessageId());
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setDeliveryMode(MessageProperties.DEFAULT_DELIVERY_MODE);  // 持久化

        String json = new ObjectMapper().writeValueAsString(data);
        Message message = MessageBuilder.withBody(json.getBytes())
                .andProperties(properties)
                .build();

        CorrelationData correlationData = new CorrelationData(properties.getMessageId());
        rabbitTemplate.send(exchange, routingKey, message, correlationData);
    } catch (Exception e) {
        log.warn("发送消息失败（降级处理，不影响主业务）, exchange={}, routingKey={}", exchange, routingKey, e);
    }
}
```

最后看消费者（notification-service 微服务）：

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    @RabbitListener(queues = "#{T(com.example.notification.config.RabbitMQConfig).BORROW_SUCCESS_QUEUE}")
    public void handleBorrowSuccess(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            String json = new String(message.getBody());
            NotificationMessage notification = new ObjectMapper().readValue(json, NotificationMessage.class);

            // 处理通知逻辑（保存到数据库、发送邮件/短信等）
            notificationService.sendNotification(notification);

            // 手动 ACK：确认消息已处理
            channel.basicAck(deliveryTag, false);
            log.info("借阅通知处理成功, messageId={}", notification.getMessageId());
        } catch (Exception e) {
            log.error("借阅通知处理失败, deliveryTag={}", deliveryTag, e);
            // 处理失败，拒绝消息（可以选择是否重新入队）
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
```

这就是项目里 RabbitMQ 的完整流程：**生产者发消息到 Exchange → Exchange 根据 RoutingKey 路由到 Queue → 消费者从 Queue 取消息处理 → 手动 ACK**。

---

## 二、什么是消息队列？为什么需要 MQ？

### 什么是消息队列？

消息队列（Message Queue，MQ）是一种**异步通信机制**，生产者把消息发到队列，消费者从队列取消息处理，生产者和消费者不需要直接调用，解耦了。

RabbitMQ 是实现了 AMQP 协议的消息队列中间件，用 Erlang 语言开发，性能好、可靠性高。

### 项目里为什么用 RabbitMQ？

以借阅成功通知为例：
- **不用 MQ**：借阅成功后，直接在借阅方法里调用通知服务（发邮件/短信/保存通知记录），如果通知服务慢（如发邮件要 2 秒），借阅接口就要等 2 秒，用户体验差；如果通知服务挂了，借阅接口也失败
- **用 MQ**：借阅成功后，发一条消息到 RabbitMQ（几毫秒），立即返回给用户。通知服务异步消费消息，慢慢处理通知。通知服务挂了也不影响借阅，消息还在队列里，服务恢复后继续消费

### MQ 的三大核心作用

| 作用 | 说明 | 项目里的体现 |
|------|------|-------------|
| **异步** | 把不需要同步处理的操作异步化，降低接口响应时间 | 借阅成功后异步发通知，借阅接口不用等通知处理完 |
| **解耦** | 生产者和消费者不需要直接依赖，通过消息队列解耦 | 借阅服务不需要依赖通知服务，只需要发消息；通知服务可以独立开发、部署、升级 |
| **削峰** | 高峰期大量请求先到队列，消费者按自己的处理能力慢慢消费，保护下游服务 | 如果同时有 1000 人借阅，1000 条通知消息先到队列，通知服务按处理能力慢慢消费，不会被压垮 |

### 项目里的消息场景

| 场景 | RoutingKey | 消费者 | 说明 |
|------|-----------|--------|------|
| 借阅成功通知 | `borrow.success` | notification-service | 借阅成功后发通知 |
| 归还成功通知 | `return.success` | notification-service | 归还成功后发通知 |
| 操作日志 | `operation.log` | （项目里消费者未完全实现） | 记录操作日志 |
| 延迟到期提醒 | `delay.borrow` | 延迟队列 → 死信队列 → notification-service | 借阅后延迟发送到期提醒 |

---

## 三、RabbitMQ 的核心概念

```
┌──────────┐    发送消息     ┌──────────┐   根据RoutingKey   ┌──────────┐   取消息    ┌──────────┐
│ Producer │ ──────────────▶ │ Exchange │ ─────────────────▶ │  Queue   │ ──────────▶ │ Consumer │
│ (生产者)  │   (Exchange+    │ (交换机)  │   路由+绑定(Binding)│ (队列)   │             │ (消费者)  │
└──────────┘    RoutingKey)  └──────────┘                    └──────────┘             └──────────┘
```

| 概念 | 说明 | 项目里的例子 |
|------|------|-------------|
| **Producer（生产者）** | 发送消息的应用 | `MessageProducer`，借阅/归还成功后发消息 |
| **Exchange（交换机）** | 接收生产者的消息，根据 RoutingKey 和 Binding 路由到 Queue | `library.exchange`（Topic 类型） |
| **Queue（队列）** | 存储消息，消费者从队列取消息 | `library.borrow.success.queue`、`library.return.success.queue` 等 |
| **Binding（绑定）** | Exchange 和 Queue 之间的绑定关系，定义路由规则 | `borrow.success` 绑定到 `library.borrow.success.queue` |
| **RoutingKey（路由键）** | 生产者发消息时指定，Exchange 根据它路由到 Queue | `borrow.success`、`return.success`、`delay.borrow` |
| **Consumer（消费者）** | 从队列取消息并处理的应用 | `notification-service` 里的 `NotificationConsumer` |
| **Virtual Host（虚拟主机）** | 逻辑隔离，不同 vhost 下的 Exchange/Queue 互不影响 | `/`（默认 vhost） |
| **Connection / Channel** | Connection 是 TCP 连接，Channel 是在 Connection 里建立的轻量通道（复用连接） | Spring AMQP 自动管理 |

### 消息流转过程

1. Producer 连接 RabbitMQ，建立 Connection 和 Channel
2. Producer 发送消息到 Exchange，指定 Exchange 名称和 RoutingKey
3. Exchange 收到消息，根据 RoutingKey 和 Binding 规则，把消息路由到对应的 Queue（可能路由到多个 Queue）
4. 消息存在 Queue 里，等待消费者消费
5. Consumer 监听 Queue，收到消息后处理
6. Consumer 处理完后，发送 ACK（确认）给 RabbitMQ，RabbitMQ 把消息从 Queue 删除

---

## 四、Exchange 的四种类型

Exchange 根据类型不同，路由策略不同：

| 类型 | 路由规则 | 适用场景 | 项目里用了吗 |
|------|---------|---------|-------------|
| **Direct** | RoutingKey 完全匹配 BindingKey | 精确路由，如日志分级（error/warn/info） | 没有 |
| **Topic** | RoutingKey 通配符匹配 BindingKey（`*` 匹配一个词，`#` 匹配零个或多个词） | 灵活路由，如按模块.操作路由 | ✅ 用了（`library.exchange` 是 Topic 类型） |
| **Fanout** | 广播到所有绑定的 Queue，忽略 RoutingKey | 广播消息，如全局通知、配置更新 | 没有 |
| **Headers** | 根据消息头（Headers）匹配，不常用 | 复杂路由规则 | 没有 |

### 项目里的 Topic Exchange

```java
@Bean
public TopicExchange libraryExchange() {
    return ExchangeBuilder.topicExchange(EXCHANGE_NAME).durable(true).build();
}
```

项目用 Topic 类型，RoutingKey 用点分隔（如 `borrow.success`、`return.success`、`operation.log`、`delay.borrow`）。

Topic Exchange 的通配符：
- `*`：匹配一个词，如 `borrow.*` 匹配 `borrow.success`、`borrow.fail`
- `#`：匹配零个或多个词，如 `borrow.#` 匹配 `borrow.success`、`borrow.success.notice`

项目里的 Binding 都是精确匹配（没有用通配符），其实用 Direct 也可以。但用 Topic 更灵活，未来可以扩展（如 `#` 匹配所有消息做日志）。

---

## 五、项目里的消息流程详解

### 5.1 普通消息流程（借阅成功通知）

```
用户借阅图书
    │
    ▼
BorrowServiceImpl.borrowBook()
    │
    ├─ 扣减库存、创建借阅记录（数据库操作，事务）
    │
    └─ sendBorrowNotification()
         │
         ▼
    MessageProducer.sendBorrowSuccessMessage()
         │  message: {operationType:"BORROW", userId, bookTitle, ...}
         │  exchange: "library.exchange"
         │  routingKey: "borrow.success"
         ▼
    RabbitTemplate.send()
         │
         ▼
    RabbitMQ Broker
         │
         ├─ Exchange "library.exchange" 收到消息
         ├─ 根据 routingKey "borrow.success" 查找 Binding
         ├─ 找到 Binding: "borrow.success" → "library.borrow.success.queue"
         └─ 消息路由到 Queue "library.borrow.success.queue"
              │
              ▼
         notification-service 的 NotificationConsumer
              │  监听 "library.borrow.success.queue"
              │  收到消息
              ▼
         处理通知（保存到数据库、发邮件/短信）
              │
              ▼
         手动 ACK（channel.basicAck）
              │
              ▼
         RabbitMQ 从 Queue 删除消息
```

### 5.2 延迟消息流程（到期提醒）—— 死信队列实现

项目里用**死信队列（DLX）**实现延迟消息：

```
借阅成功
    │
    ▼
发送消息到延迟队列（routingKey: "delay.borrow"）
    │
    ▼
延迟队列 "library.delay.queue"
    │  配置了：
    │  - x-message-ttl: 60000ms（消息存活60秒）
    │  - x-dead-letter-exchange: "library.dlx.exchange"（过期后转发到死信交换机）
    │  - x-dead-letter-routing-key: "dlx.borrow.remind"
    │
    ├─ 消息在延迟队列等待 60 秒
    │
    └─ 60 秒后消息过期，成为"死信"
         │
         ▼
    死信交换机 "library.dlx.exchange"
         │  根据 routingKey "dlx.borrow.remind" 路由
         ▼
    死信队列 "library.dlx.queue"
         │
         ▼
    消费者从死信队列取消息，处理到期提醒
```

### 什么是死信（Dead Letter）？

消息在以下情况会成为死信：
1. **消息被拒绝**（`basicNack`/`basicReject`）且 `requeue=false`
2. **消息过期**（TTL 到期，Time To Live）
3. **队列达到最大长度**

死信消息会被转发到配置的死信交换机（DLX，Dead-Letter Exchange），然后路由到死信队列。

### 项目里为什么用死信队列实现延迟？

RabbitMQ 本身没有直接的"延迟队列"功能，但可以通过"TTL + 死信队列"模拟：
- 消息发到一个设置了 TTL 的队列
- 消息在队列里等待 TTL 时间
- TTL 到期后消息成为死信，转发到死信交换机
- 死信交换机路由到死信队列
- 消费者从死信队列取消息，此时距离发送已经过了 TTL 时间

项目里 TTL 设为 60 秒（演示用），实际项目应该根据到期时间动态计算延迟时长（如到期前 1 天提醒）。

### 延迟队列的局限

用 TTL + 死信队列实现延迟有一个局限：**RabbitMQ 只会检查队列头部的消息是否过期**。如果第一条消息 TTL 是 60 秒，第二条是 10 秒，第二条会等第一条过期后才被检查（实际已经过期了 50 秒），导致延迟不准确。

生产项目如果需要精确的延迟消息，应该用 **RabbitMQ 延迟插件（rabbitmq_delayed_message_exchange）**，它支持每条消息独立的延迟时间。

---

## 六、消息可靠性保证

消息可能在三个环节丢失：
1. **生产者 → Exchange**：生产者发消息，但消息没到达 Exchange（网络问题、Exchange 不存在）
2. **Exchange → Queue**：消息到达 Exchange，但没有路由到任何 Queue（RoutingKey 没有匹配的 Binding）
3. **Queue → Consumer**：消息到了 Queue，但消费者处理失败/崩溃，消息丢失

项目里针对这三个环节都有保证措施。

### 6.1 生产者确认（ConfirmCallback）

```java
// application.yml
spring:
  rabbitmq:
    publisher-confirm-type: correlated  # 开启生产者确认
    publisher-returns: true              # 开启消息返回
```

```java
// MessageProducer.init()
@PostConstruct
public void init() {
    // ConfirmCallback：消息到达 Exchange 后回调
    rabbitTemplate.setConfirmCallback((CorrelationData correlationData, boolean ack, String cause) -> {
        if (ack) {
            log.debug("消息成功到达交换机, messageId={}", correlationData != null ? correlationData.getId() : "unknown");
        } else {
            log.error("消息未到达交换机, messageId={}, cause={}", correlationData != null ? correlationData.getId() : "unknown", cause);
            // 实际项目中：记录失败日志，触发重试或告警
        }
    });
}
```

`ConfirmCallback` 在消息到达 Exchange 后触发：
- `ack=true`：消息成功到达 Exchange
- `ack=false`：消息没到达 Exchange（如 Exchange 不存在），需要重试或告警

每条消息有唯一的 `CorrelationData`（messageId），用于在回调中识别是哪条消息。

### 6.2 消息返回（ReturnCallback）

```java
// ReturnCallback：消息无法路由到 Queue 时触发
rabbitTemplate.setReturnsCallback(returned -> {
    log.error("消息无法路由到队列, exchange={}, routingKey={}, replyCode={}, replyText={}",
            returned.getExchange(), returned.getRoutingKey(),
            returned.getReplyCode(), returned.getReplyText());
    // 实际项目中：处理无法路由的消息，如保存到数据库或告警
});
```

`ReturnCallback` 在消息到达 Exchange 但无法路由到任何 Queue 时触发（如 RoutingKey 写错了，没有匹配的 Binding）。

注意：需要设置 `spring.rabbitmq.template.mandatory=true`（或 `publisher-returns: true`），无法路由的消息才会返回给生产者，否则直接丢弃。

### 6.3 消费者手动 ACK

```yaml
# application.yml
spring:
  rabbitmq:
    listener:
      simple:
        acknowledge-mode: manual  # 手动确认模式
        prefetch: 5               # 每次预取5条消息（限流）
```

```java
@RabbitListener(queues = "...")
public void handleBorrowSuccess(Message message, Channel channel) throws IOException {
    long deliveryTag = message.getMessageProperties().getDeliveryTag();
    try {
        // 处理消息...
        notificationService.sendNotification(notification);

        // 手动 ACK：确认消息已处理成功，RabbitMQ 删除消息
        channel.basicAck(deliveryTag, false);
    } catch (Exception e) {
        log.error("处理失败", e);
        // 处理失败，拒绝消息
        // basicNack(deliveryTag, multiple, requeue)
        // requeue=false：不重新入队，消息成为死信或被丢弃
        // requeue=true：重新入队，可能导致无限循环
        channel.basicNack(deliveryTag, false, false);
    }
}
```

**三种 ACK 模式**：

| 模式 | 说明 | 优缺点 |
|------|------|--------|
| **auto（自动）** | 消费者收到消息后自动 ACK，不管处理是否成功 | 简单，但可能丢消息（消费者收到后崩溃，消息已 ACK 删除） |
| **manual（手动）** | 消费者处理完后手动调用 `basicAck`，处理失败调用 `basicNack` | 可靠，不会丢消息，但代码复杂一点 |
| **none** | 不确认，RabbitMQ 认为消息一直未确认，消费者断开后消息重新入队 | 不常用 |

项目用 **manual（手动 ACK）**，这是企业项目的标准做法，保证消息不丢失。

### 6.4 消息持久化

```java
properties.setDeliveryMode(MessageProperties.DEFAULT_DELIVERY_MODE);  // 持久化
```

消息持久化（`deliveryMode=2`）：消息写入磁盘，RabbitMQ 重启后消息不丢失。

同时 Queue 和 Exchange 也要持久化（`durable=true`）：
```java
return ExchangeBuilder.topicExchange(EXCHANGE_NAME).durable(true).build();
return QueueBuilder.durable(BORROW_SUCCESS_QUEUE).build();
```

**持久化三要素**：Exchange 持久化 + Queue 持久化 + 消息持久化，三者都满足，RabbitMQ 重启后消息才不丢失。

### 消息可靠性总结

| 环节 | 可能丢失的原因 | 解决方案 | 项目里的配置 |
|------|--------------|---------|-------------|
| 生产者→Exchange | 网络问题、Exchange 不存在 | ConfirmCallback | `publisher-confirm-type: correlated` |
| Exchange→Queue | RoutingKey 无匹配 | ReturnCallback + mandatory | `publisher-returns: true` |
| Queue→Consumer | 消费者处理失败/崩溃 | 手动 ACK + 消息持久化 | `acknowledge-mode: manual` + `deliveryMode=2` |
| RabbitMQ 宕机 | 消息在内存中丢失 | Exchange/Queue/消息持久化 | `durable=true` + 消息持久化 |

---

## 七、消息重复消费与幂等性

### 为什么会重复消费？

消息可能被重复消费的场景：
1. **消费者处理成功，但 ACK 没发出去**（网络问题、消费者崩溃），RabbitMQ 认为消息未确认，重新投递给消费者
2. **生产者重发**：生产者没收到 ConfirmCallback，重发消息
3. **消息重新入队**：`basicNack(requeue=true)` 后消息重新入队，再次被消费

### 什么是幂等性？

幂等性：同一个操作执行一次和执行多次的效果相同。

比如"把通知标记为已读"执行一次和执行多次效果一样（都是已读），是幂等的。但"扣减库存"执行一次扣 1，执行两次扣 2，不是幂等的。

消息队列消费必须保证幂等性，因为消息可能重复投递。

### 项目里的幂等性保证

项目里的消息有 `messageId`（唯一标识），消费者可以用它做幂等：

```java
// 消费者处理前先判断 messageId 是否已处理过
if (redisService.hasKey("library:mq:processed:" + messageId)) {
    // 已处理过，直接 ACK，不重复处理
    channel.basicAck(deliveryTag, false);
    return;
}

// 处理消息...
notificationService.sendNotification(notification);

// 处理成功后，标记 messageId 已处理（存入 Redis，设置过期时间）
redisService.set("library:mq:processed:" + messageId, "1", 24, TimeUnit.HOURS);

// ACK
channel.basicAck(deliveryTag, false);
```

项目里的 `NotificationConsumer` 目前没有实现幂等性判断（简化了），这是可以改进的点。生产项目必须实现幂等性。

### 常见的幂等性方案

| 方案 | 说明 | 适用场景 |
|------|------|---------|
| **唯一 ID + Redis/数据库去重** | 消息有唯一 ID，处理前查是否已处理，处理后标记已处理 | 通用方案，项目里可用 |
| **数据库唯一约束** | 利用数据库唯一索引，重复插入时报错（如订单号唯一） | 插入操作 |
| **乐观锁** | 带版本号更新，`UPDATE ... SET version=version+1 WHERE id=? AND version=?` | 更新操作 |
| **状态机** | 业务有状态流转，只有特定状态才能执行操作（如订单只有"待支付"才能支付） | 有状态的业务 |

---

## 八、本课必须记住的 7 件事

1. **MQ 三大作用**：异步（降低响应时间）、解耦（生产者消费者不直接依赖）、削峰（保护下游服务）
2. **RabbitMQ 核心概念**：Producer → Exchange（根据 RoutingKey）→ Binding → Queue → Consumer
3. **Exchange 四种类型**：Direct（精确匹配）、Topic（通配符，项目用的）、Fanout（广播）、Headers（消息头）
4. **消息可靠性三环节**：生产者确认（ConfirmCallback）、消息返回（ReturnCallback）、消费者手动 ACK；加上持久化（Exchange/Queue/消息都 durable）
5. **手动 ACK**：处理成功 `basicAck`，处理失败 `basicNack`；auto 模式可能丢消息，manual 更可靠
6. **死信队列（DLX）实现延迟消息**：消息发到设置 TTL 的延迟队列，过期后成为死信转发到死信交换机→死信队列→消费者；项目里用它实现到期提醒
7. **消息幂等性**：消息可能重复投递，消费者必须保证幂等（唯一 ID + 去重、数据库唯一约束、乐观锁、状态机）

---

## 九、本节关键代码

```java
// 生产者：发消息
@Component
@RequiredArgsConstructor
public class MessageProducer {
    private final RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init() {
        // ConfirmCallback：消息到达 Exchange 后回调
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.debug("消息成功到达交换机, messageId={}", correlationData.getId());
            } else {
                log.error("消息未到达交换机, messageId={}, cause={}", correlationData.getId(), cause);
            }
        });

        // ReturnCallback：消息无法路由到 Queue 时回调
        rabbitTemplate.setReturnsCallback(returned -> {
            log.error("消息无法路由, routingKey={}", returned.getRoutingKey());
        });
    }

    public void sendBorrowSuccessMessage(NotificationMessage message) {
        message.setMessageId(UUID.randomUUID().toString());
        sendMessage("library.exchange", "borrow.success", message);
    }

    private void sendMessage(String exchange, String routingKey, Object data) {
        try {
            MessageProperties properties = new MessageProperties();
            properties.setMessageId(message.getMessageId());
            properties.setContentType("application/json");
            properties.setDeliveryMode(MessageProperties.DEFAULT_DELIVERY_MODE);  // 持久化

            String json = new ObjectMapper().writeValueAsString(data);
            Message msg = MessageBuilder.withBody(json.getBytes()).andProperties(properties).build();
            CorrelationData correlationData = new CorrelationData(message.getMessageId());

            rabbitTemplate.send(exchange, routingKey, msg, correlationData);
        } catch (Exception e) {
            log.warn("发送消息失败（降级）, routingKey={}", routingKey, e);
        }
    }
}

// 消费者：手动 ACK
@Component
public class NotificationConsumer {

    @RabbitListener(queues = "library.borrow.success.queue")
    public void handleBorrowSuccess(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            String json = new String(message.getBody());
            NotificationMessage notification = new ObjectMapper().readValue(json, NotificationMessage.class);

            // 幂等性判断（项目里简化了，生产项目应实现）
            // if (已处理过) { channel.basicAck(deliveryTag, false); return; }

            // 处理通知
            notificationService.sendNotification(notification);

            // 手动 ACK：处理成功
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理失败", e);
            // 处理失败，拒绝消息（requeue=false 不重新入队）
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
```

---

## 十、本节练习

### 练习1：观察消息流转

启动 RabbitMQ（docker-compose up -d）和项目，然后：
1. 调用借阅接口，借阅一本图书
2. 打开 RabbitMQ 管理界面（http://localhost:15672，guest/guest）
3. 观察 Exchanges 里的 `library.exchange`，Queues 里的各个队列
4. 观察 `library.borrow.success.queue` 里是否有消息（消费者处理后应该没有了，因为 ACK 了）
5. 查看 notification-service 的日志，确认收到了消息

### 练习2：验证手动 ACK 和消息重投

在 `NotificationConsumer.handleBorrowSuccess` 里，处理成功后注释掉 `channel.basicAck(deliveryTag, false)`，然后：
1. 调用借阅接口
2. 观察 RabbitMQ 管理界面，队列里的消息是否一直存在（因为没 ACK）
3. 重启 notification-service，观察消息是否被重新投递（因为消费者断开后，未 ACK 的消息重新入队）
4. 恢复 basicAck

> **目的**：理解手动 ACK 的作用——不 ACK 消息不会被删除，消费者断开后重新入队。

### 练习3：实现消息幂等性

在 `NotificationConsumer` 里实现幂等性：
1. 用 Redis 存已处理的 messageId，key 为 `library:mq:processed:{messageId}`，过期时间 24 小时
2. 处理消息前先查 Redis，如果已处理过，直接 ACK 返回
3. 处理成功后，把 messageId 存入 Redis
4. 手动构造一条重复消息（用相同 messageId），观察第二次是否被跳过

---

## 十一、自测题

### Q1：RabbitMQ 的核心概念有哪些？消息从生产者到消费者的流转过程是什么？

<details>
<summary>点击查看答案</summary>

**核心概念**：
- **Producer（生产者）**：发送消息的应用
- **Exchange（交换机）**：接收生产者消息，根据 RoutingKey 和 Binding 路由到 Queue
- **Queue（队列）**：存储消息，消费者从队列取消息
- **Binding（绑定）**：Exchange 和 Queue 之间的绑定关系，定义路由规则
- **RoutingKey（路由键）**：生产者发消息时指定，Exchange 根据它路由
- **Consumer（消费者）**：从队列取消息并处理的应用
- **Virtual Host（虚拟主机）**：逻辑隔离

**消息流转过程**：
1. Producer 连接 RabbitMQ，建立 Connection 和 Channel
2. Producer 发送消息到 Exchange，指定 Exchange 名称和 RoutingKey
3. Exchange 收到消息，根据 RoutingKey 和 Binding 规则路由到对应的 Queue（可能多个）
4. 消息存在 Queue 里，等待消费者
5. Consumer 监听 Queue，收到消息后处理
6. Consumer 处理完后发送 ACK，RabbitMQ 把消息从 Queue 删除

</details>

### Q2：Exchange 的四种类型是什么？项目里用的哪种？

<details>
<summary>点击查看答案</summary>

**四种 Exchange 类型**：
1. **Direct**：RoutingKey 完全匹配 BindingKey 才路由。适用于精确路由，如日志分级（error/warn/info 分别路由到不同队列）
2. **Topic**：RoutingKey 通配符匹配 BindingKey。`*` 匹配一个词，`#` 匹配零个或多个词。适用于灵活路由，如按模块.操作路由
3. **Fanout**：广播到所有绑定的 Queue，忽略 RoutingKey。适用于广播消息，如全局通知、配置更新
4. **Headers**：根据消息头（Headers）属性匹配，不常用。适用于复杂路由规则

**项目里用的是 Topic 类型**（`library.exchange`），RoutingKey 用点分隔（`borrow.success`、`return.success`、`operation.log`、`delay.borrow`）。虽然项目里的 Binding 都是精确匹配（没用通配符），用 Direct 也可以，但 Topic 更灵活，未来可以扩展通配符路由。

</details>

### Q3：消息可能在哪些环节丢失？怎么保证消息可靠性？

<details>
<summary>点击查看答案</summary>

**消息可能丢失的三个环节**：
1. **生产者 → Exchange**：生产者发消息，但消息没到达 Exchange（网络问题、Exchange 不存在）
2. **Exchange → Queue**：消息到达 Exchange，但没有路由到任何 Queue（RoutingKey 无匹配 Binding）
3. **Queue → Consumer**：消息到了 Queue，但消费者处理失败/崩溃，消息丢失

**保证可靠性的方案**：
1. **生产者确认（ConfirmCallback）**：消息到达 Exchange 后回调，ack=true 成功，ack=false 失败需重试。配置 `publisher-confirm-type: correlated`
2. **消息返回（ReturnCallback）**：消息无法路由到 Queue 时触发回调。配置 `publisher-returns: true`
3. **消费者手动 ACK**：处理成功调用 `basicAck`，处理失败调用 `basicNack`。配置 `acknowledge-mode: manual`。auto 模式可能丢消息（消费者收到后崩溃但已自动 ACK）
4. **持久化**：Exchange 持久化（durable=true）+ Queue 持久化（durable=true）+ 消息持久化（deliveryMode=2），RabbitMQ 重启后消息不丢失
5. **集群部署**：RabbitMQ 镜像队列/仲裁队列，单节点宕机消息不丢失

**项目里的配置**：
- `publisher-confirm-type: correlated` + ConfirmCallback
- `publisher-returns: true` + ReturnCallback
- `acknowledge-mode: manual` + 手动 basicAck/basicNack
- Exchange/Queue durable=true + 消息 deliveryMode=2

</details>

### Q4：什么是死信队列？项目里怎么用死信队列实现延迟消息？

<details>
<summary>点击查看答案</summary>

**死信（Dead Letter）**：消息在以下情况会成为死信：
1. 消息被消费者拒绝（basicNack/basicReject）且 requeue=false
2. 消息过期（TTL 到期）
3. 队列达到最大长度

死信消息会被转发到队列配置的死信交换机（DLX，Dead-Letter Exchange），然后路由到死信队列。

**项目里用死信队列实现延迟消息的流程**：
1. 生产者发送到期提醒消息到延迟队列（`library.delay.queue`），routingKey=`delay.borrow`
2. 延迟队列配置了：
   - `x-message-ttl: 60000`（消息存活 60 秒）
   - `x-dead-letter-exchange: library.dlx.exchange`（过期后转发到死信交换机）
   - `x-dead-letter-routing-key: dlx.borrow.remind`
3. 消息在延迟队列等待 60 秒
4. 60 秒后消息过期，成为死信，转发到死信交换机 `library.dlx.exchange`
5. 死信交换机根据 routingKey `dlx.borrow.remind` 路由到死信队列 `library.dlx.queue`
6. 消费者从死信队列取消息，此时距离发送已经过了 60 秒，处理到期提醒

**局限**：用 TTL+死信队列实现延迟，RabbitMQ 只检查队列头部消息是否过期，如果第一条 TTL 长第二条 TTL 短，第二条会等第一条过期后才被检查，延迟不准确。生产项目需要精确延迟时用 RabbitMQ 延迟插件（rabbitmq_delayed_message_exchange）。

</details>

### Q5：什么是消息幂等性？为什么需要幂等性？怎么实现？

<details>
<summary>点击查看答案</summary>

**幂等性**：同一个操作执行一次和执行多次的效果相同。比如"把通知标记为已读"执行一次和多次效果一样，是幂等的；"扣减库存"执行一次扣1，执行两次扣2，不是幂等的。

**为什么需要幂等性**：消息队列中消息可能被重复投递：
1. 消费者处理成功但 ACK 没发出去（网络问题/消费者崩溃），RabbitMQ 认为未确认，重新投递
2. 生产者没收到 ConfirmCallback，重发消息
3. basicNack(requeue=true) 后消息重新入队，再次被消费

所以消费者必须保证幂等性，否则重复消费会导致数据错误（如重复扣库存、重复发通知）。

**实现幂等性的方案**：
1. **唯一 ID + 去重**：消息有唯一 ID（messageId），处理前查 Redis/数据库是否已处理，处理后标记已处理。项目里可用这个方案
2. **数据库唯一约束**：利用唯一索引，重复插入时报错（如订单号唯一）。适用于插入操作
3. **乐观锁**：带版本号更新，`UPDATE ... SET version=version+1 WHERE id=? AND version=?`，重复更新时版本号不匹配影响行数为0。适用于更新操作
4. **状态机**：业务有状态流转，只有特定状态才能执行操作（如订单只有"待支付"才能支付，已支付的订单重复支付直接返回）。适用于有状态的业务
5. **分布式锁**：处理前先获取分布式锁（key=messageId），获取到才处理，处理完释放。重复消息获取不到锁直接跳过

项目里的消息有 messageId，但 NotificationConsumer 目前没有实现幂等性判断（简化了），生产项目必须补充。

</details>

---

## 十二、面试题

### 面试题1：你们项目里 RabbitMQ 是怎么用的？消息可靠性怎么保证？

> **答题要点**：
> 1. **使用场景**：
>    - 借阅/归还成功后异步发送通知（不阻塞主流程）
>    - 操作日志异步记录
>    - 借阅到期延迟提醒（通过死信队列实现）
> 2. **架构**：
>    - 生产者：主服务 library-management 的 MessageProducer，用 RabbitTemplate 发消息
>    - 消费者：独立微服务 notification-service 的 NotificationConsumer，用 @RabbitListener 消费
>    - Exchange：Topic 类型 `library.exchange`
>    - Queue：`library.borrow.success.queue`、`library.return.success.queue`、`library.operation.log.queue`、延迟队列+死信队列
> 3. **消息可靠性保证**：
>    - **生产者确认**：`publisher-confirm-type: correlated` + ConfirmCallback，消息到达 Exchange 后回调，失败记录日志/重试
>    - **消息返回**：`publisher-returns: true` + ReturnCallback，无法路由到 Queue 时回调
>    - **手动 ACK**：`acknowledge-mode: manual`，消费者处理成功 basicAck，失败 basicNack，避免 auto 模式丢消息
>    - **持久化**：Exchange/Queue durable=true + 消息 deliveryMode=2，RabbitMQ 重启不丢消息
>    - **降级处理**：消息发送失败 try-catch 降级，不影响主业务（借阅/归还不受 MQ 故障影响）
> 4. **死信队列实现延迟**：延迟队列配置 TTL+DLX，消息过期后转发到死信队列，消费者消费，实现到期提醒
> 5. **待优化点（主动说）**：消费者目前没有实现消息幂等性（应加 messageId 去重），延迟队列用 TTL 实现不够精确（应用延迟插件），生产项目应补充

### 面试题2：RabbitMQ 怎么保证消息不丢失？从生产者、MQ、消费者三个角度说。

> **答题要点**：
> 1. **生产者端**：
>    - **ConfirmCallback（发布确认）**：`publisher-confirm-type: correlated`，消息到达 Exchange 后触发回调，ack=true 成功，ack=false 失败需重试/告警
>    - **ReturnCallback（消息返回）**：`publisher-returns: true` + `mandatory=true`，消息到达 Exchange 但无法路由到 Queue 时触发回调，返回消息内容
>    - **本地消息表/事务消息**：极端场景下，把消息存本地数据库，定时任务重试发送，确保消息一定发出去
> 2. **MQ 端（Broker）**：
>    - **Exchange 持久化**：`durable=true`，RabbitMQ 重启后 Exchange 不丢失
>    - **Queue 持久化**：`durable=true`，重启后 Queue 不丢失
>    - **消息持久化**：`deliveryMode=2`（PERSISTENT），消息写入磁盘，重启后不丢失
>    - **集群部署**：镜像队列/仲裁队列，单节点宕机消息不丢失，高可用
>    - **队列长度限制/死信队列**：队列满了消息不丢弃，转死信队列
> 3. **消费者端**：
>    - **手动 ACK**：`acknowledge-mode: manual`，消费者处理完业务后手动 `basicAck`，处理失败 `basicNack`。auto 模式消费者收到消息就自动 ACK，处理过程中崩溃会丢消息
>    - **幂等性**：消息可能重复投递（ACK 丢失、生产者重发），消费者必须保证幂等（唯一 ID 去重、数据库唯一约束、乐观锁、状态机）
>    - **prefetch 限流**：`prefetch: 5`，每次只预取 N 条消息，防止消费者被大量消息压垮
>    - **失败处理**：basicNack(requeue=false) 不重新入队（避免无限循环），转死信队列人工处理；或 requeue=true 配合重试次数限制
> 4. **总结**：生产者确认 + MQ 持久化 + 消费者手动 ACK，三者配合保证消息至少被消费一次（at-least-once），加上幂等性保证效果上的 exactly-once

### 面试题3：什么是死信队列？什么场景下会用？延迟队列怎么实现？

> **答题要点**：
> 1. **死信队列（DLX，Dead-Letter Exchange）**：当消息成为死信时，会被 RabbitMQ 自动转发到配置的死信交换机，然后路由到死信队列。死信队列本质上还是普通队列，只是用来接收死信消息。
> 2. **消息成为死信的三种情况**：
>    - 消息被消费者拒绝（`basicNack`/`basicReject`）且 `requeue=false`（不重新入队）
>    - 消息过期（TTL，Time To Live 到期），包括队列设置的 `x-message-ttl` 或消息单独设置的 expiration
>    - 队列达到最大长度（`x-max-length`），最早的消息被挤出成为死信
> 3. **使用场景**：
>    - **失败消息处理**：消费失败的消息不直接丢弃，转死信队列，后续人工排查或定时重试
>    - **延迟队列**：通过"TTL + 死信队列"实现延迟消息（下面详细说）
>    - **异常告警**：死信队列有消息说明有消费失败，监控死信队列长度可以告警
> 4. **延迟队列实现（TTL + 死信队列）**：
>    - 创建一个延迟队列，配置 `x-message-ttl`（消息存活时间）、`x-dead-letter-exchange`（死信交换机）、`x-dead-letter-routing-key`（死信路由键）
>    - 生产者把消息发到延迟队列
>    - 消息在延迟队列等待 TTL 时间，期间没有消费者消费
>    - TTL 到期后消息成为死信，自动转发到死信交换机
>    - 死信交换机根据死信路由键路由到死信队列
>    - 消费者监听死信队列，此时消息已经延迟了 TTL 时间，处理业务
> 5. **项目里的应用**：借阅成功后发到期提醒消息到延迟队列（TTL=60秒演示），60秒后转发到死信队列，notification-service 消费死信队列发送到期提醒
> 6. **局限和优化**：
>    - TTL+死信队列的局限：RabbitMQ 只检查队列头部消息是否过期，如果第一条 TTL 长第二条 TTL 短，第二条会等第一条过期后才被检查，延迟不准确
>    - 优化方案：用 RabbitMQ 延迟插件 `rabbitmq_delayed_message_exchange`，支持每条消息独立延迟时间，更精确
>    - 其他方案：用 Redis 的 ZSet 做延迟队列（score=到期时间戳，定时轮询），或用时间轮算法

---

## 十三、下一课预告

**第13课：React 前端入门——JSX、useState、useEffect、组件、Props**

我们会搞清楚：
- React 是什么？项目里的前端技术栈（React 18 + TypeScript + Vite + Ant Design + Zustand）
- JSX 是什么？怎么在 JavaScript 里写 HTML？
- 函数组件和类组件的区别？项目里用的哪种？
- `useState` 怎么用？什么是状态？为什么需要状态？
- `useEffect` 怎么用？什么是副作用？什么时候执行？
- Props 是什么？父组件怎么给子组件传数据？
- 项目里的 `BookList.tsx` 页面是怎么工作的？
- React 的渲染流程：状态变化 → 重新渲染 → 更新 DOM

package com.example.library.common.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 配置类
 *
 * <p>定义交换机（Exchange）、队列（Queue）、绑定（Binding）。
 *
 * <p>RabbitMQ 核心概念：
 * <ul>
 *   <li>Producer：消息生产者，将消息发送到 Exchange</li>
 *   <li>Exchange：交换机，根据 routing key 将消息路由到 Queue</li>
 *   <li>Queue：队列，存储消息，消费者从队列取消息</li>
 *   <li>Binding：绑定，定义 Exchange 和 Queue 之间的路由规则</li>
 *   <li>Consumer：消息消费者，从队列接收并处理消息</li>
 * </ul>
 *
 * <p>Exchange 类型：
 * <ul>
 *   <li>Direct：精确匹配 routing key</li>
 *   <li>Topic：通配符匹配（* 匹配一个词，# 匹配零个或多个词）</li>
 *   <li>Fanout：广播到所有绑定的队列</li>
 *   <li>Headers：根据消息头匹配</li>
 * </ul>
 */
@Configuration
public class RabbitMQConfig {

    // ============================================================
    // 1. 业务交换机（Topic 类型，支持通配符路由）
    // ============================================================
    public static final String EXCHANGE_NAME = "library.exchange";

    // ============================================================
    // 2. 借阅成功通知队列
    // ============================================================
    public static final String BORROW_SUCCESS_QUEUE = "library.borrow.success.queue";
    public static final String BORROW_SUCCESS_ROUTING_KEY = "borrow.success";

    // ============================================================
    // 3. 归还成功通知队列
    // ============================================================
    public static final String RETURN_SUCCESS_QUEUE = "library.return.success.queue";
    public static final String RETURN_SUCCESS_ROUTING_KEY = "return.success";

    // ============================================================
    // 4. 操作日志队列
    // ============================================================
    public static final String OPERATION_LOG_QUEUE = "library.operation.log.queue";
    public static final String OPERATION_LOG_ROUTING_KEY = "operation.log";

    // ============================================================
    // 5. 延迟消息（到期提醒）—— 使用死信队列实现
    // ============================================================
    // 延迟队列：消息进入后，等待 TTL 到期，然后被转发到死信交换机
    public static final String DELAY_QUEUE = "library.delay.queue";
    public static final String DELAY_ROUTING_KEY = "delay.borrow";

    // 死信交换机（DLX）：接收延迟队列到期的消息
    public static final String DLX_EXCHANGE = "library.dlx.exchange";
    // 死信队列：消费者从这里消费到期的消息（即真正需要处理的到期提醒）
    public static final String DLX_QUEUE = "library.dlx.queue";
    public static final String DLX_ROUTING_KEY = "dlx.borrow.remind";

    // 消息 TTL（毫秒），演示用 60 秒。实际项目中应根据到期时间动态设置
    public static final int DELAY_TTL = 60000;

    // ============================================================
    // Bean 定义
    // ============================================================

    /**
     * 业务交换机（Topic 类型）
     */
    @Bean
    public TopicExchange libraryExchange() {
        // durable: true 表示交换机持久化，RabbitMQ 重启后不丢失
        return ExchangeBuilder.topicExchange(EXCHANGE_NAME).durable(true).build();
    }

    /**
     * 借阅成功队列
     */
    @Bean
    public Queue borrowSuccessQueue() {
        return QueueBuilder.durable(BORROW_SUCCESS_QUEUE).build();
    }

    /**
     * 借阅成功队列绑定到交换机
     */
    @Bean
    public Binding borrowSuccessBinding() {
        return BindingBuilder.bind(borrowSuccessQueue())
                .to(libraryExchange())
                .with(BORROW_SUCCESS_ROUTING_KEY);
    }

    /**
     * 归还成功队列
     */
    @Bean
    public Queue returnSuccessQueue() {
        return QueueBuilder.durable(RETURN_SUCCESS_QUEUE).build();
    }

    @Bean
    public Binding returnSuccessBinding() {
        return BindingBuilder.bind(returnSuccessQueue())
                .to(libraryExchange())
                .with(RETURN_SUCCESS_ROUTING_KEY);
    }

    /**
     * 操作日志队列
     */
    @Bean
    public Queue operationLogQueue() {
        return QueueBuilder.durable(OPERATION_LOG_QUEUE).build();
    }

    @Bean
    public Binding operationLogBinding() {
        return BindingBuilder.bind(operationLogQueue())
                .to(libraryExchange())
                .with(OPERATION_LOG_ROUTING_KEY);
    }

    // ============================================================
    // 延迟消息（死信队列实现）
    // ============================================================

    /**
     * 死信交换机
     */
    @Bean
    public TopicExchange dlxExchange() {
        return ExchangeBuilder.topicExchange(DLX_EXCHANGE).durable(true).build();
    }

    /**
     * 死信队列（消费者从这里消费到期提醒消息）
     */
    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable(DLX_QUEUE).build();
    }

    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxQueue())
                .to(dlxExchange())
                .with(DLX_ROUTING_KEY);
    }

    /**
     * 延迟队列
     *
     * <p>关键配置：
     * <ul>
     *   <li>x-dead-letter-exchange：消息过期后转发到的死信交换机</li>
     *   <li>x-dead-letter-routing-key：转发时使用的 routing key</li>
     *   <li>x-message-ttl：消息在队列中的存活时间（毫秒），到期后成为死信</li>
     * </ul>
     *
     * <p>工作流程：
     * 1. 生产者发送消息到延迟队列
     * 2. 消息在延迟队列等待 TTL 到期
     * 3. 到期后消息被自动转发到死信交换机
     * 4. 死信交换机路由到死信队列
     * 5. 消费者从死信队列消费消息（即到期提醒）
     */
    @Bean
    public Queue delayQueue() {
        Map<String, Object> args = new HashMap<>();
        // 设置死信交换机
        args.put("x-dead-letter-exchange", DLX_EXCHANGE);
        // 设置死信 routing key
        args.put("x-dead-letter-routing-key", DLX_ROUTING_KEY);
        // 设置消息 TTL（演示用 60 秒）
        args.put("x-message-ttl", DELAY_TTL);

        return QueueBuilder.durable(DELAY_QUEUE)
                .withArguments(args)
                .build();
    }

    @Bean
    public Binding delayBinding() {
        return BindingBuilder.bind(delayQueue())
                .to(libraryExchange())
                .with(DELAY_ROUTING_KEY);
    }
}

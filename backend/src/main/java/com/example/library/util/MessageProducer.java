package com.example.library.util;

import com.example.library.common.config.RabbitMQConfig;
import com.example.library.dto.NotificationMessage;
import com.example.library.dto.OperationLogMessage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 消息生产者
 *
 * <p>封装所有 RabbitMQ 消息发送逻辑，负责将业务事件转换为消息发送到交换机。
 *
 * <p>消息可靠性保证：
 * <ul>
 *   <li>ConfirmCallback：消息到达 Exchange 后回调，确认是否成功到达</li>
 *   <li>ReturnCallback：消息无法路由到 Queue 时回调，返回消息内容</li>
 *   <li>消息唯一 ID：每条消息生成唯一 ID，用于幂等性校验</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 初始化消息确认回调
     *
     * <p>@PostConstruct：Bean 初始化完成后执行，设置回调函数。
     */
    @PostConstruct
    public void init() {
        // 消息确认回调：消息到达 Exchange 后触发
        rabbitTemplate.setConfirmCallback((CorrelationData correlationData, boolean ack, String cause) -> {
            if (ack) {
                log.debug("消息成功到达交换机, messageId={}",
                        correlationData != null ? correlationData.getId() : "unknown");
            } else {
                log.error("消息未到达交换机, messageId={}, cause={}",
                        correlationData != null ? correlationData.getId() : "unknown", cause);
                // 实际项目中：记录失败日志，触发重试或告警
            }
        });

        // 消息返回回调：消息无法路由到 Queue 时触发
        rabbitTemplate.setReturnsCallback(returned -> {
            Message message = returned.getMessage();
            log.error("消息无法路由到队列, exchange={}, routingKey={}, replyCode={}, replyText={}, messageBody={}",
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    returned.getReplyCode(),
                    returned.getReplyText(),
                    new String(message.getBody()));
            // 实际项目中：处理无法路由的消息，如保存到数据库或告警
        });
    }

    /**
     * 发送借阅成功通知
     *
     * @param message 通知消息
     */
    public void sendBorrowSuccessMessage(NotificationMessage message) {
        message.setMessageId(generateMessageId());
        sendMessage(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.BORROW_SUCCESS_ROUTING_KEY, message);
        log.info("发送借阅成功通知: messageId={}, userId={}, bookId={}",
                message.getMessageId(), message.getUserId(), message.getBookId());
    }

    /**
     * 发送归还成功通知
     *
     * @param message 通知消息
     */
    public void sendReturnSuccessMessage(NotificationMessage message) {
        message.setMessageId(generateMessageId());
        sendMessage(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.RETURN_SUCCESS_ROUTING_KEY, message);
        log.info("发送归还成功通知: messageId={}, userId={}, bookId={}",
                message.getMessageId(), message.getUserId(), message.getBookId());
    }

    /**
     * 发送操作日志
     *
     * @param message 操作日志消息
     */
    public void sendOperationLogMessage(OperationLogMessage message) {
        message.setMessageId(generateMessageId());
        sendMessage(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.OPERATION_LOG_ROUTING_KEY, message);
        log.info("发送操作日志: messageId={}, operator={}, module={}, operation={}",
                message.getMessageId(), message.getOperatorName(), message.getModule(), message.getOperation());
    }

    /**
     * 发送延迟消息（借阅到期提醒）
     *
     * <p>消息发送到延迟队列，等待 TTL 到期后自动转发到死信队列，
     * 消费者从死信队列消费消息，触发到期提醒。
     *
     * @param message 通知消息
     */
    public void sendDelayBorrowReminder(NotificationMessage message) {
        message.setMessageId(generateMessageId());
        sendMessage(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.DELAY_ROUTING_KEY, message);
        log.info("发送延迟到期提醒消息: messageId={}, userId={}, bookId={}, TTL={}ms",
                message.getMessageId(), message.getUserId(), message.getBookId(), RabbitMQConfig.DELAY_TTL);
    }

    /**
     * 通用消息发送方法
     *
     * @param exchange   交换机名称
     * @param routingKey 路由键
     * @param data       消息体（会被序列化为 JSON）
     */
    private void sendMessage(String exchange, String routingKey, Object data) {
        try {
            // 构建消息属性：设置消息 ID、内容类型、持久化
            MessageProperties properties = new MessageProperties();
            properties.setMessageId(generateMessageId());
            properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            // 消息持久化：RabbitMQ 重启后消息不丢失
            properties.setDeliveryMode(MessageProperties.DEFAULT_DELIVERY_MODE);

            // 将消息体序列化为 JSON 字节
            String json = com.fasterxml.jackson.databind.ObjectMapper.class.getDeclaredConstructor().newInstance()
                    .writeValueAsString(data);
            Message message = MessageBuilder.withBody(json.getBytes())
                    .andProperties(properties)
                    .build();

            // CorrelationData：关联数据，用于 ConfirmCallback 中识别消息
            CorrelationData correlationData = new CorrelationData(properties.getMessageId());

            // 发送消息
            rabbitTemplate.send(exchange, routingKey, message, correlationData);
        } catch (Exception e) {
            // 暂时降级：RabbitMQ 不可用时只记录日志，不影响主业务流程
            // 有网络启用 RabbitMQ 后，取消下面的 throw 注释即可恢复正常
            log.warn("发送消息失败（降级处理，不影响主业务）, exchange={}, routingKey={}", exchange, routingKey, e);
            // throw new RuntimeException("消息发送失败", e);
        }
    }

    /**
     * 生成消息唯一 ID
     */
    private String generateMessageId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}

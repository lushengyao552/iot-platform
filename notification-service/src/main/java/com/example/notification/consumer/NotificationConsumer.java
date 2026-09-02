package com.example.notification.consumer;

import com.example.notification.model.NotificationRecord;
import com.example.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通知消息消费者
 *
 * <p>独立微服务，从 RabbitMQ 消费主应用发送的借阅/归还通知消息。
 *
 * <p>这是微服务架构的典型模式：
 * <ul>
 *   <li>主应用（生产者）：业务操作完成后发送消息到 RabbitMQ</li>
 *   <li>RabbitMQ（消息中间件）：异步解耦，削峰填谷</li>
 *   <li>通知服务（消费者）：独立部署，消费消息并发送通知</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Value("${notification.queue.borrow-success}")
    private String borrowSuccessQueue;

    @Value("${notification.queue.return-success}")
    private String returnSuccessQueue;

    /**
     * 消费借阅成功通知
     */
    @RabbitListener(queues = "${notification.queue.borrow-success}")
    public void handleBorrowSuccess(Message message, Channel channel) {
        handleNotification(message, channel, "BORROW");
    }

    /**
     * 消费归还成功通知
     */
    @RabbitListener(queues = "${notification.queue.return-success}")
    public void handleReturnSuccess(Message message, Channel channel) {
        handleNotification(message, channel, "RETURN");
    }

    /**
     * 通用通知处理方法
     */
    @SuppressWarnings("unchecked")
    private void handleNotification(Message message, Channel channel, String operationType) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            // 解析消息体（JSON 字符串）
            String json = new String(message.getBody());
            Map<String, Object> data = objectMapper.readValue(json, Map.class);

            // 构建通知记录
            NotificationRecord record = NotificationRecord.builder()
                    .messageId(getString(data, "messageId"))
                    .operationType(operationType)
                    .userId(getLong(data, "userId"))
                    .username(getString(data, "username"))
                    .bookId(getLong(data, "bookId"))
                    .bookTitle(getString(data, "bookTitle"))
                    .content(getString(data, "content"))
                    .status("SENT")
                    .createTime(LocalDateTime.now())
                    .build();

            // 保存通知记录（含幂等性校验）
            notificationService.save(record);

            // 模拟发送通知（实际项目中调用邮件/短信/推送服务）
            log.info("========== [通知微服务] 发送{}通知 ==========", operationType.equals("BORROW") ? "借阅" : "归还");
            log.info("通知ID: {}, 消息ID: {}", record.getId(), record.getMessageId());
            log.info("接收用户: {} ({})", record.getUsername(), record.getUserId());
            log.info("图书: {} ({})", record.getBookTitle(), record.getBookId());
            log.info("通知内容: {}", record.getContent());
            log.info("==================================================");

            // 手动确认消息
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("消费通知消息失败, deliveryTag={}, operationType={}", deliveryTag, operationType, e);
            try {
                // 消费失败，拒绝消息并重新入队（实际项目中应设置重试次数，超过后进入死信队列）
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ioException) {
                log.error("拒绝消息失败", ioException);
            }
        }
    }

    // ============================================================
    // 工具方法
    // ============================================================

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

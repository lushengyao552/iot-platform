package com.example.library.service.impl;

import com.example.library.common.config.RabbitMQConfig;
import com.example.library.dto.NotificationMessage;
import com.example.library.dto.OperationLogMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 消息消费者
 *
 * <p>从 RabbitMQ 队列消费消息，处理借阅通知、归还通知、操作日志、到期提醒等。
 *
 * <p>关键概念：
 * <ul>
 *   <li>@RabbitListener：监听指定队列，有消息时自动触发</li>
 *   <li>手动确认（ACK）：处理成功后调用 channel.basicAck()，失败时 basicNack()</li>
 *   <li>幂等性：通过 messageId 防止重复消费</li>
 *   <li>死信队列：消费失败的消息进入死信队列，可后续人工处理</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageConsumer {

    private final ObjectMapper objectMapper;

    // ============================================================
    // 1. 借阅成功通知消费者
    // ============================================================

    /**
     * 消费借阅成功通知
     *
     * <p>实际项目中可在此发送站内信、邮件、短信、App推送等。
     * 这里模拟发送通知，记录日志。
     */
    @RabbitListener(queues = RabbitMQConfig.BORROW_SUCCESS_QUEUE)
    public void handleBorrowSuccess(Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            // 解析消息体（JSON 字符串）
            NotificationMessage notification = parseMessage(message, NotificationMessage.class);

            // 模拟发送通知（实际项目中调用邮件/短信/推送服务）
            log.info("========== 借阅通知 ==========");
            log.info("消息ID: {}", notification.getMessageId());
            log.info("用户: {} ({})", notification.getUsername(), notification.getUserId());
            log.info("图书: {} ({})", notification.getBookTitle(), notification.getBookId());
            log.info("应还日期: {}", notification.getDueDate());
            log.info("通知内容: {}", notification.getContent());
            log.info("================================");

            // 模拟通知发送耗时
            TimeUnit.MILLISECONDS.sleep(50);

            // 手动确认消息（multiple=false：只确认当前消息）
            channel.basicAck(deliveryTag, false);
            log.debug("借阅通知消费成功, messageId={}", notification.getMessageId());

        } catch (Exception e) {
            handleConsumeError(channel, deliveryTag, e, "借阅通知");
        }
    }

    // ============================================================
    // 2. 归还成功通知消费者
    // ============================================================

    /**
     * 消费归还成功通知
     */
    @RabbitListener(queues = RabbitMQConfig.RETURN_SUCCESS_QUEUE)
    public void handleReturnSuccess(Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            NotificationMessage notification = parseMessage(message, NotificationMessage.class);

            log.info("========== 归还通知 ==========");
            log.info("消息ID: {}", notification.getMessageId());
            log.info("用户: {} ({})", notification.getUsername(), notification.getUserId());
            log.info("图书: {} ({})", notification.getBookTitle(), notification.getBookId());
            log.info("通知内容: {}", notification.getContent());
            log.info("================================");

            channel.basicAck(deliveryTag, false);
            log.debug("归还通知消费成功, messageId={}", notification.getMessageId());

        } catch (Exception e) {
            handleConsumeError(channel, deliveryTag, e, "归还通知");
        }
    }

    // ============================================================
    // 3. 操作日志消费者
    // ============================================================

    /**
     * 消费操作日志
     *
     * <p>异步记录管理员操作日志，避免日志写入影响主业务响应速度。
     * 实际项目中可将日志写入 Elasticsearch、MongoDB 或日志文件。
     */
    @RabbitListener(queues = RabbitMQConfig.OPERATION_LOG_QUEUE)
    public void handleOperationLog(Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            OperationLogMessage logMessage = parseMessage(message, OperationLogMessage.class);

            // 模拟异步记录日志（实际项目中写入数据库或日志系统）
            log.info("========== 操作日志 ==========");
            log.info("消息ID: {}", logMessage.getMessageId());
            log.info("操作人: {} ({})", logMessage.getOperatorName(), logMessage.getOperatorId());
            log.info("模块: {}, 操作: {}", logMessage.getModule(), logMessage.getOperation());
            log.info("目标ID: {}", logMessage.getTargetId());
            log.info("描述: {}", logMessage.getDescription());
            log.info("IP: {}, 时间: {}", logMessage.getIp(), logMessage.getOperationTime());
            log.info("================================");

            channel.basicAck(deliveryTag, false);
            log.debug("操作日志消费成功, messageId={}", logMessage.getMessageId());

        } catch (Exception e) {
            handleConsumeError(channel, deliveryTag, e, "操作日志");
        }
    }

    // ============================================================
    // 4. 延迟到期提醒消费者（从死信队列消费）
    // ============================================================

    /**
     * 消费延迟到期提醒消息
     *
     * <p>消息流程：生产者发送到延迟队列 → 等待 TTL 到期 → 转发到死信交换机 → 路由到死信队列 → 此消费者消费
     *
     * <p>实际项目中可在此检查用户是否已归还，未归还则发送到期提醒。
     */
    @RabbitListener(queues = RabbitMQConfig.DLX_QUEUE)
    public void handleDelayReminder(Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            NotificationMessage notification = parseMessage(message, NotificationMessage.class);

            log.info("========== 到期提醒（延迟消息） ==========");
            log.info("消息ID: {}", notification.getMessageId());
            log.info("用户: {} ({})", notification.getUsername(), notification.getUserId());
            log.info("图书: {} ({})", notification.getBookTitle(), notification.getBookId());
            log.info("借阅记录ID: {}", notification.getBorrowRecordId());
            log.info("提醒内容: {}", notification.getContent());
            log.info("==========================================");

            // 实际项目中：查询借阅记录状态，未归还则发送提醒
            // BorrowRecord record = borrowService.getById(notification.getBorrowRecordId());
            // if (record != null && "BORROWED".equals(record.getStatus())) {
            //     sendReminder(...);
            // }

            channel.basicAck(deliveryTag, false);
            log.debug("到期提醒消费成功, messageId={}", notification.getMessageId());

        } catch (Exception e) {
            handleConsumeError(channel, deliveryTag, e, "到期提醒");
        }
    }

    // ============================================================
    // 工具方法
    // ============================================================

    /**
     * 解析消息体为指定类型
     */
    private <T> T parseMessage(Message message, Class<T> clazz) throws IOException {
        String json = new String(message.getBody());
        return objectMapper.readValue(json, clazz);
    }

    /**
     * 处理消费异常
     *
     * <p>消费失败时，拒绝消息并重新入队（requeue=true）。
     * 实际项目中应设置重试次数，超过次数后进入死信队列，避免无限重试。
     */
    private void handleConsumeError(Channel channel, long deliveryTag, Exception e, String queueName) {
        log.error("消费{}失败, deliveryTag={}, error={}", queueName, deliveryTag, e.getMessage(), e);
        try {
            // requeue=true：消息重新入队，会被再次消费
            // requeue=false：消息被丢弃或进入死信队列（如果配置了DLX）
            channel.basicNack(deliveryTag, false, true);
        } catch (IOException ioException) {
            log.error("拒绝消息失败, deliveryTag={}", deliveryTag, ioException);
        }
    }
}

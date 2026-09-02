package com.example.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 借阅/归还通知消息
 *
 * <p>通过 RabbitMQ 异步发送，通知服务消费后可发送站内信、邮件、短信等。
 *
 * <p>实现 Serializable 接口，确保消息可以被 Java 序列化传输
 * （实际项目中推荐使用 JSON 序列化，更通用）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 消息唯一 ID（用于幂等性校验，防止重复消费） */
    private String messageId;

    /** 操作类型：BORROW-借阅，RETURN-归还 */
    private String operationType;

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 图书 ID */
    private Long bookId;

    /** 书名 */
    private String bookTitle;

    /** 借阅记录 ID */
    private Long borrowRecordId;

    /** 应还日期（借阅时提醒） */
    private String dueDate;

    /** 消息内容 */
    private String content;

    /** 消息发送时间 */
    private LocalDateTime sendTime;
}

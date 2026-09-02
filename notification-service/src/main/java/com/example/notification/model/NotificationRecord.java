package com.example.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知记录模型
 *
 * <p>演示用内存存储，实际项目中应存储到数据库。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRecord {

    /** 通知 ID */
    private Long id;

    /** 消息 ID（用于幂等性校验） */
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

    /** 通知内容 */
    private String content;

    /** 通知状态：PENDING-待发送，SENT-已发送，FAILED-发送失败 */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;
}

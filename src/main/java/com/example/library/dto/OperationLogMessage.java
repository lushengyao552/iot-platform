package com.example.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志消息
 *
 * <p>管理员操作（新增/删除/修改图书）后异步记录日志，
 * 避免日志写入影响主业务响应速度。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationLogMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 消息唯一 ID */
    private String messageId;

    /** 操作人 ID */
    private Long operatorId;

    /** 操作人用户名 */
    private String operatorName;

    /** 操作模块：BOOK-图书，CATEGORY-分类，USER-用户 */
    private String module;

    /** 操作类型：CREATE-新增，UPDATE-更新，DELETE-删除 */
    private String operation;

    /** 操作目标 ID */
    private Long targetId;

    /** 操作描述 */
    private String description;

    /** 请求 IP */
    private String ip;

    /** 操作时间 */
    private LocalDateTime operationTime;
}

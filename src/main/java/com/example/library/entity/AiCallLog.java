package com.example.library.entity;

import lombok.Data;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import java.io.Serializable;

@Data
@TableName("ai_call_log")
public class AiCallLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long userId;
    private String model;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Long llmDuration;
    private Long toolDuration;
    private Long totalDuration;
    private String toolNames;
    private String status;
    private String errorMsg;
    private String userQuery;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

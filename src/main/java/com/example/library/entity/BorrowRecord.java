package com.example.library.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 借阅记录实体类
 *
 * <p>对应数据库表 borrow_record
 */
@Data
@TableName("borrow_record")
public class BorrowRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 记录ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 图书ID */
    private Long bookId;

    /** 借阅日期 */
    private LocalDate borrowDate;

    /** 应还日期 */
    private LocalDate dueDate;

    /** 实际归还日期 */
    private LocalDate returnDate;

    /** 状态：BORROWED-借阅中，RETURNED-已归还，OVERDUE-已逾期 */
    private String status;

    /** 逾期罚款 */
    private BigDecimal fine;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}

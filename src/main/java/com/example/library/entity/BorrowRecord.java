package com.example.library.entity;
// ## 题 2.4 BorrowRecord 实体

// **文件**：`entity/BorrowRecord.java`，表 `borrow_record`
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import java.math.BigDecimal;
import java.time.LocalDate;
// | 字段 | 类型 |
// |------|------|
// | id | Long (@TableId AUTO) |
// | userId | Long |
// | bookId | Long |
// | borrowDate | LocalDate |
// | dueDate | LocalDate |
// | returnDate | LocalDate |
// | status | String |
// | fine | BigDecimal |
// | createTime | LocalDateTime (INSERT) |
// | updateTime | LocalDateTime (INSERT_UPDATE) |
// | deleted | Integer (@TableLogic) |
@Data 
@TableName ("borrow_record")
public class BorrowRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId (type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long bookId;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private String status;
    private BigDecimal fine;    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic 
    private Integer deleted;
}

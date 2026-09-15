package com.example.library.entity;
// **文件**：`entity/Book.java`，表 `book`
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
// | isbn | String |
// | title | String |
// | author | String |
// | publisher | String |
// | publishDate | LocalDate |
// | categoryId | Long |
// | price | BigDecimal |
// | stock | Integer |
// | totalStock | Integer |
// | description | String |
// | coverUrl | String |
// | createTime | LocalDateTime (INSERT) |
// | updateTime | LocalDateTime (INSERT_UPDATE) |
// | deleted | Integer (@TableLogic) |
@Data 
@TableName ("book")
public class Book implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private LocalDate publishDate;
    private Long categoryId;
    private BigDecimal price;
    private Integer stock;
    private Integer totalStock;
    private String description;
    private String coverUrl;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}

package com.example.library.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 图书实体类
 *
 * <p>对应数据库表 book
 */
@Data
@TableName("book")
public class Book implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 图书ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** ISBN编号 */
    private String isbn;

    /** 书名 */
    private String title;

    /** 作者 */
    private String author;

    /** 出版社 */
    private String publisher;

    /** 出版日期 */
    private LocalDate publishDate;

    /** 分类ID */
    private Long categoryId;

    /** 价格 */
    private BigDecimal price;

    /** 可借库存 */
    private Integer stock;

    /** 总藏书量 */
    private Integer totalStock;

    /** 图书简介 */
    private String description;

    /** 封面图片URL */
    private String coverUrl;

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

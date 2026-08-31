package com.example.library.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 图书信息 VO（包含分类名称等关联信息）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "图书信息")
public class BookVO {

    @Schema(description = "图书ID", example = "1")
    private Long id;

    @Schema(description = "ISBN编号", example = "9787111213826")
    private String isbn;

    @Schema(description = "书名", example = "Java核心技术")
    private String title;

    @Schema(description = "作者", example = "Cay S. Horstmann")
    private String author;

    @Schema(description = "出版社", example = "机械工业出版社")
    private String publisher;

    @Schema(description = "出版日期", example = "2022-01-01")
    private LocalDate publishDate;

    @Schema(description = "分类ID", example = "1")
    private Long categoryId;

    @Schema(description = "分类名称", example = "计算机科学")
    private String categoryName;

    @Schema(description = "价格", example = "119.00")
    private BigDecimal price;

    @Schema(description = "可借库存", example = "5")
    private Integer stock;

    @Schema(description = "总藏书量", example = "5")
    private Integer totalStock;

    @Schema(description = "是否可借", example = "true")
    private Boolean available;

    @Schema(description = "图书简介", example = "Java领域经典著作...")
    private String description;

    @Schema(description = "封面图片URL")
    private String coverUrl;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}

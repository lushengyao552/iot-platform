package com.example.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 新增图书 DTO
 */
@Data
@Schema(description = "新增图书请求")
public class BookAddDTO {

    @NotBlank(message = "ISBN不能为空")
    @Size(max = 20, message = "ISBN长度不能超过20个字符")
    @Schema(description = "ISBN编号", example = "9787111213826", requiredMode = Schema.RequiredMode.REQUIRED)
    private String isbn;

    @NotBlank(message = "书名不能为空")
    @Size(max = 200, message = "书名长度不能超过200个字符")
    @Schema(description = "书名", example = "Java核心技术", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotBlank(message = "作者不能为空")
    @Size(max = 100, message = "作者长度不能超过100个字符")
    @Schema(description = "作者", example = "Cay S. Horstmann", requiredMode = Schema.RequiredMode.REQUIRED)
    private String author;

    @Size(max = 100, message = "出版社长度不能超过100个字符")
    @Schema(description = "出版社", example = "机械工业出版社")
    private String publisher;

    @Schema(description = "出版日期", example = "2022-01-01")
    private LocalDate publishDate;

    @Schema(description = "分类ID", example = "1")
    private Long categoryId;

    @DecimalMin(value = "0.00", message = "价格不能为负数")
    @Digits(integer = 8, fraction = 2, message = "价格格式不正确")
    @Schema(description = "价格", example = "99.00")
    private BigDecimal price;

    @NotNull(message = "库存数量不能为空")
    @Min(value = 0, message = "库存数量不能为负数")
    @Schema(description = "库存数量", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer stock;

    @Size(max = 2000, message = "图书简介长度不能超过2000个字符")
    @Schema(description = "图书简介", example = "Java领域经典著作...")
    private String description;

    @Size(max = 500, message = "封面URL长度不能超过500个字符")
    @Schema(description = "封面图片URL", example = "https://example.com/cover.jpg")
    private String coverUrl;
}

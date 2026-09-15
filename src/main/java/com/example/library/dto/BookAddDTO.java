package com.example.library.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BookAddDTO {
    @NotBlank(message = "ISBN不能为空")
    @Size(max = 20, message = "ISBN最长20个字符")
    private String isbn;

    @NotBlank(message = "书名不能为空")
    @Size(max = 200, message = "书名最长200个字符")
    private String title;

    @NotBlank(message = "作者不能为空")
    @Size(max = 100, message = "作者最长100个字符")
    private String author;

    @Size(max = 100)
    private String publisher;

    private LocalDate publishDate;

    private Long categoryId;

    @DecimalMin(value = "0.00", message = "价格不能为负数")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal price;

    @NotNull(message = "库存数量不能为空")
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    @Size(max = 2000)
    private String description;

    @Size(max = 500)
    private String coverUrl;
}

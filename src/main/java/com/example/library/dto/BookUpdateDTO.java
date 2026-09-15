package com.example.library.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BookUpdateDTO {
    @Size(max = 20)
    private String isbn;

    @Size(max = 200)
    private String title;

    @Size(max = 100)
    private String author;

    @Size(max = 100)
    private String publisher;

    private LocalDate publishDate;

    private Long categoryId;

    @DecimalMin(value = "0.00", message = "价格不能为负数")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal price;

    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    @Size(max = 2000)
    private String description;

    @Size(max = 500)
    private String coverUrl;
}

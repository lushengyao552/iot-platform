package com.example.library.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookVO {
    private Long id;
    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private LocalDate publishDate;
    private Long categoryId;
    private String categoryName;
    private BigDecimal price;
    private Integer stock;
    private Integer totalStock;
    private String description;
    private String coverUrl;
    private Boolean available;
    private LocalDateTime createTime;
}

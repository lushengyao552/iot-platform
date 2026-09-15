package com.example.library.dto;

import lombok.Data;

@Data
public class BookQueryDTO {
    private String title;
    private String author;
    private String isbn;
    private Long categoryId;
    private Boolean onlyAvailable;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String orderBy = "create_time";
    private String orderDir = "desc";
}

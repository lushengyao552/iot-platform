package com.example.library.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryVO {
    private Long id;
    private String name;
    private String description;
    private Integer sort;
    private Long bookCount;
    private LocalDateTime createTime;
}

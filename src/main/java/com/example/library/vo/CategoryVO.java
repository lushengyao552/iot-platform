package com.example.library.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 图书分类 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "图书分类")
public class CategoryVO {

    @Schema(description = "分类ID", example = "1")
    private Long id;

    @Schema(description = "分类名称", example = "计算机科学")
    private String name;

    @Schema(description = "分类描述", example = "计算机编程、算法等技术书籍")
    private String description;

    @Schema(description = "排序号", example = "1")
    private Integer sort;

    @Schema(description = "该分类下的图书数量", example = "15")
    private Integer bookCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}

package com.example.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 图书查询条件 DTO
 *
 * <p>支持多条件组合查询 + 分页
 */
@Data
@Schema(description = "图书查询条件")
public class BookQueryDTO {

    @Schema(description = "书名（模糊查询）", example = "Java")
    private String title;

    @Schema(description = "作者（模糊查询）", example = "周志明")
    private String author;

    @Schema(description = "ISBN（精确查询）", example = "9787115428028")
    private String isbn;

    @Schema(description = "分类ID", example = "1")
    private Long categoryId;

    @Schema(description = "是否只查询有库存的图书", example = "false")
    private Boolean onlyAvailable;

    @Min(value = 1, message = "页码最小为 1")
    @Schema(description = "页码", example = "1", defaultValue = "1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 100, message = "每页条数最大为 100")
    @Schema(description = "每页条数", example = "10", defaultValue = "10")
    private Integer pageSize = 10;

    @Schema(description = "排序字段", example = "create_time", defaultValue = "create_time")
    private String orderBy = "create_time";

    @Schema(description = "排序方向：asc/desc", example = "desc", defaultValue = "desc")
    private String orderDir = "desc";
}

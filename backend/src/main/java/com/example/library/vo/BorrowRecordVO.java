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
 * 借阅记录 VO（包含用户名、书名等关联信息）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "借阅记录")
public class BorrowRecordVO {

    @Schema(description = "记录ID", example = "1")
    private Long id;

    @Schema(description = "用户ID", example = "2")
    private Long userId;

    @Schema(description = "用户名", example = "user")
    private String username;

    @Schema(description = "用户昵称", example = "普通用户")
    private String nickname;

    @Schema(description = "图书ID", example = "1")
    private Long bookId;

    @Schema(description = "书名", example = "Java核心技术")
    private String bookTitle;

    @Schema(description = "图书作者", example = "Cay S. Horstmann")
    private String bookAuthor;

    @Schema(description = "借阅日期", example = "2026-08-01")
    private LocalDate borrowDate;

    @Schema(description = "应还日期", example = "2026-08-31")
    private LocalDate dueDate;

    @Schema(description = "实际归还日期", example = "2026-08-25")
    private LocalDate returnDate;

    @Schema(description = "状态：BORROWED-借阅中，RETURNED-已归还，OVERDUE-已逾期", example = "BORROWED")
    private String status;

    @Schema(description = "逾期罚款", example = "0.00")
    private BigDecimal fine;

    @Schema(description = "是否逾期", example = "false")
    private Boolean overdue;

    @Schema(description = "剩余天数（负数表示已逾期天数）", example = "15")
    private Integer remainingDays;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}

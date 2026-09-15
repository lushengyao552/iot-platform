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
public class BorrowRecordVO {
    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private String status;
    private BigDecimal fine;
    private Boolean overdue;
    private Integer remainingDays;
    private LocalDateTime createTime;
}

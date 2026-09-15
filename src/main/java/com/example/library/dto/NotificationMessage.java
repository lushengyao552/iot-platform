package com.example.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    private String messageId;
    private String operationType;
    private Long userId;
    private String username;
    private Long bookId;
    private String bookTitle;
    private Long borrowRecordId;
    private String dueDate;
    private String content;
    private LocalDateTime sendTime;
}

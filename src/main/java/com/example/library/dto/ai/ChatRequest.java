package com.example.library.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {
    private Long sessionId;

    @NotBlank(message = "消息不能为空")
    private String message;
}
